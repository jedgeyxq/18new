package com.lsid.play.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Share implements Filter {
	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("XDomainRequestAllowed", "1");
		response.setContentType("application/json");
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		String ip = AutoConfig.getremoteip(request);
		String useragent = request.getHeader("User-Agent");
		Map<String, Object> returnvalue = new HashMap<String, Object>();

		try {
			String prefix = "";
			if (Files.exists(Paths.get("testprefix"), new LinkOption[0])) {
				List<String> lines = Files.readAllLines(Paths.get("testprefix"), Charset.forName("UTF-8"));
				if (lines != null && !lines.isEmpty()) {
					prefix = ((String) lines.get(0)).trim();
				}
			}

			String eid = request.getParameter("eid");
			if (!AutoConfig.config((String) null, "lsid.eids").contains(eid)) {
				throw new Exception("wrongeid[" + eid + "]");
			}

			String ticket = request.getParameter("ticket");
			String[] encuserinfosplitdataticket = AutoConfig.readticket(ticket).split("#");
			String encuserinfo = encuserinfosplitdataticket[0];
			String dataticket = null;
			if (encuserinfosplitdataticket.length == 2) {
				dataticket = encuserinfosplitdataticket[1];
			}

			if (encuserinfo == null || encuserinfo.trim().isEmpty()) {
				throw new Exception("invalidticket");
			}

			String myopenid = DefaultCipher
					.enc((new ObjectMapper()).readTree(DefaultCipher.dec(encuserinfo)).get("openid").asText());
			String myhead = (new ObjectMapper()).readTree(DefaultCipher.dec(encuserinfo)).get("headimgurl").asText();
			String mynick = (new ObjectMapper()).readTree(DefaultCipher.dec(encuserinfo)).get("nickname").asText();
			returnvalue.put("myopenid", myopenid);
			returnvalue.put("myhead", myhead);
			returnvalue.put("mynick", mynick);
			String otheropenid;
			if (request.getParameter("url4wxjssdk") != null) {
				otheropenid = AutoConfig.innerpost(AutoConfig.rotation((String) null, "lsid.interface.wx"),
						Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.wx.connectimeoutinsec")),
						Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.wx.socketimeoutinsec")),
						new String[]{"eid", eid, "url4wxjssdk", request.getParameter("url4wxjssdk")});
				returnvalue.put("wxsign", (new ObjectMapper()).readTree(otheropenid));
			}

			otheropenid = request.getParameter("otheropenid");
			if (AutoConfig.cacheuserdata(eid, myopenid, "shareprize", prefix + myopenid, "u").isEmpty()) {
				AutoConfig.cacheuserdata(eid, myopenid, "shareprize", prefix + myopenid, "u", encuserinfo);
			}

			if (otheropenid != null && !otheropenid.trim().isEmpty() && !otheropenid.equals(myopenid)) {
				String encotheruserinfo = AutoConfig.cacheuserdata(eid, otheropenid, "shareprize", prefix + otheropenid,
						"u");
				String otherhead = (new ObjectMapper()).readTree(DefaultCipher.dec(encotheruserinfo)).get("headimgurl")
						.asText();
				String othernick = (new ObjectMapper()).readTree(DefaultCipher.dec(encotheruserinfo)).get("nickname")
						.asText();
				returnvalue.put("otheropenid", otheropenid);
				returnvalue.put("otherhead", otherhead);
				returnvalue.put("othernick", othernick);
				long fired = AutoConfig.incremented(eid, otheropenid, "shareprize", prefix + otheropenid, "c");
				if (fired >= 6L) {
					returnvalue.put("code", 4);
				} else if (AutoConfig.cacheuserdata(eid, myopenid, "shareprize", prefix + myopenid + otheropenid, "f")
						.isEmpty()) {
					if (request.getParameter("fired") != null) {
						long otherfired = AutoConfig.incrementcache(eid, otheropenid, "shareprize",
								prefix + otheropenid, "c", 1);
						AutoConfig.cacheuserdata(eid, myopenid, "shareprize", prefix + myopenid + otheropenid, "f",
								"fired");
						
						AutoConfig.cacheuserdata(eid, otheropenid, "shareprize", otheropenid + otherfired, "whofired",
								encuserinfo);
						
						returnvalue.put("code", 5);
						if (dataticket != null) {
							this.datarecord(dataticket, ip, useragent, request.getParameter("lng"),
									request.getParameter("lat"), "2", otheropenid);
							if (otherfired == 6L) {
								this.datarecord(dataticket, ip, useragent, request.getParameter("lng"),
										request.getParameter("lat"), "1", otheropenid);
							}
						}
					} else {
						returnvalue.put("code", 6);
						returnvalue.put("fired", fired);
					}
				} else {
					returnvalue.put("code", 7);
				}
			} else if (!AutoConfig.cacheuserdata(eid, myopenid, "shareprize", prefix + myopenid, "p").isEmpty()) {
				returnvalue.put("code", 3);
			} else {
				long fired = AutoConfig.incremented(eid, myopenid, "shareprize", prefix + myopenid, "c");
				if (fired < 6L) {
					returnvalue.put("code", 2);
					returnvalue.put("fired", fired);
				} else if (request.getParameter("prized") != null) {
					AutoConfig.cacheuserdata(eid, myopenid, "shareprize", prefix + myopenid, "p", "prized");
					returnvalue.put("code", 0);
					if (dataticket != null) {
						this.datarecord(dataticket, ip, useragent, request.getParameter("lng"),
								request.getParameter("lat"), "0", "");
					}
				} else {
					Vector<Map<String, String>> whofired = new Vector<Map<String, String>>();
					for (int i =1;i<7;i++) {
						try {
							String encotheruserinfo = AutoConfig.cacheuserdata(eid, myopenid, "shareprize", myopenid + i, "whofired");
							Map<String, String> info = new HashMap<String, String>();
							String openid = DefaultCipher
									.enc((new ObjectMapper()).readTree(DefaultCipher.dec(encotheruserinfo)).get("openid").asText());
							String head = (new ObjectMapper()).readTree(DefaultCipher.dec(encotheruserinfo)).get("headimgurl").asText();
							String nick = (new ObjectMapper()).readTree(DefaultCipher.dec(encotheruserinfo)).get("nickname").asText();
							returnvalue.put("openid", DefaultCipher.enc(openid));
							returnvalue.put("head", head);
							returnvalue.put("nick", nick);
							whofired.add(info);
						}catch(Exception e) {
							break;
						}
					}
					if (whofired.size()==6) {
						returnvalue.put("whofired", whofired);
					}
					
					List<String> paramvalueslist = new ArrayList<String>();
					paramvalueslist.add("eid");
					paramvalueslist.add(eid);
					paramvalueslist.add("openid");
					paramvalueslist.add(DefaultCipher.dec(myopenid));
					String[] cardids = AutoConfig.config(eid, "share.prize.wxcardid").split("#");
					String[] var26 = cardids;
					int var25 = cardids.length;

					for (int var24 = 0; var24 < var25; ++var24) {
						String cardid = var26[var24];
						paramvalueslist.add("cardid");
						paramvalueslist.add(cardid);
					}

					String[] paramvalues = new String[paramvalueslist.size()];
					paramvalueslist.toArray(paramvalues);
					String cardsign = AutoConfig.innerpost(AutoConfig.rotation((String) null, "lsid.interface.wx"),
							Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.wx.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.wx.socketimeoutinsec")),
							paramvalues);
					returnvalue.put("code", 1);
					returnvalue.put("coupon", (new ObjectMapper()).readTree(cardsign));
				}
			}

			returnvalue.put("result", "success");
			response.getWriter().write((new ObjectMapper()).writeValueAsString(returnvalue));
		} catch (Exception var27) {
			StringWriter errors = new StringWriter();
			var27.printStackTrace(new PrintWriter(errors));
			returnvalue.put("result", "fail");
			if (returnvalue.get("code") == null) {
				returnvalue.put("code", 8);
			}

			returnvalue.put("request",
					request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
			returnvalue.put("reason", var27.getMessage());
			returnvalue.put("detail", errors.toString());
			response.getWriter().write((new ObjectMapper()).writeValueAsString(returnvalue));
		}

	}

	public void init(FilterConfig arg0) throws ServletException {
	}

	private void datarecord(String dataticket, String ip, String useragent, String lng, String lat, String actionid,
			String otheropenid) throws Exception {
		String data = AutoConfig.readticket(dataticket);
		String[] d = data.split("#");
		String enc = AutoConfig.getenc(d);
		String poolid = "actions";
		String prizeid = "0";
		String uuid = AutoConfig.getuuid(d);
		String eid = AutoConfig.geteid(d);
		String loc = null;
		if (lng != null && !lng.trim().isEmpty() && !lng.trim().equals("null") && lat != null && !lat.trim().isEmpty()
				&& !lat.trim().equals("null")) {
			loc = AutoConfig.innerpost(AutoConfig.rotation((String) null, "lsid.interface.loc"),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.loc.connectimeoutinsec")),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.loc.socketimeoutinsec")),
					new String[]{"lng", lng, "lat", lat});
		} else {
			loc = AutoConfig.innerpost(AutoConfig.rotation((String) null, "lsid.interface.loc"),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.loc.connectimeoutinsec")),
					Integer.parseInt(AutoConfig.config((String) null, "lsid.interface.loc.socketimeoutinsec")),
					new String[]{"ip", AutoConfig.getinip(d)});
		}

		String datafterpoolid = AutoConfig.datafterpoolid(data, otheropenid, useragent, ip, loc, poolid);
		String datafterluck = AutoConfig.datafterluck(datafterpoolid, actionid, "");
		String row = enc + "_" + poolid + "_" + prizeid + "_" + uuid + "_"
				+ AutoConfig.getlucktime(datafterluck.split("#"));
		String datafinish = AutoConfig.datafinish(datafterluck, "finished");
		AutoConfig.cacheuserdata(eid, row, "prize", row, "p", datafinish);
	}
	
	
}