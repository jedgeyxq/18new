package lsid.mapred;

import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.util.DefaultCipher;

public class TimeCountMapper extends TableMapper<Text, IntWritable>  {
	
	private final IntWritable ONE = new IntWritable(1);

   	public void map(ImmutableBytesWritable row, Result value, Context context) throws IOException, InterruptedException {
   		try{
   			byte[] c =value.getValue(Bytes.toBytes("cf"), Bytes.toBytes("p"));
   			if (c != null && Bytes.toString(c) != null) {
   				String[] parts = Bytes.toString(c).split("#");
   				if ("t1".equals(parts[2])) {
   					long lucktime = 0l;
		   			if (parts.length>33) {
		   				try {
		   					lucktime = Long.parseLong(parts[16]);
		   				}catch(Exception e) {
		   					//do nothing
		   				}
		   			}
   					if(lucktime>=1517414400000l&&lucktime<=1519747199000l){    					
	   					int pool=Integer.valueOf(parts[34].toString());
	   					if(pool%2!=0){
	   					Text t = new Text();
	   					String prizeid=parts[34]+parts[35];
	   					String prizinfo="";
	   					String rowval = null;
	   					if(parts.length>40){
	   						String status=new String(DefaultCipher.dec(parts[40]));  	   					
	 	   					if(status.toLowerCase().startsWith("success")){
	   	   						rowval=prizeid+"_"+"success";
	   	   					}else if(status.toLowerCase().startsWith("fail")){
	   	   						//rowval=prizeid+"_"+status.replace("#", ",");
	   	   						rowval=prizeid+"_"+"fail";
	   	   					}
	   					}else if(parts.length>38){
	   						 prizinfo=parts[38];
	   						if(prizinfo.isEmpty()){
	   							rowval=prizeid+"_0";
	   						}else{
	   							rowval=prizeid+"_1";
	   						}
	   					}
	   					if(rowval!=null){
	   						t.set(rowval);
	   						context.write(t, ONE);    						
	   					}
   					}
   					
//   					long lucktime = 0l;
//		   			if (parts.length>33) {
//		   				try {
//		   					lucktime = Long.parseLong(parts[16]);
//		   				}catch(Exception e) {
//		   					//do nothing
//		   				}
//		   			}
//		   			SimpleDateFormat sdf = new SimpleDateFormat("HH");
//		   			String curhour =sdf.format(lucktime);
//		   			
//		   			String gen = "0";				
//					try {
//		   				gen=String.valueOf(new ObjectMapper().readTree(new String(DefaultCipher.dec(parts[19]))).get("sex").asInt());
//	   				}catch(Exception e) {
//	   					//do nohting
//	   				}
//		   			
//		   			Text t = new Text();
//		   			t.set(curhour+"_"+gen);
//			   		context.write(t, ONE);   					
   				}
   			}
   			}
   		}catch(Exception ex){
   			throw new IOException(ex);
   		}
   		
   		
   	}
}