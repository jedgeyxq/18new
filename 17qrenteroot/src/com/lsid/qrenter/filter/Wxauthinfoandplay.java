package com.lsid.qrenter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Wxauthinfoandplay implements Filter {
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
			String dataticket = request.getParameter("dataticket");
			String data = AutoConfig.readticket(dataticket);
			String[] d = data.split("#");
			String eid = AutoConfig.geteid(d);
			String enc = AutoConfig.getenc(d);
			Long scantime = AutoConfig.getintime(d);
			String from = AutoConfig.getfrom(d);
			String uuid = null;
			if (!AutoConfig.config((String) null, "lsid.eids").contains(eid)) {
				throw new Exception("wrongeid[" + eid + "]");
			}

			if (AutoConfig.config(eid, "fixedcode.h5play." + DefaultCipher.dec(enc)).isEmpty()) {
				throw new Exception("wrongcode[" + DefaultCipher.dec(enc) + "]");
			}

			String nextdata = data;
			String encuserinfo = AutoConfig.innerpost(AutoConfig.rotation((String) null, "lsid.interface.wx"),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.wx.connectimeoutinsec")),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.wx.socketimeoutinsec")),
					new String[]{"eid", eid, "wxcode", request.getParameter("code")});
			String encplayid = DefaultCipher
					.enc((new ObjectMapper()).readTree(DefaultCipher.dec(encuserinfo)).get("openid").asText());

			try {
				uuid = AutoConfig.getuuid(d);
			} catch (Exception var21) {
				uuid = encplayid;
				nextdata = AutoConfig.databeforeinfo(data, encplayid);
				AutoConfig.nocache(eid, "scan", enc + "_" + encplayid + "_" + scantime, "s", nextdata);
			}

			nextdata = AutoConfig.datafterinfo(nextdata, encuserinfo);
			nextdata = AutoConfig.datafterplayid(nextdata, encplayid);
			d = nextdata.split("#");
			AutoConfig.cacheuserdata(eid, uuid, "userinfo", uuid, from, AutoConfig.getencuserinfo(d) + "#" + encplayid);
			AutoConfig.incrementcache(eid, encplayid, "count", encplayid, "suser_" + AutoConfig.getfrom(d), 1);
			String activityid = DefaultCipher.dec(enc);
			nextdata = AutoConfig.datafteractivityid(nextdata, activityid);
			AutoConfig.nocache(eid, "scan", enc + "_" + uuid + "_" + scantime, "s", nextdata);
			dataticket = AutoConfig.generateticket(nextdata, "refreshable");
			String ticket = AutoConfig.generateticket(encuserinfo + "#" + dataticket, "refreshable");
			String[] startend = AutoConfig.config(eid, "fixedcode.h5play." + DefaultCipher.dec(enc) + ".active")
					.split("#");
			Date curr = new Date();
			if (curr.after(new Date(Long.parseLong(startend[0])))
					&& curr.before(new Date(Long.parseLong(startend[1])))) {
				response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")
						+ AutoConfig.config(eid, "fixedcode.h5play." + DefaultCipher.dec(enc)) + "ticket=" + ticket
						+ "&eid=" + eid + (request.getQueryString() != null ? "&" + request.getQueryString() : ""));
			} else if (curr.before(new Date(Long.parseLong(startend[0])))) {
				response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")
						+ AutoConfig.config(eid, "fixedcode.h5play." + DefaultCipher.dec(enc) + ".beforepage"));
			} else if (curr.after(new Date(Long.parseLong(startend[1])))) {
				response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")
						+ AutoConfig.config(eid, "fixedcode.h5play." + DefaultCipher.dec(enc) + ".afterpage"));
			}
		} catch (Exception var22) {
			StringWriter errors = new StringWriter();
			var22.printStackTrace(new PrintWriter(errors));
			response.getWriter().write(errors.toString());
		}

	}

	public void init(FilterConfig arg0) throws ServletException {
	}

	public static void main(String[] s) {
		System.out.println(new Date(1537891199000L));
	}
}