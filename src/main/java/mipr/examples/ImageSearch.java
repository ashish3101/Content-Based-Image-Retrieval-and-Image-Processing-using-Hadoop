package mipr.examples;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.filecache.DistributedCache;
import java.net.URI;
import java.io.*;  

/**
 * OpenIMAJ and MIPR is used for conversion
 */
public class ImageSearch extends Configured implements Tool {
    public int run(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.printf("Usage: %s [generic options] <input> <output>\n",
                    getClass().getSimpleName());
            ToolRunner.printGenericCommandUsage(System.err);
            return -1;
        }

        Job job = Job.getInstance(super.getConf(), "Image Search");
	DistributedCache.addCacheFile(new URI("/cachefile1"),job.getConfiguration());
        job.setJarByClass(getClass());
	job.setOutputKeyClass(NullWritable.class);
    	job.setOutputValueClass(Text.class);
	job.setInputFormatClass(TextInputFormat.class);
    	job.setOutputFormatClass(TextOutputFormat.class);
	job.setMapOutputKeyClass(DoubleWritable.class);
	job.setMapOutputValueClass(Text.class);
        job.setMapperClass(ImageSearchMapper.class);
	job.setReducerClass(ImageSearchReducer.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int exitCode = ToolRunner.run(conf, new ImageSearch(), args);
        System.exit(exitCode);
    }

    public static class ImageSearchMapper extends Mapper<LongWritable, Text, DoubleWritable, Text> {


        private final static DoubleWritable one = new DoubleWritable();
    	private Text word = new Text();
        
    	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		Path[] cacheFiles = context.getLocalCacheFiles();
		FileInputStream fileStream = new FileInputStream(cacheFiles[0].toString());
		StringBuilder builder = new StringBuilder();
		int ch;
		while((ch = fileStream.read()) != -1){
		    builder.append((char)ch);
		}
		String mainfile = builder.toString();
        	String line = value.toString();
        	String[] tokens = line.split("\t");
		String[] tokens1 = mainfile.split("\t");
		long diff = 0;
		long squaresumdiff = 0;

        	word.set(tokens[0]);
		for(int i=1;i<257;i++)
		{
		diff = Integer.parseInt(tokens[i]) - Integer.parseInt(tokens1[i]);
		squaresumdiff = squaresumdiff + diff*diff;
		}
		double diff1 = Math.sqrt(squaresumdiff);		
		one.set(diff1);
        	context.write(one,word);
    	}
    }


	public static class ImageSearchReducer extends Reducer<DoubleWritable, Text, NullWritable, Text> {
		
		private Text result = new Text();
	    public void reduce(Text key, Iterable<Text> values, Context context) 
	      throws IOException, InterruptedException {
		String translations = "";
		for (Text val : values) {
		    translations += "\t"+val.toString();
		}
		result.set(translations);
		context.write(NullWritable.get(), result);
	    }
	}

}
