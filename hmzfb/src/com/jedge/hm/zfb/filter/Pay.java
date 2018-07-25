package com.jedge.hm.zfb.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.jedge.hm.zfb.util.Config;

public class Pay implements Filter {

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
			String ip = request.getHeader("X-FORWARDED-FOR");
			if (ip == null) {
				ip = req.getRemoteAddr();
			}
			if (Config.HUAMEI_ZFBALLOWIP!=null&&!Config.HUAMEI_ZFBALLOWIP.trim().isEmpty()&&!Config.HUAMEI_ZFBALLOWIP.contains(ip)) {
				throw new Exception("Not allowed ip=["+ip+"]");
			}
			String orderid = request.getParameter("orderid");
			Long amount = Long.parseLong(request.getParameter("amount"));
			String content = request.getParameter("content");
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("orderid", orderid);
			data.put("amount", amount);
			data.put("content", content);

			String expectedsign = Config.createSign(data, Config.HUAXIN_PARTNERKEY);

			if (expectedsign.equals(request.getParameter("sign"))&&orderid != null && amount > 0 && content != null) {
				// 商户订单号，商户网站订单系统中唯一订单号，必填
				String out_trade_no = orderid;
				// 订单名称，必填
				String subject = "华美优惠券";// new
											// String(request.getParameter("WIDsubject").getBytes("ISO-8859-1"),"UTF-8");
				if (request.getParameter("subject") != null) {
					subject = request.getParameter("subject");
				}
				// 付款金额，必填
				String total_amount = String.valueOf(Double.parseDouble(String.valueOf(amount)) / 100);
				// 商品描述，可空
				String body = content;
				// 超时时间 可空
				String timeout_express = "2m";
				// 销售产品码 必填
				String product_code = "huameicoupon";// "QUICK_WAP_WAY";
				if (request.getParameter("productcode") != null) {
					product_code = request.getParameter("productcode");
				}
				/**********************/
				// SDK 公共请求类，包含公共请求参数，以及封装了签名与验签，开发者无需关注签名与验签
				// 调用RSA签名方式
				AlipayClient client = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", Config.HUAMEI_ZFBAPPID,
						Config.HUAMEI_ZFBPRIKEY, "json", "UTF-8", Config.HUAMEI_ZFBPUBKEY, "RSA");
				AlipayTradeWapPayRequest alipay_request = new AlipayTradeWapPayRequest();

				// 封装请求支付信息
				AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
				model.setOutTradeNo(out_trade_no);
				model.setSubject(subject);
				model.setTotalAmount(total_amount);
				model.setBody(body);
				model.setTimeoutExpress(timeout_express);
				model.setProductCode(product_code);
				alipay_request.setBizModel(model);
				// 设置异步通知地址
				alipay_request.setNotifyUrl(Config.ZFB_PAYNOTIFY);
				// 设置同步地址
				alipay_request.setReturnUrl(Config.ZFB_PAYRETURN);

				// form表单生产
				String form = "";
				// 调用SDK生成表单
				form = client.pageExecute(alipay_request).getBody();
				response.setContentType("text/html;charset=UTF-8");
				response.getWriter().write(form);// 直接将完整的表单html输出到页面
				response.getWriter().flush();
				response.getWriter().close();

			} else {
				throw new Exception(
						"Wrong orderid=[" + orderid + "],amount=[" + amount + "(fen)],content=[" + content + "], your sign=["+request.getParameter("sign")+"], expected sign=["+expectedsign+"]");
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
