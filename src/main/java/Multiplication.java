import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configuration;


public class Multiplication  extends Configured implements Tool {

    public static class Map extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration(); // for accessing m, n and p
            int m = Integer.parseInt(conf.get("m"));
            int p = Integer.parseInt(conf.get("p"));
            String line = value.toString();
            String[] indicesAndValue = line.split(",");
            Text outputKey = new Text();
            Text outputValue = new Text();
            if (indicesAndValue[0].equals("m")) {
                for (int k = 0; k < p; k++) {
                    outputKey.set(indicesAndValue[1] + "," + k);
                    outputValue.set(indicesAndValue[0] + "," + indicesAndValue[2]
                            + "," + indicesAndValue[3]);
                    context.write(outputKey, outputValue);
                }
            } else {
                for (int i = 0; i < m; i++) {
                    outputKey.set(i + "," + indicesAndValue[2]);
                    outputValue.set("n," + indicesAndValue[1] + ","
                            + indicesAndValue[3]);
                    context.write(outputKey,    outputValue);
                }
                System.out.println("In Map");
            }
        }
    }

    public static class Reduce extends Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            System.out.println("In Reduce");
            String[] value;
            HashMap<Integer, Integer> hashA = new HashMap<Integer, Integer>();
            HashMap<Integer, Integer> hashB = new HashMap<Integer, Integer>();
            for (Text val : values) {
                value = val.toString().split(",");
                if (value[0].equals("m")) {
                    hashA.put(Integer.parseInt(value[1]), Integer.parseInt(value[2]));
                } else {
                    hashB.put(Integer.parseInt(value[1]), Integer.parseInt(value[2]));
                }
            }
            int n = Integer.parseInt(context.getConfiguration().get("n"));
            int result = 0;
            int m_ij;
            int n_jk;
            for (int j = 0; j < n; j++) {
                m_ij = hashA.get(j);
                n_jk = hashB.get(j);
                result += m_ij * n_jk;
            }
                context.write(key,new Text(Integer.toString(result)));
        }
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Multiplication(), args);
        System.exit(exitCode);
    }

    public int run(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.printf("Usage: %s <m> <n> <p> <input> <output>\nWhere A is an m-by-n matrix; B is an n-by-p matrix.",
                    getClass().getSimpleName()); ToolRunner.printGenericCommandUsage(System.err);
            return -1;
        }

        Configuration conf = new Configuration();
        /*
         * Use there configurations for your mapper and reducer classes.
         * You can access them with the command conf.get("x");
         */
        conf.set("m", args[0]);
        conf.set("n", args[1]);
        conf.set("p", args[2]);


        Job job = Job.getInstance(conf, "Multiplication");
        job.setJarByClass(Multiplication.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[3]));
        FileOutputFormat.setOutputPath(job, new Path(args[4]));

        boolean result = job.waitForCompletion(true);
        return (result ? 0 : 1);
    }
}

