/* Copyright (C) 2013, Siddharth Gopal (gcdart AT gmail)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2.1 of the GNU Lesser General Public License
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package hadoop;
import java.io.IOException;
import java.util.Vector;

import ml.BinarySVM;
import ml.LogisticRegression;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.*;

import base.Example;
import base.WeightParameter;

public class TrainingDriver extends Configured implements Tool {
	public final static int iterations = 100;
	
	public static class TrainingDriverMapper extends Mapper<LongWritable,Text,IntWritable,WeightParameter> {
		int once = 0;
		Vector<Example> data;		
		
		void printUsage(){
			Runtime r = Runtime.getRuntime();
			r.gc();
			double mb = (1024*1024);
			System.out.println(" \tSetup Memory ");
			System.out.println(" \tUsed Memory:" + (r.totalMemory() - r.freeMemory())/mb );
			System.out.println(" \tTotal Memory:" + r.totalMemory()/mb );
			System.out.println(" \tMax Memory:" + (r.maxMemory())/mb );
		}
		
		protected void setup(Context context ) throws IOException {
			if( once == 1 ) return; once = 1;
			
			Configuration conf = context.getConfiguration();			
			System.out.println(" Opening Training Instances From " + conf.get("gc.TrainingDriver.dataset") );            

			data = new Vector<Example>();
			Path[] listedPaths = DistributedCache.getLocalCacheFiles( conf );
			FileSystem fs = FileSystem.getLocal( conf );	
			
			// Parse the training data
			for( Path p : listedPaths ) {
				System.out.println(" Opening Distributed Cache file " + p.toString() );
				SequenceFile.Reader reader = new SequenceFile.Reader(fs,p,conf);
				Writable Key = (Writable) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
				Writable Value = (Writable) ReflectionUtils.newInstance(reader.getValueClass(), conf);
				
				int cnt = 0;
				while( reader.next(Key,Value) ){
					data.add( new Example((Example)Value) );
					cnt++;
					if ( cnt%5000 == 0 ) {
						printUsage();
						System.out.println( " \t data.size() = " + data.size() );
					}
				}
			}
			System.out.println(" Loaded " + data.size()+ " Instances ");
		}
		
		public void map(LongWritable key, Text v, Context context ) throws IOException, InterruptedException {
			int node = Integer.parseInt(v.toString());
			System.out.print(" Training Classifier for class-label = " + node );
			
			WeightParameter param = new WeightParameter();
			param.node = node;

			Configuration conf = context.getConfiguration();	
			
			if ( conf.get("gc.TrainingDriver.classifier").equals("svm") ) {
				double C = Double.parseDouble( conf.get("gc.TrainingDriver.svm.C","1") );
				double eps = Double.parseDouble( conf.get("gc.TrainingDriver.svm.eps",".1") );
				int max_iter = conf.getInt("gc.TrainingDriver.svm.maxiter",1000);
				BinarySVM.optimize(data, param, C , eps , max_iter);
			} 
			else if ( conf.get("gc.TrainingDriver.classifier").equals("lr") ) {
				double lambda = Double.parseDouble( conf.get("gc.TrainingDriver.lr.lambda","1") );
				double eps = Double.parseDouble( conf.get("gc.TrainingDriver.lr.eps","1e-4") );
				int maxnfn = conf.getInt("gc.TrainingDriver.lr.maxnfn",1000);
				LogisticRegression.optimize(data, param, lambda, eps, maxnfn);
			}
			
			context.write( new IntWritable(node), param );
			printUsage();
		}		
	}
	
	public static Configuration addPathToDC( Configuration conf , String path ) throws IOException {
        FileSystem fs = FileSystem.get( conf );
        FileStatus[] fstatus = fs.globStatus( new Path(path) );
        Path[] listedPaths = FileUtil.stat2Paths( fstatus );
        for( Path p : listedPaths ) {   
                System.out.println(" Add File to DC " + p.toUri().toString() );
                DistributedCache.addCacheFile( p.toUri() , conf);
        }
        return conf;
    }
	
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        
        String input = conf.get("gc.TrainingDriver.input");
        String output = conf.get("gc.TrainingDriver.output");
        String dataset = conf.get("gc.TrainingDriver.dataset");
        String jobname = conf.get("gc.TrainingDriver.name");
        
        if ( input == null || output == null || dataset == null || jobname == null ) {
        	System.out.println(" Incorrect parameters ");
        	System.exit(0);
        }
        
        conf = addPathToDC(conf, conf.get("gc.TrainingDriver.dataset" )  + "*");

        Job job = new Job( conf );
        job.setJarByClass( TrainingDriver.class );
        job.setJobName(jobname);

        job.setOutputKeyClass( IntWritable.class );
        job.setOutputValueClass( WeightParameter.class );

        job.setMapperClass( TrainingDriverMapper.class);
        job.setMapOutputKeyClass( IntWritable.class );
        job.setMapOutputValueClass( WeightParameter.class );

        job.setInputFormatClass( TextInputFormat.class );
        job.setOutputFormatClass( SequenceFileOutputFormat.class );
		SequenceFileOutputFormat.setOutputCompressionType(job, SequenceFile.CompressionType.RECORD );
        job.setNumReduceTasks( 0 );

        FileInputFormat.setInputPaths( job , input );
        FileOutputFormat.setOutputPath(job , new Path(output) );

        System.out.println(" Input dir = " + input );
        System.out.println(" Output dir = " + output );
        System.out.println(" Training Input = " + dataset );
        System.out.println(" Name = " + jobname );
        
        if( job.waitForCompletion(true) == false ) {
            System.err.println(" Job " + jobname + " Failed (miserably)");
            System.exit(2);
        }        
        return 0;
    }
    
    public static void main( String args[] ) throws Exception {
        ToolRunner.run( new Configuration(), new TrainingDriver() , args );
    }
} 	
