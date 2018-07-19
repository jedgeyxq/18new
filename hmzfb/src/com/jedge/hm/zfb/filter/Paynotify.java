package com.jedge.hm.zfb.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.fluent.Request;

import com.alipay.api.internal.util.AlipaySignature;
import com.jedge.hm.zfb.util.Config;

public class Paynotify implements Filter {

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
			Map<String, String> params = new HashMap<String, String>();
			Map<String, String[]> requestParams = request.getParameterMap();
			for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
				String name = (String) iter.next();
				String[] values = (String[]) requestParams.get(name);
				String valueStr = "";
				for (int i = 0; i < values.length; i++) {
					valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
				}
				// 乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
				// valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");
				params.put(name, valueStr);
			}
			// 获取支付宝的通知返回参数，可参考技术文档中页面跳转同步通知参数列表(以下仅供参考)//
			// 商户订单号

			String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
			// 支付宝交易号

			String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "UTF-8");

			// 交易状态
			String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");

			// 获取支付宝的通知返回参数，可参考技术文档中页面跳转同步通知参数列表(以上仅供参考)//
			// 计算得出通知验证结果
			// boolean AlipaySignature.rsaCheckV1(Map<String, String> params, String
			// publicKey, String charset, String sign_type)
			boolean verify_result = AlipaySignature.rsaCheckV1(params, Config.HUAMEI_ZFBPUBKEY, "UTF-8", "RSA");

			if (verify_result) {// 验证成功
				//////////////////////////////////////////////////////////////////////////////////////////
				// 请在这里加上商户的业务逻辑程序代码

				// ——请根据您的业务逻辑来编写程序（以下代码仅作参考）——

				if (trade_status.equals("TRADE_FINISHED")) {
					// 判断该笔订单是否在商户网站中已经做过处理
					// 如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
					// 请务必判断请求时的total_fee、seller_id与通知时获取的total_fee、seller_id为一致的
					// 如果有做过处理，不执行商户的业务程序

					// 注意：
					// 如果签约的是可退款协议，退款日期超过可退款期限后（如三个月可退款），支付宝系统发送该交易状态通知
					// 如果没有签约可退款协议，那么付款完成后，支付宝系统发送该交易状态通知。
				} else if (trade_status.equals("TRADE_SUCCESS")) {
					// 判断该笔订单是否在商户网站中已经做过处理
					// 如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
					// 请务必判断请求时的total_fee、seller_id与通知时获取的total_fee、seller_id为一致的
					// 如果有做过处理，不执行商户的业务程序

					// 注意：
					// 如果签约的是可退款协议，那么付款完成后，支付宝系统发送该交易状态通知。
				}
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("orderid", out_trade_no);
				data.put("ali_order_no", trade_no);
				data.put("nonce_str", new Random().nextInt(100000));
				data.put("orderStatus", "2");
				
				String sign = Config.createSign(data, Config.HUAXIN_PARTNERKEY);
				Request.Get(Config.HUAXIN_PAYNOTIFY + "?orderid=" + data.get("orderid") + "&ali_order_no="
						+ data.get("ali_order_no") + "&nonce_str=" + data.get("nonce_str") + "&orderStatus="
						+ data.get("orderStatus") + "&sign=" + sign)
						.connectTimeout(20000).socketTimeout(20000).execute().returnContent()
						.asString(Charset.forName("UTF-8"));

				// ——请根据您的业务逻辑来编写程序（以上代码仅作参考）——
				response.getWriter().write("success"); // 请不要修改或删除

				//////////////////////////////////////////////////////////////////////////////////////////
			} else {// 验证失败
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("orderid", out_trade_no);
				data.put("ali_order_no", trade_no);
				data.put("nonce_str", new Random().nextInt(100000));
				data.put("orderStatus", "3");
				String sign = Config.createSign(data, Config.HUAXIN_PARTNERKEY);
				Request.Get(Config.HUAXIN_PAYNOTIFY + "?orderid=" + data.get("orderid") + "&ali_order_no="
						+ data.get("ali_order_no") + "&nonce_str=" + data.get("nonce_str") + "&orderStatus="
						+ data.get("orderStatus") + "&sign=" + sign)
						.connectTimeout(20000).socketTimeout(20000).execute().returnContent()
						.asString(Charset.forName("UTF-8"));
				response.getWriter().write("fail");
			}
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			System.out.println(new Date()+" ==== error when handing ["+request.getRequestURI()+"]"+errors.toString());
		}

	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}

}
