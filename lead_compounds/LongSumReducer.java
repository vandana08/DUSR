package lead_compounds;

import java.io.IOException;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
 
public class LongSumReducer extends Reducer<Text, FloatWritable,
                                                 Text, FloatWritable> {
 
  private double cutoff = 0.8;
  private FloatWritable result = new FloatWritable();
  public void reduce(Text key, Iterable<FloatWritable> values,
                     Context context) throws IOException, InterruptedException {
	  for (FloatWritable val : values) {
		  float sim=val.get();
		  System.out.println("--" + sim);
		  if(sim >= cutoff)
		  {
			  result.set(sim);
			  context.write(key, result);
		  }
	  }
  
   
}
}
