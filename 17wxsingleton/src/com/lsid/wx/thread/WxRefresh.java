package com.lsid.wx.thread;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.HashMap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;
import com.lsid.wx.listener.WxSingleton;

public class WxRefresh extends Thread {
	private static final String[] types = { "client_credential", "jsapi", "wx_card" };

	private String appid = null;
	private String secret = null;
	private boolean running = true;
	
	public WxRefresh(String appid, String secret) {
		this.appid = appid;
		this.secret = secret;
		WxSingleton.cache.put(appid, new HashMap<String, Object>());
	}

	public void run() {
		try {
			System.out.println("===="+new Date()+"====Thread started for appid["+appid+"],secret["+DefaultCipher.enc(secret)+"]");
		} catch (Exception e1) {
			//do nothing
		}
		while (AutoConfig.isrunning&&running) {
			for (String type : types) {
				if (AutoConfig.isrunning&&running) {
					if (WxSingleton.cache.get(appid).get(type + "value") == null) {
						refresh(type);
					} else if (System.currentTimeMillis()
							- Long.parseLong(WxSingleton.cache.get(appid).get(type + "time").toString())
							+ 120000 > Long.parseLong(WxSingleton.cache.get(appid).get(type + "expire").toString())) {
						if (type.equals(types[0])) {
							for (String type2 : types) {
								if (AutoConfig.isrunning&&running) {
									refresh(type2);
								}
							}
							break;
						} else {
							refresh(type);
						}
					}
				}
			}
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
		try {
			System.out.println("===="+new Date()+"====Thread exited for appid["+appid+"],secret["+DefaultCipher.enc(secret)+"]");
		} catch (Exception e) {
			AutoConfig.log(e, "===="+new Date()+"====Thread exited for appid["+appid+"]");
		}
	}

	private void refresh(String type) {
		CloseableHttpClient request = null;
		CloseableHttpResponse response = null;
		String url = null;
		try {
			Files.createDirectories(Paths.get("wxparams"));
			String jsonfield = "ticket";
			JsonNode jn = null;
			if (types[0].equals(type)) {
				url = AutoConfig.config(null, "lsid.interface.wxtoken")+"?grant_type=" + types[0] + "&appid="
						+ appid + "&secret="
						+ secret;
				jn = new ObjectMapper().readTree(AutoConfig.outerpost(url, Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxtoken.connectimeoutinsec")), 
						Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxtoken.socketimeoutinsec"))));
				jsonfield = "access_token";
			} else {
				url = AutoConfig.config(null, "lsid.interface.wxticket")+"?type=" + type + "&access_token="
						+ WxSingleton.cache.get(appid).get(types[0] + "value");
				jn = new ObjectMapper().readTree(AutoConfig.outerpost(url, Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxticket.connectimeoutinsec")), 
						Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxticket.socketimeoutinsec"))));
			}
			if (jn.get(jsonfield) != null && jn.get("expires_in") != null) {
				WxSingleton.cache.get(appid).put(type + "value", jn.get(jsonfield).asText());
				WxSingleton.cache.get(appid).put(type + "expire", jn.get("expires_in").asLong() * 1000);
				WxSingleton.cache.get(appid).put(type + "time", System.currentTimeMillis());
				String[] wx = AutoConfig.config(null, "lsid.interface.wx").split(AutoConfig.SPLIT);
				Files.write(Paths.get("wxparams").resolve(appid+"_"+type), DefaultCipher.enc(jn.get(jsonfield).asText()+","+jn.get("expires_in").asLong()).getBytes("UTF-8"), 
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				for (String w : wx) {
					AutoConfig.innerpost(w, Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),"appid", appid, type, jn.get(jsonfield).asText());
					Thread.sleep(1000);
				}
			} else {
				throw new Exception(jn.toString());
			}
		} catch (Exception ex) {
			try {
				Files.write(Paths.get("wxparams").resolve(appid+"_"+type), ex.getMessage().getBytes("UTF-8"), 
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (Exception e) {
				//do nothing
			}
			AutoConfig.log(ex, "Got exception when refreshing [" + appid + "][" + type + "] from url ["+url+"]");
			WxSingleton.cache.remove(appid);
			running = false;
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				if (request != null) {
					request.close();
				}
			} catch (Exception ex) {
				// do nothing
			}
		}
	}

}
