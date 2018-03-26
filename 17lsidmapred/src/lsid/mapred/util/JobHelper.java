package lsid.mapred.util;

import java.io.IOException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import java.io.File;

public class JobHelper {

    public JobHelper() {

    }
    

    public static Path addJarToDistributedCache(Class<?> classToAdd, Configuration conf) throws IOException {

        String jar = classToAdd.getProtectionDomain().getCodeSource().getLocation().getPath();

        File jarFile = new File(jar);

        Path hdfsJar = new Path("/data/lsmapred/" + jarFile.getName());

        FileSystem hdfs = FileSystem.get(conf);

        hdfs.copyFromLocalFile(false, true, new Path(jar), hdfsJar);
        hdfs.close();
        return hdfsJar;
    }
}