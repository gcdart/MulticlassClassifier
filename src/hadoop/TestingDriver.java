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
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import base.Example;
import base.PIF;
import base.PIFArray;
import base.WeightParameter;

public class TestingDriver extends Configured implements Tool {
	
	public static class TestingDriverMapper extends Mapper<IntWritable,WeightParameter,IntWritable,PIFArray> {
		int once = 0;
		Vector<Example> data;
		PriorityQueue<PIF>[] pqs;
		
		void printUsage(){
			Runtime r = Runtime.getRuntime();
			r.gc();
			double mb = (1024*1024);
			System.out.println(" \tSetup Memory ");
			System.out.println(" \tUsed Memory:" + (r.totalMemory() - r.freeMemory())/mb );
			System.out.println(" \tTotal Memory:" + r.totalMemory()/mb );
			System.out.println(" \tMax Memory:" + (r.maxMemory())/mb );
		}		
		
		@SuppressWarnings("unchecked")
		protected void setup(Context context ) throws IOException {
			if( once == 1 ) return;
			once = 1;			
			Configuration conf = context.getConfiguration();			
			System.out.println(" Opening testing Instances = " 
					+ conf.get("gc.TestingDriver.dataset") );          

			data = new Vector<Example>();
			Path[] listedPaths = DistributedCache.getLocalCacheFiles( conf );
			FileSystem fs = FileSystem.getLocal( conf );	
			
			// Parse the testing data
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
			
			pqs = (PriorityQueue<PIF>[]) new PriorityQueue[data.size()];
			for ( int i = 0; i < pqs.length; ++i )
				pqs[i] = new PriorityQueue<PIF>();
		}
		
		public void map(IntWritable key, WeightParameter param, Context context ) throws IOException, InterruptedException {
			int node = key.get(), cnt = 0;
			System.out.println(" class-label = " + node + " weight-vector norm = " + param.norm() );
			
			Configuration conf = context.getConfiguration();	
			int rcut = conf.getInt("gc.TestingDriver.rank",1);

			for ( Example E : data ) {
				float score = (float) param.getScore(E);
				pqs[cnt].add ( new PIF(node,score) );
				if ( pqs[cnt].size() > rcut ) 
					pqs[cnt].remove();
				cnt = cnt + 1;
			}			
			printUsage();
		}
		
		public void cleanup( Context context ) throws IOException, InterruptedException {
			int cnt = 0;
			for ( Example E : data ) {
				PIFArray pifarray = new PIFArray();
				pifarray.pifs.addAll( pqs[cnt] );				
				context.write( new IntWritable(E.docid) ,pifarray );
				cnt = cnt + 1;
			}
		}
	}

	public static class TestingDriverReducer extends Reducer<IntWritable,PIFArray,IntWritable,PIFArray> {
		
		public void reduce( IntWritable key , Iterable<PIFArray> values , Context context ) throws IOException , InterruptedException {
			int rcut = context.getConfiguration().getInt("gc.TestingDriver.rank",1);
			
			java.util.PriorityQueue<PIF> best = new java.util.PriorityQueue<PIF>();

			for ( PIFArray v : values ) for ( PIF sc : v.pifs ) {
				best.add( new PIF(sc) );
				if ( best.size() > rcut )
					best.remove();
			}
			
			PIFArray pifarray = new PIFArray();
			while ( best.size() > 0 ) 
				pifarray.pifs.add( best.poll() );
			
			Collections.reverse(pifarray.pifs);
			context.write( key , pifarray );
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
        String input =  conf.get("gc.TestingDriver.input");
        String output = conf.get("gc.TestingDriver.output");
        String jobname = conf.get("gc.TestingDriver.name");
        String dataset = conf.get("gc.TestingDriver.dataset");

        if ( input == null || output == null || dataset == null || jobname == null ) {
        	System.out.println(" Incorrect parameters ");
        	System.exit(0);
        }

        conf = addPathToDC(conf, conf.get("gc.TestingDriver.dataset" )  + "*");

        Job job = new Job( conf );
        job.setJarByClass( TestingDriverMapper.class );
        job.setJobName(jobname);

        job.setOutputKeyClass( IntWritable.class );
        job.setOutputValueClass( PIFArray.class );

        job.setMapperClass( TestingDriverMapper.class);
        job.setMapOutputKeyClass( IntWritable.class );
        job.setMapOutputValueClass( PIFArray.class );

        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setCombinerClass(TestingDriverReducer.class);
        job.setReducerClass(TestingDriverReducer.class);
        
        job.setOutputFormatClass( TextOutputFormat.class );
        
		FileInputFormat.setInputPaths(job, input );

        FileOutputFormat.setOutputPath(job , new Path(output) );

        System.out.println(" Input dir = " + conf.get("gc.TestingDriver.input") );
        System.out.println(" Output dir = " + conf.get("gc.TestingDriver.output") );
        System.out.println(" Testing Input = " + conf.get("gc.TestingDriver.dataset" ) );
        System.out.println(" Name = " + conf.get("gc.TestingDriver.name" ) );
        
        if( job.waitForCompletion(true) == false ) {
            System.err.println(" Job " + jobname + " Failed (miserably)");
            System.exit(2);
        }
        
        return 0;
    }
    
    public static void main( String args[] ) throws Exception {
        ToolRunner.run( new Configuration(), new TestingDriver() , args );
    }
} 	
