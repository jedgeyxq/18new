package com.lsid.qrenter.filter;

import java.io.IOException;
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

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class WxFixcode implements Filter {
	
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
    	if (request.getRequestURI().startsWith("/h/")) {
		response.sendRedirect("http://lsid.me/h/"+request.getRequestURI().substring("/h/".length()));
		return;
	}
		
    	String eid = null;
    	try{
    		if (request.getRequestURI().endsWith(".txt")){
    			chain.doFilter(req, res);
    			return;
    		}
    		
    		String useragent = request.getHeader("User-Agent");
	    	if (useragent==null||!useragent.contains("MicroMessenger")){
	    		chain.doFilter(req, res);
    			return;
	       	}
	    	
	    	String ip = AutoConfig.getremoteip(request);
	    	
	    	if (AutoConfig.config(eid, "lsid.black.ip").contains(ip)){
	    		response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.page.denied")+AutoConfig.SPLIT+"ipblack="+ip);
	    		return;
		    }
	    	
	    	if (!AutoConfig.cacheuserdata(eid, ip, "denied", ip, "ip").isEmpty()){
	    		response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.page.denied")+AutoConfig.SPLIT+"ipdenied="+ip);
	    		return;
		    }
	    	
	    	if (request.getRequestURI().startsWith("/ticket/")){
	    		String ticket = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/")+1);
	    		if (ticket.trim().isEmpty()) {
	    			throw new Exception("wrongticket["+ticket+"]");
	    		}
				String data=AutoConfig.readticket(ticket);
				String[] d = data.split(AutoConfig.SPLIT);
				eid=AutoConfig.geteid(d);
				if (AutoConfig.config(eid, "lsid.black.ip").contains(ip)){
		    		response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.page.denied")+AutoConfig.SPLIT+"ipblack="+ip);
		    		return;
			    }
		    	
		    	if (!AutoConfig.cacheuserdata(eid, ip, "denied", ip, "ip").isEmpty()){
		    		response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.page.denied")+AutoConfig.SPLIT+"ipdenied="+ip);
		    		return;
			    }
		    	String enc=AutoConfig.getenc(d);
		    	if (!AutoConfig.config(eid, "lsid.fixcode").contains(DefaultCipher.dec(enc))) {
	    			chain.doFilter(req, res);
	    			return;
	    		}
				long scantime=AutoConfig.getintime(d);
				if (AutoConfig.uuid(data)){
					String uuid=AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.wx"), 
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),
							"eid",eid,"wxcode",request.getParameter("code"),"keyprefix","lsid.uuwxid");
					String nextdata = AutoConfig.databeforeinfo(data, uuid);
					AutoConfig.nocache(eid, "scan", enc+AutoConfig.SPLIT_HBASE+uuid+AutoConfig.SPLIT_HBASE+scantime, "s", nextdata);
					String userinfoplayid=AutoConfig.cacheuserdata(eid, uuid, "userinfo", uuid, AutoConfig.getfrom(d));
					if (userinfoplayid.isEmpty()){
						String wxscope = "snsapi_base";
						String prefix="lsid.playwx";
						if (!AutoConfig.config(eid, "lsid.infowx.appid").isEmpty()){
							wxscope = "snsapi_userinfo";
							prefix="lsid.infowx";
						} else {
							nextdata = AutoConfig.datafterinfo(nextdata, "notconfig");
						}
						response.sendRedirect("https://open.weixin.qq.com/connect/oauth2/authorize?appid="+AutoConfig.config(eid, prefix+".appid")+"&redirect_uri="+AutoConfig.config(eid, "lsid.host.userentry")+"ticket/"+AutoConfig.generateticket(nextdata, "fixedexpire")+"&response_type=code&scope="+wxscope+"&state="+new Random().nextInt(10000)+"#wechat_redirect");
					} else {
						nextdata = AutoConfig.datafterinfo(nextdata, userinfoplayid.split(AutoConfig.SPLIT)[0]);
						nextdata = AutoConfig.datafterplayid(nextdata, userinfoplayid.split(AutoConfig.SPLIT)[1]);
						goplay(userinfoplayid, nextdata, response);
					}
				} else if (AutoConfig.info(data)){
					String encuserinfo=AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.wx"), 
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),
							"eid",eid,"wxcode",request.getParameter("code"));
					String nextdata = AutoConfig.datafterinfo(data, encuserinfo);
					response.sendRedirect("https://open.weixin.qq.com/connect/oauth2/authorize?appid="+AutoConfig.config(eid, "lsid.playwx.appid")+"&redirect_uri="+AutoConfig.config(eid, "lsid.host.userentry")+"ticket/"+AutoConfig.generateticket(nextdata, "fixedexpire")+"&response_type=code&scope=snsapi_base&state="+new Random().nextInt(10000)+"#wechat_redirect");
				} else if (AutoConfig.playid(data)){
					String playid=AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.wx"), 
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.connectimeoutinsec")),
							Integer.parseInt(AutoConfig.config(null, "lsid.interface.wx.socketimeoutinsec")),
							"eid",eid,"wxcode",request.getParameter("code"),"keyprefix","lsid.playwx");
					String nextdata = AutoConfig.datafterplayid(data, playid);
					goplay(AutoConfig.getencuserinfo(d)+AutoConfig.SPLIT+playid, nextdata, response);
				} else {
					if (!AutoConfig.config(eid, "lsid.black.ip").contains(ip)&&!AutoConfig.config(eid, "lsid.white.ip").contains(ip)) {
						AutoConfig.cacheuserdata(eid, ip, "denied", ip, "ip", ip);
					}
					throw new Exception("invalidticket="+ticket);
				}
	    	} else {
	    		eid=request.getRequestURI().substring(1,request.getRequestURI().lastIndexOf("/"));
	    		if (!AutoConfig.config(null, "lsid.eids").contains(eid)) {
					throw new Exception("wrongeid["+eid+"]");
				}
	    		if (AutoConfig.config(eid, "lsid.black.ip").contains(ip)){
		    		response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.page.denied")+AutoConfig.SPLIT+"ipblack="+ip);
		    		return;
			    }
		    	
		    	if (!AutoConfig.cacheuserdata(eid, ip, "denied", ip, "ip").isEmpty()){
		    		response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.page.denied")+AutoConfig.SPLIT+"ipdenied="+ip);
		    		return;
			    }
	    		String code=request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/")+1);
	    		if (code.trim().isEmpty()) {
	    			throw new Exception("wrongcode["+code+"]");
	    		}
	    		if (!AutoConfig.config(eid, "lsid.fixcode").contains(code)) {
	    			chain.doFilter(req, res);
	    			return;
	    		}
	    		String enc = DefaultCipher.enc(code);
    			
	    		String enca = enc;
	    		String encna = "";
			String loc=AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.loc"), Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.connectimeoutinsec")),
						Integer.parseInt(AutoConfig.config(null, "lsid.interface.loc.socketimeoutinsec")),"ip",ip);
					
			String data = AutoConfig.dataweixininit(eid, enc, enca, encna, useragent, ip, loc);
			response.sendRedirect("https://open.weixin.qq.com/connect/oauth2/authorize?appid="+AutoConfig.config(null, "lsid.uuwxid.appid")+"&redirect_uri="+AutoConfig.config(eid, "lsid.host.userentry")+"ticket/"+AutoConfig.generateticket(data, "fixedexpire")+"&response_type=code&scope=snsapi_base&state="+new Random().nextInt(10000)+"#wechat_redirect");
		}
    	}catch(Exception ex){
    		AutoConfig.log(ex, "Failed in processing request=["+request.getRequestURI()+"] due to below exception:");
    		response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.page.error")+AutoConfig.SPLIT+URLEncoder.encode(ex.getMessage(),"UTF-8"));
    	}
	}
	
	private void goplay(String userinfoplayid, String data, HttpServletResponse response) throws Exception{
		String[] d = data.split(AutoConfig.SPLIT);
		String eid=AutoConfig.geteid(d);
		String uuid=AutoConfig.getuuid(d);
		String from=AutoConfig.getfrom(d);
		String enc=AutoConfig.getenc(d);
		String playid = userinfoplayid.substring(userinfoplayid.indexOf(AutoConfig.SPLIT)+1);
		Long scantime = AutoConfig.getintime(d);
		
		AutoConfig.cacheuserdata(eid, uuid, "userinfo", uuid, from, userinfoplayid);
		
		AutoConfig.incrementcache(eid, playid, "count", playid, "suser"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d), 1);
		
		String activityid = DefaultCipher.dec(enc);
		
		String nextdata = AutoConfig.datafteractivityid(data, activityid);
		AutoConfig.nocache(eid, "scan", enc+AutoConfig.SPLIT_HBASE+uuid+AutoConfig.SPLIT_HBASE+scantime, "s", nextdata);
		String active=AutoConfig.config(eid, "lsid.activity"+activityid+".active");
		if (!active.isEmpty()&&System.currentTimeMillis()<Long.parseLong(active.split(AutoConfig.SPLIT)[0])){
			response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.activity"+activityid+".beforepage"));
		} else if (!active.isEmpty()&&System.currentTimeMillis()>Long.parseLong(active.split(AutoConfig.SPLIT)[1])){
			response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.activity"+activityid+".afterpage"));
		} else {
			String playticket = "";
			if ("true".equals(AutoConfig.config(eid, "lsid.activity"+activityid+".ticket"))) {
				playticket=AutoConfig.generateticket(nextdata, "refreshable");
			}
			response.sendRedirect(AutoConfig.config(eid, "lsid.host.userplay")+AutoConfig.config(eid, "lsid.activity"+activityid+".playpage")+playticket);
		}
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
}
