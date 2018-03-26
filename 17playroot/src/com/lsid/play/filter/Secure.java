package com.lsid.play.filter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
			String enc = AutoConfig.getenc(d);
			
			if (!AutoConfig.config(eid, "lsid.fixcode").contains(DefaultCipher.dec(enc))) {
    			
				String id = AutoConfig.getplayid(d);
				
				Date curr = new Date();
				String today = new SimpleDateFormat("yyyyMMdd").format(curr);
				String thismonth = new SimpleDateFormat("yyyyMM").format(curr);
				
				if (AutoConfig.config(eid, "lsid.black.ip").contains(ip)){
		    		throw new Exception("ipblack="+ip);
		    	}
				if (!AutoConfig.config(eid, "lsid.white.ip").contains(ip)){
				    if (!AutoConfig.cacheuserdata(eid, ip, "denied", ip, "ip").isEmpty()){
				    	throw new Exception("ipdenied="+ip);
				    }
					if (!AutoConfig.config(eid, "lsid.limit.ip.day").isEmpty()&&
							Long.parseLong(AutoConfig.config(eid, "lsid.limit.ip.day"))<AutoConfig.incremented(eid, ip, "count", ip+AutoConfig.SPLIT_HBASE+today, "pip"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d))){
			    		throw new Exception("ipdaylimit="+ip+"="+AutoConfig.config(eid, "lsid.limit.ip.day"));
			    	}
					
					if (!AutoConfig.config(eid, "lsid.limit.ip.month").isEmpty()&&
							Long.parseLong(AutoConfig.config(eid, "lsid.limit.ip.month"))<AutoConfig.incremented(eid, ip, "count", ip+AutoConfig.SPLIT_HBASE+thismonth, "pip"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d))){
			    		throw new Exception("ipmonthlimit="+ip+"="+AutoConfig.config(eid, "lsid.limit.ip.month"));
			    	}
				}
				
				if (AutoConfig.config(eid, "lsid.black.id").contains(id)){
		    		throw new Exception("idblack="+id);
		    	}
				if (!AutoConfig.config(eid, "lsid.white.id").contains(id)){
					if (!AutoConfig.config(eid, "lsid.limit.id.day").isEmpty()&&
							Long.parseLong(AutoConfig.config(eid, "lsid.limit.id.day"))<AutoConfig.incremented(eid, id, "count", id+AutoConfig.SPLIT_HBASE+today, "puser"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d))){
			    		throw new Exception("iddaylimit="+id+"="+AutoConfig.config(eid, "lsid.limit.id.day"));
			    	}
					
					if (!AutoConfig.config(eid, "lsid.limit.id.month").isEmpty()&&
							Long.parseLong(AutoConfig.config(eid, "lsid.limit.id.month"))<AutoConfig.incremented(eid, id, "count", id+AutoConfig.SPLIT_HBASE+thismonth, "puser"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d))){
			    		throw new Exception("idmonthlimit="+id+"="+AutoConfig.config(eid, "lsid.limit.id.month"));
			    	}
				}
				
				if (AutoConfig.config(eid, "lsid.black.enc").contains(enc)){
		    		throw new Exception("encblack="+enc);
		    	}
				if (!AutoConfig.config(eid, "lsid.white.enc").contains(enc)){
					if (!AutoConfig.config(eid, "lsid.limit.enc.day").isEmpty()&&
							Long.parseLong(AutoConfig.config(eid, "lsid.limit.enc.day"))<AutoConfig.incremented(eid, enc, "count", enc+AutoConfig.SPLIT_HBASE+today, "penc"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d))){
			    		throw new Exception("encdaylimit="+enc+"="+AutoConfig.config(eid, "lsid.limit.enc.day"));
			    	}
					
					if (!AutoConfig.config(eid, "lsid.limit.enc.month").isEmpty()&&
							Long.parseLong(AutoConfig.config(eid, "lsid.limit.enc.month"))<AutoConfig.incremented(eid, enc, "count", enc+AutoConfig.SPLIT_HBASE+thismonth, "penc"+AutoConfig.SPLIT_HBASE+AutoConfig.getfrom(d))){
			    		throw new Exception("encmonthlimit="+enc+"="+AutoConfig.config(eid, "lsid.limit.enc.month"));
			    	}
				}
			}
			
			request.setAttribute("data", data);
			chain.doFilter(request, response);
		
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
