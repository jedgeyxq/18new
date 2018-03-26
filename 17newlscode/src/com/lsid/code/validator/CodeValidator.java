package com.lsid.code.validator;

import java.text.SimpleDateFormat;

import com.lsid.autoconfig.client.AutoConfig;

public class CodeValidator {
	public static String valid(String namespace, String table, String raw){
		String returnvalue = null;
		try{
			String template = AutoConfig.config(namespace, "lsid.code.valid."+table);
			if (template.isEmpty()){
				throw new Exception("configerror");
			}
			String[] templates = template.split(AutoConfig.SPLIT);
			String[] raws = raw.split(AutoConfig.SPLIT);
			if (templates.length==raws.length){
				boolean haserror = false;
				for (int i = 0; i< templates.length; i++){
					if (templates[i].startsWith("any:")){
						continue;
					}
					if (templates[i].startsWith("contains:")){
						if (!templates[i].substring(9).contains(AutoConfig.SPLIT_HBASE+raws[i]+AutoConfig.SPLIT_HBASE)){
							haserror=true;
							break;
						}
						continue;
					}
					if (templates[i].startsWith("date:")){
						new SimpleDateFormat(templates[i].substring(5)).parse(raws[i]);
					}
				}
				if (!haserror){
					returnvalue = raw;
				}
			}
		}catch(Exception ex){
			//do nothing
		}
		return returnvalue;
	}
	
}
