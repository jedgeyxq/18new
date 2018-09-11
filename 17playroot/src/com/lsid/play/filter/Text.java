package com.lsid.play.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;
import com.lsid.util.SensitiveWords;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

public class Text implements Filter {
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
			String eid = request.getParameter("eid");
			if (!AutoConfig.config((String) null, "lsid.eids").contains(eid)) {
				throw new Exception("wrongeid[" + eid + "]");
			}
			String queryrow = request.getParameter("queryrow");
			if (!AutoConfig.config(eid, "fixedcode.h5play.iamaijitext.queryrow").equals(queryrow)) {
				throw new Exception("wrongqueryrow");
			}
			
			String ticket = request.getParameter("ticket");
			String reviewpass = request.getParameter("pass");
			if (reviewpass!=null&&!reviewpass.trim().isEmpty()) {//review
				if (AutoConfig.config(eid, "fixedcode.h5play.iamaijitext.pass").equals(reviewpass)) {
					if ("passall".equals(request.getParameter("action"))) {
						AutoConfig.cacheuserdata(eid, queryrow, "sendtext", queryrow, "reviewfrom", String.valueOf(Long.MAX_VALUE));
					} else if ("startreview".equals(request.getParameter("action"))) {
						long total = AutoConfig.incremented(eid, queryrow, "sendtext", queryrow, "numoftexts");
						AutoConfig.cacheuserdata(eid, queryrow, "sendtext", queryrow, "reviewfrom",String.format("%019d", total));
					} else if (request.getParameter("passtill")!=null) {
						AutoConfig.cacheuserdata(eid, queryrow, "sendtext", queryrow, "reviewfrom",String.format("%019d", Long.parseLong(request.getParameter("passtill"))));
					} else if (request.getParameter("openid")!=null&&request.getParameter("time")!=null) {
						AutoConfig.cacheuserdata(eid, request.getParameter("openid"), "sendtext", 
								request.getParameter("openid")+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMddHH").format(new Date(Long.parseLong(request.getParameter("time")))), "review", "rejected");
						returnvalue.put("openid", request.getParameter("openid"));
					} else {
						long reviewfrom = Long.MAX_VALUE;
						try{
							reviewfrom = Long.parseLong(AutoConfig.cacheuserdata(eid, queryrow, "sendtext", queryrow, "reviewfrom"));
						}catch(Exception e) {
							//do nothing
						}
						if (reviewfrom < Long.MAX_VALUE) {
							long textstart = -1;
							long numoftexts = -1;
							try {
								textstart = Long.parseLong(request.getParameter("reviewstart"));
								numoftexts = Long.parseLong(request.getParameter("numofreviews"));
							}catch(Exception e) {
								textstart = reviewfrom;
								numoftexts = 1;
							}
							if (textstart<reviewfrom) {
								textstart=reviewfrom;
							}
							if (textstart>=0&&numoftexts>0) {
								Vector<Map<String, String>> texts = new Vector<Map<String, String>>((int) (numoftexts));
								int numoftextsread=0;
								String firstsentinhour = null;
								String nextsentinhour = null;
									while (texts.size()<numoftexts) {
										String enuserinfosplittextsplittimesplitreview = AutoConfig.cacheuserdata(eid, queryrow+AutoConfig.SPLIT_HBASE+(textstart+numoftextsread), "sendtext", queryrow+AutoConfig.SPLIT_HBASE+(textstart+numoftextsread), "text");
										if (enuserinfosplittextsplittimesplitreview!=null&&!enuserinfosplittextsplittimesplitreview.trim().isEmpty()) {
											try {
												Map<String, String> text = new HashMap<String, String>();
												String[] s = enuserinfosplittextsplittimesplitreview.split(AutoConfig.SPLIT);
												String encuserinfo = s[0];
												String t = URLDecoder.decode(s[1],"UTF-8");
												text.put("openid", DefaultCipher
														.enc(new ObjectMapper().readTree(DefaultCipher.dec(encuserinfo)).get("openid").asText()));
												nextsentinhour = new SimpleDateFormat("yyyyMMddHH").format(new Date(Long.parseLong(s[2])));
												if (firstsentinhour==null) {
													firstsentinhour = new String(nextsentinhour);
												}
												if (!firstsentinhour.equals(nextsentinhour)) {
													break;
												}
												if (!"rejected".equals(AutoConfig.cacheuserdata(eid, text.get("openid"), "sendtext", 
														text.get("openid")+AutoConfig.SPLIT_HBASE+nextsentinhour, "review"))) {
													text.put("head", new ObjectMapper().readTree(DefaultCipher.dec(encuserinfo)).get("headimgurl").asText());
													text.put("nick", new ObjectMapper().readTree(DefaultCipher.dec(encuserinfo)).get("nickname").asText());
													text.put("time", s[2]);
													text.put("seq", String.valueOf(textstart+numoftextsread));
													t=t.replaceAll("<", "&lt;");
													t=t.replaceAll(">", "&gt;");
													text.put("text", t);
													texts.add(text);
												}
											}catch(Exception e) {
												//ignore wrong data
											}
											numoftextsread++;
											
										} else {
											break;
										}
									}
								
								
								returnvalue.put("code", 7);
								returnvalue.put("texts", texts);
								returnvalue.put("textstart", textstart+numoftextsread);
							} else {
								throw new Exception("wrongrangereview["+textstart+"]["+numoftexts+"]");
							}
						}
					}
				} else {
					throw new Exception("wrongpass["+reviewpass+"]");
				}
			} else if (ticket==null||ticket.trim().isEmpty()) {//query
				long textstart = -1;
				long numoftexts = -1;
				try {
					textstart = Long.parseLong(request.getParameter("textstart"));
					numoftexts = Long.parseLong(request.getParameter("numoftexts"));
				}catch(Exception e) {
					textstart = AutoConfig.incremented(eid, queryrow, "sendtext", queryrow, "numoftexts");
					numoftexts = 1;
					if (textstart>0) {
						textstart = textstart - 1;
					}
				}
				
				if (textstart>=0&&numoftexts>0) {
					long reviewfrom = Long.MAX_VALUE;
					try{
						reviewfrom = Long.parseLong(AutoConfig.cacheuserdata(eid, queryrow, "sendtext", queryrow, "reviewfrom"));
					}catch(Exception e) {
						//do nothing
					}
					Vector<Map<String, String>> texts = new Vector<Map<String, String>>((int) (numoftexts));
					int numoftextsread=0;
					if (reviewfrom>textstart) {
						while (texts.size()<numoftexts&&textstart+numoftextsread<reviewfrom) {
							String enuserinfosplittextsplittimesplitreview = AutoConfig.cacheuserdata(eid, queryrow+AutoConfig.SPLIT_HBASE+(textstart+numoftextsread), "sendtext", queryrow+AutoConfig.SPLIT_HBASE+(textstart+numoftextsread), "text");
							if (enuserinfosplittextsplittimesplitreview!=null&&!enuserinfosplittextsplittimesplitreview.trim().isEmpty()) {
								try {
									Map<String, String> text = new HashMap<String, String>();
									String[] s = enuserinfosplittextsplittimesplitreview.split(AutoConfig.SPLIT);
									String encuserinfo = s[0];
									String t = URLDecoder.decode(s[1],"UTF-8");
									text.put("openid", DefaultCipher
											.enc(new ObjectMapper().readTree(DefaultCipher.dec(encuserinfo)).get("openid").asText()));
									if (!"rejected".equals(AutoConfig.cacheuserdata(eid, text.get("openid"), "sendtext", 
											text.get("openid")+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMddHH").format(new Date(Long.parseLong(s[2]))), "review"))) {
										text.put("head", new ObjectMapper().readTree(DefaultCipher.dec(encuserinfo)).get("headimgurl").asText());
										text.put("nick", new ObjectMapper().readTree(DefaultCipher.dec(encuserinfo)).get("nickname").asText());
										text.put("time", s[2]);
										text.put("seq", String.valueOf(textstart+numoftextsread));
										t=t.replaceAll("<", "&lt;");
										t=t.replaceAll(">", "&gt;");
										text.put("text", t);
										texts.add(text);
									}
								}catch(Exception e) {
									//ignore wrong data
								}
								numoftextsread++;
							} else {
								break;
							}
						}
					}
					
					returnvalue.put("code", 0);
					returnvalue.put("texts", texts);
					returnvalue.put("textstart", textstart+numoftextsread);
				} else {
					throw new Exception("wrongrangequery["+textstart+"]["+numoftexts+"]");
				}
			} else {//send
				String text = request.getParameter("text");
				
				if (text == null || text.trim().isEmpty()||text.length()>Integer.parseInt(AutoConfig.config(eid, "fixedcode.h5play.iamaijitext.maxlength"))||SensitiveWords.contains(text)) {
					returnvalue.put("code", 1);
					throw new Exception("wrongtext");
				}
			
				String[] encuserinfosplitdataticket = AutoConfig.readticket(ticket).split("#");
				String encuserinfo = encuserinfosplitdataticket[0];
				String dataticket = null;
				if (encuserinfosplitdataticket.length == 2) {
					dataticket = encuserinfosplitdataticket[1];
				}

				if (encuserinfo == null || encuserinfo.trim().isEmpty()||dataticket == null || dataticket.trim().isEmpty()) {
					throw new Exception("invalidticket");
				}
				String openid = DefaultCipher
						.enc(new ObjectMapper().readTree(DefaultCipher.dec(encuserinfo)).get("openid").asText());
				
				if ("rejected".equals(AutoConfig.cacheuserdata(eid, openid, "sendtext", 
						openid+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMddHH").format(new Date()), "review"))) {
					returnvalue.put("code", 6);
					throw new Exception("rejectedid["+openid+"]");
				}

				if (AutoConfig.incremented(eid, openid, "sendtext", 
						openid+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMdd").format(new Date()), "countperid")>=Long.parseLong(AutoConfig.config(eid, "fixedcode.h5play.iamaijitext.maxperidperday"))) {
					returnvalue.put("code", 2);
					throw new Exception("limitid["+openid+"]");
				}
				AutoConfig.incrementcache(eid, openid, "sendtext", 
						openid+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMdd").format(new Date()), "countperid", 1);
				if (AutoConfig.incremented(eid, ip, "sendtext", 
						ip+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMdd").format(new Date()), "countperip")>=Long.parseLong(AutoConfig.config(eid, "fixedcode.h5play.iamaijitext.maxperipperday"))) {
					returnvalue.put("code", 3);
					throw new Exception("limitip["+ip+"]");
				}
				AutoConfig.incrementcache(eid, ip, "sendtext", 
						ip+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMdd").format(new Date()), "countperip", 1);
				
				long position = AutoConfig.incrementcache(eid, queryrow, "sendtext", queryrow, "numoftexts", 1);
				AutoConfig.cacheuserdata(eid, queryrow+AutoConfig.SPLIT_HBASE+(position-1), "sendtext", queryrow+AutoConfig.SPLIT_HBASE+(position-1), "text", encuserinfo+AutoConfig.SPLIT+URLEncoder.encode(text,"UTF-8")+AutoConfig.SPLIT+System.currentTimeMillis()+AutoConfig.SPLIT+"pending");
				this.datarecord(dataticket, ip, useragent, request.getParameter("lng"),
						request.getParameter("lat"), "3", "");
				returnvalue.put("code", 4);
			}
			returnvalue.put("result", "success");
			response.getWriter().write((new ObjectMapper()).writeValueAsString(returnvalue));
		} catch (Exception var27) {
			StringWriter errors = new StringWriter();
			var27.printStackTrace(new PrintWriter(errors));
			returnvalue.put("result", "fail");
			if (returnvalue.get("code") == null) {
				returnvalue.put("code", 5);
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