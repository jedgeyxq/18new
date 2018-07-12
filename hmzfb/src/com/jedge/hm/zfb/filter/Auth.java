package com.jedge.hm.zfb.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jedge.hm.zfb.data.Cache;
import com.jedge.hm.zfb.util.Config;

public class Auth implements Filter {
	
	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain arg2)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("XDomainRequestAllowed", "1");
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		try {
			String codeString = request.getParameter("codeString");
			String projectId = request.getParameter("projectId");
			if (codeString != null&&projectId != null) {
				String ticket = Cache.write("codeString", codeString);
				Cache.write(ticket, "projectId", projectId);
				response.sendRedirect("https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?app_id="+Config.HUAMEI_ZFBAPPID+"&redirect_uri="+URLEncoder.encode(Config.ZFB_AUTHBACK+"/"+ticket,"UTF-8")+"&scope=auth_userinfo");
			} else {
				throw new Exception("Wrong codeString=["+codeString+"], projectId=["+projectId+"]");
			}
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
