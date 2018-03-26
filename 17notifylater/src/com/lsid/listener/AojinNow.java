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
import java.util.Date;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class AojinNow implements ServletContextListener{
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null,"outerlaterfolder").isEmpty()){
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
		Path thefolder = Paths.get(AutoConfig.config(null,"outerlaterfolder")).resolve("later");
		final Path therrorfolder = Paths.get(AutoConfig.config(null,"outerlaterfolder")).resolve("laterror");
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
					if (!"a".equals(namespace)) {
						continue;
					}
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
											if (Files.readAllBytes(Paths.get(file.toString())).length==size){
												Thread.sleep(10);
												String contentstr = Files.readAllLines(Paths.get(file.toString()), Charset.forName("UTF-8")).get(0);
												String[] content = contentstr.split(AutoConfig.SPLIT);
												if (AutoConfig.finished(contentstr)) {
													prizetoaojin(AutoConfig.getenc(content), DefaultCipher.dec(AutoConfig.getplayid(content)), AutoConfig.config(namespace, "lsid.pool"+AutoConfig.getpoolid(content)+".prize"+AutoConfig.getprizeid(content)+".desc"));
													Files.delete(Paths.get(file.toString()));
												} else {
													authtoaojin(AutoConfig.getinip(content), AutoConfig.getenc(content), URLDecoder.decode(content[6],"UTF-8"), AutoConfig.getencuserinfo(content));
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

	private static void prizetoaojin(String enc, String openid, String prizeid) throws Exception{
	    	String aojinreturn = null;
	    	String aojinenc = null;
	    	String code = null;
    		code = DefaultCipher.dec(enc);
    		
    		aojinenc = DefaultCipher.enc("038_9h&*fq%Eof06",code);
    		String aojinhost="http://api.aojinzhice.xin/webService/rest/lieshi/postPrizeInfo";
	    	aojinreturn = Request.Post(aojinhost).socketTimeout(10000).connectTimeout(10000).addHeader("token", "aojinzhice&lieshi20170307").bodyForm(
	    			Form.form().add("channelName", "180101").add("code", aojinenc).add("openId", openid).add("prizeId", prizeid).build(),Charset.forName("UTF-8")).execute().returnContent().asString(Charset.forName("UTF-8"));
	    	JsonNode jn = new ObjectMapper().readTree(aojinreturn);
	    	if (!"1".equals(jn.get("result").asText())||1!=jn.get("resultcode").asInt()){
	    		throw new Exception(jn.toString());
	    	}
    	}
		
	private static void authtoaojin(String ip, String enc, String useragent, String encwxuser) throws Exception{
	    	String aojinreturn = null;
	    	String aojinenc = null;
	    	String code = null;
	    	String wxuser = null;
   		code = DefaultCipher.dec(enc);
    		wxuser = DefaultCipher.dec(encwxuser);
    		
    		aojinenc = DefaultCipher.enc("038_9h&*fq%Eof06",code);
	    	String aojinhost="http://api.aojinzhice.com/webService/rest/lieshi/postInfo";
	    	aojinreturn = Request.Post(aojinhost).socketTimeout(10000).connectTimeout(10000).addHeader("token", "aojinzhice&lieshi20170307").bodyForm(
	    			Form.form().add("channelName", "180101").add("code", aojinenc).add("phoneAgent", useragent).add("ipAddress", ip).add("userInfo", wxuser).build(),Charset.forName("UTF-8")).execute().returnContent().asString(Charset.forName("UTF-8"));
	    	JsonNode jn = new ObjectMapper().readTree(aojinreturn);
	    	if (!"1".equals(jn.get("result").asText())||1!=jn.get("resultcode").asInt()){
	    		throw new Exception(jn.toString());
	    	}
    }
}
