package com.lsid.wx.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.lsid.autoconfig.client.AutoConfig;

public class WxPay {
	
	/* 
     * 发起支付请求 
     * body 商品描述 
     * out_trade_no 订单号 
     * total_fee    订单金额        单位  元 
     * product_id   商品ID 
     */  
    public static Map<String,String> SendPayment(String eid, String body,String out_trade_no,double total_fee,String product_id,String ip,String openid) throws Exception{  
        String url = "https://api.mch.weixin.qq.com/pay/unifiedorder";  
        String xml = WXParamGenerate(eid, body,out_trade_no,total_fee,product_id,ip,openid);  
        String res = httpsRequest(url,"POST",xml);  
        Map<String,String> data = null;  
        try {  
            data = doXMLParse(res);  
        } catch (Exception e) {  
        }  
        return data;  
    } 
    
    //随机字符串
    public static String NonceStr(){  
        String res = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 30);  
        return res;  
    } 
    
    public static String GetIp() {  
        InetAddress ia=null;  
        try {  
            ia=InetAddress.getLocalHost();  
            String localip=ia.getHostAddress();  
            return localip;  
        } catch (Exception e) {  
            return null;  
        }  
    }  
    
     public static String GetSign(String eid, Map<String,String> param){  
        String StringA =  formatUrlMap(param, false, false);  
        MD5Util tosign = new MD5Util();
        String stringSignTemp = tosign.sign(StringA+"&key="+AutoConfig.config(eid, "lsid.wxmchkey")).toUpperCase();  
        return stringSignTemp;  
     } 
     
     //Map转xml数据  
     public static String GetMapToXML(Map<String,String> param){  
         StringBuffer sb = new StringBuffer();  
         sb.append("<xml>");  
         for (Map.Entry<String,String> entry : param.entrySet()) {   
                sb.append("<"+ entry.getKey() +">");  
                sb.append(entry.getValue());  
                sb.append("</"+ entry.getKey() +">");  
        }    
         sb.append("</xml>");  
         return sb.toString();  
     }  
      
     
 	public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)){
        	ip = "127.0.0.1";
        }
        return ip;
    }
      
    //微信统一下单参数设置  
    public static String WXParamGenerate(String eid, String description,String out_trade_no,double total_fee,String product_id,String ip,String openid){  
        int fee = (int)(total_fee * 100.00);  
        Map<String,String> param = new HashMap<String,String>();  
        param.put("appid", AutoConfig.config(eid, "playwx.appid"));  
        param.put("mch_id", AutoConfig.config(eid, "lsid.wxmchid"));  
        param.put("nonce_str",NonceStr()); 
        param.put("body", description);  
        param.put("out_trade_no",out_trade_no);  
        param.put("total_fee", fee+"");  
        param.put("spbill_create_ip", ip);  
        param.put("notify_url", AutoConfig.config(eid, "lsid.wxpaycallback"));  
        param.put("trade_type", "JSAPI"); 
        param.put("openid",openid);  
        param.put("product_id", product_id+"");  
        String sign = GetSign(eid, param);  
        param.put("sign", sign);  
        return GetMapToXML(param);  
    }  
      
    //发起微信支付请求  
    public static String httpsRequest(String requestUrl, String requestMethod, String outputStr) throws Exception{    
        HttpURLConnection conn = null;    
        InputStream inputStream = null;    
        InputStreamReader inputStreamReader = null;    
        BufferedReader bufferedReader = null;    
      try {    
          URL url = new URL(requestUrl);    
          conn = (HttpURLConnection) url.openConnection();    
            
          conn.setDoOutput(true);    
          conn.setDoInput(true);    
          conn.setUseCaches(false);    
          conn.setRequestMethod(requestMethod);    
          conn.setRequestProperty("content-type", "application/x-www-form-urlencoded");    
          if (null != outputStr) {    
              OutputStream outputStream = conn.getOutputStream();    
              // 注意编码格式    
              outputStream.write(outputStr.getBytes("UTF-8"));    
              outputStream.close();    
          }    
          // 从输入流读取返回内容    
          inputStream = conn.getInputStream();    
          inputStreamReader = new InputStreamReader(inputStream, "utf-8");    
          bufferedReader = new BufferedReader(inputStreamReader);    
          String str = null;  
          StringBuffer buffer = new StringBuffer();    
          while ((str = bufferedReader.readLine()) != null) {    
              buffer.append(str);    
          }    
          return buffer.toString();    
      } finally {
	    	  if (bufferedReader!=null) {
	          bufferedReader.close();
	    	  }
	    	  if (inputStreamReader!=null) {
	          inputStreamReader.close();
	    	  }
	    	  if (inputStream!=null) {
	          inputStream.close();
	    	  }
	      inputStream = null;   
	      if (conn!=null) {
	    	    	  conn.disconnect();
	      }
      }  
    }    
        
    //退款的请求方法    
    @SuppressWarnings("deprecation")
	public static String httpsRequest2(String eid, String requestUrl, String requestMethod, String outputStr) throws Exception {    
          KeyStore keyStore  = KeyStore.getInstance("PKCS12");    
          StringBuilder res = new StringBuilder("");    
          FileInputStream instream = new FileInputStream(new File("/data/wxssl/"+eid+"/apiclient_cert.p12"));    
          try {    
              keyStore.load(instream, "".toCharArray());    
          } finally {    
              instream.close();    
          }    
  
          // Trust own CA and all self-signed certs    
          SSLContext sslcontext = SSLContexts.custom()    
                  .loadKeyMaterial(keyStore, "1313329201".toCharArray())    
                  .build();    
          // Allow TLSv1 protocol only    
          SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(    
                  sslcontext,    
                  new String[] { "TLSv1" },    
                  null,    
                  SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);    
          CloseableHttpClient httpclient = HttpClients.custom()    
                  .setSSLSocketFactory(sslsf)    
                  .build();    
          try {    
  
              HttpPost httpost = new HttpPost("https://api.mch.weixin.qq.com/secapi/pay/refund");    
              httpost.addHeader("Connection", "keep-alive");    
              httpost.addHeader("Accept", "*/*");    
              httpost.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");    
              httpost.addHeader("Host", "api.mch.weixin.qq.com");    
              httpost.addHeader("X-Requested-With", "XMLHttpRequest");    
              httpost.addHeader("Cache-Control", "max-age=0");    
              httpost.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0) ");    
               StringEntity entity2 = new StringEntity(outputStr ,Consts.UTF_8);    
               httpost.setEntity(entity2);   
              CloseableHttpResponse response = httpclient.execute(httpost);    
                 
              try {    
                  HttpEntity entity = response.getEntity();    
                  if (entity != null) {    
                      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));    
                      String text = "";  
                      res.append(text);    
                      while ((text = bufferedReader.readLine()) != null) {    
                          res.append(text);    
                      }    
                         
                  }    
                  EntityUtils.consume(entity);    
              } finally {    
                  response.close();    
              }    
          } finally {    
              httpclient.close();    
          }    
          return  res.toString();    
              
    }  
        
    //xml解析    
    @SuppressWarnings("rawtypes")
	public static Map<String, String> doXMLParse(String strxml) throws Exception {    
          strxml = strxml.replaceFirst("encoding=\".*\"", "encoding=\"UTF-8\"");    
          if(null == strxml || "".equals(strxml)) {    
              return null;    
          }   
          InputStream in = null;
          try {
            	  
	          Map<String,String> m = new HashMap<String,String>();     
	          in = new ByteArrayInputStream(strxml.getBytes("UTF-8"));    
	          SAXBuilder builder = new SAXBuilder();    
	          Document doc = builder.build(in);    
	          Element root = doc.getRootElement();    
	          List list = root.getChildren();    
	          Iterator it = list.iterator();    
	          while(it.hasNext()) {    
	              Element e = (Element) it.next();    
	              String k = e.getName();    
	              String v = "";    
	              List children = e.getChildren();    
	              if(children.isEmpty()) {    
	                  v = e.getTextNormalize();    
	              } else {    
	                  v = getChildrenText(children);    
	              }    
	                  
	              m.put(k, v);    
	          }    
	          return m;
          }finally {
	        	  if (in !=null) {
	            	  in.close(); 
	        	  }
          }    
    } 
    
    /** 
     *  
     * 方法用途: 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序），并且生成url参数串<br> 
     * 实现步骤: <br> 
     *  
     * @param paraMap   要排序的Map对象 
     * @param urlEncode   是否需要URLENCODE 
     * @param keyToLower    是否需要将Key转换为全小写 
     *            true:key转化成小写，false:不转化 
     * @return 
     */  
    public static String formatUrlMap(Map<String, String> paraMap, boolean urlEncode, boolean keyToLower)  
    {  
        String buff = "";  
        Map<String, String> tmpMap = paraMap;  
        try  
        {  
            List<Map.Entry<String, String>> infoIds = new ArrayList<Map.Entry<String, String>>(tmpMap.entrySet());  
            // 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）  
            Collections.sort(infoIds, new Comparator<Map.Entry<String, String>>()  
            {  
   
                @Override  
                public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2)  
                {  
                    return (o1.getKey()).toString().compareTo(o2.getKey());  
                }  
            });  
            // 构造URL 键值对的格式  
            StringBuilder buf = new StringBuilder();  
            for (Map.Entry<String, String> item : infoIds)  
            {  
                if (item.getKey()!=null&&!item.getKey().isEmpty()&&!item.getValue().isEmpty())  
                {  
                    String key = item.getKey();  
                    String val = item.getValue();  
                    if (urlEncode)  
                    {  
                        val = URLEncoder.encode(val, "utf-8");  
                    }  
                    if (keyToLower)  
                    {  
                        buf.append(key.toLowerCase() + "=" + val);  
                    } else  
                    {  
                        buf.append(key + "=" + val);  
                    }  
                    buf.append("&");  
                }  
   
            }  
            buff = buf.toString();  
            if (buff.isEmpty() == false)  
            {  
                buff = buff.substring(0, buff.length() - 1);  
            }  
        } catch (Exception e)  
        {  
           return null;  
        }  
        return buff;  
    } 
        
    @SuppressWarnings("rawtypes")
	public static String getChildrenText(List children) {    
          StringBuffer sb = new StringBuffer();    
          if(!children.isEmpty()) {    
              Iterator it = children.iterator();    
              while(it.hasNext()) {    
                  Element e = (Element) it.next();    
                  String name = e.getName();    
                  String value = e.getTextNormalize();    
                  List list = e.getChildren();    
                  sb.append("<" + name + ">");    
                  if(!list.isEmpty()) {    
                      sb.append(getChildrenText(list));    
                  }    
                  sb.append(value);    
                  sb.append("</" + name + ">");    
              }    
          }     
          return sb.toString();    
    }
    
}
