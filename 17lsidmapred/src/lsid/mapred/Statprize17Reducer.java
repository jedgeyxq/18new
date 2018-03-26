package lsid.mapred;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class Statprize17Reducer  extends TableReducer<Text, IntWritable, ImmutableBytesWritable>  {

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
			if (parts.length==10) {
				String eid = parts[0];
				String file = parts[1];
				String from = parts[2];
				String province = parts[3];
				String city = parts[4];
				String prod = parts[5];
				String activity = parts[6];
				String pool = parts[7];
				String prize = parts[8];
				String gen = parts[9];
				
				bw.write("d#"+eid+"#"+from+"trendata#"+file+"#"+province+"_"+city+"_"+prod+"_"+activity+"_"+pool+"_"+prize+"_"+gen+"_"+i);
				bw.newLine();
				bw.flush();
			} else if (parts.length==11) {
				String eid = parts[0];
				String file = parts[1];
				String from = parts[2];
				String province = parts[3];
				String city = parts[4];
				String prod = parts[5];
				String activity = parts[6];
				String pool = parts[7];
				String prize = parts[8];
				String gen = parts[9];
				//String uuid = parts[10];
				
				bw.write("i#"+eid+"#"+from+"timesdata/"+file+"#"+province+"_"+city+"_"+gen+"_"+activity+"_"+prod+"_"+i+"#"+1);
				bw.newLine();
				bw.flush();

				bw.write("i#"+eid+"#"+from+"usersdata/"+file+"#"+province+"_"+city+"_"+prod+"_"+activity+"_"+pool+"_"+prize+"_"+gen+"#"+1);
				bw.newLine();
				bw.flush();
			} else if (parts.length==12) {
				String eid = parts[0];
				String file = parts[1];
				String from = parts[2];
				String province = parts[3];
				String city = parts[4];
				String prod = parts[5];
				String activity = parts[6];
				String pool = parts[7];
				String prize = parts[8];
				String gen = parts[9];
				bw.write("d#"+eid+"#"+from+"confirmprizedata#"+file+"#"+province+"_"+city+"_"+prod+"_"+activity+"_"+pool+"_"+prize+"_"+gen+"_"+i);
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
  			bw.close();
  		}
  		if (s!=null) {
  			s.close();
  		}
  	}

}