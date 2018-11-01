package lsid.mapred;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

import com.lsid.util.DefaultCipher;

public class ScanhourMapper extends TableMapper<Text, IntWritable>  {
	
	private final IntWritable ONE = new IntWritable(1);
	private final SimpleDateFormat day = new java.text.SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat hour = new java.text.SimpleDateFormat("HH");

	/*
	column=cf:s, value=
			play#
			wx#
			qq#
			JVx%2BnpYUfsmuoNBPFO5PXOLsPZSwz9jkit6ld%2FgLdt0%3D#
			JVx%2BnpYUfsmuoNBPFO5PXF7jkvFyCJWbxcaylNTexgaGFIMTEL2pAfTb%2BccrFZIuN%2FE0RWQImAQz%0AuLlp1LLS8A%3D%3D#
			#
			Mozilla%2F5.0+%28Windows+NT+6.1%3B+WOW64%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%2F39.0.2171.95+Safari%2F537.36+MicroMessenger%2F6.5.2.501+NetType%2FWIFI+WindowsWechat+QBCore%2F3.43.556.400+QQBrowser%2F9.0.2524.400#
			101.81.231.157#
			%E4%B8%8A%E6%B5%B7%E5%B8%82#
			%E4%B8%8A%E6%B5%B7%E5%B8%82#
			%E9%97%B8%E5%8C%97%E5%8C%BA#
			%E5%A4%A9%E6%BD%BC%E8%B7%AF#
			619%E5%8F%B7#
			%E4%B8%AD%E5%9B%BD#
			121.48789948999993#
			31.249161578948787#
			1501655437921#
			8X3jIkm6oXqNGJtDUsJ1HnwPlKGQmXHKHrwCh0qyz4M%253D#
			1501655441076#
			l%2FSGu5OlE7PRpyxJ7ShfJYRGbrXh8IcES1IlDsb2%2Bf0%2BFA538JTILSpmTIueHgG5CKQT%2FmBoYPb1%0AU87HlMOQL4asg7H%2BUQ5WHn6jAOZn%2F0o2OhviDpvbVIY%2BKdUPvxhiEnU9%2Fd90H3qCv%2FtUbihRUyBx%0A91205YClpdzoQ1K%2F2b2pyDlpy3GXqudadZxlogb20hz%2FS%2FL9vig0ObchGIpUj0eElrifcaRxVHEd%0AaL%2BGr4LZfYj%2BXdzGH%2B%2FFIkl68HnDG3wQgt%2Buw6FZBZnZ98pZQn5kzExndT6QT1FzQyA3nsOhkP%2B5%0AhJGh40VMXszAeX0NBu0hw54SmLab3Y6PaUoqcp%2FWMNYuJTfOSIiFBdC4iewnBQRs5JgcohukrRn1%0A2YckZkREbspzPFh64reBXe8%2Bjy1sKRZXxo7UAP33ayhdDPI%3D#
			8X3jIkm6oXqNGJtDUsJ1HnwPlKGQmXHKHrwCh0qyz4M%253D#
			0
	 */
	
   	public void map(ImmutableBytesWritable row, Result value, Context context) throws IOException, InterruptedException {
   		try{
   			byte[] c = value.getValue(Bytes.toBytes("cf"), Bytes.toBytes("s"));
	   		if (c!=null&&Bytes.toString(c)!=null) {
	   			String[] parts = Bytes.toString(c).split("#");
	   			String eid = parts[2];
	   			if (eid.equals("t2")) {
	   			long scantime = 0l;
	   			if (parts.length>16) {
	   				try {
	   					scantime = Long.parseLong(parts[16]);
	   				}catch(Exception e) {
	   					//do nothing
	   				}
	   			}
	   				Text t = new Text();
		   			t.set(Bytes.toBytes(day.format(new Date(scantime))+"_"+hour.format(new Date(scantime))));
			   		context.write(t, ONE);
	   			}
	   		}
   			
   		}catch(Exception ex){
   			throw new IOException(ex);
   		}
   		
   		
   	}
}