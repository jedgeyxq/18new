package com.lsid.qrenter.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Random;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;

public class Wxscanandauthinfo implements Filter {

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
			String removefirstsplitter = request.getRequestURI().replaceFirst("/", "");
			String originaluri = removefirstsplitter.replaceFirst(removefirstsplitter.substring(0, removefirstsplitter.indexOf("/")), "");
			String eid = originaluri.substring(1, originaluri.lastIndexOf("/"));
			if (!AutoConfig.config(null, "lsid.eids").contains(eid)) {
				throw new Exception("wrongeid[" + eid + "]");
			}

			response.sendRedirect(AutoConfig.config(null, "lsid.host.userentry") + "pubwxauth?appid="
					+ AutoConfig.config(eid, "lsid.infowx.appid") + "&redirect_uri="
					+ URLEncoder.encode(AutoConfig.config(null, "lsid.host.userentry") + "wxauthinfoplay"
							+ originaluri + "?eid=" + eid + "&" + request.getQueryString(), "UTF-8")
					+ "&response_type=code&scope=snsapi_userinfo&state=" + new Random().nextInt(10000)
					+ "&connect_redirect=1#wechat_redirect");
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
