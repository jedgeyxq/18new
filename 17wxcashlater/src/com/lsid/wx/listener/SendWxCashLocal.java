package com.lsid.wx.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.UUID;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class SendWxCashLocal implements ServletContextListener{
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	public static void main(String[] s) throws Exception {
		String[] files = Paths.get("tosend").toFile().list();
		String mchid="1321475601";
		String eid = "c";
		for (String file:files) {
			String[] parts = DefaultCipher.dec(new String(Files.readAllBytes(Paths.get("tosend").resolve(file)),"UTF-8")).split(",");
			String params = params("wx6c5fea776983a636", mchid, 
					DefaultCipher.dec("Aw1fSZ9ZFYhK2pioC%2BIZnPqSNfscd6kID2yGkxYIqHRTPMwG83lfZbbc03CkPyG8"), 
					"192.168.10.73", parts[4], 
					"红包", 
					Integer.parseInt(parts[5]));
			String result = transfer(eid,params, mchid);
			System.out.println(result);
			if (result.startsWith("success")) {
				Files.deleteIfExists(Paths.get("tosend").resolve(file));
			}
		}
	}
	
		
	private static String transfer(String namespace, String params, String mchid){
		HttpPost httppost = new HttpPost("https://api.mch.weixin.qq.com/mmpaymkttransfers/promotion/transfers");
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30*1000)
				.setConnectionRequestTimeout(30*1000).
				setSocketTimeout(30*1000).build();
		httppost.setConfig(requestConfig);
        CloseableHttpClient req = null;
		CloseableHttpResponse res = null;
		String paymentno = "unkown";
    	String paymenttime = String.valueOf(new Date().getTime());
    	String reason = "";
    	BufferedReader bufferedReader = null;
    	try {
        	boolean resultSuccess = false;
	   		req = getwxclient(namespace, mchid);
	   		StringEntity ss = new StringEntity(params,"UTF-8");
        	httppost.setEntity(ss);
        	res = req.execute(httppost);
        	HttpEntity entity = res.getEntity();
            StringBuffer s = new StringBuffer();
            if (entity != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
                String text=null;
                while ((text = bufferedReader.readLine()) != null) {
                	s.append(text);
                    if (text.contains("<result_code><![CDATA[SUCCESS]]></result_code>")){
                    	resultSuccess = true;
                    } else if (text.contains("<payment_no>")&&text.contains("</payment_no>")){
                    	paymentno = text.substring(text.indexOf("<payment_no>")+21,text.indexOf("</payment_no>")-3);
                    } else if (text.contains("<payment_time>")&&text.contains("</payment_time>")){
                    	paymenttime = text.substring(text.indexOf("<payment_time>")+23,text.indexOf("</payment_time>")-3);
                    } else if (text.contains("<err_code>")&&text.contains("</err_code>")){
                    	reason=text.substring(text.indexOf("<err_code>")+19,text.indexOf("</err_code>")-3);
                    }
                }
            }
            EntityUtils.consume(entity);
            if (resultSuccess){
            	return "success"+"#"+"#"+paymentno+"#"+paymenttime;
            } else {
            	return "fail"+"#"+reason+"#"+paymentno+"#"+paymenttime;
            }
        } catch(Exception ex){
        	ex.printStackTrace();
        	try {
    			reason = URLEncoder.encode(ex.getMessage(),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
    		return "fail"+"#"+reason+"#"+paymentno+"#"+paymenttime;
		}finally{
    		try {
    			if (bufferedReader!=null){
    				bufferedReader.close();
    				bufferedReader = null;
    			}
    			if (res!=null){
        			res.close();
        			res = null;
				}
	        	if (req!=null){
	        		req.close();
	        		req = null;
	        	}
    		} catch (IOException e) {
				e.printStackTrace();
			}
    	}
	}
	private static CloseableHttpClient getwxclient(String eid, String mchid) throws Exception {
		InputStream is = null;
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			is = Files.newInputStream(Paths.get("wxssl").resolve(eid).resolve("apiclient_cert.p12"));
			keyStore.load(is, mchid.toCharArray());

			@SuppressWarnings("deprecation")
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					SSLContexts.custom().loadKeyMaterial(keyStore, mchid.toCharArray())
							.build(),
					new String[] { "TLSv1" }, null, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
			return HttpClients.custom().setSSLSocketFactory(sslsf).build();
		} catch (Exception ex) {
			throw ex;
		} finally {
			if (is != null) {
				is.close();
			}
		}

	}

	private static String params(String appId, String mchId, String apiKey, String billIp, String openId, String desc, int amount){
		String billNo = mchId+new SimpleDateFormat("yyyyMMdd").format(new Date())+String.valueOf(new Date().getTime()).substring(3);
		String nonce = create_nonce_str();
        StringBuilder s = new StringBuilder("<xml>");  
        s.append("<spbill_create_ip>").append(billIp).append("</spbill_create_ip>").  
        append("<partner_trade_no>").append(billNo).append("</partner_trade_no>").  
        append("<mchid>").append(mchId).append("</mchid>").  
        append("<nonce_str>").append(nonce).append("</nonce_str>").  
        append("<openid>").append(openId).append("</openid>").  
        append("<desc>").append(desc).append("</desc>").  
        append("<amount>").append(amount).append("</amount>").  
        append("<check_name>").append("NO_CHECK").append("</check_name>").  
        append("<device_info>").append("wx").append("</device_info>").  
        append("<mch_appid>").append(appId).append("</mch_appid>");
        String p = "amount="+amount+"&check_name=NO_CHECK&desc="+desc+"&device_info=wx&mch_appid="+appId+"&mchid="+mchId+
        		"&nonce_str="+nonce+"&openid="+openId+"&partner_trade_no="+billNo+"&spbill_create_ip="+billIp+"&key="+apiKey;
        MessageDigest crypt=null;
		try {
			crypt = MessageDigest.getInstance("MD5");
		    crypt.reset();
		    crypt.update(p.getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
        String signature = byteToHex(crypt.digest());
        s.append("<sign>").append(signature.toUpperCase()).append("</sign>");
        s.append("</xml>");        
        return s.toString();
	}
	
	private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private static String create_nonce_str() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

	private static String getip(){
		String returnvalue = "127.0.0.1";
		try{
		    Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
		    for (; n.hasMoreElements();){
		        NetworkInterface e = n.nextElement();
		        Enumeration<InetAddress> a = e.getInetAddresses();
		        for (; a.hasMoreElements();){
		            InetAddress addr = a.nextElement();
		            if (addr.getHostAddress()!=null&&addr.getHostAddress().split("\\.").length==4&&!addr.getHostAddress().equals("127.0.0.1")){
		            	returnvalue = addr.getHostAddress();
		            }
		        }
		    }
		}catch(Exception ex){
			//do nothing
		}
		return returnvalue;
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
