package com.lsid.hbase.util;

import org.apache.hadoop.hbase.util.Bytes;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class HBaseUtil {
	private static final String hashvalue="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static byte[] rowkey(String eid, String hash, String row){
		if (hash!=null&&!hash.trim().isEmpty()){
			return Bytes.toBytes(hashvalue.charAt(Math.abs(hash.hashCode())%hashvalue.length())+row+AutoConfig.SPLIT_HBASE+eid);
		} else {
			return Bytes.toBytes(row+AutoConfig.SPLIT_HBASE+eid);
		}
	}
	
	public static void main(String[] s) throws Exception{
		//String enc = DefaultCipher.enc("Poh96Q9EF");
		//System.out.println(new String(rowkey("t1",enc,enc)));
		System.out.println("1508731139216="+new java.util.Date(1508731139216l));
	}
}
