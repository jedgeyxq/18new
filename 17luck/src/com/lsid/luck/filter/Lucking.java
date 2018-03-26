package com.lsid.luck.filter;

import java.io.IOException;
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
import com.lsid.luck.util.Luck;

public class Lucking implements Filter {
	
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
    	try {
    		if (request.getParameter("poolid")!=null&&!request.getParameter("poolid").trim().isEmpty()&&!request.getParameter("poolid").equals("null")) {
    			AutoConfig.innerechok(response, Luck.draw(request.getParameter("eid"), request.getParameter("poolid")));
    		} else {
    			AutoConfig.innerechok(response, new ObjectMapper().writeValueAsString(Luck.getprogress(request.getParameter("eid"))));
    		}
		} catch (Exception e) {
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			AutoConfig.innerechno(response, request, e);
		}
    	
    }

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
	}

}
