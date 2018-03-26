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

public class SendWxCash implements ServletContextListener{
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null,"wxcashlaterfolder").isEmpty()){
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
		final Path thefolder = Paths.get(AutoConfig.config(null,"wxcashlaterfolder")).resolve("cashlater");
		final Path therrorfolder = Paths.get(AutoConfig.config(null,"wxcashlaterfolder")).resolve("casherror");
		System.out.println("========"+new Date()+"======== started watching "+thefolder);
		while(AutoConfig.isrunning){
		
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
							if (Files.isDirectory(Paths.get(file.toString()))){
								return FileVisitResult.CONTINUE;
							} else {
								String eid = Paths.get(file.toString()).getParent().getFileName().toString();
								String orderid=Paths.get(file.toString()).getFileName().toString();
								String enc=orderid.split(AutoConfig.SPLIT_HBASE)[0];
								String prizerecord = "";
								try {
									try{
										prizerecord = AutoConfig.cacheuserdata(eid, enc, "prize", orderid, "p");
									}catch(Exception e){
										return FileVisitResult.CONTINUE;
									}
					    			if (prizerecord.isEmpty()){
					    				Files.deleteIfExists(Paths.get(file.toString()));
					    			} else {
					    				String[] d = prizerecord.split(AutoConfig.SPLIT);
					    				if (!AutoConfig.fromwx(prizerecord)){		
					    					throw new Exception("notwx");
					    				}
					    				if (!AutoConfig.config(eid, "lsid.pool"+AutoConfig.getpoolid(d)+".prize"+AutoConfig.getprizeid(d)+".type").equals("cash")){		
					    					throw new Exception("nothb");
					    				}
					    				int amount = 0;
					    				try{
					    					amount = Integer.parseInt(AutoConfig.config(eid, "lsid.pool"+AutoConfig.getpoolid(d)+".prize"+AutoConfig.getprizeid(d)+".value"));
					    				}catch(Exception e){
					    					throw new Exception("invalidamount");
					    				}
					    				
				    					String encprodinfo = AutoConfig.getencprodainfo(d);
				    					if (encprodinfo.isEmpty()) {
				    						encprodinfo = AutoConfig.getencprodnainfo(d);
				    					}
				    					if (encprodinfo.isEmpty()) {
				    						throw new Exception("invalidcode");
				    					}
				    					try {
						    				DefaultCipher.dec(encprodinfo);
					    				}catch(Exception e) {
					    					throw new Exception("invalidcodeinfo");
					    				}
				    					if (amount>100&&DefaultCipher.dec(encprodinfo).contains(AutoConfig.SPLIT+"test")) {
					    					throw new Exception("testcode");
					    				}
				    				
					    				boolean toretry = false;
					    				String retrycontent = null;
					    				if (!Files.readAllLines(Paths.get(file.toString()), Charset.forName("UTF-8")).isEmpty()) {
					    					retrycontent = Files.readAllLines(Paths.get(file.toString()), Charset.forName("UTF-8")).get(0);
					    					String[] retry = retrycontent.split(AutoConfig.SPLIT);
						    				String status = "";
						    				if (retry.length>0){
						    					status = retry[0];
						    				}
						    				String wxcode = "";
						    				if (retry.length>1){
						    					wxcode = retry[1];
						    				}
						    				String time = "0";
						    				if (retry.length>3){
						    					time = retry[3];
						    				}
						    				boolean toretryin2hours = "fail".equals(status)&&("NOTENOUGH".equals(wxcode)||"FREQ_LIMIT".equals(wxcode))&&(System.currentTimeMillis()-Long.valueOf(time))>2*60*60*1000l;
											boolean toretryinoneday = "fail".equals(status)&&("SENDNUM_LIMIT".equals(wxcode)||"V2_ACCOUNT_SIMPLE_BAN".equals(wxcode))&&!new SimpleDateFormat("dd").format(new Date()).equals(new SimpleDateFormat("dd").format(new Date(Long.valueOf(time))));
											boolean toretryinquarterday = "fail".equals(status)&&!"FREQ_LIMIT".equals(wxcode)&&!"SENDNUM_LIMIT".equals(wxcode)&&!"NOTENOUGH".equals(wxcode)&&!"V2_ACCOUNT_SIMPLE_BAN".equals(wxcode)&&(System.currentTimeMillis()-Long.valueOf(time))>6*60*60*1000l;
											toretry = retrycontent.isEmpty()||toretryin2hours||toretryinoneday||toretryinquarterday;
										} else {
											toretry = true;
										}
					    				if (toretry){
											String params = params(AutoConfig.config(eid, "lsid.playwx.appid"), AutoConfig.config(eid, "lsid.wxmchid"), 
													AutoConfig.config(eid, "lsid.wxmchkey"), getip(), DefaultCipher.dec(AutoConfig.getplayid(d)), 
													URLDecoder.decode(AutoConfig.config(eid, "lsid.pool"+AutoConfig.getpoolid(d)+".prize"+AutoConfig.getprizeid(d)+".wxdesc"),"UTF-8"), 
													Integer.parseInt(AutoConfig.config(eid, "lsid.pool"+AutoConfig.getpoolid(d)+".prize"+AutoConfig.getprizeid(d)+".value")));
											String result = transfer(eid, params);
											Files.write(Paths.get(file.toString()), result.getBytes("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING);
											AutoConfig.cacheuserdata(eid, enc, "prize", orderid, "p", AutoConfig.dataupdatelastatus(prizerecord, result));
										}
										if (retrycontent!=null&&retrycontent.startsWith("success")){
											try{	
												Files.deleteIfExists(Paths.get(file.toString()));
											}catch(Exception e){
												return FileVisitResult.CONTINUE;
											}
										}
					    			}
								}catch(Exception ex){
									AutoConfig.log(ex, "error when processing "+orderid);
									if (!Files.exists(therrorfolder.resolve(eid))){
										Files.createDirectories(therrorfolder.resolve(eid));
									}
									Files.write(Paths.get(file.toString()), ex.toString().getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
									Files.move(Paths.get(file.toString()), therrorfolder.resolve(eid).resolve(orderid), StandardCopyOption.REPLACE_EXISTING);
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
			} catch (IOException ex) {
				AutoConfig.log(ex, "System exited due to below exception:");
				System.exit(1);
			}
		}
	}
		
	private static String transfer(String namespace, String params){
		HttpPost httppost = new HttpPost(AutoConfig.config(null, "lsid.interface.wxtransfer"));
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxtransfer.connectimeoutinsec"))*1000)
				.setConnectionRequestTimeout(Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxtransfer.connectimeoutinsec"))*1000).
				setSocketTimeout(Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxtransfer.socketimeoutinsec"))*1000).build();
		httppost.setConfig(requestConfig);
        CloseableHttpClient req = null;
		CloseableHttpResponse res = null;
		String paymentno = "unkown";
    	String paymenttime = String.valueOf(new Date().getTime());
    	String reason = "";
    	BufferedReader bufferedReader = null;
    	try {
        	boolean resultSuccess = false;
	   		req = getwxclient(namespace);
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
            	return "success"+AutoConfig.SPLIT+AutoConfig.SPLIT+paymentno+AutoConfig.SPLIT+paymenttime;
            } else {
            	return "fail"+AutoConfig.SPLIT+reason+AutoConfig.SPLIT+paymentno+AutoConfig.SPLIT+paymenttime;
            }
        } catch(Exception ex){
        	AutoConfig.log(ex, "error when transferring " + params);
    		try {
    			reason = URLEncoder.encode(ex.getMessage(),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
    		return "fail"+AutoConfig.SPLIT+reason+AutoConfig.SPLIT+paymentno+AutoConfig.SPLIT+paymenttime;
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
				AutoConfig.log(e, "error when closing resources in transfer");
			}
    	}
	}
	private static CloseableHttpClient getwxclient(String eid) throws Exception {
		InputStream is = null;
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			is = Files.newInputStream(Paths.get("/data/wxssl").resolve(eid).resolve("apiclient_cert.p12"));
			keyStore.load(is, AutoConfig.config(eid, "lsid.wxmchid").toCharArray());

			@SuppressWarnings("deprecation")
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					SSLContexts.custom().loadKeyMaterial(keyStore, AutoConfig.config(eid, "lsid.wxmchid").toCharArray())
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
//		System.out.println(appId+":"+mchId+":"+apiKey+":"+billIp+":"+openId+":"+desc+":"+amount);
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

}
