package de.uulm.vs.dcn.benchmarking;

import java.io.File;
import java.nio.file.Files;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.uulm.vs.dcn.Cryptographer;
import de.uulm.vs.dcn.DefaultSplitCombineStrategy;
import de.uulm.vs.dcn.ShamirSplitCombine;
import de.uulm.vs.dcn.SplitCombineStrategy;
import de.uulm.vs.dcn.benchmarking.specs.BenchmarkSpec;
import de.uulm.vs.dcn.benchmarking.specs.IncreasingBPR;
import de.uulm.vs.dcn.benchmarking.specs.IncreasingK;
import de.uulm.vs.dcn.benchmarking.specs.IncreasingN;

/**
 * 
 * @author Juri Dispan
 *
 */
public class Benchmarking {
  public static void main(String[] args) throws Exception {
    var options = new Options();

    var optOutput = new Option("o", "out-folder", true, "output folder");
    options.addOption(optOutput);

    var mode = new Option("c", "config-file", true, "config file");
    options.addOption(mode);

    try {
      var parser = new DefaultParser();
      var cmd = parser.parse(options, args);
      var outFolder = cmd.getOptionValue("out-folder", ".");
      var cfgFile = cmd.getOptionValue("config-file", "config.txt");

      var tasks =
          Files.readAllLines(new File(cfgFile).toPath()).stream()
              .filter(line -> !line.startsWith("#"))
              .map(line -> line.split("\\s*,\\s*"))
              .collect(Collectors.toList());
      for (var task : tasks) {
        BiFunction<Integer, Integer, SplitCombineStrategy> sup =
            Boolean.parseBoolean(task[5]) ? ShamirSplitCombine::new
                : DefaultSplitCombineStrategy::new;

        var min = Integer.parseInt(task[6]);
        var max = Integer.parseInt(task[7]);
        int bpr, step, n, k, delay = Integer.parseInt(task[9]);

        BenchmarkSpec spec;
        switch (task[0]) {
        case "n":
          k = Integer.parseInt(task[2]);
          bpr = Integer.parseInt(task[3]);
          step = Integer.parseInt(task[8]);

          Cryptographer.BYTES_PER_ROUND = bpr;
          spec = new IncreasingN(min, max, step, k);
          break;
        case "k":
          n = Integer.parseInt(task[1]);
          bpr = Integer.parseInt(task[3]);
          step = Integer.parseInt(task[8]);

          Cryptographer.BYTES_PER_ROUND = bpr;
          spec = new IncreasingK(n, min, max, step);
          break;
        case "bpr":
          n = Integer.parseInt(task[1]);
          k = Integer.parseInt(task[2]);

          spec = new IncreasingBPR(n, k, min, max);
          break;
        default:
          throw new ParseException("Invalid variable mode in config file.");
        }
        Cryptographer.DELAY = delay;

        spec.setLen(Integer.parseInt(task[4]));
        spec.setStrategySupp(sup);
        spec.setName(String.join("_", task));
        spec.setOutFolder(outFolder);

        spec.runSpec();
      }

      // runBenchmarks(bytesPerRound, outFolder);
    } catch (ParseException | NumberFormatException e) {
      System.out.println(e.getMessage());
      new HelpFormatter().printHelp("Enhanced DCN Benchmarking", options);
      System.exit(1);
    }
  }

}
