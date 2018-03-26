package com.lsid.play.filter;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

public class Play implements Filter {

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
			String uuid = AutoConfig.getuuid(d);
			String playid = AutoConfig.getplayid(d);
			String enc = AutoConfig.getenc(d);
			String code = DefaultCipher.dec(enc);
			boolean isfixcode = AutoConfig.config(eid, "lsid.fixcode").contains(code);
			String encproda = AutoConfig.getencprodainfo(d);
			String encprodna = AutoConfig.getencprodnainfo(d);
			String useragent = request.getHeader("User-Agent");
			String loc = null;
			
			if (AutoConfig.playfirst(d)) {// play ticket (refreshable)//ticket
				List<Map<String, String>> pools = new ArrayList<Map<String, String>>();
				List<Map<String, Object>> prs = new ArrayList<Map<String, Object>>();
				if (!"a,c,p,y,t,r,h,q".contains(eid)
						|| isfixcode ||
						AutoConfig
								.cachecodedata(eid, enc, "oldprize",
										enc + AutoConfig.SPLIT_HBASE + "-1" + AutoConfig.SPLIT_HBASE + "-1", "p")
								.isEmpty()) {
					if (AutoConfig.more(data)) {// more interface ticket (refreshable) 
						Enumeration<String> params = request.getParameterNames();
						List<String> paramvalues4more = new ArrayList<String>(10);
						while (params.hasMoreElements()) {
							String param = params.nextElement();
							paramvalues4more.add(param);
							paramvalues4more.add(URLEncoder.encode(request.getParameter(param), "UTF-8"));
						}
						String moresult = null;
						String url = null;
						if (AutoConfig.config(eid, "lsid.interface.playmore.hashfield").equals("code")&&
								!isfixcode) {
							url = AutoConfig.hash(eid, "lsid.interface.playmore", enc);
						} else {
							url = AutoConfig.hash(eid, "lsid.interface.playmore", uuid);
						}
						String[] paramvalues = new String[paramvalues4more.size()];
						paramvalues4more.toArray(paramvalues);
						moresult = AutoConfig.innerpost(url,
								Integer.parseInt(AutoConfig.config(eid, "lsid.interface.playmore.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(eid, "lsid.interface.playmore.socketimeoutinsec")),
								paramvalues);
						returnvalue.put("more", new ObjectMapper().readTree(moresult));
						returnvalue.put("result", "success");

					} else {
						String lng = request.getParameter("lng");
						String lat = request.getParameter("lat");
						String poolids = "";
						if (isfixcode) {
							poolids=AutoConfig.config(eid, "lsid.fixcode.pool."+code);
						} else {//绑定奖池
							poolids=AutoConfig.cachecodedata(eid, enc, "once", enc, "po");
							if (poolids.isEmpty()) {
								String type = "pool";
								String from = AutoConfig.getfrom(d);
								String encuserinfo = AutoConfig.getencuserinfo(d);
								if ((lng != null && !lng.trim().isEmpty() && !lng.trim().equals("null") && lat != null
										&& !lat.trim().isEmpty() && !lat.trim().equals("null"))) {
									poolids = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.rule"),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.rule.connectimeoutinsec")),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.rule.socketimeoutinsec")),
											"eid", eid, "type", type, "from", from, "encproda", encproda, "encprodna",
											encprodna, "enc", enc, "encuserinfo", encuserinfo, "openid", uuid, "lng", lng,
											"lat", lat);
								} else {
									poolids = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.rule"),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.rule.connectimeoutinsec")),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.rule.socketimeoutinsec")),
											"eid", eid, "type", type, "from", from, "encproda", encproda, "encprodna",
											encprodna, "enc", enc, "encuserinfo", encuserinfo, "openid", uuid, "ip", ip);
								}
								AutoConfig.cachecodedata(eid, enc, "once", enc, "po", poolids);
							}
						}
						//获取localtion
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
						String[] locs = loc.split(AutoConfig.SPLIT);
						returnvalue.put("lng", locs[locs.length - 2]);
						returnvalue.put("lat", locs[locs.length - 1]);

						for (String poolid : poolids.split(AutoConfig.SPLIT)) {
							if ("true".equals(AutoConfig.config(eid, "lsid.pool" + poolid + ".ticket"))) {
								String event = "xeventy";
								if (request.getParameter("event")!=null&&!request.getParameter("event").trim().isEmpty()&&!request.getParameter("event").trim().equalsIgnoreCase("null")) {
									event = request.getParameter("event").trim();
								}
								String datafterpoolid = AutoConfig.datafterpoolid(data, event,
										useragent, ip, loc, poolid);
								//奖池重复抽 链接
								if (AutoConfig.config(eid, "lsid.pool" + poolid + ".repeat").equals("true")) {
									Map<String, String> po = new HashMap<String, String>();
									po.put("id", poolid);
									String ticket = AutoConfig.generateticket(datafterpoolid, "refreshable");
									po.put("ticket", ticket);
									po.put("type", AutoConfig.config(eid, "lsid.pool" + poolid + ".type"));
									po.put("require", AutoConfig.config(eid, "lsid.pool" + poolid + ".require"));
									pools.add(po);
								} else {
									//红包 一次奖池
									String prizes = AutoConfig.cacheuserdata(eid, enc, "once",
											enc + AutoConfig.SPLIT_HBASE + poolid, "pr");
									//是否抽过奖
									if (prizes.isEmpty()) {
										Map<String, String> po = new HashMap<String, String>();
										po.put("id", poolid);
										String ticket = AutoConfig.generateticket(datafterpoolid, "onetime");
										po.put("ticket", ticket);
										po.put("type", AutoConfig.config(eid, "lsid.pool" + poolid + ".type"));
										po.put("require", AutoConfig.config(eid, "lsid.pool" + poolid + ".require"));
										pools.add(po);
									} else {
										String[] prize = prizes.split(AutoConfig.SPLIT);
										for (String prizeid : prize) {
											String datafterluck = AutoConfig.cacheuserdata(eid, enc, "prize",
													enc + AutoConfig.SPLIT_HBASE + poolid + AutoConfig.SPLIT_HBASE
															+ prizeid,
													"p");
											
											//没领奖品
											if (!AutoConfig.finished(datafterluck)) {
												Map<String, Object> pr = new HashMap<String, Object>();
												pr.put("id", prizeid);
												pr.put("poolid", poolid);
												pr.put("type", AutoConfig.config(eid,
														"lsid.pool" + poolid + ".prize" + prizeid + ".type"));
												pr.put("pooltype",
														AutoConfig.config(eid, "lsid.pool" + poolid + ".type"));
												pr.put("value", AutoConfig.config(eid,
														"lsid.pool" + poolid + ".prize" + prizeid + ".value"));
												pr.put("amount", AutoConfig.config(eid,
														"lsid.pool" + poolid + ".prize" + prizeid + ".amount"));
												pr.put("require", AutoConfig.config(eid,
														"lsid.pool" + poolid + ".prize" + prizeid + ".require"));
												pr.put("desc", AutoConfig.config(eid,
														"lsid.pool" + poolid + ".prize" + prizeid + ".desc"));
												String ticket = AutoConfig.generateticket(datafterluck, "onetime");
												pr.put("ticket", ticket);
												prs.add(pr);
											} else if (AutoConfig.finished(datafterluck)) {
												//如果不是666大奖
												String[] requires = AutoConfig.config(eid,
														"lsid.pool" + poolid + ".prize" + prizeid + ".require").split(AutoConfig.SPLIT);
												if (requires.length>1) {
													String userinputs = DefaultCipher.dec(datafterluck.split(AutoConfig.SPLIT)[38]);
													for (int i=0;i<requires.length;i++) {
														if (!userinputs.contains(":"+requires[i]+":")) {
															Map<String, Object> pr = new HashMap<String, Object>();
															pr.put("id", prizeid);
															pr.put("poolid", poolid);
															pr.put("type", AutoConfig.config(eid,
																	"lsid.pool" + poolid + ".prize" + prizeid + ".type"));
															pr.put("pooltype",
																	AutoConfig.config(eid, "lsid.pool" + poolid + ".type"));
															pr.put("value", AutoConfig.config(eid,
																	"lsid.pool" + poolid + ".prize" + prizeid + ".value"));
															pr.put("amount", AutoConfig.config(eid,
																	"lsid.pool" + poolid + ".prize" + prizeid + ".amount"));
															pr.put("require", requires[i]);
															pr.put("desc", AutoConfig.config(eid,
																	"lsid.pool" + poolid + ".prize" + prizeid + ".desc"));
															String ticket = AutoConfig.generateticket(datafterluck, "onetime");
															pr.put("ticket", ticket);
															prs.add(pr);
															break;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				returnvalue.put("prizes", prs);
				returnvalue.put("pools", pools);

				if (!AutoConfig.config(eid, "lsid.interface.playmore").isEmpty()) {
					Map<String, String> m = new HashMap<String, String>();
					m.put("ticket", AutoConfig.generateticket(AutoConfig.datamore(data), "refreshable"));
					returnvalue.put("more", m);
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
				returnvalue.put("playid", playid);
				if (!isfixcode) {
					returnvalue.put("countenc", AutoConfig.incremented(eid, enc, "count", enc,
							"penc" + AutoConfig.SPLIT_HBASE + AutoConfig.getfrom(d)));
				}
				String prodid = "";
				String activedate = "";
				if (!encproda.isEmpty()&&!isfixcode) {
					prodid = DefaultCipher.dec(encproda).split(AutoConfig.SPLIT)[Integer
							.parseInt(AutoConfig.config(eid, "lsid.code.proda.index"))];
					if ("repeat".equals(prodid)) {
						activedate = "详情请见包装 详情请见包装";
					} else {
						activedate = DefaultCipher.dec(encproda).split(AutoConfig.SPLIT)[Integer
								.parseInt(AutoConfig.config(eid, "lsid.code.timea.index"))];
					}
				}
				returnvalue.put("prodid", prodid);
				returnvalue.put("activedate", activedate);
				returnvalue.put("eid", eid);
				returnvalue.put("credit", AutoConfig.incremented(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
				returnvalue.put("result", "success");
				
				if (eid.equals("t2")){
					
					String tasks7ticket = request.getParameter("7tasks");
					if (tasks7ticket!=null&&!tasks7ticket.trim().isEmpty()){
						int min = 0;
						try{
							min = Integer.parseInt(AutoConfig.readticket(tasks7ticket));
						}catch(Exception e){
							//do nothing
						}
						if (min<1){
							throw new Exception("invalid7tasks");
						}
						try{
							AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.sync", playid),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.connectimeoutinsec")),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.socketimeoutinsec")), "eid",
									eid, "sync", playid+"-"+AutoConfig.config(eid, "lsid.7tasks.poolid")+"-"+min);
						}catch(Exception e){
							returnvalue.put("7tasksresult", "-1");
						}
						if (!"-1".equals(returnvalue.get("7tasksresult"))){
							String prizeid = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.luck.client"),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.luck.client.connectimeoutinsec")),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.luck.client.socketimeoutinsec")),
									"eid", eid, "poolid", AutoConfig.config(eid, "lsid.7tasks.poolid"));
							
							String datafterpoolid=AutoConfig.datafterpoolid(data, "7tasks",
									useragent, ip, loc, AutoConfig.config(eid, "lsid.7tasks.poolid"));
							
							String datafterluck = AutoConfig.datafterluck(datafterpoolid, prizeid, "");
							
							long seq = AutoConfig.incrementcachemore(eid, playid, "count", playid,
									"puser" + AutoConfig.SPLIT_HBASE + AutoConfig.getfrom(d), 1);
							
							String row = enc + AutoConfig.SPLIT_HBASE + AutoConfig.config(eid, "lsid.7tasks.poolid") + AutoConfig.SPLIT_HBASE + prizeid + 
									AutoConfig.SPLIT_HBASE + uuid + AutoConfig.SPLIT_HBASE
										+ AutoConfig.getlucktime(datafterluck.split(AutoConfig.SPLIT));
							
							String status = "finished";
							String datafinish = AutoConfig.datafinish(datafterluck, status);
							AutoConfig.cacheuserdata(eid, enc, "prize", row, "p", datafinish);
							
							AutoConfig.cacheuserdata(eid, playid, "userdata",
									playid + AutoConfig.SPLIT_HBASE + seq, "d", row);
							
							AutoConfig.incrementcache(eid, playid, "count", playid, 
									"task0"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1);
							AutoConfig.incrementcache(eid, playid, "count", playid, 
									"task1"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1);
							AutoConfig.incrementcache(eid, playid, "count", playid, 
									"task2"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1);
							AutoConfig.incrementcache(eid, playid, "count", playid, 
									"task3"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1);
							AutoConfig.incrementcache(eid, playid, "count", playid, 
									"task4"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1);
							AutoConfig.incrementcache(eid, playid, "count", playid, 
									"task5"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1);
							AutoConfig.incrementcache(eid, playid, "count", playid, 
									"task6"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1);
							
							if (AutoConfig.config(eid, "lsid.pool" + AutoConfig.config(eid, "lsid.7tasks.poolid") + ".prize" + prizeid + ".type").equals("cash")) {
								if (AutoConfig.fromwx(data)) {
									AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.wx", enc),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),
											"eid", eid, "orderid", row);
								}
							}
							
							returnvalue.put("7tasksresult", 
									AutoConfig.config(eid, "lsid.pool" + AutoConfig.config(eid, "lsid.7tasks.poolid") + ".prize" + prizeid + ".desc"));
						}
					} else {
						returnvalue.put("7tasksresult", "none");
					}
					
					Map<String, Long> tasks = new HashMap<String, Long>();
					tasks.put("task0",AutoConfig.incremented(eid, playid, "count", playid, 
							"task0"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
					tasks.put("task1",AutoConfig.incremented(eid, playid, "count", playid, 
							"task1"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
					tasks.put("task2",AutoConfig.incremented(eid, playid, "count", playid, 
							"task2"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
					tasks.put("task3",AutoConfig.incremented(eid, playid, "count", playid, 
							"task3"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
					tasks.put("task4",AutoConfig.incremented(eid, playid, "count", playid, 
							"task4"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
					tasks.put("task5",AutoConfig.incremented(eid, playid, "count", playid, 
							"task5"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
					tasks.put("task6",AutoConfig.incremented(eid, playid, "count", playid, 
							"task6"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d)));
					returnvalue.put("tasks", tasks);
					long min = tasks.get("task0");
					if (tasks.get("task1")<min){
						min = tasks.get("task1");
					}
					if (tasks.get("task2")<min){
						min = tasks.get("task2");
					}
					if (tasks.get("task3")<min){
						min = tasks.get("task3");
					}
					if (tasks.get("task4")<min){
						min = tasks.get("task4");
					}
					if (tasks.get("task5")<min){
						min = tasks.get("task5");
					}
					if (tasks.get("task6")<min){
						min = tasks.get("task6");
					}
					
					if (min>0){
						returnvalue.put("7tasks", AutoConfig.generateticket(String.valueOf(min), "onetime"));
					} else {
						returnvalue.put("7tasks", "none");
					}
				}
				
			} else if (AutoConfig.playluck(d)) {// luck ticket (once/refreshable)			
				//奖池tickcket
				String poolid = AutoConfig.getpoolid(d);
				
				String active = AutoConfig.config(eid, "lsid.pool" + poolid + ".active");
				if (!active.isEmpty()
						&& System.currentTimeMillis() < Long.parseLong(active.split(AutoConfig.SPLIT)[0])) {
					throw new Exception("before");
				} 
				else if (!active.isEmpty()
						&& System.currentTimeMillis() > Long.parseLong(active.split(AutoConfig.SPLIT)[1])) {
					throw new Exception("after");
				}
				String prizes = "";
				if (!isfixcode) {
					prizes=AutoConfig.cacheuserdata(eid, enc, "once", enc + AutoConfig.SPLIT_HBASE + poolid, "pr");
				}

				if (prizes.isEmpty()) {
					if (!AutoConfig.config(eid, "lsid.pool"+poolid+".require").isEmpty()) {
						if (AutoConfig.config(eid, "lsid.pool"+poolid+".require").startsWith("credit")) {
							int need = Integer.parseInt(AutoConfig.config(eid, "lsid.pool"+poolid+".require").substring(6));
							Long havecredit = AutoConfig.incremented(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d));
							if (havecredit>=need) {
								//剩余积分只能抽一次
								AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.sync", playid),
										Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.connectimeoutinsec")),
										Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.socketimeoutinsec")), "eid",
										eid, "sync", playid+"-"+havecredit);
								AutoConfig.incrementcache(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),-1*need);
							} else {
								throw new Exception("lackcredit");
							}
						}
					}
					if (!AutoConfig.config(eid, "lsid.pool" + poolid + ".repeat").equals("true")||!AutoConfig.config(eid, "lsid.pool"+poolid+".require").isEmpty()) {
						String hash = enc;
						Long havecredit = 0l;
						if (!AutoConfig.config(eid, "lsid.pool"+poolid+".require").isEmpty()) {
							hash = playid;
						}
						if (AutoConfig.config(eid, "lsid.pool"+poolid+".require").startsWith("credit")) {
							havecredit = AutoConfig.incremented(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d));
						}
						// 只能抽一个奖池
						AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.sync", hash),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.socketimeoutinsec")), "eid",
								eid, "sync", hash+"-"+poolid+"-"+havecredit);
					}
					prizes = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.luck.client"),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.luck.client.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.luck.client.socketimeoutinsec")),
								"eid", eid, "poolid", poolid);
					
					if (eid.equals("t2")&&"task".equals(AutoConfig.config(eid, "lsid.pool"+poolid+".type"))&&
							AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizes + ".desc")!=null&&
									!AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizes + ".desc").trim().isEmpty()&&
									!"thankyou".equals(AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizes + ".desc"))){
						AutoConfig.incrementcache(eid, playid, "count", playid, 
								AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizes + ".desc")+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),1);
					}
					
				}

				if (!AutoConfig.config(eid, "lsid.pool" + poolid + ".repeat").equals("true")) {
					AutoConfig.cacheuserdata(eid, enc, "once", enc + AutoConfig.SPLIT_HBASE + poolid, "pr", prizes);
				}
				String[] prs = prizes.split(AutoConfig.SPLIT);
				List<Map<String, Object>> jsonprs = new ArrayList<Map<String, Object>>();
				
				for (String prizeid : prs) {
					Map<String, Object> pr = new HashMap<String, Object>();
					pr.put("id", prizeid);
					pr.put("poolid", poolid);
					pr.put("type", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type"));
					pr.put("pooltype", AutoConfig.config(eid, "lsid.pool" + poolid + ".type"));
					pr.put("amount", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".amount"));
					pr.put("require", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require"));
					pr.put("desc", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".desc"));
					pr.put("value", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value"));
					String require = AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require");
					String[] requires = require.split(AutoConfig.SPLIT);
					for (String reqireone : requires) {
						if (reqireone.startsWith("UUID")) {
							require = UUID.randomUUID().toString().replaceAll("-", "").substring(0,
									Integer.parseInt(reqireone.substring(4)));
						}
					}
					String datafterluck = AutoConfig.datafterluck(data, prizeid, require);
					String hash = enc;
					String row = enc + AutoConfig.SPLIT_HBASE + poolid + AutoConfig.SPLIT_HBASE + prizeid;
					if (AutoConfig.config(eid, "lsid.pool" + poolid + ".repeat").equals("true")) {
						row += AutoConfig.SPLIT_HBASE + uuid + AutoConfig.SPLIT_HBASE
								+ AutoConfig.getlucktime(datafterluck.split(AutoConfig.SPLIT));
					}
					if (isfixcode) {
						hash = row;
					}
					if (!AutoConfig.finished(AutoConfig.cacheuserdata(eid, hash, "prize", row, "p"))) {
						AutoConfig.cacheuserdata(eid, hash, "prize", row, "p", datafterluck);
						if (!isfixcode) {
							AutoConfig.incrementcachemore(eid, enc, "count", enc,
									"penc" + AutoConfig.SPLIT_HBASE + AutoConfig.getfrom(d), 1);
						}
						long seq = AutoConfig.incrementcachemore(eid, playid, "count", playid,
								"puser" + AutoConfig.SPLIT_HBASE + AutoConfig.getfrom(d), 1);
						AutoConfig.incrementcachemore(eid, ip, "count", ip,
								"pip" + AutoConfig.SPLIT_HBASE + AutoConfig.getfrom(d), 1);
						//个人中心 表 userdata
						AutoConfig.cacheuserdata(eid, playid, "userdata",
								playid + AutoConfig.SPLIT_HBASE + seq, "d", row);
						if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require")
								.isEmpty()) {
							
							String status = "finished";
							String datafinish = AutoConfig.datafinish(datafterluck, status);
							AutoConfig.cacheuserdata(eid, hash, "prize", row, "p", datafinish);
						}
						if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
								.equals("product")) {
							if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require")
									.isEmpty()) {
								String prodid = "";
								if (!encproda.isEmpty()) {
									prodid = DefaultCipher.dec(encproda).split(AutoConfig.SPLIT)[Integer
											.parseInt(AutoConfig.config(eid, "lsid.code.proda.index"))];
								} else if (!encprodna.isEmpty()) {
									prodid = DefaultCipher.dec(encprodna).split(AutoConfig.SPLIT)[Integer
											.parseInt(AutoConfig.config(eid, "lsid.code.prodna.index"))];
								}
								if (!prodid.isEmpty()) {
									String prodesc = AutoConfig.config(eid, "lsid.prod.desc." + prodid);
									pr.put("value", prodesc);
								}
							} else {
								String ticket = AutoConfig.generateticket(datafterluck, "onetime");
								pr.put("ticket", ticket);
							}
						} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
								.equals("link")) {
							if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require")
									.isEmpty()) {
								pr.put("value",
										MessageFormat.format(
												AutoConfig.config(eid,
														"lsid.pool" + poolid + ".prize" + prizeid + ".value"),
												new Object[] { playid, enc, AutoConfig.config(eid,
														"lsid.pool" + poolid + ".prize" + prizeid + ".desc") }));
							} else {
								String ticket = AutoConfig.generateticket(datafterluck, "onetime");
								pr.put("ticket", ticket);
							}
						} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
								.equals("pool")) {
							String newpoolid=null;
							newpoolid = AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value");
							String boundedpo = AutoConfig.cachecodedata(eid, enc, "once", enc, "po");
							String newbound = "";
							if (boundedpo.isEmpty()) {
								newbound = newpoolid;
							} else if (!boundedpo.contains(newpoolid)){
								newbound = boundedpo+ AutoConfig.SPLIT+newpoolid;
							}
							if (!newbound.isEmpty()) {
								AutoConfig.cachecodedata(eid, enc, "once", enc, "po", newbound);
							}
							String newpoolprizes = AutoConfig.cacheuserdata(eid, enc, "once", enc + AutoConfig.SPLIT_HBASE + newpoolid, "pr");
							if (newpoolprizes.isEmpty()&&"true".equals(AutoConfig.config(eid, "lsid.pool" + newpoolid + ".ticket"))) {
								List<Map<String, String>> pools = new ArrayList<Map<String, String>>();
								String datarepoolid = AutoConfig.datarepoolid(data, newpoolid);
								datarepoolid = datarepoolid.replace("#xeventy#","#"+poolid+"#");
								String ticketype = "onetime";
								if (AutoConfig.config(eid,"lsid.pool" + newpoolid + ".repeat").equals("true")) {
									ticketype = "refreshable";
								}
								Map<String, String> po = new HashMap<String, String>();
								po.put("id", newpoolid);
								String ticket = AutoConfig.generateticket(datarepoolid, ticketype);
								po.put("ticket", ticket);
								po.put("type", AutoConfig.config(eid, "lsid.pool" + newpoolid + ".type"));
								po.put("require", AutoConfig.config(eid, "lsid.pool" + newpoolid + ".require"));
								pools.add(po);
								returnvalue.put("pools", pools);
							} else if (!newpoolprizes.isEmpty()) {
								String[] newprize = newpoolprizes.split(AutoConfig.SPLIT);
								for (String newprizeid : newprize) {
									String newdatafterluck = AutoConfig.cacheuserdata(eid, enc, "prize",
											enc + AutoConfig.SPLIT_HBASE + newpoolid + AutoConfig.SPLIT_HBASE
													+ newprizeid,
											"p");
									if (!AutoConfig.finished(newdatafterluck)) {
										newdatafterluck = newdatafterluck.replace("#xeventy#","#"+poolid+"#");
										Map<String, Object> newpr = new HashMap<String, Object>();
										newpr.put("id", newprizeid);
										newpr.put("poolid", newpoolid);
										newpr.put("type", AutoConfig.config(eid,
												"lsid.pool" + newpoolid + ".prize" + newprizeid + ".type"));
										newpr.put("pooltype",
												AutoConfig.config(eid, "lsid.pool" + newpoolid + ".type"));
										newpr.put("value", AutoConfig.config(eid,
												"lsid.pool" + newpoolid + ".prize" + newprizeid + ".value"));
										newpr.put("amount", AutoConfig.config(eid,
												"lsid.pool" + newpoolid + ".prize" + newprizeid + ".amount"));
										newpr.put("require", AutoConfig.config(eid,
												"lsid.pool" + newpoolid + ".prize" + newprizeid + ".require"));
										newpr.put("desc", AutoConfig.config(eid,
												"lsid.pool" + newpoolid + ".prize" + newprizeid + ".desc"));
										String ticket = AutoConfig.generateticket(newdatafterluck, "onetime");
										newpr.put("ticket", ticket);
										jsonprs.add(newpr);
									}
								}
							}
						} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
								.equals("coupon")) {
							if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require")
									.isEmpty()) {
								if (AutoConfig.fromwx(data)) {
									List<String> paramvalueslist = new ArrayList<String>();
									paramvalueslist.add("eid");
									paramvalueslist.add(eid);
									paramvalueslist.add("openid");
									paramvalueslist.add(DefaultCipher.dec(playid));
									String[] cardids = AutoConfig
											.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value")
											.split(AutoConfig.SPLIT);
									for (String cardid : cardids) {
										paramvalueslist.add("cardid");
										paramvalueslist.add(cardid);
									}
									String[] paramvalues = new String[paramvalueslist.size()];
									paramvalueslist.toArray(paramvalues);
									String cardsign = AutoConfig.innerpost(
											AutoConfig.rotation(null, "lsid.interface.wx"),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),
											paramvalues);
									pr.put("value", new ObjectMapper().readTree(cardsign));
								}
							} else {
								String ticket = AutoConfig.generateticket(datafterluck, "onetime");
								pr.put("ticket", ticket);
							}
						} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type").equals("cash")) {
							if (AutoConfig.fromwx(data)) {
								if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require").isEmpty()) {
									AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.wx", enc),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
											Integer.parseInt(
													AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),
											"eid", eid, "orderid", row);
								} else {
									String ticket = AutoConfig.generateticket(datafterluck, "onetime");
									pr.put("ticket", ticket);
								}
							}
						} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
								.equals("credit")) {
							if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require")
									.isEmpty()) {
								AutoConfig.incrementcache(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),
										Integer.parseInt(AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value")));
							} else {
								String ticket = AutoConfig.generateticket(datafterluck, "onetime");
								pr.put("ticket", ticket);
							}
							
						} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
								.equals("inkind")) {
							String ticket = AutoConfig.generateticket(datafterluck, "onetime");
							pr.put("ticket", ticket);
						} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
								.equals("thankyou")) {
							//do nothing
						} else {
							throw new Exception("notsupport["
									+ AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
									+ "]");
						}
						jsonprs.add(pr);
					}
				}
				returnvalue.put("prizes", jsonprs);
				returnvalue.put("result", "success");
			} else if (AutoConfig.playfinish(d)||d.length<42) {// finish ticket (once)
				//确认领奖 奖品ticket
				String poolid = AutoConfig.getpoolid(d);
				String prizeid = AutoConfig.getprizeid(d);
				
				String hash = enc;
				String row = enc + AutoConfig.SPLIT_HBASE + poolid + AutoConfig.SPLIT_HBASE + prizeid;
				if (AutoConfig.config(eid, "lsid.pool" + poolid + ".repeat").equals("true")) {
					row += AutoConfig.SPLIT_HBASE + uuid + AutoConfig.SPLIT_HBASE + AutoConfig.getlucktime(d);
				}
				
				if (isfixcode) {
					hash = row;
				}
				Map<String, Object> pr = new HashMap<String, Object>();
				pr.put("id", prizeid);
				pr.put("type", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type"));
				pr.put("pooltype", AutoConfig.config(eid, "lsid.pool" + poolid + ".type"));
				pr.put("amount", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".amount"));
				pr.put("desc", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".desc"));
				pr.put("poolid", poolid);
				pr.put("value", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value"));
				String require = AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".require");
				
				boolean neednextinputs = false;
				String[] requires = AutoConfig.config(eid,
						"lsid.pool" + poolid + ".prize" + prizeid + ".require").split(AutoConfig.SPLIT);
				if (requires.length>1) {
					require = "";
					String userinputs = "";
					if (d.length>38) {
						userinputs = DefaultCipher.dec(d[38]);
					}
					int i = 0;
					for (i=0;i<requires.length;i++) {
						if (!userinputs.contains(":"+requires[i]+":")) {
							require = requires[i];
							break;
						}
					}
					i++;
					if (i>0&&i<requires.length) {
						pr.put("require", requires[i]);
						neednextinputs = true;
					}
				}

				if (AutoConfig.cacheuserdata(eid, hash, "prize", row, "p").split(AutoConfig.SPLIT).length>=40&&require.isEmpty()) {
					throw new Exception("done");
				}
				String active = AutoConfig.config(eid, "lsid.pool" + poolid + ".active");
				if (!active.isEmpty()
						&& System.currentTimeMillis() < Long.parseLong(active.split(AutoConfig.SPLIT)[0])) {
					throw new Exception("before");
				} 
//				else if (!active.isEmpty()
//						&& System.currentTimeMillis() > Long.parseLong(active.split(AutoConfig.SPLIT)[1])) {
//					throw new Exception("after");
//				}
				if (require.startsWith("UUID") && !DefaultCipher.dec(AutoConfig.getsysrequire(d)).equals(request.getParameter(require))) {
					throw new Exception("requirerror");
				}
				if (require.startsWith("ADDR")
						&& (request.getParameter(require) == null || request.getParameter(require).trim().isEmpty()
								|| request.getParameter(require).equals("null"))) {
					throw new Exception("missingrequire");
				}

				String status = "finished";
				if (request.getParameter(require) != null && !request.getParameter(require).trim().isEmpty()
						&& !request.getParameter(require).equals("null")) {
					status = request.getParameter(require);
					if (d.length>38) {
						status=DefaultCipher.dec(d[38])+AutoConfig.SPLIT+":"+require+":"+status;
						data=data.substring(0,data.indexOf(AutoConfig.SPLIT+d[38]+AutoConfig.SPLIT));
					} else {
						if (neednextinputs) {
							status=":"+require+":"+status;
						}
					}
				}
				String datafinish = AutoConfig.datafinish(data, status);
				if (neednextinputs) {
					String ticket = AutoConfig.generateticket(datafinish, "onetime");
					pr.put("ticket", ticket);
				}
				AutoConfig.cacheuserdata(eid, hash, "prize", row, "p", datafinish);
				if (!neednextinputs) {
					if (AutoConfig.config(eid, "lsid.pool" + poolid + ".repeat").equals("true")) {
						if (!AutoConfig.config(eid, "lsid.pool"+poolid+".require").isEmpty()) {
							AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.sync", playid),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.connectimeoutinsec")),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.socketimeoutinsec")), "eid",
									eid, "sync", row);
						}
					} else {
						AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.sync", enc),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.sync.socketimeoutinsec")), "eid",
								eid, "sync", row);
					}
					
					if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type").equals("product")) {
						String prodid = "";
						if (!encproda.isEmpty()) {
							prodid = DefaultCipher.dec(encproda).split(AutoConfig.SPLIT)[Integer
									.parseInt(AutoConfig.config(eid, "lsid.code.proda.index"))];
						} else if (!encprodna.isEmpty()) {
							prodid = DefaultCipher.dec(encprodna).split(AutoConfig.SPLIT)[Integer
									.parseInt(AutoConfig.config(eid, "lsid.code.prodna.index"))];
						}
						if (!prodid.isEmpty()) {
							String prodesc = AutoConfig.config(eid, "lsid.prod.desc." + prodid);
							pr.put("value", prodesc);
						}
					} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type").equals("cash")) {
						if (AutoConfig.fromwx(data)) {
							AutoConfig.innerpost(AutoConfig.hash(null, "lsid.interface.wx", enc),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")), "eid",
									eid, "orderid", row);
						}
					} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
							.equals("credit")) {
						AutoConfig.incrementcache(eid, playid, "credit", playid, "pts"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d),
								Integer.parseInt(AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value")));
					} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type").equals("link")) {
						pr.put("value", MessageFormat.format(
								AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value"),
								new Object[] { playid, enc,
										AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".desc") }));
					} else if (AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".type")
							.equals("coupon")) {
						if (AutoConfig.fromwx(data)) {
							List<String> paramvalueslist = new ArrayList<String>();
							paramvalueslist.add("eid");
							paramvalueslist.add(eid);
							paramvalueslist.add("openid");
							paramvalueslist.add(DefaultCipher.dec(playid));
							String[] cardids = AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value")
									.split(AutoConfig.SPLIT);
							for (String cardid : cardids) {
								paramvalueslist.add("cardid");
								paramvalueslist.add(cardid);
							}
							String[] paramvalues = new String[paramvalueslist.size()];
							paramvalueslist.toArray(paramvalues);
							String cardsign = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.wx"),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
									Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),
									paramvalues);
							pr.put("value", new ObjectMapper().readTree(cardsign));
						}
					} else {
						pr.put("value", AutoConfig.config(eid, "lsid.pool" + poolid + ".prize" + prizeid + ".value"));
					}
				}
				returnvalue.put("prize", pr);
				returnvalue.put("result", "success");
			} else {
				throw new Exception("invalidticketvalue");
			}
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
