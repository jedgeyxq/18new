package com.lsid.qrenter.filter;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;
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

public class Wxauthuuidandinfo implements Filter {
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
			String data = AutoConfig.readticket(request.getParameter("dataticket"));
			String[] d = data.split("#");
			String enc = AutoConfig.getenc(d);
			long scantime = AutoConfig.getintime(d);
			String eid = AutoConfig.geteid(d);
			if (!AutoConfig.config((String) null, "lsid.eids").contains(eid)) {
				throw new Exception("wrongeid[" + eid + "]");
			}

			if (AutoConfig.config(eid, "fixedcode.h5play." + DefaultCipher.dec(enc)).isEmpty()) {
				throw new Exception("wrongcode[" + DefaultCipher.dec(enc) + "]");
			}

			String uuid = AutoConfig.innerpost(AutoConfig.rotation((String) null, "lsid.interface.wx"),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.wx.connectimeoutinsec")),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.wx.socketimeoutinsec")),
					new String[]{"eid", eid, "wxcode", request.getParameter("code"), "keyprefix", "lsid.uuwxid"});
			String nextdata = AutoConfig.databeforeinfo(data, uuid);
			String originalquerystring = "";
			if (request.getQueryString() != null) {
				originalquerystring = "&";
				String[] params = request.getQueryString().split("&");
				String[] var19 = params;
				int var18 = params.length;

				for (int var17 = 0; var17 < var18; ++var17) {
					String param = var19[var17];
					if (!param.startsWith("code")) {
						originalquerystring = originalquerystring + "&" + param;
					}
				}
			}

			AutoConfig.nocache(eid, "scan", enc + "_" + uuid + "_" + scantime, "s", nextdata);
			response.sendRedirect(AutoConfig.config((String) null, "lsid.host.userentry") + "wxscanauthinfo/" + eid
					+ "/" + DefaultCipher.dec(enc) + "?dataticket=" + AutoConfig.generateticket(nextdata, "fixedexpire")
					+ originalquerystring);
		} catch (Exception var20) {
			StringWriter errors = new StringWriter();
			var20.printStackTrace(new PrintWriter(errors));
			response.getWriter().write(errors.toString());
		}

	}

	public void init(FilterConfig arg0) throws ServletException {
	}
}