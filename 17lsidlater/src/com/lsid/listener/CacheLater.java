package com.lsid.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.lsid.autoconfig.client.AutoConfig;

public class CacheLater implements ServletContextListener{
	
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null,"cachelaterfolder").isEmpty()){
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
	
	private void process(){
		Path thefolder = Paths.get(AutoConfig.config(null,"cachelaterfolder")).resolve("writehbaselater");
		final Path therrorfolder = Paths.get(AutoConfig.config(null,"cachelaterfolder")).resolve("writehbaselaterror");
		System.out.println("========"+new Date()+"======== started watching "+thefolder);
		while(AutoConfig.isrunning){
			String[] namespaces = thefolder.toFile().list();
			if (namespaces==null) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					//do nothing
				}
			} else {
				for (String namespace:namespaces){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						//do nothing
					}
					BufferedReader br = null;
					try {
						Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh","-c", "ls "+thefolder.resolve(namespace).toString()+" -rt | head -n 10000"});
						br = new BufferedReader(new InputStreamReader(p.getInputStream()));
						String line = br.readLine();
						while (line!=null){
							Path file = thefolder.resolve(namespace).resolve(line);
							if (AutoConfig.isrunning){
								if (!Files.isDirectory(Paths.get(file.toString()))){
									if (Files.readAllBytes(Paths.get(file.toString())).length==0&&System.currentTimeMillis()-Files.getLastModifiedTime(Paths.get(file.toString())).toMillis()>24*60*60*1000) {
										Files.delete(Paths.get(file.toString()));
									} else {
										String filename = Paths.get(file.toString()).getFileName().toString();
										try{
											long size = Long.valueOf(filename.substring(filename.lastIndexOf(AutoConfig.SPLIT)+1));
											String tablename=filename.substring(0,filename.indexOf(AutoConfig.SPLIT));
											if (Files.readAllBytes(Paths.get(file.toString())).length==size){
												Thread.sleep(10);
												String contentstr = Files.readAllLines(Paths.get(file.toString()), Charset.forName("UTF-8")).get(0);
												String[] content = contentstr.split(AutoConfig.SPLIT);
												String action = content[0];
												String hash = content[1];
												String row = content[2];
												String column = content[3];
												if ("put".equals(action)){
													String value = contentstr.substring((action+AutoConfig.SPLIT+hash+AutoConfig.SPLIT+row+AutoConfig.SPLIT+column+AutoConfig.SPLIT).length());
													String returnvalue = null;
													try{
														returnvalue = AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.hbase.write", hash), 
																Integer.parseInt(AutoConfig.config(null, "lsid.interface.hbase.connectimeoutinsec")),
																Integer.parseInt(AutoConfig.config(null, "lsid.interface.hbase.socketimeoutinsec")),
																"eid", namespace, "table", tablename, "hash", hash, "row", row, "col", column, "value", value);
													}catch(Exception e){
														//do nothing
													}
													if ("ok".equals(returnvalue)){
														Files.delete(Paths.get(file.toString()));
													}
												} else if ("increment".equals(action)){
													long amount = 0;
													if (content.length>4){
														amount = Long.parseLong(content[4]);
													}
													String returnvalue = null;
													try{
														boolean countotal = false;
														try{
															new SimpleDateFormat("yyyyMMdd").parse(row.substring(row.lastIndexOf(AutoConfig.SPLIT_HBASE)+1)); 
															countotal = true;
														}catch(Exception ex){
															//do nothing
														}
														if (countotal){
															AutoConfig.incrementcache(namespace, "total", tablename, "total", column, 1);
														}
														returnvalue = AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.hbase.write", hash), 
																Integer.parseInt(AutoConfig.config(null, "lsid.interface.hbase.connectimeoutinsec")),
																Integer.parseInt(AutoConfig.config(null, "lsid.interface.hbase.socketimeoutinsec")),
																"eid", namespace, "table", tablename, "hash", hash, "row", row, "col", column, "amount", String.valueOf(amount));
														}catch(Exception e){
														//do nothing
													}
													if ("ok".equals(returnvalue)){
														Files.delete(Paths.get(file.toString()));
													}
												} else {
													throw new Exception("unsupported action ["+action+"]");
												}
											}
										}catch(Exception e){
											e.printStackTrace();
											AutoConfig.log(e,"error processing "+file);
											Files.createDirectories(therrorfolder.resolve(namespace));
											Files.move(Paths.get(file.toString()), therrorfolder.resolve(namespace).resolve(filename), StandardCopyOption.REPLACE_EXISTING);
										}
									}
								}
							} else {
								break;
							}
							line = br.readLine();
						}
					} catch (Exception e) {
						AutoConfig.log(e, "System exited due to below exception:");
						System.exit(1);
					}finally{
						if (br!=null){
							try {
								br.close();
							} catch (IOException e) {
								AutoConfig.log(e, "error closing command: ls "+thefolder.resolve(namespace).toString()+" -rt | head -n 10000");
							}
						}
						br = null;
					}
				}
			}
		}
	}

}
