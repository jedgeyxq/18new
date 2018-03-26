package com.lsid.ticket.listener;

import java.io.IOException;
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

public class TicketExpire implements ServletContextListener{
	
	public static final String typerefreshable = "refreshable";
	public static final String typeonetime = "onetime";
	public static final String typefixedexpire = "fixedexpire";
	
	private static final Path cache = Paths.get("ticketscache");
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		try {
			Files.createDirectories(cache);
			
			new Thread(new Runnable(){
	
				@Override
				public void run() {
					while(!AutoConfig.isrunning){
						try {
							Thread.sleep(10000);
						} catch (Exception ex) {
							//do nothing
						}
					}
					while(AutoConfig.isrunning){
						try {
							Thread.sleep(10000);
						} catch (Exception ex) {
							//do nothing
						}

						try {
							Files.walkFileTree(cache, new FileVisitor<Object>(){
	
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
										if (Files.isDirectory(Paths.get(file.toString()))){
											return FileVisitResult.CONTINUE;
										} else {
											try{
												String ticketvalue = new String(Files.readAllBytes(Paths.get(file.toString())),"UTF-8");
												Long time = Long.parseLong(ticketvalue.substring(0,13));
												if (System.currentTimeMillis()-time>=Long.parseLong(AutoConfig.config(null, "lsid.expire.ticket"))){
													Files.deleteIfExists(Paths.get(file.toString()));
												}
											}catch(Exception ex){
												Files.deleteIfExists(Paths.get(file.toString()));
												AutoConfig.log(ex, "deleted ["+file.toString()+"] due to beow exception:");
											}
											return FileVisitResult.CONTINUE;
										}
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
						} catch (IOException ex) {
							AutoConfig.log(ex, "System exited due to below exception:");
							System.exit(1);
						}
					}
				}
			}).start();
			System.out.println("===="+new Date()+"====started watching "+cache);
		} catch (IOException e1) {
			AutoConfig.log(e1, "System exited due to below exception:");
			System.exit(1);
		}
	}
	
	public static void cache(String ticket, String value, String type, boolean refresh) throws Exception{
		if (ticket==null||ticket.trim().isEmpty()||value==null||value.trim().isEmpty()||
				!typerefreshable.equals(type)&&!typeonetime.equals(type)&&!typefixedexpire.equals(type)){
			throw new Exception("fail to cache ticket");
		}
		if (refresh){
			Files.write(cache.resolve(ticket), (System.currentTimeMillis()+type+AutoConfig.SPLIT+value).getBytes("UTF-8"), StandardOpenOption.CREATE);
		} else {
			Files.write(cache.resolve(ticket), (System.currentTimeMillis()+type+AutoConfig.SPLIT+value).getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
		}
	}
	
	public static synchronized String read(String ticket) throws Exception{
		if (!Files.exists(cache.resolve(ticket))){
			throw new Exception("invalidticket");
		}
		String ticketvalue = new String(Files.readAllBytes(cache.resolve(ticket)),"UTF-8");
		String type=ticketvalue.substring(13,ticketvalue.indexOf(AutoConfig.SPLIT));
		Long time = Long.parseLong(ticketvalue.substring(0,13));
		if (System.currentTimeMillis()-time>=Long.parseLong(AutoConfig.config(null, "lsid.expire.ticket"))){
			Files.deleteIfExists(cache.resolve(ticket));
			return null;
		} else {
			if (typerefreshable.equals(type)){
				cache(ticket, ticketvalue.substring(ticketvalue.indexOf(AutoConfig.SPLIT)+1), type, true);
			} else if (typeonetime.equals(type)){
				Files.deleteIfExists(cache.resolve(ticket));
			}
		}	
		return ticketvalue.substring(ticketvalue.indexOf(AutoConfig.SPLIT)+1);
	}
	
}
