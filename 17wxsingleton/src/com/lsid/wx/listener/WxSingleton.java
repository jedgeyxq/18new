package com.lsid.wx.listener;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.wx.thread.WxRefresh;

public class WxSingleton implements ServletContextListener{
	public static Map<String, Map<String, Object>> cache = new HashMap<String, Map<String, Object>>();
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null, "lsid.eids").isEmpty()){
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						//do nothing
					}
				}
				while(AutoConfig.isrunning){
					String[] eids=AutoConfig.config(null, "lsid.eids").split(AutoConfig.SPLIT);
					for (String eid:eids){
						if (!AutoConfig.config(eid, "lsid.playwx.appid").isEmpty()&&cache.get(AutoConfig.config(eid, "lsid.playwx.appid"))==null){
							new WxRefresh(AutoConfig.config(eid, "lsid.playwx.appid"), AutoConfig.config(eid, "lsid.playwx.secret")).start();
						}
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							//do nothing
						}
					}
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						//do nothing
					}
				}
			}
			
		}).start();
		
	}

}
