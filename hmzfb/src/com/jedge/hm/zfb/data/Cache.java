package com.jedge.hm.zfb.data;

import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

public class Cache {
	private static Map<String, String> cache = new Hashtable<String, String>(10000);
	public synchronized static String write(String name, String value) {
		String key = UUID.randomUUID().toString().replaceAll("-", "");
		cache.put(key+"-"+name, value);
		return key;
	}
	public synchronized static void write(String key, String name, String value) {
		cache.put(key+"-"+name, value);
	}
	public synchronized static String read(String key, String name) {
		try {
			return cache.get(key+"-"+name);
		}finally {
			cache.remove(key+"-"+name);
		}
	}
	
}
