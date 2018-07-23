package com.jedge.hm.zfb.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Date;

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
import com.jedge.hm.zfb.data.Cache;
import com.jedge.hm.zfb.util.Config;

public class Authback implements Filter {

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
			String zfbcode = request.getParameter("auth_code");
			String ticket = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/") + 1);
			String codeString = Cache.read(ticket, "codeString");
			String projectId = Cache.read(ticket, "projectId");

			if (zfbcode != null && codeString != null && projectId != null) {
				AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", Config.HUAMEI_ZFBAPPID,
						Config.HUAMEI_ZFBPRIKEY, "json", "GBK", Config.HUAMEI_ZFBPUBKEY);

				AlipaySystemOauthTokenRequest zfbtokenrequest = new AlipaySystemOauthTokenRequest();
				zfbtokenrequest.setCode(zfbcode);
				zfbtokenrequest.setGrantType("authorization_code");
				AlipaySystemOauthTokenResponse oauthTokenResponse = alipayClient.execute(zfbtokenrequest);
				String accessToken = oauthTokenResponse.getAccessToken();

				AlipayUserUserinfoShareRequest zfbsharerequest = new AlipayUserUserinfoShareRequest();
				AlipayUserUserinfoShareResponse userinfoShareResponse = alipayClient.execute(zfbsharerequest,
						accessToken);
				String zfbinfo = userinfoShareResponse.getBody();
				JsonNode jn = new ObjectMapper().readTree(zfbinfo);
				if (jn.get("alipay_user_userinfo_share_response") != null
						&& jn.get("alipay_user_userinfo_share_response").get("avatar") != null) {
					String openid = jn.get("alipay_user_userinfo_share_response").get("user_id").asText();
					String nickname = jn.get("alipay_user_userinfo_share_response").get("nick_name").asText();
					String sex = jn.get("alipay_user_userinfo_share_response").get("gender").asText();
					String headimgurl = jn.get("alipay_user_userinfo_share_response").get("avatar").asText();
					String unionid = "";
					String scanWay = "1";
					String province = jn.get("alipay_user_userinfo_share_response").get("province").asText();
					String city = jn.get("alipay_user_userinfo_share_response").get("city").asText();
					String country = "China";
					String url = Config.HUAXIN_AUTHFINISH + "?openid=" + openid + "&nickname=" + nickname + "&sex="
							+ sex + "&headimgurl=" + headimgurl + "&unionid=" + unionid + "&scanWay=" + scanWay
							+ "&province=" + province + "&city=" + city + "&country=" + country + "&codeString="
							+ codeString + "&projectId=" + projectId;
					System.out.println(new Date()+" ==== after auth ["+url+"]");
					response.sendRedirect(url);
				} else {
					throw new Exception("Wrong zfbinfo format=" + URLEncoder.encode(zfbinfo, "UTF-8"));
				}

			} else {
				throw new Exception("Wrong ticket=[" + ticket+"], codeString=["+codeString+"], projectId=["+projectId+"], auth_code=["+zfbcode+"]");
			}
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			response.getWriter().write(errors.toString());
		}

	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}

}
