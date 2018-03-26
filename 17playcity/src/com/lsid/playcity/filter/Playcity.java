package com.lsid.playcity.filter;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class Playcity implements Filter {

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
		String ip = AutoConfig.getremoteip(request);
		try {
			
			Map<String, Object> returnvalue = new HashMap<String, Object>();

			String data = (String) request.getAttribute("data");

			String[] d = data.split(AutoConfig.SPLIT);
			String eid = AutoConfig.geteid(d);
			
			Path gonefolder = Paths.get("gone").resolve(eid);
			if (!Files.exists(gonefolder)) {
				Files.createDirectories(gonefolder);
			}
			
			String uuid = AutoConfig.getuuid(d);
			String playid = AutoConfig.getplayid(d);
			String enc = AutoConfig.getenc(d);
			if (d[0].equals("self")) {
				returnvalue.put("credit", AutoConfig.incremented(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
				long total = AutoConfig.incremented(eid, playid, "count", playid,
						"puser" + AutoConfig.SPLIT_HBASE + AutoConfig.getfrom(d));
				returnvalue.put("totalorders", total);
				int itemsonepage = 10;
				int page = Integer.parseInt(request.getParameter("page"));
				long start = total - itemsonepage*page;
				long end = start - itemsonepage;
				if (end<0) {
					end = 0;
				}
				if (start>0) {
					List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(10);
					for (long i = start;i>end;i--) {
						String row = AutoConfig.cacheuserdata(eid, playid, "userdata",
								playid + AutoConfig.SPLIT_HBASE + i, "d");
						if (row.isEmpty()) {
							continue;
						}
						String one = "";
						String[] rowparts = row.split(AutoConfig.SPLIT_HBASE);
						if (rowparts.length==3) {
							one = AutoConfig.cacheuserdata(eid, rowparts[0], "prize", row, "p");
						} else {
							if (AutoConfig.config(eid, "lsid.fixcode").contains(DefaultCipher.dec(rowparts[0]))) {
								one = AutoConfig.cacheuserdata(eid, row, "prize", row, "p");
							} else {
								one = AutoConfig.cacheuserdata(eid, rowparts[0], "prize", row, "p");
							}
						}
						if (one.isEmpty()) {
							continue;
						}
						Map<String, Object> item = new HashMap<String, Object>();
						String[] parts = one.split("#");
		   				String from = parts[1];
			   			String activity = parts[21];
			   			String pool = parts[34];
			   			String prize = parts[35];
			   			String headimgurl = "unknown";
			   			String nick = "unknown";
			   			String playip = parts[24];
			   			String province = parts[25];
			   			String city = parts[26];
			   			String district = parts[27];
			   			String street = parts[28];
			   			String num = parts[29];
			   			String playplayid = parts[20];
			   			String lucktime = parts[33];
			   			
			   			try {
			   				headimgurl=String.valueOf(new ObjectMapper().readTree(new String(DefaultCipher.dec(parts[19]))).get("headimgurl").asText());
			   				nick=String.valueOf(new ObjectMapper().readTree(new String(DefaultCipher.dec(parts[19]))).get("nickname").asText());
			   			}catch(Exception e) {
		   					//do nothing
		   				}
						item.put("id", row);
						item.put("from", from);
						item.put("headimgurl", headimgurl);
						item.put("nick", nick);
						item.put("openid", playplayid);
						item.put("time", lucktime);
						item.put("ip", playip);
						item.put("addr", province+city+district+street+num);
						item.put("activity", URLDecoder.decode(AutoConfig.config(eid, "lsid.activity"+activity+".name"),"UTF-8"));
						item.put("poolid", pool);
						item.put("prizeid", URLDecoder.decode(AutoConfig.config(eid, "lsid.pool"+pool+".prize"+prize+".name"),"UTF-8"));
						item.put("sysreq", DefaultCipher.dec(AutoConfig.getsysrequire(parts)));
						if (parts.length>38) {
							item.put("userinput", DefaultCipher.dec(parts[38]));
						} else {
							item.put("userinput", "");
						}
						if (parts.length>40) {
							item.put("status", DefaultCipher.dec(parts[40]));
						} else {
							item.put("status", "");
						}
						items.add(item);
					}
					returnvalue.put("orderslist", items);
				}
				String headimgurl = "";
				String nickname = "";
				String city = "";
				if (AutoConfig.fromwx(data)) {
					String encuserinfo = AutoConfig.getencuserinfo(d);
					headimgurl = new ObjectMapper().readTree(new String(DefaultCipher.dec(encuserinfo)))
							.get("headimgurl").asText();
					nickname = URLEncoder.encode(new ObjectMapper().readTree(new String(DefaultCipher.dec(encuserinfo)))
							.get("nickname").asText(), "UTF-8");
					city = URLEncoder.encode(new ObjectMapper().readTree(new String(DefaultCipher.dec(encuserinfo)))
							.get("city").asText(), "UTF-8");
					if (request.getParameter("url4wxjssdk") != null
							&& !request.getParameter("url4wxjssdk").trim().isEmpty()
							&& !request.getParameter("url4wxjssdk").trim().equals("null")) {
						String wxsign = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.wx"),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")), "eid",
								eid, "url4wxjssdk", request.getParameter("url4wxjssdk"));
						returnvalue.put("wxsign", new ObjectMapper().readTree(wxsign));
					}
				}
				returnvalue.put("headimgurl", headimgurl);
				returnvalue.put("nickname", nickname);
				returnvalue.put("city", city);
				returnvalue.put("playid", AutoConfig.getplayid(d));
				
			} else if (d[0].equals("list")) {
				List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
				if (!AutoConfig.config(eid, "lsid.playcity.items").isEmpty()) {
					for (int i = 0; i < Integer.parseInt(AutoConfig.config(eid, "lsid.playcity.items"));i++) {
						if (AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".pts").isEmpty()) {
							continue;
						}
						Map<String, Object> item = new HashMap<String, Object>();
						item.put("id", i);
						item.put("name", AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".name"));
						item.put("detail", AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".detail"));
						item.put("img", AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".img"));
						item.put("pts", Integer.parseInt(AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".pts")));
						item.put("cost", AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".cost"));
						long gone = 0;
						if (Files.exists(gonefolder.resolve("item"+item.get("id")))) {
							gone = Files.size(gonefolder.resolve("item"+item.get("id")));
						}
						item.put("remaining", Long.parseLong(AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".remaining"))-gone);
						item.put("type", AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".type"));
						Long startmillis = Long.parseLong(AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".start"));
						Long endmillis = Long.parseLong(AutoConfig.config(eid, "lsid.poolexchange.prize"+i+".end"));
						item.put("start", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startmillis)));
						item.put("end", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(endmillis)));
						String status = "online";
						long current = System.currentTimeMillis();
						if (current > endmillis) {
							status = "after";
						}
						if (current < startmillis) {
							status = "before";
						}
						
						item.put("status", status);
						
						items.add(item);
					}
				}
				returnvalue.put("items", items);
			} else if (d[0].equals("exchange")) {
				if (Integer.parseInt(AutoConfig.config(eid, "lsid.playcity.items"))<=Integer.parseInt(request.getParameter("itemid"))) {
					throw new Exception("wrongid["+request.getParameter("itemid")+"]");
				}
				
				Long startmillis = Long.parseLong(AutoConfig.config(eid, "lsid.poolexchange.prize"+request.getParameter("itemid")+".start"));
				Long endmillis = Long.parseLong(AutoConfig.config(eid, "lsid.poolexchange.prize"+request.getParameter("itemid")+".end"));
				long current = System.currentTimeMillis();
				if (current > endmillis) {
					throw new Exception("after");
				}
				if (current < startmillis) {
					throw new Exception("before");
				}
				
				long gone = 0;
				if (Files.exists(gonefolder.resolve("item"+request.getParameter("itemid")))) {
					gone = Files.size(gonefolder.resolve("item"+request.getParameter("itemid")));
				}
				Long remaining = Long.parseLong(AutoConfig.config(eid, "lsid.poolexchange.prize"+request.getParameter("itemid")+".remaining"))-gone;
				
				if (Integer.parseInt(request.getParameter("amount"))<=0||Integer.parseInt(request.getParameter("amount"))>remaining) {
					throw new Exception("exceed["+remaining+"]");
				}
				
				if (request.getParameter("ADDR")==null||request.getParameter("ADDR").trim().isEmpty()||request.getParameter("ADDR").trim().equalsIgnoreCase("null")) {
					throw new Exception("missingADDR");
				}
				
				int need = Integer.parseInt(AutoConfig.config(eid, "lsid.poolexchange.prize"+request.getParameter("itemid")+".pts"))*Integer.parseInt(request.getParameter("amount"));
				Long havecredit = AutoConfig.incremented(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d));
				if (havecredit>=need) {
					AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.sync", playid),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.socketimeoutinsec")), "eid",
							eid, "sync", playid+"-"+havecredit);
					AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.sync", "item"+request.getParameter("itemid")+"-"+remaining),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.socketimeoutinsec")), "eid",
							eid, "sync", "item"+request.getParameter("itemid")+"-"+remaining);
					AutoConfig.incrementcache(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1*need);
					Files.write(gonefolder.resolve("item"+request.getParameter("itemid")), new byte[Integer.parseInt(request.getParameter("amount"))], StandardOpenOption.CREATE, StandardOpenOption.APPEND);
					String loc = null;
					String lng = request.getParameter("lng");
					String lat = request.getParameter("lat");
					if ((lng != null && !lng.trim().isEmpty() && !lng.trim().equals("null") && lat != null
							&& !lat.trim().isEmpty() && !lat.trim().equals("null"))) {
						loc = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.loc"),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.socketimeoutinsec")),
								"lng", lng, "lat", lat);
					} else {
						loc = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.loc"),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.socketimeoutinsec")),
								"ip", AutoConfig.getinip(d));
					}
					String nextdata = AutoConfig.datafterpoolid(data, "exchange", request.getHeader("User-Agent"), ip, loc, "exchange");
					nextdata = AutoConfig.datafterluck(nextdata, request.getParameter("itemid"), "ADDR");
					String userinputs = request.getParameter("ADDR")+",兑换数量:"+request.getParameter("amount");
					String datafinish = AutoConfig.datafinish(nextdata, userinputs);
					String row = enc + AutoConfig.SPLIT_HBASE + "exchange" + AutoConfig.SPLIT_HBASE + request.getParameter("itemid") + AutoConfig.SPLIT_HBASE + uuid + AutoConfig.SPLIT_HBASE + AutoConfig.getlucktime(nextdata.split(AutoConfig.SPLIT));
					String hash = enc;
					if (AutoConfig.config(eid, "lsid.fixcode").contains(DefaultCipher.dec(enc))) {
						hash = row;
					}
					AutoConfig.cacheuserdata(eid, hash, "prize", row, "p", datafinish);
					long seq = AutoConfig.incrementcachemore(eid, playid, "count", playid,
							"puser" + AutoConfig.SPLIT_HBASE + AutoConfig.getfrom(d), 1);
					AutoConfig.cacheuserdata(eid, playid, "userdata",
							playid + AutoConfig.SPLIT_HBASE + seq, "d", row);
					
				} else {
					throw new Exception("lackcredit");
				}
			} else {
				throw new Exception("invalidticketvalue");
			}
			returnvalue.put("result", "success");
			
			AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(returnvalue));
		} catch (Exception ex) {
			String reason = ex.getMessage();
			AutoConfig.log(ex,
					"Failed in processing request [" + request.getRequestURI() + "] due to below exception:");
			Map<String, String> result = new HashMap<String, String>();
			result.put("result", "fail");
			result.put("reason", reason);
			AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
}
