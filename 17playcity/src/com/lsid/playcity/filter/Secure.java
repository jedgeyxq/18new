package com.lsid.playcity.filter;

import java.io.IOException;
import java.util.ArrayList;
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

public class Secure implements Filter {
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	response.setHeader("Access-Control-Allow-Origin", "*");
    	response.setHeader("XDomainRequestAllowed","1");
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
    	String ip = AutoConfig.getremoteip(request);
    	String eid = null;
		try{
			String ticket = request.getParameter("ticket");
			if (ticket==null||ticket.trim().isEmpty()||ticket.equals("null")){
				throw new Exception("missingticket");
			}
			
			String data = AutoConfig.readticket(request.getParameter("ticket"));
			String[] d = data.split(AutoConfig.SPLIT);
			eid = AutoConfig.geteid(d);
			if (!AutoConfig.config(null, "lsid.eids").contains(eid)) {
				throw new Exception("wrongeid["+eid+"]");
			}
			if (AutoConfig.config(eid, "lsid.fixcode").isEmpty()) {
				throw new Exception("noplaycity");
			}
			if (d[0].equals("play")) {
				Map<String, Object> returnvalue = new HashMap<String, Object>();
				returnvalue.put("self", AutoConfig.generateticket("self"+data.substring(data.indexOf(AutoConfig.SPLIT)), "refreshable"));
				returnvalue.put("list", AutoConfig.generateticket("list"+data.substring(data.indexOf(AutoConfig.SPLIT)), "refreshable"));
				returnvalue.put("exchange", AutoConfig.generateticket("exchange"+data.substring(data.indexOf(AutoConfig.SPLIT)), "refreshable"));
				returnvalue.put("eid", eid);
				List<Map<String, String>> pools = new ArrayList<Map<String, String>>();
				String poolids=AutoConfig.config(eid, "lsid.fixcode.pool."+DefaultCipher.dec(AutoConfig.getenc(d)));
				String event = "xeventy";
				if (request.getParameter("event")!=null&&!request.getParameter("event").trim().isEmpty()&&!request.getParameter("event").trim().equalsIgnoreCase("null")) {
					event = request.getParameter("event").trim();
				}
				String useragent = request.getHeader("User-Agent");
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
				for (String poolid:poolids.split(AutoConfig.SPLIT)) {
					if (AutoConfig.config(eid, "lsid.pool" + poolid + ".repeat").equals("true")) {
						Map<String, String> po = new HashMap<String, String>();
						po.put("id", poolid);
						po.put("ticket", AutoConfig.generateticket(AutoConfig.datafterpoolid(data, event,
								useragent, ip, loc, poolid), "refreshable"));
						po.put("type", AutoConfig.config(eid, "lsid.pool" + poolid + ".type"));
						po.put("require", AutoConfig.config(eid, "lsid.pool" + poolid + ".require"));
						pools.add(po);
					}
				}
				returnvalue.put("pools", pools);
				returnvalue.put("result", "success");
				AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(returnvalue));
			} else if (d[0].contains("self")||d[0].contains("list")||d[0].contains("exchange")){
				request.setAttribute("data", data);
				chain.doFilter(request, response);
			} else {
				throw new Exception("wrongaction["+d[0]+"]");
			}
		}catch(Exception ex){
			String reason = ex.getMessage();
			if (ex.getMessage().equals("invalidticket")&&
					!AutoConfig.config(eid, "lsid.black.ip").contains(ip)&&!AutoConfig.config(eid, "lsid.white.ip").contains(ip)){
				try{
					AutoConfig.cacheuserdata(eid, ip, "denied", ip, "ip", ip);
				}catch(Exception e){
					//do nothing
				}
			}
			AutoConfig.log(ex, "Failed in processing request ["+request.getRequestURI()+"] due to below exception:");
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
