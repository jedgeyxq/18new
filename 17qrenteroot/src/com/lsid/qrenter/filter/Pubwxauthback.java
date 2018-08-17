package com.lsid.qrenter.filter;

import com.lsid.autoconfig.client.AutoConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Pubwxauthback implements Filter {
	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("XDomainRequestAllowed", "1");
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		try {
			String ticket = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/") + 1);
			String thirduri = AutoConfig.readticket(ticket);
			if (thirduri == null || thirduri.trim().isEmpty()) {
				throw new Exception("invalidticket=[" + ticket + "]");
			}

			String param2 = request.getQueryString();
			thirduri = URLDecoder.decode(thirduri, "UTF-8");
			String split = "?";
			if (thirduri.contains("?")) {
				split = "&";
			}

			response.sendRedirect(thirduri + split + param2);
		} catch (Exception var10) {
			StringWriter errors = new StringWriter();
			var10.printStackTrace(new PrintWriter(errors));
			response.getWriter().write(errors.toString());
		}

	}

	public void init(FilterConfig arg0) throws ServletException {
	}
}