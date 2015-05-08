package mipr.examples;

import mipr.image.FImageWritable;
import mipr.image.MBFImageWritable;
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
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

/**
 * OpenIMAJ and MIPR is used for conversion
 */
public class Img2HistJob extends Configured implements Tool {
    public int run(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.printf("Usage: %s [generic options] <input> <output>\n",
                    getClass().getSimpleName());
            ToolRunner.printGenericCommandUsage(System.err);
            return -1;
        }

        Job job = Job.getInstance(super.getConf(), "Histogram of Image Job");
        job.setJarByClass(getClass());
        job.setInputFormatClass(MBFImageInputFormat.class);
	job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapperClass(Img2HistJobMapper.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.setNumReduceTasks(0);
	job.setOutputKeyClass(Text.class);
	job.setOutputValueClass(Text.class);
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int exitCode = ToolRunner.run(conf, new Img2HistJob(), args);
        System.exit(exitCode);
    }

    public static class Img2HistJobMapper extends Mapper<NullWritable, MBFImageWritable,
            Text, Text> {

        private FImage gray_image;
        private MBFImage color_image;
	private Text fileName = new Text();
        private FImageWritable fiw = new FImageWritable();

        public void map(NullWritable key, MBFImageWritable value, Context context)
                throws IOException, InterruptedException {
            color_image = value.getImage();
	    int[] sbins = new int[768];
	    StringBuilder sb = new StringBuilder();
	    int bin = 0;

            if (color_image != null) {
		for (int r = 0; r < color_image.getHeight(); r++)
		{
			for (int c = 0; c < color_image.getWidth(); c++)
			{
				bin = (int) (color_image.getBand(0).pixels[r][c] * 255);
				sbins[bin]++;
				bin = (int) (color_image.getBand(1).pixels[r][c] * 255);
				sbins[bin+256]++;
				bin = (int) (color_image.getBand(2).pixels[r][c] * 255);
				sbins[bin+512]++;
			}
		}
		for (int inte : sbins) {
		sb.append(inte).append("\t");
	    	}
		fileName.set(value.getFileName() + "." + value.getFormat());
                context.write(fileName, new Text(sb.toString()));
            }
        }
    }

}
