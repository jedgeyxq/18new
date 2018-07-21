package com.jedge.hm.zfb.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
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

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jedge.hm.zfb.util.Config;

public class Payquery implements Filter {

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
		Map<String, String> returnvalue = new HashMap<String, String>();
		try {
			StringBuffer jb = new StringBuffer();
			String line = null;
			BufferedReader reader = null;
			try {
				reader = request.getReader();
				while ((line = reader.readLine()) != null) {
					jb.append(line);
				}
			} finally {
				reader.close();
			}
			JsonNode jn = new ObjectMapper().readTree(jb.toString());
			String orderid = jn.has("orderid")?jn.get("orderid").asText():null;
			String ali_order_no = jn.has("ali_order_no")?jn.get("ali_order_no").asText():null;
			String nonce_str = jn.has("nonce_str")?jn.get("nonce_str").asText():null;
			String sign = jn.has("sign")?jn.get("sign").asText():null;
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("orderid", orderid);
			data.put("ali_order_no", ali_order_no);
			data.put("nonce_str", nonce_str);

			String expectedsign = Config.createSign(data, Config.HUAXIN_PARTNERKEY);

			if (expectedsign.equals(sign)) {
				AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
						Config.HUAMEI_ZFBAPPID, Config.HUAMEI_ZFBPRIKEY, "json", "GBK", Config.HUAMEI_ZFBPUBKEY, "RSA");
				AlipayTradeQueryRequest alirequest = new AlipayTradeQueryRequest();
				alirequest.setBizContent("{" + "\"out_trade_no\":\"" + (orderid == null ? "" : orderid) + "\","
						+ "\"trade_no\":\"" + (ali_order_no == null ? "" : ali_order_no) + "\"  }");
				AlipayTradeQueryResponse aliresponse = alipayClient.execute(alirequest);
				if (aliresponse.isSuccess()) {
					returnvalue.put("result", "success");
					returnvalue.put("orderid", aliresponse.getOutTradeNo());
					returnvalue.put("ali_order_no", aliresponse.getTradeNo());
					returnvalue.put("time_end",
							new SimpleDateFormat("yyyyMMddHHmmss").format(aliresponse.getSendPayDate()));
					if ("WAIT_BUYER_PAY".equals(aliresponse.getTradeStatus())) {
						returnvalue.put("orderStatus", "1");
					}
					if ("TRADE_SUCCESS".equals(aliresponse.getTradeStatus())
							|| "TRADE_FINISHED".equals(aliresponse.getTradeStatus())) {
						returnvalue.put("orderStatus", "2");
					}
					if ("TRADE_CLOSED".equals(aliresponse.getTradeStatus())) {
						returnvalue.put("orderStatus", "3");
					}
				} else {
					returnvalue.put("result", "success");
					returnvalue.put("orderid", orderid==null?"":orderid);
					returnvalue.put("ali_order_no", ali_order_no==null?"":ali_order_no);
					returnvalue.put("time_end","");
					returnvalue.put("orderStatus", "0");
				}
				response.getWriter().write(new ObjectMapper().writeValueAsString(returnvalue));
			} else {
				returnvalue.put("code", "1");
				throw new Exception("Wrong postdata =["+jb.toString()+"] orderid=[" + orderid + "],ali_order_no=["
						+ ali_order_no + "],nonce_str=[" + nonce_str
						+ "],your sign=[" + sign + "],expected sign=[" + expectedsign + "]");
			}
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			returnvalue.put("result", "fail");
			if (returnvalue.get("code") == null) {
				returnvalue.put("code", "2");
			}
			returnvalue.put("reason", errors.toString());
			response.getWriter().write(new ObjectMapper().writeValueAsString(returnvalue));
		}

	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}

}
