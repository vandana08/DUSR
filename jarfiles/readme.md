Step 1: Before setting up Hadoop cluster, all nodes should be configured first. It can be done by following steps in given link: http://blog.puneethabm.in/hadoop-cloudera-cluster-set-up/
Step 2: Hadoop multi node cluster setup is done by following this link: http://tecadmin.net/set-up-hadoop-multi-node-cluster-on-centos-redhat/
Step 3: Install CDK on Master node.
Step 4: Download an active reference molecule in sdf format (say file1) against which similar shaped compounds are to be searched.
Step 5: Download a chemical database of compounds in sdf format (say file2) from which structurally similar shaped compounds are to be extracted. 
Step 6: Make a directory ‘/data/cache/lib’ in Hadoop filesystem and copy following jar in this directory:
	cdk-core.jar
	cdk-data.jar
	cdk-fingerprint.jar
	cdk-interfaces.jar
	cdk-io.jar
	cdk-ioformats.jar
	cdk-isomorphism.jar
	cdk-nonotify.jar
	cdk-qsar.jar
	cdk-qsarmolecular.jar
	cdk-standard.jar
	cdk-valencycheck.jar
	jgraph-0.5.3.jar
	vectmath-1.3.1.jar
Step 7: Copy file1 and file2 to Hadoop filesystem.
Step 8: Download file ‘USR_n.jar’ and run following commands for DistMapVector (DMV) approach: 
hadoop jar /path/to/USRn.jar USR.AggregateJob /path/to/file1 /path/to/file2 /path/to/output_file
In this output_file is the required output.
Step 9: For running via DistMapVectorLibScreen (DMVLS):
i)	Download ‘lib_noreduce.jar’ and run following command for building library:
hadoop jar /path/to/USR_library.jar USR_library.AggregateJob / path/to/file2 /path/to/output_file1
ii)	Download ‘screening.jar’ and run following command for final screening of compounds:

hadoop jar /path/to/screening.jar lead_compounds.AggregateJob /path/to/file1 /path/to/output_file1 /path/to/final_output_file
In this final_output_file is the required output.
