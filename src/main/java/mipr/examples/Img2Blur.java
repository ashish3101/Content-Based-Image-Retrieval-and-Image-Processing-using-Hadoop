package mipr.examples;

import mipr.image.FImageWritable;
import mipr.image.MBFImageWritable;
import mipr.mapreduce.MBFImageOutputFormat;
import mipr.mapreduce.FImageOutputFormat;
import mipr.mapreduce.MBFImageInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import java.io.IOException;


/**
 * OpenIMAJ and MIPR is used for conversion
 */
public class Img2Blur extends Configured implements Tool {
    public int run(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.printf("Usage: %s [generic options] <input> <output>\n",
                    getClass().getSimpleName());
            ToolRunner.printGenericCommandUsage(System.err);
            return -1;
        }

        Job job = Job.getInstance(super.getConf(), "MIPR job converting color images to Canny Edge Detection Image");
        job.setJarByClass(getClass());
        job.setInputFormatClass(MBFImageInputFormat.class);
        job.setOutputFormatClass(MBFImageOutputFormat.class);
        job.setMapperClass(Img2BlurMapper.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.setNumReduceTasks(0);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(FImageWritable.class);
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int exitCode = ToolRunner.run(conf, new Img2Blur(), args);
        System.exit(exitCode);
    }

    public static class Img2BlurMapper extends Mapper<NullWritable, MBFImageWritable,
            NullWritable, MBFImageWritable> {
        private MBFImage color_image;
        private MBFImageWritable fiw = new MBFImageWritable();

        public void map(NullWritable key, MBFImageWritable value, Context context)
                throws IOException, InterruptedException {
            color_image = value.getImage();

            if (color_image != null) {
		color_image.processInplace(new FGaussianConvolve(2f,3));
                fiw.setFormat(value.getFormat());
                fiw.setFileName(value.getFileName());
                fiw.setImage(color_image);
                context.write(NullWritable.get(), fiw);
            }
        }
    }

}
