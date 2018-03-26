package com.lsid.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.console.util.DataFiles;

public class LsidSocketServer implements ServletContextListener{
	
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		AutoConfig.isrunning = false;
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null, "lsid.interface.consoledata.socket").isEmpty()||
						AutoConfig.config(null, "lsid.interface.consoledata.socket").split(AutoConfig.SPLIT).length!=2){
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
		String port = AutoConfig.config(null, "lsid.interface.consoledata.socket").split(AutoConfig.SPLIT)[1];
		System.out.println("========"+new Date()+"======== started listening port ["+port+"]");
		try {
			final ServerSocket ss = new ServerSocket(Integer.parseInt(port));
			new Thread(new Runnable() {

				@Override
				public void run() {
					while (AutoConfig.isrunning) {
						try {
							final Socket s = ss.accept();
							new Thread(new Runnable() {

								@Override
								public void run() {
									BufferedReader br = null;
									try {
										br  =new BufferedReader(new InputStreamReader(s.getInputStream()));
										String line = br.readLine();
										while(line!=null) {
											String[] parts = line.split(AutoConfig.SPLIT);
											String action = parts[0];
											String eid = parts[1];
											String folder = parts[2];
											String file = parts[3];
											if (action.equals("d")) {
												String content = parts[4];
												DataFiles.hadoopdata(eid, folder, file, content);
											} else {
												String i = parts[4];
												DataFiles.hadoopidata(eid, folder, file, i);
											}
											line = br.readLine();
										}
									}catch(Exception e) {
										AutoConfig.log(e, "Error processing socket due to below exception:");
									} finally {
										if (br!=null) {
											try {
												br.close();
											} catch (IOException e) {
												AutoConfig.log(e, "Error closing br due to below exception:");
											}
										}
										if (s!=null) {
											try {
												s.close();
											} catch (IOException e) {
												AutoConfig.log(e, "Error closing socket due to below exception:");
											}
										}
									}
								}
								
							}).start();
						} catch (IOException e) {
							AutoConfig.log(e, "System exit due to below exception:");
							System.exit(1);
						}
						
					}
				}
				
			}).start();
			while (AutoConfig.isrunning) {
				Thread.sleep(30000);
			}
			ss.close();
		}catch(Exception e) {
			AutoConfig.log(e, "System exit due to below exception:");
			System.exit(1);
		}
	}

}
