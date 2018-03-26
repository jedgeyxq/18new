package com.lsid.code.filter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;

public class SecureUploadCode implements Filter {
	public static final String CLIENTSECRET = "clientsecret";
	public static final String PREFIX = "prefix";
	public static final String DATALENGTH = "datalength";
	public static final String TABLE = "table";

	public void destroy() {
    }
    
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {
    	HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	response.setHeader("Access-Control-Allow-Origin", "*");
    	response.setHeader("XDomainRequestAllowed","1");
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
    	String user = request.getParameter("user");
    	String clientimestr = request.getParameter("t");
    	long clientime = 0;
    	try{
    		clientime = Long.parseLong(clientimestr);
    	}catch(Exception e){
    		//do nothing
    	}
    	if (System.currentTimeMillis()-clientime>60000){
    		out(response, "clientimerror");
    	} else {
    		String active = request.getParameter("active");
    		String table = null;
    		int datalength = 0;
    		if (active!=null&&active.startsWith("a")){
    			try{
    				datalength = Integer.parseInt(active.substring(1));
    				table = "a";
    			}catch(Exception ex){
    				//don nothing
    			}
    		} else if (active!=null&&active.startsWith("na")){
    			try{
    				datalength = Integer.parseInt(active.substring(2));
    				table = "na";
    			}catch(Exception ex){
    				//don nothing
    			}
    		}
    		if (active!=null&&datalength>0&&table!=null){
    			String host = AutoConfig.config(user,"lsid.host.userentry");
    			String apikey = AutoConfig.config(user, "lsid.code.apikey");
    			String clientsecret = AutoConfig.config(user, "lsid.code.clientsecret");
    			String prefix = null;
				if (!host.isEmpty()&&host.endsWith("/")){
					prefix = host+request.getParameter("user")+"/";
				} else if (!host.isEmpty()&&!host.endsWith("/")){
					prefix = host+"/"+request.getParameter("user")+"/";
				}
				if (prefix==null||apikey.isEmpty()||clientsecret.isEmpty()){
					out(response, "hostconfigerror");
				} else {
			    	String sign = request.getParameter("sign");
			    	String verifysign = sign("user="+user+"&active="+active+"&t="+clientimestr+"&key="+apikey);
			    	if (verifysign.equals(sign)){
			    		req.setAttribute(PREFIX, prefix);
			    		req.setAttribute(CLIENTSECRET, clientsecret);
			    		req.setAttribute(DATALENGTH, datalength);
			    		req.setAttribute(TABLE, table);
			    		chain.doFilter(req, res);
			    	} else {
			    		out(response, "signerror["+verifysign+"]");
			    	}
				}
    		} else {
    			out(response, "activerror");
	    	}
    	}
    }
    
    public static void out(HttpServletResponse response, String reason) throws IOException{
    	Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, String>> fails = new ArrayList<Map<String, String>>(1);
		Map<String, String> fail = new HashMap<String, String>();
		fail.put("reason", reason);
		fails.add(fail);
		result.put("result", "fail");
		result.put("successcount", 0);
		result.put("fails", fails);
		AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));
    }

    public void init(FilterConfig arg0) throws ServletException {
    }   
    
    private static String sign(String raw){
    	MessageDigest crypt = null;
    	try {
    		crypt = MessageDigest.getInstance("MD5");
    	    crypt.reset();
    	   	crypt.update(raw.getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	    return byteArrayToHexString(crypt.digest());
    }
    
	private static String[] HexCode = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    public static String byteToHexString(byte b)
    {
        int n = b;
        if (n < 0)
        {
            n = 256 + n;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return HexCode[d1] + HexCode[d2];
    }

    public static String byteArrayToHexString(byte[] b)
    {
        String result = "";
        for (int i = 0; i < b.length; i++)
        {
            result = result + byteToHexString(b[i]);
        }
        return result;
    }
}
