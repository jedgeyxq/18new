package com.lsid.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.lsid.autoconfig.client.AutoConfig;

public class Distribute implements ServletContextListener{
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null,"lsidcountercachefolder").isEmpty()){
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
		System.out.println("========"+new Date()+"======== started distributing");
		while(AutoConfig.isrunning){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			Path thefolder = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sortedone");
			try {
				Files.walkFileTree(thefolder, new FileVisitor<Object>(){

					@Override
					public FileVisitResult postVisitDirectory(Object dir, IOException exc) throws IOException {
						if (AutoConfig.isrunning){
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.TERMINATE;
						}
					}

					@Override
					public FileVisitResult preVisitDirectory(Object dir, BasicFileAttributes attrs) throws IOException {
						if (AutoConfig.isrunning){
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.TERMINATE;
						}
					}

					@Override
					public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) throws IOException {
						if (AutoConfig.isrunning){
							if (!Files.isDirectory(Paths.get(file.toString()))&&Paths.get(file.toString()).getFileName().toString().equals("merged")
									&&Files.exists(Paths.get(file.toString()).getParent().resolve("todistribute"))){
								try {
									Thread.sleep(10);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								BufferedReader br = null;
								try{
									String namespace = Paths.get(file.toString()).getParent().getParent().getParent().getFileName().toString();
									String tablename = Paths.get(file.toString()).getParent().getParent().getFileName().toString();
									String column = Paths.get(file.toString()).getParent().getFileName().toString();
									br = Files.newBufferedReader(Paths.get(file.toString()), Charset.forName("UTF-8"));
									String line = br.readLine();
									while(line!=null){
										String[] parts = line.split(AutoConfig.SPLIT);
										String hash = parts[0];
										String row = parts[1];
										String counter = parts[2];
										long position = 0;
										String[] urls = AutoConfig.config(namespace, "lsid.interface.cache.increment.read").split(AutoConfig.SPLIT);
										for (int i = 0; i< urls.length;i++){
											String returnvalue = null;
											try{
												returnvalue = AutoConfig.innerpost(urls[i], 
														Integer.parseInt(AutoConfig.config(namespace, "lsid.interface.cache.increment.connectimeoutinsec")),
														Integer.parseInt(AutoConfig.config(namespace, "lsid.interface.cache.increment.socketimeoutinsec")),
														"eid", namespace, "table", tablename, "col", column, "greater", counter);
												position += Long.parseLong(returnvalue);
											}catch(Exception ex){
												AutoConfig.log(ex, "error when requesting "+urls[i]+" responsing ["+returnvalue+"]");
											}
										}
										Path positionfile = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sortedall").resolve(namespace).resolve(tablename).resolve(column).resolve(hash).resolve(row);
										Files.deleteIfExists(positionfile);
										if (!Files.exists(positionfile.getParent())){
											Files.createDirectories(positionfile.getParent());
										}
										Files.write(positionfile, String.valueOf(position+1).getBytes("UTF-8"), StandardOpenOption.CREATE);
										line = br.readLine();
									}
									String[] urls = AutoConfig.config(namespace, "lsid.interface.cache.increment.read").split(AutoConfig.SPLIT);
									long total = 0;
									for (int i = 0; i< urls.length;i++){
										String returnvalue = null;
										try{
											returnvalue = AutoConfig.innerpost(urls[i], 
													Integer.parseInt(AutoConfig.config(namespace, "lsid.interface.cache.increment.connectimeoutinsec")),
													Integer.parseInt(AutoConfig.config(namespace, "lsid.interface.cache.increment.socketimeoutinsec")),
													 "eid", namespace, "table", tablename, "col", column, "total", "");
											total += Long.parseLong(returnvalue);
										}catch(Exception ex){
											AutoConfig.log(ex, "error when requesting "+urls[i]+" responsing ["+returnvalue+"]");
										}
									}
									Path totalfile = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sortedtotal").resolve(namespace).resolve(tablename).resolve(column).resolve("total");
									Files.deleteIfExists(totalfile);
									if (!Files.exists(totalfile.getParent())){
										Files.createDirectories(totalfile.getParent());
									}
									Files.write(totalfile, String.valueOf(total).getBytes("UTF-8"), StandardOpenOption.CREATE);
								}catch(Exception e){
									AutoConfig.log(e,"error processing "+file);
								} finally {
									Files.deleteIfExists(Paths.get(file.toString()).getParent().resolve("todistribute"));
									if (br!=null){
										br.close();
										br = null;
									}
								}
							}
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.TERMINATE;
						}
						
					}

					@Override
					public FileVisitResult visitFileFailed(Object file, IOException exc) throws IOException {
						if (AutoConfig.isrunning){
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.TERMINATE;
						}
					}  
					  
				});
			} catch (Exception e) {
				AutoConfig.log(e, "System exited due to below exception:");
				System.exit(1);
			}
		}
	}
	
}
