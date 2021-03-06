package USR;

import org.apache.hadoop.fs.FSDataInputStream;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.io.FloatWritable;
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


public class ProjectionMapper extends Mapper<Object, Text, Text, FloatWritable> {
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
  protected void map(Object key, Text value, Context context)
      throws IOException, InterruptedException {
	  String titleField = null;
	  try 
		{
		  IteratingMDLReader ireader=null;
		  StringReader sreader = new StringReader(value.toString());
				ireader= new IteratingMDLReader(sreader, NoNotificationChemObjectBuilder.getInstance());
			
			Properties prop = new Properties();
			prop.setProperty("ForceReadAs3DCoordinates", "true");
			PropertiesListener listener = new PropertiesListener(prop);
			ireader.addChemObjectIOListener(listener);
			ireader.customizeJob();
			int nmol = 0;
			while (ireader.hasNext()) {
				IAtomContainer target = (IAtomContainer) ireader.next();
				String title = (String) (titleField != null ? target.getProperty(titleField) : target.getProperty(CDKConstants.TITLE));
				if (title == null)
				{
					title="Not_Available";
				}
				
				//logp value
				IMolecularDescriptor xlogP = new XLogPDescriptor();
				Object[] xlogPparams = {
		                checkAromaticity,
		            Boolean.TRUE,
		        };
				xlogP.setParameters(xlogPparams);
	            double xlogPvalue = ((DoubleResult) xlogP.calculate(target).getValue()).doubleValue();
	            //H-bond acceptor
	            IMolecularDescriptor acc = new HBondAcceptorCountDescriptor();
	            Object[] hBondparams = {checkAromaticity};
	            acc.setParameters(hBondparams);
	            int acceptors = ((IntegerResult) acc.calculate(target).getValue()).intValue();
	            //H-bond donor
	            IMolecularDescriptor don = new HBondDonorCountDescriptor();
	            don.setParameters(hBondparams);
	            int donors = ((IntegerResult) don.calculate(target).getValue()).intValue();
	            //Molecular weight
	            IMolecularDescriptor mw = new WeightDescriptor();
	            Object[] mwparams = {"*"};
	            mw.setParameters(mwparams);
	            double mwvalue = ((DoubleResult) mw.calculate(target).getValue()).doubleValue();
	            //rotatable bond
	            IMolecularDescriptor rotata = new RotatableBondsCountDescriptor();
	            rotata.setParameters(hBondparams);
	            int rotatablebonds = ((IntegerResult) rotata.calculate(target).getValue()).intValue();
	            //TPSA
	            IMolecularDescriptor polar_sa = new TPSADescriptor();
	            Object[] tpsaparam = {checkAromaticity};
	            polar_sa.setParameters(tpsaparam);
	            double tpsavalue = ((DoubleResult) polar_sa.calculate(target).getValue()).doubleValue();
	            //Checking lipinki's rule
	            if (xlogPvalue > 5.0) {
	            	lipinskifailures += 1;
	            	}
	            	if (acceptors > 10) {
	            	lipinskifailures += 1;
	            	}
	            	if (donors > 5) {
	            	lipinskifailures += 1;
	            	}
	            	if (mwvalue > 500.0) {
	            	lipinskifailures += 1;
	            	}
	            	if (rotatablebonds > 10.0) {
	            	lipinskifailures += 1;
	            	}
	            	if (tpsavalue > 140.0) {
		            	lipinskifailures += 1;
		            	}
	            
	            System.out.println("xlogPvalue \t" +  xlogPvalue);
	            System.out.println("H-bond acceptor \t" +  acceptors);
	            System.out.println("H-bond donor \t" +  donors);
	            System.out.println("Molecular weights \t" +  mwvalue);
	            System.out.println("Rotatable bonds \t" +  rotatablebonds);
	            System.out.println("Total polar surface area \t" +  tpsavalue);
	            
				float[] moments;
				nmol++;
				if (!has3D(target) || target.getAtomCount() == 1) {
				System.err.println("\nERROR: " + title + " had no 3D coordinates or else has a single atom. Using NAs");
				moments = new float[12];
				for (int i = 0; i < 12; i++) moments[i] = Float.NaN;
				} else {
				moments = DistanceMoment.generateMoments(target);
				float sum = 0;
				for (int i = 0; i < queryMoments.length; i++) {
				sum += Math.abs(queryMoments[i] - moments[i]);
				}
				float sim = (float) (1.0 / (1.0 + sum / 12.0));
				//System.out.println("title \t" +  title + "\tsim\t" + sim);
				word.set(title);
				count.set(sim);
				System.out.println("---title \t" +  word + "\tsim\t" + count);
				context.write(word, count);
				}
				
			}
			} catch (CDKException e) {
				e.printStackTrace();
			}
		}
      
}
