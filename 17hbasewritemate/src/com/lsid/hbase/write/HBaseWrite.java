package com.lsid.hbase.write;

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
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.lsid.hbase.util.HBaseUtil;

public class HBaseWrite {
	
	private static HBaseWrite instance = null;
	public static final String unavailable = "==lsidunavailable==";
	
	public static synchronized HBaseWrite getinstance() throws Exception{
		if (instance==null){
			instance = new HBaseWrite();
		}
		return instance;
	}

	private Connection conn = null;
	private Admin admin = null;
	
	private HBaseWrite() throws Exception{
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
	
	public void put(String namespace, String tablename, String hash, String row, String column, String value) throws Exception{
		Table ht = null;
		try{
			createtable("lsid", "17"+tablename);
			Put p = new Put(HBaseUtil.rowkey(namespace, hash, row));
			p.addColumn(Bytes.toBytes("cf"), Bytes.toBytes(column), Bytes.toBytes(value));
			ht = conn.getTable(TableName.valueOf("lsid:17"+tablename));
			ht.put(p);
		}catch(IOException ex){
			release();
			throw new Exception(unavailable);
		}finally{
			if (ht!=null){
				ht.close();
				ht = null;
			}
		}
	}

	public void increment(String namespace, String tablename, String hash, String row, String column, long amount) throws Exception{
		Table ht = null;
		try{
			createtable("lsid", "17"+tablename);
			ht = conn.getTable(TableName.valueOf("lsid:17"+tablename));
			Increment incr = new Increment(HBaseUtil.rowkey(namespace, hash, row));
			incr.addColumn(Bytes.toBytes("cf"), Bytes.toBytes(column), amount);
			ht.increment(incr);
		}catch(IOException ex){
			release();
			throw new Exception(unavailable);
		}finally{
			if (ht!=null){
				ht.close();
				ht = null;
			}
		}
	}
	
}
