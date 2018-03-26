package lsid.mapred;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class Statscan17Reducer  extends TableReducer<Text, IntWritable, ImmutableBytesWritable>  {

	Socket s = null;
	BufferedWriter bw = null;
	
	@Override
	public void setup(Context context) throws IOException {
		s = new Socket(context.getConfiguration().get("socketip"),context.getConfiguration().getInt("socketport", 17178));
		bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
	}
	
	public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
		try {
			long i = 0;
			for (IntWritable val : values) {
				i += val.get();
			}
			String[] parts = key.toString().split("_");
			String eid = parts[0];
			String file = parts[1];
			String from = parts[2];
			String province = parts[3];
			String city = parts[4];
			String prod = parts[5];
			
			bw.write("d#"+eid+"#"+from+"scandata#"+file+"#"+province+"_"+city+"_"+prod+"_"+i);
			bw.newLine();
			bw.flush();
		}catch(Exception e) {
			new IOException(e);
		}
	}
	@Override
	public void cleanup(Context context) throws IOException {
  		if (bw!=null) {
  			bw.close();
  		}
  		if (s!=null) {
  			s.close();
  		}
  	}

}