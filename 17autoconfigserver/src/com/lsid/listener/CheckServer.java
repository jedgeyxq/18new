package com.lsid.listener;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;

import com.lsid.filter.autoconfig.server.GetFilter;


public class CheckServer implements ServletContextListener{
	private long times = 0;
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				process();
			}
    		
    	}).start();
	}
	
	private void process(){
		Path thefolder = Paths.get("sailorlive");
		System.out.println("===="+new Date()+"====Started watching ["+thefolder+"]");
		while(true) {
			try {
				Thread.sleep(60000);
				if (times==12*60) {
					times=0;
				}
				if (times==0) {
					Request.Post(GetFilter.get("default", "lsid.interface.notify")).connectTimeout(30*1000).socketTimeout(30*1000).bodyForm(
							Form.form().add("eid", "default").add("msgtype", "normal").add("content", "ok").build(),Charset.forName("UTF-8")).execute().returnContent().asString(Charset.forName("UTF-8"));
				}
				times++;
				
				String[] livefiles = thefolder.toFile().list();
				if (livefiles!=null) {
					for (String livefile:livefiles) {
						//文件更新时间超过1分钟，就有可能服务器挂了
						if (System.currentTimeMillis()-Files.getLastModifiedTime(thefolder.resolve(livefile)).toMillis()>60000) {
							String theserver = "";
							InputStream is = null;
							try{
								Properties p = new Properties();
								is = Files.newInputStream(Paths.get("sailors/hostandescription.properties"));
								p.load(is);
								for (Object key:p.keySet()) {
									if (key.toString().contains(livefile.replace("-", ":"))) {
										theserver=key.toString();
										break;
									}
								}
								
							} finally {
								if (is!=null){
									is.close();
									is = null;
								}
							}
							Request.Post(GetFilter.get("default", "lsid.interface.notify")).connectTimeout(30*1000).socketTimeout(30*1000).bodyForm(
									Form.form().add("eid", "default").add("msgtype", "down").add("content", theserver).build(),Charset.forName("UTF-8")).execute().returnContent().asString(Charset.forName("UTF-8"));
							Files.deleteIfExists(thefolder.resolve(livefile));
						}
					}
				}
			}catch(Exception e) {
				System.out.println("===="+new Date()+"====System exited due to below exception:");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

}
