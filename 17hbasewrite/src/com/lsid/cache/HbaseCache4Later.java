package com.lsid.cache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import com.lsid.autoconfig.client.AutoConfig;

public class HbaseCache4Later {
	public static void later(String namespace, String tablename, String column, String hash, String row, String value) throws Exception{
		if (value==null||"null".equals(value.toLowerCase())||value.trim().isEmpty()){
			value="";
		}
		
		writefile(namespace, tablename, "put"+AutoConfig.SPLIT+hash+AutoConfig.SPLIT+row+AutoConfig.SPLIT+column+AutoConfig.SPLIT+value);
	}
	
	public static void later(String namespace, String tablename, String column, String hash, String row, long amount) throws Exception{
		if (amount!=0){
			writefile(namespace, tablename, "increment"+AutoConfig.SPLIT+hash+AutoConfig.SPLIT+row+AutoConfig.SPLIT+column+AutoConfig.SPLIT+amount);
		}
	}
	
	public static void writefile(String namespace, String tablename, String content) throws Exception{
		byte[] contents = content.getBytes("UTF-8");
		Path thefolder = Paths.get(AutoConfig.config(null,"hbaselaterfolder")).resolve("writehbaselater").resolve(namespace);
		if (!Files.exists(thefolder)){
			Files.createDirectories(thefolder);
		}
		Files.write(thefolder.resolve(tablename+AutoConfig.SPLIT+UUID.randomUUID().toString().replaceAll("-", "")+AutoConfig.SPLIT+contents.length), contents, StandardOpenOption.CREATE_NEW);
		
	}
}
