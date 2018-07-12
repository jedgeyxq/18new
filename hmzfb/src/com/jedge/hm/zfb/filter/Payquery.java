package com.jedge.hm.zfb.filter;

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
			String orderid = request.getParameter("orderid");
			String ali_order_no = request.getParameter("ali_order_no");
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("orderid", orderid);
			data.put("ali_order_no", ali_order_no);
			data.put("nonce_str", request.getParameter("nonce_str"));

			String sign = Config.createSign(data, Config.HUAXIN_PARTNERKEY);

			if (sign.equals(request.getParameter("sign"))) {
				AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
						Config.HUAMEI_ZFBAPPID, Config.HUAMEI_ZFBPRIKEY, "json", "GBK", Config.HUAMEI_ZFBPUBKEY,
						"RSA2");
				AlipayTradeQueryRequest alirequest = new AlipayTradeQueryRequest();
				alirequest.setBizContent("{" + "\"out_trade_no\":\"" + orderid + "\"," + "\"trade_no\":\"" +ali_order_no+ "\"  }");
				AlipayTradeQueryResponse aliresponse = alipayClient.execute(alirequest);
				if (aliresponse.isSuccess()) {
					returnvalue.put("result", "success");
					returnvalue.put("orderid", aliresponse.getOutTradeNo());
					returnvalue.put("ali_order_no", aliresponse.getTradeNo());
					returnvalue.put("time_end", new SimpleDateFormat("yyyyMMddHHmmss").format(aliresponse.getSendPayDate()));
					if ("WAIT_BUYER_PAY".equals(aliresponse.getTradeStatus())) {
						returnvalue.put("orderStatus", "1");
					}
					if ("TRADE_SUCCESS".equals(aliresponse.getTradeStatus())||"TRADE_FINISHED".equals(aliresponse.getTradeStatus())) {
						returnvalue.put("orderStatus", "2");
					}
					if ("TRADE_CLOSED".equals(aliresponse.getTradeStatus())) {
						returnvalue.put("orderStatus", "3");
					}
				} else {
					returnvalue.put("code", "0");
					throw new Exception("Alipay error: [" + aliresponse.getCode() + "]");
				}
			} else {
				returnvalue.put("code", "1");
				throw new Exception("Wrong orderid=[" + request.getParameter("orderid") + "],ali_order_no=["
						+ request.getParameter("ali_order_no") + "],nonce_str=[" + request.getParameter("nonce_str")
						+ "],your sign=[" + request.getParameter("sign") + "],expected sign=[" + sign + "]");
			}
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			returnvalue.put("result", "fail");
			if (returnvalue.get("code")==null) {
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
