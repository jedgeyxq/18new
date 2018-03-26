package com.lsid.wx.filter;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;
import com.lsid.wx.util.MD5Util;
import com.lsid.wx.util.WxPay;

public class WxInterfaces implements Filter {
	
	private static final Map<String, String> cache = new HashMap<String, String>();
	private static final Path wxparams = Paths.get("wxparams");
	
	@Override
	public void destroy() {
		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain arg2)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	response.setHeader("Access-Control-Allow-Origin", "*");
    	response.setHeader("XDomainRequestAllowed","1");
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
    	try {
    		if (request.getParameter("appid")!=null&&!request.getParameter("appid").trim().isEmpty()&&!request.getParameter("appid").equals("null")) {
    			String appid = request.getParameter("appid").trim();
    			String cachekey = "";
    			String cachevalue = "";
    			if (request.getParameter("client_credential")!=null&&!request.getParameter("client_credential").trim().isEmpty()&&!request.getParameter("client_credential").equals("null")){
	    			cachekey=appid+"client_credential";
	    			cachevalue=request.getParameter("client_credential");
	    		} else if (request.getParameter("jsapi")!=null&&!request.getParameter("jsapi").trim().isEmpty()&&!request.getParameter("jsapi").equals("null")){
	    			cachekey=appid+"jsapi";
	    			cachevalue=request.getParameter("jsapi");
	    		} else if (request.getParameter("wx_card")!=null&&!request.getParameter("wx_card").trim().isEmpty()&&!request.getParameter("wx_card").equals("null")){
	    			cachekey=appid+"wx_card";
	    			cachevalue=request.getParameter("wx_card");
	    		}
    			cache.put(cachekey, cachevalue);
	    		Files.write(wxparams.resolve(cachekey), DefaultCipher.enc(cachevalue).getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    			AutoConfig.innerechok(response, "ok");
	    		
    		} else {
    		
	    		String eid = request.getParameter("eid");
	    		
	    		if (eid==null||eid.trim().isEmpty()||eid.equals("null")){
	    			throw new Exception("missingeid");
	    		}
	    		if (request.getParameter("payamount")!=null&&!request.getParameter("payamount").trim().isEmpty()&&!request.getParameter("payamount").equals("null")) {
	    			Map<String,String> mapinfo= WxPay.SendPayment(eid, request.getParameter("body"), request.getParameter("payorder"), 
	    					Double.parseDouble(request.getParameter("payamount")),request.getParameter("productid"),AutoConfig.getremoteip(request),
	    					DefaultCipher.dec(request.getParameter("playid")));
				if(mapinfo.get("return_code").equals("SUCCESS")){
					String prepay_id=mapinfo.get("prepay_id");
					long timestemps=System.currentTimeMillis();
						    								
					String str1=WxPay.NonceStr();
					String pack = "prepay_id="+prepay_id;

					String sss = "appId="+AutoConfig.config(eid, "lsid.playwx.appid")+"&nonceStr="+str1+"&package="+pack +"&signType=MD5&timeStamp="+timestemps;
					MD5Util tosign = new MD5Util();
					 String sign2 = tosign.sign(sss+"&key="+AutoConfig.config(eid, "lsid.wxmchkey")).toUpperCase();  
					 Map<String, String> result = new HashMap<String, String>();
					 result.put("prepay_id", prepay_id);
					 result.put("timestemps", String.valueOf(timestemps));
					 result.put("nonce_str", str1);
					 result.put("sign", sign2);
					 result.put("payorder", request.getParameter("payorder"));
					 result.put("result", "0");
					 result.put("reason", "");
					 AutoConfig.innerechok(response, new ObjectMapper().writeValueAsString(result));
				}
	    		} else if (request.getParameter("url4wxjssdk")!=null&&!request.getParameter("url4wxjssdk").trim().isEmpty()&&!request.getParameter("url4wxjssdk").equals("null")){
	    			AutoConfig.innerechok(response, new ObjectMapper().writeValueAsString(sign4wxconfigshare(eid, request.getParameter("noncestr"), request.getParameter("timestamp"), request.getParameter("url4wxjssdk"))));
	    		} else if (request.getParameterValues("cardid")!=null&&
	    				request.getParameter("openid")!=null&&!request.getParameter("openid").trim().isEmpty()&&!request.getParameter("openid").equals("null")){
					List<Map<String,String>> returnvalue =  new ArrayList<Map<String,String>>();
	    			for (String cardid:request.getParameterValues("cardid")){
	    				if (cardid!=null&&!cardid.trim().isEmpty()&&!cardid.equals("null")){
	    					returnvalue.add(sign4wxcard(eid, cardid, request.getParameter("openid")));
	    				}
	    			}
	    			AutoConfig.innerechok(response, new ObjectMapper().writeValueAsString(returnvalue));
	    		} else if (request.getParameter("wxcode")!=null&&!request.getParameter("wxcode").trim().isEmpty()&&!request.getParameter("wxcode").equals("null")){
	    			if (request.getParameter("keyprefix")!=null&&!request.getParameter("keyprefix").trim().isEmpty()&&!request.getParameter("keyprefix").equals("null")){
	    				AutoConfig.innerechok(response, getopenid(eid, request.getParameter("wxcode"), request.getParameter("keyprefix")));
	    			} else {
	    				AutoConfig.innerechok(response, getuserinfo(eid, request.getParameter("wxcode")));
	    			}
	    		} else if (request.getParameter("orderid")!=null&&!request.getParameter("orderid").trim().isEmpty()&&!request.getParameter("orderid").equals("null")){
	    			String prizerecord = AutoConfig.cacheuserdata(eid, request.getParameter("orderid").split(AutoConfig.SPLIT_HBASE)[0], "prize", request.getParameter("orderid"), "p");
					if (!AutoConfig.fromwx(prizerecord)){
						throw new Exception("notwx");
					}
					String[] d = prizerecord.split(AutoConfig.SPLIT);
					if (!AutoConfig.config(eid, "lsid.pool"+AutoConfig.getpoolid(d)+".prize"+AutoConfig.getprizeid(d)+".type").equals("cash")){
						throw new Exception("nothb");
					}
					try{
						Integer.parseInt(AutoConfig.config(eid, "lsid.pool"+AutoConfig.getpoolid(d)+".prize"+AutoConfig.getprizeid(d)+".value"));
					}catch(Exception e){
						throw new Exception("invalidamount");
					}
					try{
						Path cashfolder=Paths.get(AutoConfig.config(null, "wxcashlaterfolder")).resolve("cashlater").resolve(eid);
						if (!Files.exists(cashfolder)){
							Files.createDirectories(cashfolder);
						}
						Files.write(cashfolder.resolve(request.getParameter("orderid")), new byte[0], StandardOpenOption.CREATE_NEW);
						AutoConfig.innerechok(response, "ok");
					}catch(Exception ex){
						throw new Exception("conflict");
					}
	    		} else {
	    			throw new Exception("missingparams");
	    		}
    		}
    		
		} catch (Exception e) {
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			AutoConfig.innerechno(response, request, e);
		}
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		try {
			if (!Files.exists(wxparams)) {
					Files.createDirectories(wxparams);
			}
			String[] files = wxparams.toFile().list();
			if (files!=null) {
				for (String file:files) {
					String val = new String(Files.readAllBytes(wxparams.resolve(file)),"UTF-8");
					System.out.println("===="+new Date()+"==== Loaded ["+file+"]["+val+"]");
					cache.put(file, DefaultCipher.dec(val));
				}
			}
		} catch (Exception e) {
			AutoConfig.log(e, "System exited due to below exception:");
			System.exit(1);
		}
	}
	
	private Map<String, String> sign4wxconfigshare(String eid, String noncestr, String timestamp, String url) throws Exception{
		Map<String, String> ret = new HashMap<String, String>();
        String signature = "";
        if (noncestr==null||noncestr.trim().isEmpty()||noncestr.trim().toLowerCase().equals("null")) {
        	noncestr = create_nonce_str();
        }
        if (timestamp==null||timestamp.trim().isEmpty()||timestamp.trim().toLowerCase().equals("null")) {
        	timestamp = create_timestamp();
        }
        String params = "jsapi_ticket=" + cache.get(AutoConfig.config(eid, "lsid.playwx.appid")+"jsapi") +
                  "&noncestr=" + noncestr +
                  "&timestamp=" + timestamp +
                  "&url=" + URLDecoder.decode(url,"UTF-8");

        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(params.getBytes("UTF-8"));
        signature = byteToHex(crypt.digest());
        ret.put("appId", AutoConfig.config(eid, "lsid.infowx.appid"));
        ret.put("nonceStr", noncestr);
        ret.put("timestamp", timestamp);
        ret.put("signature", signature);
        return ret;
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
        return UUID.randomUUID().toString();
    }

    private static String create_timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }
	
	private Map<String, String> sign4wxcard(String eid, String cardid, String openid) throws Exception{
		if (!AutoConfig.config(eid, "lsid.youzan.appid").isEmpty()){
			SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    	String timestampyz=sf.format(new Date());
	    	String paramyz="app_id"+AutoConfig.config(eid, "lsid.youzan.appid")+"card_id"+cardid+"formatjsonmethodkdt.ump.coupon.takesign_methodmd5timestamp"+timestampyz+"v1.0weixin_openid"+openid;
	        MessageDigest cryptmd5 = MessageDigest.getInstance("MD5");
	        cryptmd5.reset();
	        cryptmd5.update((AutoConfig.config(eid, "lsid.youzan.secret")+paramyz+AutoConfig.config(eid, "lsid.youzan.secret")).getBytes("UTF-8"));
	        String signyz = byteToHex(cryptmd5.digest());
	    	String r = AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.wxcard.code")
	    			+ "?sign="+URLEncoder.encode(signyz,"UTF-8")
	    			+"&weixin_openid="+ URLEncoder.encode(openid,"UTF-8")
	    			+"&card_id="+ URLEncoder.encode(cardid,"UTF-8")
	    			+"&timestamp="+ URLEncoder.encode(timestampyz,"UTF-8")
	    			+"&v="+ URLEncoder.encode("1.0","UTF-8")
	    			+"&app_id="+ URLEncoder.encode(AutoConfig.config(eid, "lsid.youzan.appid"),"UTF-8")
	    			+"&method="+ URLEncoder.encode("kdt.ump.coupon.take","UTF-8")
	    			+"&sign_method="+ URLEncoder.encode("md5","UTF-8")
	    			+"&format="+ URLEncoder.encode("json","UTF-8"), 
	    			Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxcard.code.connectimeoutinsec")),
	    			Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxcard.code.socketimeoutinsec")));
	    	JsonNode jn = new ObjectMapper().readTree(r);
	    	String code = jn.get("response").get("promocard").get("verify_code").asText();
	    	
	    	String api_ticket = cache.get(AutoConfig.config(eid, "lsid.playwx.appid")+"wx_card");
	    	Map<String, String> ret = new HashMap<String, String>();
	    	Map<String, String> rettmp = new HashMap<String, String>();
	        String timestamp = create_timestamp();
	        String signature = "";
	        String[] paramstmp =  {api_ticket  , timestamp , cardid ,code, openid};
	        String params=getparams(paramstmp);
	        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
	        crypt.reset();
	        crypt.update(params.getBytes("UTF-8"));
	        signature = byteToHex(crypt.digest());
	        rettmp.put("timestamp", timestamp);
	        rettmp.put("openid", openid);
	        rettmp.put("code", code);
	        rettmp.put("signature", signature);
	        ret.put("cardId", cardid);
	        ret.put("cardExt", new ObjectMapper().writeValueAsString(rettmp));
	        return ret;
		} else {
			Map<String, String> ret = new HashMap<String, String>();
        	Map<String, String> rettmp = new HashMap<String, String>();
            String timestamp = create_timestamp();
            String signature = "";
            String api_ticket=cache.get(AutoConfig.config(eid, "lsid.playwx.appid")+"wx_card");
            String[] paramstmp =  {api_ticket  , timestamp , cardid};
            String params=getparams(paramstmp);
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(params.getBytes("UTF-8"));
            signature = byteToHex(crypt.digest());
            rettmp.put("timestamp", timestamp);
            rettmp.put("openid", "");
            rettmp.put("signature", signature);
            ret.put("cardId", cardid);
            ret.put("cardExt", new ObjectMapper().writeValueAsString(rettmp));
	        return ret;
		}
	}
	
    private static String getparams(String[] paramstmp){
    	String str="";
    	Arrays.sort(paramstmp);
    	for(String s : paramstmp){
    		str+=s;
    	}
    	return str;
    }
	
	private String getopenid(String eid, String wxcode, String keyprefix) throws Exception{
		String returnvalue = null;
		JsonNode jn = null;
		if ("lsid.uuwxid".equals(keyprefix)){
			jn = new ObjectMapper().readTree(AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.wxoauth")+"?appid="+
					AutoConfig.config(null, "lsid.uuwxid.appid")+"&secret="+AutoConfig.config(null, "lsid.uuwxid.secret")+"&code="+wxcode+"&grant_type=authorization_code",
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxoauth.connectimeoutinsec")),
	    			Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxoauth.socketimeoutinsec"))));
		} else {
			jn = new ObjectMapper().readTree(AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.wxoauth")+"?appid="+
					AutoConfig.config(eid, keyprefix+".appid")+"&secret="+AutoConfig.config(eid, keyprefix+".secret")+"&code="+wxcode+"&grant_type=authorization_code",
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxoauth.connectimeoutinsec")),
	    			Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxoauth.socketimeoutinsec"))));
		}		
		if (jn.get("openid")!=null&&jn.get("access_token")!=null){
			returnvalue = DefaultCipher.enc(jn.get("openid").asText());
		}
		if (returnvalue==null){
			throw new Exception(URLEncoder.encode(jn.toString(),"UTF-8"));
		}
		return returnvalue;
	}
	
	private String getuserinfo(String eid, String wxcode) throws Exception{
		String returnvalue = null;
		JsonNode jn = new ObjectMapper().readTree(AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.wxoauth")+"?appid="+
				AutoConfig.config(eid, "lsid.infowx.appid")+"&secret="+AutoConfig.config(eid, "lsid.infowx.secret")+"&code="+wxcode+"&grant_type=authorization_code", 
				Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxoauth.connectimeoutinsec")),
    			Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxoauth.socketimeoutinsec"))));		
		if (jn.get("openid")!=null&&jn.get("access_token")!=null){
			String infowxid = jn.get("openid").asText();
			String accessToken = jn.get("access_token").asText();
			jn = new ObjectMapper().readTree(AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.wxuserinfo")+"?access_token="+accessToken.trim()+"&openid="+infowxid.trim()+"&lang=zh_CN",
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxuserinfo.connectimeoutinsec")),
			    			Integer.parseInt(AutoConfig.config(null, "lsid.interface.wxuserinfo.socketimeoutinsec"))));
			if (jn.get("openid")!=null){
				returnvalue = DefaultCipher.enc(jn.toString());
			}
		}
		if (returnvalue==null){
			throw new Exception(URLEncoder.encode(jn.toString(),"UTF-8"));
		}
		return returnvalue;
	}
}
