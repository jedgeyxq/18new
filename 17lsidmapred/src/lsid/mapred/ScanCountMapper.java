package lsid.mapred;

import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class ScanCountMapper extends TableMapper<Text, IntWritable>  {
	
	private final IntWritable ONE = new IntWritable(1);

   	public void map(ImmutableBytesWritable row, Result value, Context context) throws IOException, InterruptedException {
   		try{
   			String[] val=Bytes.toString(row.get()).split("_");
   			if(val.length>2){
   				String eid = Bytes.toString(row.get()).split("_")[3];
   				long timestemp=Long.valueOf(Bytes.toString(row.get()).split("_")[2]);
   				SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
   				long timeStart=sdf.parse("2017-01-01 00:00:00").getTime();
   				if(timestemp>=timeStart){
   					Text t = new Text();
   		   			t.set(eid);
   			   		context.write(t, ONE);
   				}   				
   			}else{
   				String eid = Bytes.toString(row.get()).split("_")[1];
   				String eid1=eid+"1";
   				String val1=Bytes.toString(value.getValue(Bytes.toBytes("cf"), Bytes.toBytes("c")));
   		   		String year=val1.split("#")[4].substring(0,4);
   		   		if(year.equals("2017")){
   		   			Text t = new Text();
   		   			t.set(eid1);
   			   		context.write(t, ONE);
   		   		}
   			}
	   		
   		}catch(Exception ex){
   			throw new IOException(ex);
   		}
   		
   		
   	}
}