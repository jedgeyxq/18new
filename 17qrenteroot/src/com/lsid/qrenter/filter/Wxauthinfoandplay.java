package com.lsid.qrenter.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;

public class Wxauthinfoandplay implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("XDomainRequestAllowed", "1");
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		try {
			String eid = request.getParameter("eid");
			if (!AutoConfig.config(null, "lsid.eids").contains(eid)) {
				throw new Exception("wrongeid[" + eid + "]");
			}
			
			String encuserinfo=AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.wx"), 
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),
					"eid",eid,"wxcode",request.getParameter("code"));
			
			//String openid = new ObjectMapper().readTree(DefaultCipher.dec(encuserinfo)).get("openid").asText();
					
			String fixedcode=request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/")+1);
			
			String ticket = AutoConfig.generateticket(encuserinfo, "refreshable");
			
			response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "fixedcode.h5play."+fixedcode)+"ticket="+ticket+(request.getParameter("otheropenid")!=null?"&"+request.getQueryString():""));
			
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			response.getWriter().write(errors.toString());
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
	}

}
