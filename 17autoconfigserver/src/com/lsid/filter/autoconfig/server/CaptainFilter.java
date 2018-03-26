package com.lsid.filter.autoconfig.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;

public class CaptainFilter implements Filter {
	private static final Path sailorconfigfolder = Paths.get("sailors");
	private static final Path sailorconfigfile = sailorconfigfolder.resolve("hostandescription.properties");
	private static final Properties sailorhostnames = new Properties();
	
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
		if (request.getRequestURI().endsWith("whoiscaptain")){
			int captainport = request.getServerPort();
			
			String sailorhost = request.getParameter("sailorhost");
			if (sailorhost==null||sailorhost.trim().isEmpty()){
				echno(response, "missingsailorhost");
				return;
			}
			if (!sailorhost.endsWith("/")){
				sailorhost+="/";
			}
			String sailorname = request.getParameter("sailorname");
			if (sailorname==null||sailorname.trim().isEmpty()){
				echno(response, "missingsailorname");
				return;
			}
			
			BufferedWriter bw = null;
			
			try{
				String result = post(sailorhost+"autoconfig",30,"connectimeoutinsec",GetFilter.get("default", "readcaptain.connectimeoutinsec"),"socketimeoutinsec",GetFilter.get("default", "readcaptain.socketimeoutinsec"),"captainport", String.valueOf(captainport),"captaincontext", request.getContextPath());
				if (!"ok".equals(result)){
					echno(response, result);
					return;
				}
				
				sailorhostnames.setProperty(sailorhost, sailorname);
				
				bw = Files.newBufferedWriter(sailorconfigfile, Charset.forName("UTF-8"),StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				
				sailorhostnames.store(bw,"auto generated");
				echok(response, "ok");
				return;
			}catch(Exception ex){
				echno(response, request, ex);
				return;
			}finally{
				if (bw!=null){
					bw.close();
					bw = null;
				}
			}
		} else if (request.getRequestURI().endsWith("shutmedown")){
			String sailorhost = request.getParameter("sailorhost");
			if (sailorhost==null||sailorhost.trim().isEmpty()){
				echno(response, "missingsailorhost");
				return;
			}
			if (!sailorhost.endsWith("/")){
				sailorhost+="/";
			}
			int shutdowninsec = 15;
			try{
				shutdowninsec = Integer.parseInt(request.getParameter("shutdowninsec"));
				if (shutdowninsec<15){
					shutdowninsec = 15;
				}
			}catch(Exception ex){
				//do nothing
			}
			try{
				String result = post(sailorhost+"autoconfig", 30, "shutdowninsec", String.valueOf(shutdowninsec));
				if (!"ok".equals(result)){
					echno(response, result);
					return;
				}
				echok(response, "ok");
				return;
			}catch(Exception ex){
				echno(response, request, ex);
				return;
			}
		} else {
			echno(response, "invalidcaptainrequest");
		}
    }
    
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		try {
			Files.createDirectories(sailorconfigfolder);
		} catch (IOException e) {
			log(e,"System exited due to below exception:");
			System.exit(1);
		}
		if (Files.exists(sailorconfigfile)){
			InputStream is = null;
			try {
				is = Files.newInputStream(sailorconfigfile);
				sailorhostnames.load(is);
				System.out.println("========"+new Date()+"=======loaded sailors "+sailorhostnames);
			} catch (IOException e) {
				log(e,"System exited due to below exception:");
				System.exit(1);
			} finally{
				if (is!=null){
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					is = null;
				}
			}
		}
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

	public static void echok(HttpServletResponse response, String content) throws IOException{
		PrintWriter p = response.getWriter();		
		p.write("=0=k="+content);
		p.flush();
		p.close();
		p=null;
	}

	public static void echno(HttpServletResponse response, String content) throws IOException{
		PrintWriter p = response.getWriter();		
		p.write("=n=0="+content);
		p.flush();
		p.close();
		p=null;
	}

	public static void echno(HttpServletResponse response, HttpServletRequest request, Exception e) throws IOException{
		System.out.println("========"+new Date()+"======== error processing "+request.getContextPath()+request.getRequestURI());
		e.printStackTrace();
		echno(response, e.toString()+"("+request.getServerName()+":"+request.getServerPort()+")");
	}
	
	public static void log(Exception e, String extra){
		System.out.println("========"+new Date()+"========"+extra);
		e.printStackTrace();
	}
	
	public static String post(String url, int timeoutsec, String ...paramvalues) throws Exception{
		if (timeoutsec<20){
			timeoutsec=20;
		}
		if (timeoutsec>60){
			timeoutsec=60;
		}
		Form f = Form.form();
		for (int i=0;i<paramvalues.length;i+=2){
			f.add(paramvalues[i], paramvalues[i+1]);
		}
		String returnvalue = Request.Post(url).connectTimeout(timeoutsec*1000).socketTimeout(timeoutsec*1000).bodyForm(f.build(),Charset.forName("UTF-8")).execute().returnContent().asString(Charset.forName("UTF-8"));
		if (returnvalue!=null){
			if (returnvalue.startsWith("=0=k=")){
				return returnvalue.substring("=0=k=".length());
			} else if (returnvalue.startsWith("=n=0=")){
				throw new Exception(returnvalue.substring("=n=0=".length()));
			} else {
				throw new Exception(returnvalue+"responsefrom["+url+"]");
			}
		} else {
			throw new Exception("nullresponsefrom["+url+"]");
		}
	}
	
}
