package com.lsid.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class MapredScheduler implements ServletContextListener{
	
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		AutoConfig.isrunning = false;
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning){
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				AutoConfig.iamrunning();
				process();
				AutoConfig.iamdone();
			}
    		
    	}).start();
	}
	
	private static void process(){
		try {
			ScheduledExecutorService executor1 = Executors.newScheduledThreadPool(2);  
		    long oneDay = 24 * 60 * 60 * 1000;  
		    long initDelay  = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse(new SimpleDateFormat("yyyyMMdd ").format(new Date())+"04:00:00").getTime() - System.currentTimeMillis();  
		    initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;  
		  
		    executor1.scheduleAtFixedRate(  
		            new Runnable() {
	
						@Override
						public void run() {
							try {
								
								String[] eids = AutoConfig.config(null, "lsid.eids").split(AutoConfig.SPLIT);
								String[] params = new String[3+eids.length];
								Calendar c = Calendar.getInstance();
								c.add(Calendar.DATE, -1);
								String tablename = "scan" + new SimpleDateFormat("yyyyMM").format(c.getTime());
								String start = String.valueOf(new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse(new SimpleDateFormat("yyyyMMdd").format(c.getTime())+" 00:00:00").getTime());
								String end = String.valueOf(new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse(new SimpleDateFormat("yyyyMMdd").format(c.getTime())+" 23:59:59").getTime());
								params[0] = tablename;
								params[1] = start;
								params[2] = end;
								int i = 3;
								for (String eid:eids) {
									if (!AutoConfig.config(eid, "lsid.code.proda.index").isEmpty()) {
										params[i++]=eid+"_"+AutoConfig.config(eid, "lsid.code.proda.index");
									}
								}
								runmapred("lsid.mapred.StatEnter17Main",params);
								
								params[0] = "prize" + new SimpleDateFormat("yyyyMM").format(c.getTime());
								runmapred("lsid.mapred.StatEnter17Main",params);
								
							}catch(Exception x) {
								AutoConfig.log(x, "Ignored daily mapreduce due to below exception:");
							}
						}
		            	
		            },  
		            initDelay,  
		            oneDay,  
		            TimeUnit.MILLISECONDS);  
		    
		    new Thread(new Runnable() {

				@Override
				public void run() {
					while(AutoConfig.isrunning) {
						try {
							String mapredclassparams = AutoConfig.innerpost(AutoConfig.config(null, "lsid.interface.consoledata")+"mapred", 
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.consoledata.connectimeoutinsec")), 
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.consoledata.socketimeoutinsec")));
							if (!mapredclassparams.isEmpty()) {
								String[] parts = mapredclassparams.split(AutoConfig.SPLIT);
								String mapredclass = parts[0];
								String[] params = null;
								if (parts.length>1) {
									params = mapredclassparams.substring(mapredclassparams.indexOf(AutoConfig.SPLIT)+1).split(AutoConfig.SPLIT);
								}
								runmapred(mapredclass, params);
							}
						}catch(Exception e) {
							AutoConfig.log(e, "Ignored once mapreduce due to below exception:");
						}
						try {
							Thread.sleep(60000);
						} catch (InterruptedException e) {
							
						}
					}
				}
		    	
		    }).start();
		    
		    while (AutoConfig.isrunning) {
				Thread.sleep(30000);
			}
		    executor1.shutdown();
		}catch(Exception ex) {
			AutoConfig.log(ex, "System existed due to below exception:");
			System.exit(1);
		}
	}
	
	private static void runmapred(String mapredclass, String[] params) throws IOException, InterruptedException {
		StringBuilder sb = new StringBuilder("/home/hadoop/hbase/bin/hbase ").append(mapredclass);
		if (AutoConfig.config(null, "lsid.interface.consoledata.socket").isEmpty()||
				AutoConfig.config(null, "lsid.interface.consoledata.socket").split(AutoConfig.SPLIT).length!=2) {
			throw new IOException("missing or wrong lsid.interface.consoledata.socket ["+AutoConfig.config(null, "lsid.interface.consoledata.socket")+"]");
		}
		String socketip = AutoConfig.config(null, "lsid.interface.consoledata.socket").split(AutoConfig.SPLIT)[0];
		String socketport = AutoConfig.config(null, "lsid.interface.consoledata.socket").split(AutoConfig.SPLIT)[1];
		sb.append(" "+socketip+" "+socketport);
	
		if (params!=null) {
			for (String param:params) {
				if (param!=null&&!param.trim().isEmpty()) {
					sb.append(" "+param);
				}
			}
		}
		String logfile = (new StringBuilder("running-")).append(mapredclass).append((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())).append(".log").toString();
        Files.write(Paths.get(logfile), ("running "+sb.toString()+System.lineSeparator()).getBytes("UTF-8"), 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        Process p = Runtime.getRuntime().exec(sb.toString());
        p.waitFor();
        log(p.getInputStream(), logfile);
        log(p.getErrorStream(), logfile);
	}

    private static void log(InputStream is, String logfile) throws IOException {
        BufferedReader reader = null;
        try {
	        reader = new BufferedReader(new InputStreamReader(is));
	        for(String line = null; (line = reader.readLine()) != null;){
	            Files.write(Paths.get(logfile), (line+System.lineSeparator()).getBytes("UTF-8"), 
	                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	        }
	    }finally{;
	        if(reader != null)
	            reader.close();
	        reader = null;
        }
    }
    
    public static void main(String[] s) throws Exception {
    	System.out.println(DefaultCipher.dec("J5cRe6o7QTWCgUzMYq2BJSrssfKeHiHma5vaV9TySeF6YAWSTzbCM2fBHMG%2BPoZhEro%2F%2FuKDRXtH%0AAePh4S5tt76uxIUblDt86RjtBLN4ckLAYrZL46Pvae%2F%2FwWcdnP75jqaX0RuPnQSwVhsvj94QHQEj%0AY9qYJ8lFmVZN4bJwrAXXcsgaNr6SrhIbNRX5C5Pa"));
    }

}
