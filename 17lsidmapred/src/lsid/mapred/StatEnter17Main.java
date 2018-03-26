package lsid.mapred;

import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

import lsid.mapred.util.JobHelper;

public class StatEnter17Main{
	public static void main(String[] s) throws IOException{
		try {
			String socketip = s[0];
			String socketport = s[1];
			String tablename=s[2];
			String startimemillis = s[3];
			String endtimemillis = s[4];
			
			String classname = "";
			if (tablename.length()>6) {
				try {
					new SimpleDateFormat("yyyyMM").parse(tablename.substring(tablename.length()-6));
					classname=tablename.substring(0, tablename.length()-6);
				}catch(Exception e) {
					classname=tablename;
				}
			} else {
				classname = tablename;
			}
			Configuration config = HBaseConfiguration.create();
			config.setLong("start", Long.parseLong(startimemillis));
			config.setLong("end", Long.parseLong(endtimemillis));
			config.set("socketip", socketip);
			config.setInt("socketport", Integer.parseInt(socketport));
			
			//set product index of eid, param template is eid_1
			for (int i=5;i<s.length;i++) {
				config.setInt("prodindex_"+s[i].substring(0, s[i].indexOf("_")), Integer.parseInt(s[i].substring(s[i].indexOf("_")+1)));
			}
			
			Job job = Job.getInstance(config);
			job.setJarByClass(StatEnter17Main.class);  
			
			job.addFileToClassPath(JobHelper.addJarToDistributedCache(ObjectMapper.class, config));

			Scan scan = new Scan();		
			TableMapReduceUtil.initTableMapperJob(
				"lsid:17"+tablename,
				scan,
				((TableMapper<?, ?>)Class.forName("lsid.mapred.Stat"+classname+"17Mapper").newInstance()).getClass(),  
				Text.class,       
				IntWritable.class,
				job);
			job.setReducerClass(((TableReducer<?, ?, ?>)Class.forName("lsid.mapred.Stat"+classname+"17Reducer").newInstance()).getClass());  
			job.setNumReduceTasks(1); 
			job.setOutputFormatClass(NullOutputFormat.class);
			boolean b = job.waitForCompletion(true);
			if (!b) {
				throw new IOException("error with job!");
			}
		}catch(Exception e) {
			throw new IOException(e);
		}
	}
}