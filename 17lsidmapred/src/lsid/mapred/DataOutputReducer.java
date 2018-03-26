package lsid.mapred;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;

public class DataOutputReducer  extends TableReducer<ImmutableBytesWritable, ImmutableBytesWritable, ImmutableBytesWritable>  {

	Socket s = null;
	BufferedWriter bw = null;
	
	@Override
	public void setup(Context context) throws IOException {
		s = new Socket("10.19.131.205",17178);
		bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
	}
	
	public void reduce(ImmutableBytesWritable key, Iterable<ImmutableBytesWritable> values, Context context) throws IOException, InterruptedException {
		try {
			for (ImmutableBytesWritable val : values) {
				bw.write("d#"+context.getConfiguration().get("eid")+"#"+"dataoutput#"+
						"table-"+context.getConfiguration().get("table")+"-"+context.getConfiguration().get("startime")+"#"+
						Bytes.toString(val.get()).replaceAll("#", ","));
				bw.newLine();
				bw.flush();
			}
						
		}catch(Exception e) {
			new IOException(e);
		}
	}
	@Override
	public void cleanup(Context context) throws IOException {
  		if (bw!=null) {
  			try{
  				bw.close();
  			}catch(Exception e){
  				//do nothing
  			}
  		}
  		if (s!=null) {
  			try{
  				s.close();
  			}catch(Exception e){
  				//do nothing
  			}
  		}
  	}

}