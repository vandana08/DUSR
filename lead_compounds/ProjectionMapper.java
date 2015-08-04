package lead_compounds;

import org.apache.hadoop.fs.FSDataInputStream;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import org.openscience.cdk.io.listener.PropertiesListener;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.HBondAcceptorCountDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.HBondDonorCountDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.RotatableBondsCountDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.TPSADescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.WeightDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.XLogPDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.similarity.DistanceMoment;


public class ProjectionMapper extends Mapper<LongWritable, Text, Text, FloatWritable> {
	float[] queryMoments;
	private boolean checkAromaticity = false;
	int lipinskifailures=0;
	private static final String[] names = {"LipinskiFailures"};
	private boolean has3D(IAtomContainer mol) 
	{
		for (IAtom atom : mol.atoms())
		{
			if (atom.getPoint3d() == null) return false;
		}
		return true;
	}
	
	public void setParameters(Object[] params) throws CDKException {
        if (params.length != 1) {
            throw new CDKException("RuleOfFiveDescriptor expects one parameter");
        }
        if (!(params[0] instanceof Boolean)) {
            throw new CDKException("The first parameter must be of type Boolean");
        }
        // ok, all should be fine
        checkAromaticity = (Boolean) params[0];
    }
	
	public Object[] getParameters() {
        // return the parameters as used for the descriptor calculation
        Object[] params = new Object[1];
        params[0] = checkAromaticity;
        return params;
    }
	 public String[] getDescriptorNames() {
	        return names;
	    }
	 
	 
  private Text word = new Text();
  //private Text word1 = new Text();
  private FloatWritable count = new FloatWritable(1);
  public void setup(Context context) throws IOException, InterruptedException {
	  

	    Configuration conf = context.getConfiguration();
	    //String param = conf.get("test");
	    //System.out.println(param);
	     URI[] cacheFile = DistributedCache.getCacheFiles(conf);
	    FSDataInputStream in = FileSystem.get(conf).open(new Path(cacheFile[0].getPath()));
	     try
	    {
	    	IAtomContainer query = null;
	    final MDLV2000Reader reader = new MDLV2000Reader(new InputStreamReader(in));
		Properties qprop = new Properties();
		qprop.setProperty("ForceReadAs3DCoordinates", "true");
		PropertiesListener listener = new PropertiesListener(qprop);
		reader.addChemObjectIOListener(listener);
		reader.customizeJob();
		query = NoNotificationChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
		query = reader.read(query);
		System.out.println(query.getProperty(CDKConstants.TITLE));
		if (!has3D(query)) {
			System.err.println("ERROR: Query structure must have 3D coordinates");
			System.exit(-1);
			}
		 queryMoments = DistanceMoment.generateMoments(query);
		 System.out.println(queryMoments[0]);
	    }
		catch (CDKException e) {
			e.printStackTrace();
		}

	}
 
  @Override
  protected void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {
	  String title = null;
	  String mv=null;
	  int i=0;
	  String line=value.toString();
	  float[] moments;
	  moments = new float[12];
	  for (String retval: line.split("\t")){
		  if(i==0)
		  {
	         title=retval;
	      }
		  if(i==1)
		  {
			  mv=retval;
		  }
		  i++;
	  }
	  String x=mv.substring(1,(mv.length()-1));
	  i=0;
	  for (String retval: x.split(",")){
	        // System.out.println("splitted" + retval.trim());
	         try
	         {
	           float f = Float.valueOf(retval.trim()).floatValue();
	           moments[i]=f;
	         }
	         catch (NumberFormatException nfe)
	         {
	           System.out.println("NumberFormatException: " + nfe.getMessage());
	         }
	         i++;
	        }
	  float sum = 0;
		for (i = 0; i < queryMoments.length; i++) {
		sum += Math.abs(queryMoments[i] - moments[i]);
		}
		float sim = (float) (1.0 / (1.0 + sum / 12.0));
		word.set(title);
		count.set(sim);
		System.out.println(word + "\tsim\t" + count);
		context.write(word, count);
		}
      
}
