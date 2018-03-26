package com.lsid.zfb.filter;

import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserUserinfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserUserinfoShareResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class ZfbInterfaces implements Filter {

	@Override
	public void destroy() {
		
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
	    		String eid = request.getParameter("eid");
	    		
	    		if (eid==null||eid.trim().isEmpty()||eid.equals("null")){
	    			throw new Exception("missingeid");
	    		}
	    		if (request.getParameter("zfbcode")!=null&&!request.getParameter("zfbcode").trim().isEmpty()&&!request.getParameter("zfbcode").equals("null")){
	    			if (request.getParameter("keyprefix")!=null&&!request.getParameter("keyprefix").trim().isEmpty()&&!request.getParameter("keyprefix").equals("null")){
	    				AutoConfig.innerechok(response, getopenid(eid, request.getParameter("zfbcode"), request.getParameter("keyprefix")));
	    			} else {
	    				AutoConfig.innerechok(response, getuserinfo(eid, request.getParameter("zfbcode")));
	    			}
	    		} else {
	    			throw new Exception("missingparams");
	    		}
    		
		} catch (Exception e) {
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			AutoConfig.innerechno(response, request, e);
		}
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
	}
		
	private String getopenid(String eid, String zfbcode, String keyprefix) throws Exception{
		String returnvalue = null;
		if ("lsid.uuwxid".equals(keyprefix)){
			eid = null;
		}
		AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",AutoConfig.config(eid, keyprefix+".appid"),
				AutoConfig.config(eid, keyprefix+".privatekey"),"json","GBK",
				AutoConfig.config(eid, keyprefix+".publickey"));
		AlipaySystemOauthTokenRequest zfbtokenrequest = new AlipaySystemOauthTokenRequest();
		zfbtokenrequest.setCode(zfbcode);
		zfbtokenrequest.setGrantType("authorization_code");
	    AlipaySystemOauthTokenResponse oauthTokenResponse = alipayClient.execute(zfbtokenrequest);
	    String zfbid = oauthTokenResponse.getUserId();
	    
		returnvalue = DefaultCipher.enc(zfbid);
		
		if (returnvalue==null){
			throw new Exception(URLEncoder.encode(oauthTokenResponse.getMsg(),"UTF-8"));
		}
		return returnvalue;
	}
	
	private String getuserinfo(String eid, String zfbcode) throws Exception{
		
		String returnvalue = null;
		
		AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",AutoConfig.config(eid, "lsid.infozfb.appid"),
				AutoConfig.config(eid, "lsid.infozfb.privatekey"),"json","GBK",
				AutoConfig.config(eid, "lsid.infozfb.publickey"));

		AlipaySystemOauthTokenRequest zfbtokenrequest = new AlipaySystemOauthTokenRequest();
		zfbtokenrequest.setCode(zfbcode);
		zfbtokenrequest.setGrantType("authorization_code");
	    AlipaySystemOauthTokenResponse oauthTokenResponse = alipayClient.execute(zfbtokenrequest);
	    String accessToken = oauthTokenResponse.getAccessToken();
		
		AlipayUserUserinfoShareRequest zfbsharerequest = new AlipayUserUserinfoShareRequest();
	    AlipayUserUserinfoShareResponse userinfoShareResponse = alipayClient.execute(zfbsharerequest, accessToken);
		String zfbinfo = userinfoShareResponse.getBody();
		JsonNode jn = new ObjectMapper().readTree(zfbinfo);
		if (jn.get("alipay_user_userinfo_share_response")!=null&&jn.get("alipay_user_userinfo_share_response").get("avatar")!=null){
			returnvalue = DefaultCipher.enc(zfbinfo);
		}
		if (returnvalue==null){
			throw new Exception(URLEncoder.encode(jn.toString(),"UTF-8"));
		}
		return returnvalue;
	}
}
