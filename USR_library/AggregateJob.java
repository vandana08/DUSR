package USR_library;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;


public class AggregateJob extends Configured implements Tool {
	
  @Override
  public int run(String[] args) throws Exception {
	  Job job = new Job(new Configuration());
	  Configuration conf = job.getConfiguration();
	  FileSystem fs = FileSystem.get(conf);
	  job.setJarByClass(getClass());
     Path dcache = new Path("/data/cache/lib");
     try {
     FileStatus[] jars = fs.globStatus(new Path( dcache.toString() + "/*.jar"));
     for (int i=0; i< jars.length; i++) {
     Path path = jars[i].getPath();
     if (fs.exists(path) && jars[i].isFile()) {
     DistributedCache.addFileToClassPath(new Path(dcache.toString() + "/" + path.getName()), job.getConfiguration());
     }
     }
     } catch (IOException e) {
     e.printStackTrace();
     }
    job.setJobName(getClass().getSimpleName());
    job.setInputFormatClass(SDFInputFormat.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
 
    job.setMapperClass(ProjectionMapper.class);
    job.setNumReduceTasks(0);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    return job.waitForCompletion(true) ? 0 : 1;
  }
 
  public static void main(String[] args) throws Exception {
    int rc = ToolRunner.run(new AggregateJob(), args);
    System.exit(rc);
  }
}
