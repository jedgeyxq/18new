package com.lsid.console.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.console.util.AesUtil;
import com.lsid.console.util.DataFiles;
import com.sun.corba.se.spi.activation.Repository;

import sun.misc.BASE64Decoder;

public class SecureFilter implements Filter {
	private static Map<String, Map<String, String>> session = new HashMap<String, Map<String, String>>();
	private final long sessionexpire = 120*60*1000;
	
	public void destroy() {
		
    }
    
	public static void refreshpassword(String token, String newpassword){
		session.get(token).put("password", newpassword);
	}

	public static void refreshself(String token, String name, String self){
		session.get(token).put("name", name);
		session.get(token).put("self", self);
	}
	
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {
    	HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	response.setHeader("Access-Control-Allow-Origin", "*");
    	response.setHeader("XDomainRequestAllowed","1");
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
    	try {
    		if (request.getRequestURI().endsWith("/l0gin")){
    			if (request.getSession().getAttribute("time")!=null&&System.currentTimeMillis()-Long.parseLong(request.getSession().getAttribute("time").toString())<1000){
    				throw new Exception("toofrequent");
    			}
    			request.getSession().setAttribute("time", System.currentTimeMillis());
    			String pswdstr = new String(decryptByPrivateKey(new BASE64Decoder().decodeBuffer(URLDecoder.decode(request.getParameter("passw0rd"),"UTF-8"))));
    			String key = pswdstr.substring(0,pswdstr.indexOf(AutoConfig.SPLIT));
    			String pswd = pswdstr.substring(pswdstr.indexOf(AutoConfig.SPLIT)+1);
    			String self = DataFiles.login(URLEncoder.encode(request.getParameter("name"),"UTF-8"), pswd);
    			System.out.println("self===="+self);
    			if (self==null||self.trim().isEmpty()){
    				Map<String, Object> result = new HashMap<String, Object>();
    				result.put("result", "fail");
    				result.put("reason", "account");
    				result.put("value", "");
    				out(response, result);
    			} else {
	    			Map<String, String> data0 = new HashMap<String, String>();
	    			data0.put("time", String.valueOf(System.currentTimeMillis()));
	    			data0.put("eid", self.substring(self.lastIndexOf(AutoConfig.SPLIT)+1));
	    			data0.put("key", key);
	    			data0.put("self", self);
	    			data0.put("name", request.getParameter("name"));
	    			data0.put("password", pswd);
			        
	    			String newtoken0 = UUID.randomUUID().toString().replaceAll("-", "");
			        session.put(newtoken0, data0);
					
			        newtoken0 = aesenc(newtoken0, data0.get("key"));
			        
			        Map<String, String> data1 = new HashMap<String, String>();
	    			data1.put("time", String.valueOf(System.currentTimeMillis()));
	    			data1.put("eid", self.substring(self.lastIndexOf(AutoConfig.SPLIT)+1));
	    			data1.put("key", key);
	    			data1.put("self", self);
	    			data1.put("name", request.getParameter("name"));
	    			data1.put("password", pswd);
			        
			        String newtoken1 = UUID.randomUUID().toString().replaceAll("-", "");
			        session.put(newtoken1, data1);
					
			        newtoken1 = aesenc(newtoken1, data1.get("key"));
			        
			        Map<String, String> data2 = new HashMap<String, String>();
	    			data2.put("time", String.valueOf(System.currentTimeMillis()));
	    			data2.put("eid", self.substring(self.lastIndexOf(AutoConfig.SPLIT)+1));
	    			data2.put("key", key);
	    			data2.put("self", self);
	    			data2.put("name", request.getParameter("name"));
	    			data2.put("password", pswd);
			        
			        String newtoken2 = UUID.randomUUID().toString().replaceAll("-", "");
			        session.put(newtoken2, data2);
					
			        newtoken2 = aesenc(newtoken2, data2.get("key"));
			        
    				Map<String, Object> result = new HashMap<String, Object>();
    				result.put("result", "success");
    				result.put("reason", "");
    				result.put("eid", self.substring(self.lastIndexOf(AutoConfig.SPLIT)+1));
    				result.put("t0ken0", newtoken0);
    				result.put("t0ken1", newtoken1);
    				result.put("t0ken2", newtoken2);
    				
    				List<Map<String, String>> actidnames = new ArrayList<Map<String, String>>();
    				try {
	    				String[] actindex = AutoConfig.config(result.get("eid").toString(), "lsid.activity").split(AutoConfig.SPLIT);
	    				if (actindex!=null) {
	    					for (String index:actindex) {
	    						if (index!=null&&!index.trim().isEmpty()&&!AutoConfig.config(result.get("eid").toString(), "lsid.activity"+index+".name").isEmpty()) {
	    							Map<String, String> actidname = new HashMap<String, String>();
	    							actidname.put("id", index);
	    							actidname.put("name", AutoConfig.config(result.get("eid").toString(), "lsid.activity"+index+".name"));
	    							actidnames.add(actidname);
	    						}
	    					}
	    				}
    				}catch(Exception e) {
    					//do nothing
    				}
    				
    				result.put("activities", actidnames);
    				
    				List<Map<String, String>> prodidnames = new ArrayList<Map<String, String>>();
    				try {
	    				String[] prodindex = AutoConfig.config(result.get("eid").toString(), "lsid.code.valid.a").split(AutoConfig.SPLIT)[Integer.parseInt(AutoConfig.config(result.get("eid").toString(), "lsid.code.proda.index"))].split("_");
	    				if (prodindex!=null) {
	    					for (String index:prodindex) {
	    						if (index!=null&&!index.trim().isEmpty()&&!AutoConfig.config(result.get("eid").toString(), "lsid.prod.desc."+index).isEmpty()) {
	    							Map<String, String> prodidname = new HashMap<String, String>();
	    		    				prodidname.put("id", index);
	    		    				prodidname.put("name", AutoConfig.config(result.get("eid").toString(), "lsid.prod.desc."+index));
	    		    				prodidnames.add(prodidname);
	    						}
	    					}
	    				}
    				}catch(Exception e) {
    					//do nothing
    				}
    				result.put("products", prodidnames);
    				
    				result.put("self", self);
    				
    				out(response, result);
    			}
    		} else if(request.getRequestURI().equals("/17console/website/info")){ 
    			chain.doFilter(request, response);
    		}else{
    			String token = request.getParameter("t0ken");
    			if (islocal(request.getServerName())){
    				chain.doFilter(request, response);
    			} else {
	    			Map<String, String> data = session.remove(token);
	    			boolean isexpire = false;
	    			if (data==null||data.isEmpty()){
	    				isexpire = true;
	    			} else {
	    				if (System.currentTimeMillis() - Long.parseLong(data.get("time"))>sessionexpire){
	    					data.clear();
	    					isexpire = true;
	    				} else {
	    					if (System.currentTimeMillis()-Long.parseLong(data.get("time"))<300){
	    	    				session.put(token, data);
	    	    				throw new Exception("toofrequent");
	    	    			}
	    					String uri = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/")+1);
	    					if ("_active_limit_td_tdu_sul_suu_suc_".contains(uri)&&!data.get("self").contains(uri)){
	    						session.put(token, data);
	    	    				throw new Exception("noauth");
	    					}
	    	    			data.put("time", String.valueOf(System.currentTimeMillis()));
	    					
	    					String newtoken = UUID.randomUUID().toString().replaceAll("-", "");
	    			        session.put(newtoken, data);
	    			        
	    			        newtoken = aesenc(newtoken, data.get("key"));
	    			        request.setAttribute(token, newtoken);
	    			        request.setAttribute(token+"eid", data.get("eid"));
	    			        request.setAttribute(token+"key", data.get("key"));
	    			        request.setAttribute(token+"self", data.get("self"));
	    			        request.setAttribute(token+"name", data.get("name"));
	    			        request.setAttribute(token+"password", data.get("password"));
	    				}
	    			}
	    			if (isexpire){
	    				Map<String, Object> result = new HashMap<String, Object>();
	    				result.put("result", "fail");
	    				result.put("reason", "expire");
	    				result.put("value", "");
	    				out(response, result);
	    			} else {
	    				chain.doFilter(request, response);
	    			}
    			}
    		}
		} catch (Exception e) {
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("result", "fail");
			result.put("reason", e.toString());
			result.put("value", "");
			out(response, result);
		}
    	
    }
    
    private boolean islocal(String tocheck){
    	boolean islocal = false;
		try{
		    Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
		    for (; n.hasMoreElements();){
		        NetworkInterface e = n.nextElement();
		        Enumeration<InetAddress> a = e.getInetAddresses();
		        for (; a.hasMoreElements();){
		            InetAddress addr = a.nextElement();
		            if (addr.getHostAddress()!=null&&addr.getHostAddress().split("\\.").length==4&&!addr.getHostAddress().equals("127.0.0.1")){
		            	islocal = tocheck.contains(addr.getHostAddress());
		            }
		        }
		    }
		}catch(Exception ex){
			//do nothing
		}
		return islocal;
	
    }
    
    private void out(HttpServletResponse response, Map<String, Object> result) throws IOException{
		PrintWriter p = response.getWriter();		
		p.write(new ObjectMapper().writeValueAsString(result));
		p.flush();
		p.close();
		p=null;
    }

    public static String aesenc(String raw, String keyValue) throws Exception{
    	SecretKeyFactory factory =   SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(keyValue.toCharArray(), AesUtil.hex("dc0da04af8fee58593442bf834b30739"),
            1000, 128);
        Key key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(AesUtil.hex("dc0da04af8fee58593442bf834b30739")));
        byte[] encVal = c.doFinal(raw.getBytes());
        return new String(Base64.encodeBase64(encVal));
    }
    
    public static String aesdec(String enc, String keyValue) throws Exception{
    	SecretKeyFactory factory =   SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(keyValue.toCharArray(), AesUtil.hex("dc0da04af8fee58593442bf834b30739"),
            1000, 128);
        Key key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(AesUtil.hex("dc0da04af8fee58593442bf834b30739")));
        byte[] decVal = c.doFinal(Base64.decodeBase64(enc.getBytes()));
        return new String(decVal);
    }

    public void init(FilterConfig arg0) throws ServletException {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(AutoConfig.isrunning){
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						//do nothing
					}
					Set<String> keys = new HashSet<String>();
					keys.addAll(session.keySet());  
			        for (String key:keys){  
			        	if (!AutoConfig.isrunning){
							break;
						}
			        	try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							//do nothing
						}
			        	try{
			        		if (System.currentTimeMillis() - Long.parseLong(session.get(key).get("time"))>sessionexpire){  
				        	    session.remove(key);  
				            }  
			        	}catch(Exception Ex){
			        		//do nothing
			        	}
			        }  
				}
			}
    		
    	}).start();
    }
    
    private static Cipher initCipher() throws Exception {
    	String priv = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAIl/A2CmjdpP6joxbdQ2pe9n9rWw"+
			"844Yn8O65tR+MndAJ/5veSl89VBXWqZ0a5FiQt8nTA4xuuKIAi/OvyeMNMFSErtJhnaJnGJwE8y7"+
			"zm32gPAiEhRa/hfT0qI5rPTn/Zljt9fmPwGybMQSH8KRrVnkN8dik6P70j41UzOTkQjFAgMBAAEC"+
			"gYAXNlIjpvqbuJIacO3RvcgVur9zvQPhIVcXfB4sEYIDj9kwozEZR/dCMP0kzNK8IJKZidDVNKAQ"+
			"TlXQLF8pFgpxuKR5TgYy/QgHiAkC426FTazNvvYcABhr52U5pAULzDMoZWquIV/2PHXoi/V1FNya"+
			"4J7S85NgMrzf9K1kYYB6AQJBANhDNTpqiBkOCOrBgtC3GR/ISYPCj3DSv/Ha6eQkHY3K5V94SBzW"+
			"3ITC8sk337ZmjtqHauR4SAdpvSb66aDDa/ECQQCiwrTVLsnmNoEhuPRMDbBeWrk6FGb4Io0DE0bW"+
			"BxZCV/zvQqBaM/RkuovTx9bFZSgGUVHi17VnbRHFqPhXwg4VAkARFi5McUJTiHJX6fYl+3F2u2Jj"+
			"kKm4pk4YE83LArd7Dn7U7jH0ZV2C98wq7ck1JiE/Tte5OW+ndklFnSl8VDRhAkEAlY2pUbXDaRiu"+
			"AQmM2JE/fQOJ7XcvwlMlFS1SSnmKl313+XVUGG5i42eR8hBPJWQ0qdFxF6ozkoxi5yfCwgianQJB"+
			"AM04Amkm1ss2Mx7X+HLGI8PdIu3+5TU3ErWwyyhToCoI2V6uL5HveRIGPi9rxun4U+zGfPJfvYdW"+
			"Uc6/FEQx1rg=";
 	   PKCS8EncodedKeySpec priPKCS8;
 	   priPKCS8 = new PKCS8EncodedKeySpec(
 					     new BASE64Decoder().decodeBuffer(priv));
 	   KeyFactory keyf = KeyFactory.getInstance("RSA");
 	   PrivateKey privateKey = keyf.generatePrivate(priPKCS8);
 	   Cipher rsacipher = Cipher.getInstance("RSA");
 	   rsacipher.init(Cipher.DECRYPT_MODE, privateKey);
 	   return rsacipher;
    }   
    
 	private static final int MAX_DECRYPT_BLOCK = 128;
 	
 	private static byte[] decryptByPrivateKey(byte[] encryptedData)  
            throws Exception {  
        Cipher cipher = initCipher();  
        int inputLen = encryptedData.length;  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        int offSet = 0;  
        byte[] cache = null;  
        int i = 0;  
        // 瀵规暟鎹垎娈佃В瀵� 
        while (inputLen - offSet > 0) {  
     	   if (inputLen - offSet > MAX_DECRYPT_BLOCK) {  
         	   cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);  
         	   
            } else {  
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);  
            }  
            out.write(cache, 0, cache.length);  
            i++;  
            offSet = i * MAX_DECRYPT_BLOCK;  
        }  
        byte[] decryptedData = out.toByteArray();  
        out.close();  
        return decryptedData;  
    }  

}
