package com.lsid.hbase.util;

import org.apache.hadoop.hbase.util.Bytes;
import com.lsid.autoconfig.client.AutoConfig;

public class HBaseUtil {
	private static final String hashvalue = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public static byte[] rowkey(String eid, String hash, String row) {
		if (hash != null && !hash.trim().isEmpty()) {
			return Bytes.toBytes(hashvalue.charAt(Math.abs(hash.hashCode()) % hashvalue.length()) + row
					+ AutoConfig.SPLIT_HBASE + eid);
		} else {
			return Bytes.toBytes(row + AutoConfig.SPLIT_HBASE + eid);
		}
	}
	
	public static void main(String[] args) {
		//0%2B%2B%2BOO21gX5QM2vmJwNc0BQ%3D%3D_q
		String eid="m";
		String hash="6nTaiTVpUSVPdnbwffLzwQ%3D%3D";
		String row="6nTaiTVpUSVPdnbwffLzwQ%3D%3D";
		String rowkey=new String(rowkey(eid,hash,row));
		System.out.println(rowkey);
		
	}

}
