package com.lsid.listener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.listener.TzbDataChild;

public class TzbData{

	public String sys_time = String.valueOf(System.currentTimeMillis());
	public String activity_name = "扫码红包活动";
	public String activity_remark = "扫码红包";
	public String data_count = "0";
	public String md5_sign = "";
	public List<TzbDataChild> data_list = new ArrayList<TzbDataChild>();

	void setdata_list(List<TzbDataChild> dl) throws Exception {
		data_count = String.valueOf(dl.size());
		md5_sign = getPhpMd5(Base64.encodeBase64(new ObjectMapper().writeValueAsString(dl).getBytes("UTF-8")));
		data_list = dl;
	}

	public static String getPhpMd5(byte[] buffer) {
		String s = null;

		MessageDigest md = null;

		char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		md.update(buffer);
		byte[] datas = md.digest();
		char[] str = new char[2 * 16];
		int k = 0;
		for (int i = 0; i < 16; i++) {
			byte b = datas[i];
			str[k++] = hexChars[b >>> 4 & 0xf];
			str[k++] = hexChars[b & 0xf];
		}
		s = new String(str);
		return s;

	}

}
