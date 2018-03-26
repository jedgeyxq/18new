package com.lsid.sync.listener;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.lsid.autoconfig.client.AutoConfig;

public class RemoveLock implements ServletContextListener{
	
	public static final Path synclock = Paths.get("synclock");
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		
		try {
			Files.createDirectories(synclock);
			
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
							Files.walkFileTree(synclock, new FileVisitor<Object>(){
	
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
												String timestr = new String(Files.readAllBytes(Paths.get(file.toString())),"UTF-8");
												Long time = Long.parseLong(timestr);
												if (System.currentTimeMillis()-time>=5*60*1000){
													Files.deleteIfExists(Paths.get(file.toString()));
												}
											}catch(Exception ex){
												AutoConfig.log(ex, "Deleted ["+file.toString()+"] due to below exception:");
												Files.deleteIfExists(Paths.get(file.toString()));
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
			System.out.println("===="+new Date()+"====started watching "+synclock);
		} catch (IOException e1) {
			AutoConfig.log(e1, "System exited due to below exception:");
			System.exit(1);
		}
	}
}
