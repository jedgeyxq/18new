package com.lsid.qrenter.filter;

import com.lsid.autoconfig.client.AutoConfig;
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

public class Pubwxauth implements Filter {
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
			String[] param = request.getQueryString().split("&");
			String hosturl = "";
			String params = "";

			for (int i = 0; i < param.length; ++i) {
				if (param[i].indexOf("redirect_uri") != -1) {
					hosturl = param[i].split("=")[1];
				}
			}

			String ticket = AutoConfig.generateticket(hosturl, "fixedexpire");

			for (int i = 0; i < param.length; ++i) {
				if (param[i].indexOf("redirect_uri") != -1) {
					param[i] = "redirect_uri=" + AutoConfig.config((String) null, "lsid.host.userentry")
							+ "pubwxauthback/" + ticket;
				}

				params = params + param[i] + "&";
			}

			if (!params.contains("connect_redirect")) {
				params = "connect_redirect=1&" + params;
			}

			params = params.substring(0, params.length() - 1);
			response.sendRedirect("https://open.weixin.qq.com/connect/oauth2/authorize?" + params);
		} catch (Exception var11) {
			StringWriter errors = new StringWriter();
			var11.printStackTrace(new PrintWriter(errors));
			response.getWriter().write(errors.toString());
		}

	}

	public void init(FilterConfig arg0) throws ServletException {
	}
}