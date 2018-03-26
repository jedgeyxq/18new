package com.lsid.location.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;

public class Baidu implements Filter {
	
	private static final String defaultvalue = "UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+
			AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN";
	private static final String defaultprefix = "UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+
			AutoConfig.SPLIT+"UNKNOWN";
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain arg2)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	response.setHeader("Access-Control-Allow-Origin", "*");
    	response.setHeader("XDomainRequestAllowed","1");
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
		try {
			if (request.getParameter("ip")!=null&&!request.getParameter("ip").trim().isEmpty()&&!request.getParameter("ip").equals("null")){
    			String value = AutoConfig.cacheuserdata(null, request.getParameter("ip"), "loc", request.getParameter("ip"), "ipaddr");
    			if (value.isEmpty()){
    				String[] aks = AutoConfig.config(null, "lsid.interface.baidu.ak").split(AutoConfig.SPLIT);
    				String[] sks = AutoConfig.config(null, "lsid.interface.baidu.sk").split(AutoConfig.SPLIT);
    				for (int i=0;i<aks.length;i++){
    					String ak=aks[i];
						String sn=calbaidusn(AutoConfig.config(null, "lsid.interface.baidu.ip"), ak, sks[i], "json", "coor","bd09ll","ip",request.getParameter("ip"));
						String baiduresult = AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.baidu.ip"), 
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.baidu.connectimeoutinsec")),
								Integer.parseInt(AutoConfig.config(null, "lsid.interface.baidu.socketimeoutinsec")),  
								"output","json", "coor","bd09ll","ip",request.getParameter("ip"),"ak",ak,"sn",sn);
						JsonNode jn = new ObjectMapper().readTree(baiduresult);
    					if (jn.get("status").asInt()==0&&
    							!"".equals(jn.get("content").get("point").get("x").asText())&&
    							!"".equals(jn.get("content").get("point").get("y").asText())){
    						value =lnglatloc(jn.get("content").get("point").get("x").asText(),
	    						jn.get("content").get("point").get("y").asText());
    		    			if (!value.isEmpty()){
    							break;
    						}
    					} else {
    						continue;
    					}
    				}
    				if (!value.isEmpty()){
    					AutoConfig.cacheuserdata(null, request.getParameter("ip"), "loc", request.getParameter("ip"), "ipaddr", value);
        			} else {
        				value = defaultvalue;
        			}
				}
    			AutoConfig.innerechok(response, value);
    		} else if (request.getParameter("lng")!=null&&!request.getParameter("lng").trim().isEmpty()&&!request.getParameter("lng").equals("null")&&
				request.getParameter("lat")!=null&&!request.getParameter("lat").trim().isEmpty()&&!request.getParameter("lat").equals("null")){
    			String value = lnglatloc(request.getParameter("lng"),request.getParameter("lat"));
				AutoConfig.innerechok(response, value);
    		} else if (request.getParameter("address")!=null&&!request.getParameter("address").trim().isEmpty()&&!request.getParameter("address").equals("null")){
    			String address=URLDecoder.decode(request.getParameter("address"), "UTF-8");
    			String city=request.getParameter("city")==null?"":URLDecoder.decode(request.getParameter("city"), "UTF-8");
    			String value = AutoConfig.cacheuserdata(null, URLEncoder.encode(address+city, "UTF-8"), "loc", 
    					URLEncoder.encode(address+city, "UTF-8"), "locaddr");
    			if (value.isEmpty()){
    				String[] aks = AutoConfig.config(null, "lsid.interface.baidu.ak").split(AutoConfig.SPLIT);
    				String[] sks = AutoConfig.config(null, "lsid.interface.baidu.sk").split(AutoConfig.SPLIT);
    				for (int i=0;i<aks.length;i++){
    					String ak=aks[i];
						JsonNode jn = null;
    					if (city.isEmpty()){
    						String sn=calbaidusn(AutoConfig.config(null, "lsid.interface.baidu.geo"), ak, sks[i], "json", "address",address);
	    					jn = new ObjectMapper().readTree(AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.baidu.geo"), 
    								Integer.parseInt(AutoConfig.config(null, "lsid.interface.baidu.connectimeoutinsec")),
    								Integer.parseInt(AutoConfig.config(null, "lsid.interface.baidu.socketimeoutinsec")), 
    							"address",address,"output","json","ak",ak, "sn", sn));
    					} else {
    						String sn=calbaidusn(AutoConfig.config(null, "lsid.interface.baidu.geo"), ak, sks[i], "json", "address",address,"city",city);
	    					jn = new ObjectMapper().readTree(AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.baidu.geo"), 
    								Integer.parseInt(AutoConfig.config(null, "lsid.interface.baidu.connectimeoutinsec")),
    								Integer.parseInt(AutoConfig.config(null, "lsid.interface.baidu.socketimeoutinsec")),  
    								"address",address,"output","json","city",city,"ak",ak, "sn", sn));
    					}
    					if (jn.get("status").asInt()==0&&jn.get("result").get("location").get("lng").asDouble()>0&&
    							jn.get("result").get("location").get("lat").asDouble()>0){
    						value =lnglatloc(String.valueOf(jn.get("result").get("location").get("lng").asDouble()),
    								String.valueOf(jn.get("result").get("location").get("lat").asDouble()));
    		    			if (!value.isEmpty()){
    							break;
    						}
    					} else {
    						continue;
        				}
    				}
    				if (!value.isEmpty()){
    					AutoConfig.cacheuserdata(null, URLEncoder.encode(address+city, "UTF-8"), "loc", 
    	    					URLEncoder.encode(address+city, "UTF-8"), "locaddr", value);
        			} else {
        				value = "UNKNOWN"+AutoConfig.SPLIT+URLEncoder.encode(city, "UTF-8")+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+URLEncoder.encode(address, "UTF-8")+AutoConfig.SPLIT+"UNKNOWN"+
        						AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN"+AutoConfig.SPLIT+"UNKNOWN";
        			}
				}
    			AutoConfig.innerechok(response, value);
    		} else {
    			throw new Exception("missingparams");
    		}
		} catch (Exception e) {
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			AutoConfig.innerechok(response, defaultvalue);
		}
		
	}
	
	private String lnglatloc(String lng, String lat) throws Exception{
		String lnglat="lng"+lng+"lat"+lat;
		String value = AutoConfig.cacheuserdata(null, lnglat, "loc", lnglat, "lngaddr");
		if (!value.isEmpty()){
			return value;
		} else {
			String[] aks = AutoConfig.config(null, "lsid.interface.baidu.ak").split(AutoConfig.SPLIT);
			String[] sks = AutoConfig.config(null, "lsid.interface.baidu.sk").split(AutoConfig.SPLIT);
			for (int i=0;i<aks.length;i++){
				String ak=aks[i];
				String sn=calbaidusn(AutoConfig.config(null, "lsid.interface.baidu.geo"), ak, sks[i], "json", "location",lat+","+lng);
				JsonNode jn = new ObjectMapper().readTree(AutoConfig.outerpost(AutoConfig.config(null, "lsid.interface.baidu.geo"), 
						Integer.parseInt(AutoConfig.config(null, "lsid.interface.baidu.connectimeoutinsec")),
						Integer.parseInt(AutoConfig.config(null, "lsid.interface.baidu.socketimeoutinsec")),  
						"location",lat+","+lng,"output","json","ak",ak, "sn",sn));
				if (jn.get("status").asInt()==0&&!"".equals(jn.get("result").get("addressComponent").get("city").asText())&&
						!"".equals(jn.get("result").get("addressComponent").get("province").asText())&&
						jn.get("result").get("location").get("lng").asDouble()>0&&
						jn.get("result").get("location").get("lat").asDouble()>0){
					value =URLEncoder.encode(jn.get("result").get("addressComponent").get("province").asText(),"UTF-8")+AutoConfig.SPLIT+
							URLEncoder.encode(jn.get("result").get("addressComponent").get("city").asText(),"UTF-8")+AutoConfig.SPLIT+
							URLEncoder.encode(jn.get("result").get("addressComponent").get("district").asText(),"UTF-8")+AutoConfig.SPLIT+
							URLEncoder.encode(jn.get("result").get("addressComponent").get("street").asText(),"UTF-8")+AutoConfig.SPLIT+
							URLEncoder.encode(jn.get("result").get("addressComponent").get("street_number").asText(),"UTF-8")+AutoConfig.SPLIT+
							URLEncoder.encode(jn.get("result").get("addressComponent").get("country").asText(),"UTF-8")+AutoConfig.SPLIT+
							jn.get("result").get("location").get("lng").asDouble()+AutoConfig.SPLIT+
							jn.get("result").get("location").get("lat").asDouble();
					break;
				} else {
					continue;
				}
			}
			if (!value.isEmpty()){
				lnglat="lng"+lng+"lat"+lat;
				AutoConfig.cacheuserdata(null, lnglat, "loc", lnglat, "lngaddr", value);
			} else {
				value = defaultprefix+AutoConfig.SPLIT+lng+AutoConfig.SPLIT+lat;
			}
		}
		return value;
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	
	private static String calbaidusn(String url, String ak, String sk, String output, String... paramvalues) throws UnsupportedEncodingException{
		Map<String, String> paramsMap = new TreeMap<String, String>();
        paramsMap.put("output", output);
        paramsMap.put("ak", ak);
        for (int i=0;i<paramvalues.length;i+=2){
        	paramsMap.put(paramvalues[i],paramvalues[i+1]);
        }
        String paramsStr = toQueryString(paramsMap);

        String wholeStr = new String(url.substring(url.indexOf("api.map.baidu.com")+17)+"?" + paramsStr + sk);

        String tempStr = URLEncoder.encode(wholeStr, "UTF-8");

		return MD5(tempStr);
	}
	
	private static String toQueryString(Map<?, ?> data)
                    throws UnsupportedEncodingException {
            StringBuffer queryString = new StringBuffer();
            for (Entry<?, ?> pair : data.entrySet()) {
                    queryString.append(pair.getKey() + "=");
                    queryString.append(URLEncoder.encode((String) pair.getValue(),
                                    "UTF-8") + "&");
            }
            if (queryString.length() > 0) {
                    queryString.deleteCharAt(queryString.length() - 1);
            }
            return queryString.toString();
    }

	private static String MD5(String md5) {
            try {
                    java.security.MessageDigest md = java.security.MessageDigest
                                    .getInstance("MD5");
                    byte[] array = md.digest(md5.getBytes());
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < array.length; ++i) {
                            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
                                            .substring(1, 3));
                    }
                    return sb.toString();
            } catch (java.security.NoSuchAlgorithmException e) {
            }
            return null;
    }
	
}
