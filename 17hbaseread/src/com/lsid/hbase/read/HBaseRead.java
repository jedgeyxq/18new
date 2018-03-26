package com.lsid.hbase.read;

import java.io.IOException;
import java.util.Date;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.NamespaceExistException;
import org.apache.hadoop.hbase.NamespaceNotFoundException;
import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import com.lsid.hbase.util.HBaseUtil;

public class HBaseRead {
	
	private static HBaseRead instance = null;
	public static final String unavailable = "==lsidunavailable==";
	public static final String notfound = "==lsidnotfound==";
	
	public static synchronized HBaseRead getinstance() throws Exception{
		if (instance==null){
			instance = new HBaseRead();
		}
		return instance;
	}

	private Connection conn = null;
	private Admin admin = null;
	
	private HBaseRead() throws Exception{
		try{
			System.out.println("===="+new Date()+"==== HBase connecting!");
			conn = ConnectionFactory.createConnection(HBaseConfiguration.create());
			if (admin==null){
				admin = conn.getAdmin();
			}
			try{
				//test connection
				admin.getNamespaceDescriptor("test");
			}catch(NamespaceNotFoundException e){
				//do nothing
			}
			System.out.println("===="+new Date()+"==== HBase connected!");
		}catch(IOException e){
			release();
			throw new Exception(unavailable);
		}
	}
	
	private void release(){
		try {
			admin.close();
		} catch (Exception e) {
			//do nothing
		}
		admin = null;
		try {
			conn.close();
		} catch (Exception e) {
			//do nothing
		}
		conn = null;
		instance = null;
	}
	
	private void createtable(String namespace, String tablename) throws Exception{
		TableName tn = TableName.valueOf(namespace+":"+tablename);
		try{
			if (admin!=null&&!admin.tableExists(tn)){
				try{
					admin.getNamespaceDescriptor(namespace);
				}catch(NamespaceNotFoundException e){
					try {
						admin.createNamespace(NamespaceDescriptor.create(namespace).build());
						System.out.println("===="+new Date()+"==== Namespace ["+namespace+"] created!");
					} catch (NamespaceExistException e1) {
						//do nothing
					}
				}
				HTableDescriptor td = new HTableDescriptor(tn);
				HColumnDescriptor cd = new HColumnDescriptor("cf");
				cd.setMaxVersions(1);
				td.addFamily(cd);
				try{
					admin.createTable(td);
				}catch(TableExistsException e){
					//do nothing
				}
				System.out.println("===="+new Date()+"====Successfully created table ["+namespace+":"+tablename+"]!");
			}
		}catch(IOException e){
			release();
			throw e;
		}
	}
	
	private byte[] get(String namespace, String tablename, String hash, String row, String column) throws Exception{
		Table ht = null;
		byte[] returnvalue = null;
		String columnfamily = "cf";
		try{
			createtable("lsid","17"+tablename);
			ht = conn.getTable(TableName.valueOf("lsid:17"+tablename));
			Get get = new Get(HBaseUtil.rowkey(namespace, hash, row));
			get.addColumn(Bytes.toBytes(columnfamily), 
					Bytes.toBytes(column));
			Result rs=ht.get(get);
			if (rs.isEmpty()){
				throw new Exception(notfound);
			}
			returnvalue = rs.getValue(Bytes.toBytes(columnfamily), 
					Bytes.toBytes(column));
		}catch(TableNotFoundException ex){
			throw new Exception(notfound);
		}catch(IOException ex){
			release();
			throw new Exception(unavailable);
		}finally{
			if (ht!=null){
				ht.close();
				ht=null;
			}
		}
		return returnvalue;
	}
	
	public String getstring(String namespace, String tablename, String hash, String row, String column) throws Exception{
		return Bytes.toString(get(namespace, tablename, hash, row, column));
	}
	
	public long getlong(String namespace, String tablename, String hash, String row, String column) throws Exception{
		return Bytes.toLong(get(namespace, tablename, hash, row, column));
	}

}
