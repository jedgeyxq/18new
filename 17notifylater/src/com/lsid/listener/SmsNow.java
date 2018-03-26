package com.lsid.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.Date;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;

public class SmsNow implements ServletContextListener{
	
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null,"smslaterfolder").isEmpty()){
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
		Path thefolder = Paths.get(AutoConfig.config(null,"smslaterfolder")).resolve("later");
		final Path therrorfolder = Paths.get(AutoConfig.config(null,"smslaterfolder")).resolve("laterror");
		try {
			Files.createDirectories(thefolder);
			System.out.println("========"+new Date()+"======== started watching "+thefolder);
		} catch (IOException e2) {
			AutoConfig.log(e2, "System exited due to below exception:");
			System.exit(1);
			return;
		}
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
											String msgtype=filename.substring(0,filename.indexOf(AutoConfig.SPLIT));
											if (Files.readAllBytes(Paths.get(file.toString())).length==size&&
													!AutoConfig.config(null, "sms.host").isEmpty()&&
													!AutoConfig.config(null, "sms.apikey").isEmpty()&&
													!AutoConfig.config(namespace, "sms.to."+msgtype).isEmpty()&&
													!AutoConfig.config(namespace, "sms.template."+msgtype).isEmpty()){
												Thread.sleep(10);
												String contentstr = Files.readAllLines(Paths.get(file.toString()), Charset.forName("UTF-8")).get(0);
												String[] content = contentstr.replaceAll("http://", "").replaceAll(":","-").replaceAll("\\.", "-").replaceAll("/", "-").split(AutoConfig.SPLIT);
												String sendcontent = MessageFormat.format(
														URLDecoder.decode(AutoConfig.config(namespace, "sms.template."+msgtype),"UTF-8"),
														(Object[])content);
												boolean onesuccess = false;
													for (String smsto:AutoConfig.config(namespace, "sms.to."+msgtype).split(AutoConfig.SPLIT)) {
														try {
															if (smsto!=null&&smsto.trim().length()==11) {
																Long.parseLong(smsto.trim());
																String result = AutoConfig.outerpost(AutoConfig.config(null, "sms.host"), Integer.parseInt(AutoConfig.config(null, "sms.host.connectimeoutinsec")),
																		Integer.parseInt(AutoConfig.config(null, "sms.host.socketimeoutinsec")),"apikey", AutoConfig.config(null, "sms.apikey"),
															    		"text", sendcontent,
															    		"mobile", smsto.trim());
																JsonNode jn = new ObjectMapper().readTree(result);
																if (jn.get("code")!=null&&jn.get("code").asInt()==0) {
																	onesuccess = true;
																}
															}
														}catch(Exception ex) {
															AutoConfig.log(ex, "error processing "+file);
														}
													}
												if (onesuccess) {
													Files.delete(Paths.get(file.toString()));
												}
											}
												
										}catch(Exception e){
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

