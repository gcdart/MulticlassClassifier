package hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import base.Example;




public class Converter extends Configured implements Tool {

	public static class ConverterMapper extends Mapper<LongWritable,Text,IntWritable,Example> {
		public void map(LongWritable key, Text v, Context context ) throws IOException, InterruptedException {
			Example E = new Example( v );
			context.write( new IntWritable(1) , E );			
		}
	}

	public static class IdentityReducer extends Reducer<IntWritable,Example, IntWritable,Example>{
		public void reduce( IntWritable k , Iterable<Example> values , Context context ) throws IOException, InterruptedException {
			for( Example e : values ) context.write( k , e );
		}
	}
	
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
			
		String input = conf.get("gc.Converter.input");
		String output = conf.get("gc.Converter.output");
		String jobname = conf.get("gc.Converter.name");

		Job job = new Job( conf );
		job.setJarByClass( Converter.class );
		job.setJobName(jobname);
		
		job.setOutputKeyClass( IntWritable.class );
		job.setOutputValueClass( Example.class );
		
		job.setMapperClass(ConverterMapper.class);
		job.setMapOutputKeyClass( IntWritable.class );
		job.setMapOutputValueClass( Example.class );
		
		job.setReducerClass(IdentityReducer.class);
		
		job.setInputFormatClass( TextInputFormat.class );
		job.setOutputFormatClass( SequenceFileOutputFormat.class );
		
		job.setNumReduceTasks( 1 );
		
		FileInputFormat.setInputPaths(job, input );
		FileOutputFormat.setOutputPath(job,new Path(output));

        System.out.println(" Input dir = " + conf.get("gc.Converter.input") );
        System.out.println(" Output dir = " + conf.get("gc.Converter.output") );
        System.out.println(" Name = " + conf.get("gc.Converter.name" ) );

		
		if( job.waitForCompletion(true) == false ) {
			System.err.println(" Job " + jobname + " Failed (miserably)");
			System.exit(2);
		}		
		
		return 0;
	}
	
	public static void main( String args[] ) throws Exception {
		ToolRunner.run( new Configuration(), new Converter() , args );
	}
}