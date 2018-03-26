package lsid.mapred;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class tzbprovinceTimeCountReducer  extends TableReducer<Text, IntWritable, ImmutableBytesWritable>  {

	Socket s = null;
	BufferedWriter bw = null;
	
	
	@Override
	public void setup(Context context) throws IOException {
		s = new Socket("10.19.131.205",17178);
		bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
	}
	
	public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
		try {
			long i = 0;
			for (IntWritable val : values) {
				i += val.get();
			}
			bw.write(("d#t1#codestat#result201805#"+key.toString()+"_"+i));
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