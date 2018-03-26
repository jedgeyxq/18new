package com.lsid.ticket.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.ticket.listener.TicketExpire;

public class ReadTicketFilter implements Filter {

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
			String value = TicketExpire.read(request.getParameter("ticket"));
			if (value != null){
				AutoConfig.innerechok(response, value);
			} else {
				AutoConfig.innerechok(response, "");
			}
		} catch (Exception e) {
			if (e.getMessage().equals("invalidticket")){
				AutoConfig.innerechok(response, "invalidticket");
			} else {
				AutoConfig.log(e, "error processing request ["+request.getRequestURI()+"] due to below exception:");
		    	AutoConfig.innerechno(response, request, e);
			}
		}
    	
    }

    public void init(FilterConfig arg0) throws ServletException {
    }
}
