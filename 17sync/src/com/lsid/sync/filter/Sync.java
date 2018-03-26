package com.lsid.sync.filter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.sync.listener.RemoveLock;

public class Sync implements Filter {
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
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
    	String eid = "";
    	String sync = "";
    	try {
    		if (request.getParameter("eid")==null||request.getParameter("eid").trim().isEmpty()||request.getParameter("eid").equals("null")){
    			throw new Exception("eidneeded");
    		} else {
    			eid = request.getParameter("eid").trim();
    		}
    		
    		if (request.getParameter("sync")==null||request.getParameter("sync").trim().isEmpty()||request.getParameter("sync").equals("null")){
    			throw new Exception("syncneeded");
    		} else {
    			sync = request.getParameter("sync").trim();
    		}
    		
    		if (!Files.exists(RemoveLock.synclock.resolve(eid))) {
    			Files.createDirectories(RemoveLock.synclock.resolve(eid));
    		}
    		Files.write(RemoveLock.synclock.resolve(eid).resolve(sync), 
	    					String.valueOf(System.currentTimeMillis()).getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
    		AutoConfig.innerechok(response, "ok");
	    } catch (Exception e) {
			Files.deleteIfExists(RemoveLock.synclock.resolve(eid).resolve(sync));
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			AutoConfig.innerechno(response, request, e);
		}
		
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
}
