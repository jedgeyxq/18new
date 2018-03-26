package com.lsid.util;

import java.util.Hashtable;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class SpringJdbc4mysql {
	private static ConfigurableApplicationContext springcontext = new ClassPathXmlApplicationContext("springcontext.xml");
	private static Map<String, JdbcTemplate> cachejdbc = new Hashtable<String, JdbcTemplate>();
	private static Map<String, TransactionTemplate> cachetransaction = new Hashtable<String, TransactionTemplate>();
	
	public static synchronized JdbcTemplate getJdbc(String sharding) {
		if (sharding==null||sharding.trim().isEmpty()){
			sharding = "0";
		}
		if (cachejdbc.get(sharding)==null){
			cachejdbc.put(sharding, new JdbcTemplate((DataSource) springcontext.getBean("dataSource"+sharding)));
		}
		return cachejdbc.get(sharding);
	}

	public static synchronized TransactionTemplate getTransaction(String sharding) {
		if (sharding==null||sharding.trim().isEmpty()){
			sharding = "0";
		}
		if (cachetransaction.get(sharding)==null){
			cachetransaction.put(sharding, new TransactionTemplate((PlatformTransactionManager) springcontext.getBean("transactionManager"+sharding)));
		}
		return cachetransaction.get(sharding);
	}
	
	public static void release(){
		springcontext.close();
		springcontext = null;
		cachejdbc.clear();
		cachejdbc = null;
		cachetransaction.clear();
		cachetransaction = null;
	}
}
