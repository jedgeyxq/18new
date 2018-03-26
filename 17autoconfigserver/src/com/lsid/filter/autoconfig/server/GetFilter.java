package com.lsid.filter.autoconfig.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetFilter implements Filter {
	private static final Path configfolder = Paths.get("captainautoconfig");
	private static final Path sailorlivefolder = Paths.get("sailorlive");
	private static final Path defaultconfig = configfolder.resolve("default.properties");
	
	public void destroy() {

    }
    
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");		
    	response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("XDomainRequestAllowed","1");
		
		String key = request.getParameter("key");
		String eid = request.getParameter("eid");
		try{
			if (Files.notExists(sailorlivefolder)){
				Files.createDirectories(sailorlivefolder);
			}
			String ip = getremoteip(request);
			Files.write(sailorlivefolder.resolve(ip+"-"+request.getParameter("sailorport")), String.valueOf(System.currentTimeMillis()).getBytes("UTF-8"), 
					StandardOpenOption.CREATE);
			CaptainFilter.echok(response, get(eid,key));
		}catch(Exception ex){
			CaptainFilter.echno(response, request, ex);
		}
    }
    
    public static String get(String eid, String key) throws IOException{
    	InputStream is = null;
		try{
			Properties p = new Properties();
			if (Files.exists(configfolder.resolve(eid+".properties"))){
				is = Files.newInputStream(configfolder.resolve(eid+".properties"));
				p.load(is);
			} else {
				is = Files.newInputStream(defaultconfig);
				p.load(is);
			}
			
			String value = "";
			if (key!=null&&p.getProperty(key)!=null){
				value = p.getProperty(key);
			}
			return value;
		} finally {
			if (is!=null){
				is.close();
				is = null;
			}
		}
    }
    
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	public static String getremoteip(HttpServletRequest request) {
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
	
}
