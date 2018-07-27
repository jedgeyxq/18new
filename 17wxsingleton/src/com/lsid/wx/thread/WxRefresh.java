package com.lsid.wx.thread;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
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
			boolean refreshed = false;
			for (String type : types) {
				if (AutoConfig.isrunning&&running) {
					if (WxSingleton.cache.get(appid).get(type + "value") == null) {
						refresh(type);
						refreshed = true;
					} else if (System.currentTimeMillis()
							- Long.parseLong(WxSingleton.cache.get(appid).get(type + "time").toString())
							+ 120000 > Long.parseLong(WxSingleton.cache.get(appid).get(type + "expire").toString())) {
						if (type.equals(types[0])) {
							for (String type2 : types) {
								if (AutoConfig.isrunning&&running) {
									refresh(type2);
									refreshed = true;
								}
							}
							break;
						} else {
							refresh(type);
							refreshed = true;
						}
					}
				}
			}
			if (refreshed) {
				notifyouterparties();
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
	
	private void notifyouterparties() {
		if (AutoConfig.config(null, "lsid.interface.sharewx."+appid)!=null&&!AutoConfig.config(null, "lsid.interface.sharewx."+appid).trim().isEmpty()) {
			String[] sharewx = AutoConfig.config(null, "lsid.interface.sharewx."+appid).split(AutoConfig.SPLIT);
			try {
				for (String w : sharewx) {
					String outernotify = w+"?AccessToken="+encrypt(WxSingleton.cache.get(appid).get(types[0] + "value").toString())+
							"&JsApiTicket="+encrypt(WxSingleton.cache.get(appid).get(types[1] + "value").toString())+
							"&WxCardTicket="+encrypt(WxSingleton.cache.get(appid).get(types[2] + "value").toString());
					System.out.println(new Date()+" ==== notifying ["+outernotify+"]");
					String result = AutoConfig.outerpost(outernotify, 
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.sharewx.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.sharewx.socketimeoutinsec")));
					if (!result.contains("success")) {
						throw new Exception("notifyerror["+result+"]");
					}
				}
			}catch(Exception e) {
				try {
					Thread.sleep(10000);
					for (String w : sharewx) {
						String outernotify = w+"?AccessToken="+encrypt(WxSingleton.cache.get(appid).get(types[0] + "value").toString())+
								"&JsApiTicket="+encrypt(WxSingleton.cache.get(appid).get(types[1] + "value").toString())+
								"&WxCardTicket="+encrypt(WxSingleton.cache.get(appid).get(types[2] + "value").toString());
						System.out.println(new Date()+" ==== renotifying ["+outernotify+"]");
						String result = AutoConfig.outerpost(outernotify, 
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.sharewx.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.sharewx.socketimeoutinsec")));
						if (!result.contains("success")) {
							throw new Exception("renotifyerror["+result+"]");
						}
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					try {
						Request.Post(AutoConfig.config(null, "lsid.interface.notify")).connectTimeout(30*1000).socketTimeout(30*1000).bodyForm(
								Form.form().add("eid", "default").add("msgtype", "down").add("content", "wxtokenerror="+e1.getMessage()).build(),Charset.forName("UTF-8")).execute().returnContent().asString(Charset.forName("UTF-8"));
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					
				}		
			}
		}
	}
	
	private String encrypt(String text) throws Exception {
		    Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");  
            int blockSize = cipher.getBlockSize();  
            byte[] dataBytes = text.getBytes();  
            int plaintextLength = dataBytes.length;  
              
            if (plaintextLength % blockSize != 0) {  
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));  
            }  
  
            byte[] plaintext = new byte[plaintextLength];  
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);  
               
            SecretKeySpec keyspec = new SecretKeySpec("1234567890123456".getBytes(), "AES");  
            IvParameterSpec ivspec = new IvParameterSpec("6543210987654321".getBytes());  
  
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);  
            byte[] encrypted = cipher.doFinal(plaintext);  
  
            return URLEncoder.encode(new org.apache.tomcat.util.codec.binary.Base64().encodeAsString(encrypted),"UTF-8");  
            
	}

	private void refresh(String type) {
		CloseableHttpClient request = null;
		CloseableHttpResponse response = null;
		String url = null;
		try {
			Files.createDirectories(Paths.get("wxparams"));
			String jsonfield = "ticket";
			JsonNode jn = null;
			String wxreturn = null;
			if (types[0].equals(type)) {
				url = AutoConfig.config(null, "lsid.interface.wxtoken")+"?grant_type=" + types[0] + "&appid="
						+ appid + "&secret="
						+ secret;
				
				wxreturn = AutoConfig.outerpost(url, Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxtoken.connectimeoutinsec")), 
						Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxtoken.socketimeoutinsec")));
				jn = new ObjectMapper().readTree(wxreturn);
				jsonfield = "access_token";
			} else {
				url = AutoConfig.config(null, "lsid.interface.wxticket")+"?type=" + type + "&access_token="
						+ WxSingleton.cache.get(appid).get(types[0] + "value");
				wxreturn = AutoConfig.outerpost(url, Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxticket.connectimeoutinsec")), 
						Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxticket.socketimeoutinsec")));
				jn = new ObjectMapper().readTree(wxreturn);
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
	
	public static void main(String[] s) throws Exception {
		//wx78a08076c77ebc52
		//System.out.println(DefaultCipher.enc("5541cbf3e59325982651f2b98b920986"));
		try {  
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");  
            int blockSize = cipher.getBlockSize();  
            byte[] dataBytes = "testwxcardticket".getBytes();  
            int plaintextLength = dataBytes.length;  
              
            if (plaintextLength % blockSize != 0) {  
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));  
            }  
  
            byte[] plaintext = new byte[plaintextLength];  
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);  
               
            SecretKeySpec keyspec = new SecretKeySpec("1234567890123456".getBytes(), "AES");  
            IvParameterSpec ivspec = new IvParameterSpec("6543210987654321".getBytes());  
  
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);  
            byte[] encrypted = cipher.doFinal(plaintext);  
  
            System.out.println("["+URLEncoder.encode(new org.apache.tomcat.util.codec.binary.Base64().encodeAsString(encrypted),"UTF-8")+"]");  
            
        } catch (Exception e) {  
            e.printStackTrace();  
            
        }  
		
		try  
        {  
            byte[] encrypted1 = new org.apache.tomcat.util.codec.binary.Base64().decode("GRegMgYO7iLGs8htnxgcKA==");  
               
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");  
            SecretKeySpec keyspec = new SecretKeySpec("1234567890123456".getBytes(), "AES");  
            IvParameterSpec ivspec = new IvParameterSpec("6543210987654321".getBytes());  
               
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);  
  
            byte[] original = cipher.doFinal(encrypted1);  
            System.out.println(new String(original));  
              
        }  
        catch (Exception e) {  
            e.printStackTrace();  
        }  
	}

}
