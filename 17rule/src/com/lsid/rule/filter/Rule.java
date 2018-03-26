package com.lsid.rule.filter;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

public class Rule implements Filter {
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

			String eid = request.getParameter("eid");
			if (eid == null || eid.trim().isEmpty() || eid.trim().equals("null")) {
				throw new Exception("missingeid");
			}

			String type = request.getParameter("type");
			if (type == null || type.trim().isEmpty() || type.trim().equals("null")) {
				throw new Exception("missingtype");
			}

			String from = request.getParameter("from");
			if (from == null || from.trim().isEmpty() || from.trim().equals("null")) {
				throw new Exception("missingfrom");
			}

			String encproda = request.getParameter("encproda");
			String encprodna = request.getParameter("encprodna");
			if ((encproda == null || encproda.trim().isEmpty() || encproda.trim().equals("null"))
					&& (encprodna == null || encprodna.trim().isEmpty() || encprodna.trim().equals("null"))) {
				throw new Exception("missingencprodana");
			}

			String enc = request.getParameter("enc");
			if (enc == null || enc.trim().isEmpty() || enc.trim().equals("null")) {
				throw new Exception("missingenc");
			}

			String encuserinfo = request.getParameter("encuserinfo");
			if (encuserinfo == null || encuserinfo.trim().isEmpty() || encuserinfo.trim().equals("null")) {
				throw new Exception("missingencuserinfo");
			}

			String openid = request.getParameter("openid");
			if (openid == null || openid.trim().isEmpty() || openid.trim().equals("null")) {
				throw new Exception("missingopenid");
			}

			String ip = request.getParameter("ip");
			String lng = request.getParameter("lng");
			String lat = request.getParameter("lat");
			if ((ip == null || ip.trim().isEmpty() || ip.trim().equals("null")) && (lng == null || lng.trim().isEmpty()
					|| lng.trim().equals("null") || lat == null || lat.trim().isEmpty() || lat.trim().equals("null"))) {
				throw new Exception("missingiplnglat");
			}
			String location = null;
			try {
				if (lng != null) {
					location = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.loc"),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.socketimeoutinsec")), "lng",
							lng, "lat", lat);
				} else {
					location = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.loc"),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.socketimeoutinsec")), "ip",
							ip);
				}
			} catch (Exception ex) {
				// do nothing
			}
			String province = "";
			String city = "";
			String district = "";
			String street = "";
			String sn = "";
			if (location != null) {
				String[] parts = location.split(AutoConfig.SPLIT);
				province = parts[0];
				city = parts[1];
				district = parts[2];
				street = parts[3];
				sn = parts[4];
			}
			if (AutoConfig.config(eid, "lsid." + type).isEmpty()) {
				throw new Exception("missingconfig" + type);
			} else {
				String[] prod = null;
				if (encproda != null&&type.equals("pool")) {
					prod = DefaultCipher.dec(encproda).split(AutoConfig.SPLIT);
				
				//是否箱码
				String ptype=prod[3];
				String city1=URLDecoder.decode(city,"utf-8");
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");//设置日期格式
				String datetime=df.format(new Date());// new Date()为获取当前系统时间 
				if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20171225")||datetime.equals("20171230")||datetime.equals("20180101")||datetime.equals("20180111")||datetime.equals("20180117"))){
					if(city1.contains("台州")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						System.out.println("=tzb66cnt=="+tzb66cnt+"==city==="+city+"==datetime=="+datetime);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20180107")||datetime.equals("20180120"))){
					if(city1.contains("九江")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20180103")||datetime.equals("20180108"))){
					if(city1.contains("南京")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20171222")||datetime.equals("20171227")||datetime.equals("20180104")||datetime.equals("20180113"))){
					if(city1.contains("南昌")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20171223")||datetime.equals("20180105")||datetime.equals("20180109"))){
					if(city1.contains("苏州")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20171226")||datetime.equals("20180106")||datetime.equals("20180112")||datetime.equals("20180116"))){
					if(city1.contains("杭州")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20171221")||datetime.equals("20171228")||datetime.equals("20180114")||datetime.equals("20180118"))){
					if(city1.contains("金华")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20171224")||datetime.equals("20180119"))){
					if(city1.contains("无锡")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("t1")&&ptype.equals("prod0")&&(datetime.equals("20171229")||datetime.equals("20180102")||datetime.equals("20180110")||datetime.equals("20180115"))){
					if(city1.contains("绍兴")){
						long tzb66cnt=AutoConfig.incrementcache(eid, city+datetime, "tzb666cnt", city+datetime,"prize", 1);
						if(tzb66cnt==10){
							//奖池id
							String returnvalue="48";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}						
				}else if(eid.equals("m")&&ptype.equals("prod0")&&datetime.equals("20180206")){
					if(city1.contains("益阳")){
						long mj888cnt=AutoConfig.incrementcache(eid, city+datetime, "mj888cnt", city+datetime,"prize", 1);
						if(mj888cnt==10){
							//奖池id
							String returnvalue="1";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}
					
				}else if(eid.equals("m")&&ptype.equals("prod0")&&datetime.equals("20180220")){
					if(city1.contains("娄底")){
						long mj888cnt=AutoConfig.incrementcache(eid, city+datetime, "mj888cnt", city+datetime,"prize", 1);
						if(mj888cnt==10){
							//奖池id
							String returnvalue="1";						
							AutoConfig.innerechok(response, returnvalue);
							return;
						}
					}
				}
			}
				Map<String, Integer> fit = new HashMap<String, Integer>();
				String[] ids = AutoConfig.config(eid, "lsid." + type).split(AutoConfig.SPLIT);
				for (String id : ids) {
					if (!AutoConfig.config(eid, "lsid." + type + id + ".rule").isEmpty()) {
						String[] parts = AutoConfig.config(eid, "lsid." + type + id + ".rule").split(AutoConfig.SPLIT);
						int value = 0;
						for (int i = 0; i < parts.length; i++) {
							if (parts[i].startsWith("p|")) {
								if (parts[i].equals("p|")) {
									value += (i + 1);
								} else if (("P|" + province).contains(parts[i].toUpperCase())) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
							if (parts[i].startsWith("c|")) {
								if (parts[i].equals("c|")) {
									value += (i + 1);
								} else if (("C|"+city).contains(parts[i].toUpperCase())) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
							if (parts[i].startsWith("d|")) {
								if (parts[i].equals("d|")) {
									value += (i + 1);
								} else if (("D|" + district).contains(parts[i].toUpperCase())) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
							if (parts[i].startsWith("s|")) {
								if (parts[i].equals("s|")) {
									value += (i + 1);
								} else if (("S|" + street).contains(parts[i].toUpperCase())) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
							if (parts[i].startsWith("sn|")) {
								if (parts[i].equals("sn|")) {
									value += (i + 1);
								} else if (("SN|" + sn).contains(parts[i].toUpperCase())) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
							if (parts[i].startsWith("a|")) {
								String active = "";
								if (encproda != null) {
									active = "a";
								} else {
									active = "na";
								}
								if (parts[i].equals("a|")) {
									value += (i + 1);
								} else if (parts[i].equals("a|" + active)) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
							if (parts[i].startsWith("pr|")) {
								if (parts[i].equals("pr|")) {
									value += (i + 1);
								} else {
									boolean toadd = true;
									String[] cons = parts[i].split("\\|");
									String active = "";
									String[] pros = null;
									if (encproda != null) {
										active = "a";
										pros = DefaultCipher.dec(encproda).split(AutoConfig.SPLIT);
									} else {
										active = "na";
										pros = DefaultCipher.dec(encprodna).split(AutoConfig.SPLIT);
									}
									for (int j = 1; j < cons.length; j++) {
										if (cons[j].startsWith("d:")) {
											try {
												//codetime=new SimpleDataFormat('yyyy-mm-dd hh:mm:ss').parse('激活码的时间');
												Date codetime = new SimpleDateFormat(AutoConfig
														.config(eid, "lsid.code.valid." + active)
														.split(AutoConfig.SPLIT)[Integer.parseInt(AutoConfig.config(eid,
																"lsid.code.time" + active + ".index"))].substring(5))
																		.parse(pros[Integer.parseInt(
																				AutoConfig.config(eid, "lsid.code.time"
																						+ active + ".index"))]);
												if (codetime.compareTo(new SimpleDateFormat(AutoConfig
														.config(eid, "lsid.code.valid." + active)
														.split(AutoConfig.SPLIT)[Integer.parseInt(AutoConfig.config(eid,
																"lsid.code.time" + active + ".index"))].substring(5))
																		.parse(cons[j].substring(2))) < 0) {
													toadd = false;
												}
											} catch (Exception e) {
												toadd = false;
											}
										} else if (cons[j].startsWith("p:")) {
											//码规则不包括  _p:testp_
											if (!parts[i].contains("_" + pros[Integer.parseInt(
													AutoConfig.config(eid, "lsid.code.prod" + active + ".index"))]
													+ "_")) {
												toadd = false;
											}
										} else {
											toadd = false;
										}
									}
									if (toadd) {
										value += (i + 1) * 10;
									} else {
										value = 0;
										break;
									}
								}
							}
							if (parts[i].startsWith("co|")) {
								if (parts[i].equals("co|")) {
									value += (i + 1);
								} else if (parts[i].contains("|" + enc)) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
							if (parts[i].startsWith("g|")) {
								if (parts[i].equals("g|")) {
									value += (i + 1);
								} else {
									String gender = "2";
									try {
										gender = new ObjectMapper().readTree(new String(DefaultCipher.dec(encuserinfo)))
												.get("sex").asText();
									} catch (Exception ex) {
										// do nothing
									}
									if (parts[i].contains("|" + gender)) {
										value += (i + 1) * 10;
									} else {
										value = 0;
										break;
									}
								}
							}
							if (parts[i].startsWith("o|")) {
								if (parts[i].equals("o|")) {
									value += (i + 1);
								} else if (parts[i].contains("|" + openid)) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
							if (parts[i].startsWith("f|")) {
								if (parts[i].equals("f|")) {
									value += (i + 1);
								} else if (parts[i].contains("|" + from)) {
									value += (i + 1) * 10;
								} else {
									value = 0;
									break;
								}
							}
						}
						if (value == 0) {
							continue;
						} else {
							fit.put(id, value);
						}
					}
				}
				if (!fit.isEmpty()) {
					Map<String, Integer> temp = sortMapByValue(fit);
					long current = System.currentTimeMillis();
					List<String> plays = new ArrayList<String>();
					String unplay = null;
					int max = 0;
					for (Entry<String, Integer> e : temp.entrySet()) {
						long start = Long.parseLong(AutoConfig.config(eid, "lsid." + type + e.getKey() + ".active")
								.split(AutoConfig.SPLIT)[0]);
						long end = Long.parseLong(AutoConfig.config(eid, "lsid." + type + e.getKey() + ".active")
								.split(AutoConfig.SPLIT)[1]);
						if (start <= current && end >= current) {
							if (max == 0) {
								max = e.getValue();
							}
							if (max == e.getValue()) {
								plays.add(e.getKey());
							}
						}
						unplay = e.getKey();
					}
					if (plays.isEmpty()) {
						AutoConfig.innerechok(response, unplay);
					} else {
						String returnvalue = plays.get(0);
						for (int i = 1; i < plays.size(); i++) {
							returnvalue += AutoConfig.SPLIT + plays.get(i);
						}
						AutoConfig.innerechok(response, returnvalue);
					}
				} else {
					throw new Exception("missingrule");
				}
			}
		} catch (Exception e) {
			AutoConfig.log(e, "error processing " + request.getRequestURI());
			AutoConfig.innerechno(response, request, e);
		}

	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

	private static Map<String, Integer> sortMapByValue(Map<String, Integer> oriMap) {
		if (oriMap == null || oriMap.isEmpty()) {
			return null;
		}
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		List<Map.Entry<String, Integer>> entryList = new ArrayList<Map.Entry<String, Integer>>(oriMap.entrySet());
		Collections.sort(entryList, new Comparator<Entry<String, Integer>>() {

			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				Entry<String, Integer> me1 = (Entry<String, Integer>) o1;
				Entry<String, Integer> me2 = (Entry<String, Integer>) o2;
				return me2.getValue().compareTo(me1.getValue());
			}

		});

		Iterator<Map.Entry<String, Integer>> iter = entryList.iterator();
		Map.Entry<String, Integer> tmpEntry = null;
		while (iter.hasNext()) {
			tmpEntry = iter.next();
			sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
		}
		return sortedMap;
	}

}
