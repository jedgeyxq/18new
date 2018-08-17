package com.lsid.qrenter.filter;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;
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

public class Wxscanandauthuuid implements Filter {
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
		String ip = AutoConfig.getremoteip(request);
		String useragent = request.getHeader("User-Agent");

		try {
			String removefirstsplitter = request.getRequestURI().replaceFirst("/", "");
			String originaluri = removefirstsplitter
					.replaceFirst(removefirstsplitter.substring(0, removefirstsplitter.indexOf("/")), "");
			String eid = originaluri.substring(1, originaluri.lastIndexOf("/"));
			String code = originaluri.substring(originaluri.lastIndexOf("/") + 1);
			if (!AutoConfig.config((String) null, "lsid.eids").contains(eid)) {
				throw new Exception("wrongeid[" + eid + "]");
			}

			if (AutoConfig.config(eid, "fixedcode.h5play." + code).isEmpty()) {
				throw new Exception("wrongcode[" + code + "]");
			}

			String loc = AutoConfig.innerpost(AutoConfig.rotation((String) null, "lsid.interface.loc"),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.loc.connectimeoutinsec")),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.loc.socketimeoutinsec")),
					new String[]{"ip", ip});
			String enc = DefaultCipher.enc(code);
			String encna = "";
			String data = AutoConfig.dataweixininit(eid, enc, enc, encna, useragent, ip, loc);
			AutoConfig.incrementcache(eid, enc, "count", enc, "senc_" + AutoConfig.getfrom(data.split("#")), 1);
			response.sendRedirect(AutoConfig.config((String) null, "lsid.host.userentry") + "pubwxauth?appid="
					+ AutoConfig.config((String) null, "lsid.uuwxid.appid") + "&redirect_uri="
					+ URLEncoder.encode(AutoConfig.config((String) null, "lsid.host.userentry") + "wxauthuuidinfo"
							+ "?dataticket=" + AutoConfig.generateticket(data, "fixedexpire")
							+ (request.getQueryString() == null ? "" : "&" + request.getQueryString()), "UTF-8")
					+ "&response_type=code&scope=snsapi_base&state=" + (new Random()).nextInt(10000)
					+ "&connect_redirect=1#wechat_redirect");
		} catch (Exception var17) {
			StringWriter errors = new StringWriter();
			var17.printStackTrace(new PrintWriter(errors));
			response.getWriter().write(errors.toString());
		}

	}

	public void init(FilterConfig arg0) throws ServletException {
	}
}