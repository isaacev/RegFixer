package edu.wisc.regfixer;

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import edu.wisc.regfixer.enumerate.Benchmark;
import edu.wisc.regfixer.enumerate.Job;
import edu.wisc.regfixer.util.ReportStream;

public class CLI {
  public static void main (String[] args) {
    boolean useColor = CLI.hasFlag("--color", args);

    if (args.length < 1) {
      System.err.println("Usage: regfixer [options] <benchmark>");
      System.exit(1);
    }

    Job job = null;

    try {
      job = Benchmark.readFromFile(args[args.length - 1]);
    } catch (IOException ex) {
      System.err.println("unable to read benchmark file");
      System.exit(1);
    }

    ReportStream report = new ReportStream(System.out, useColor);
    RegFixer.fix(job, report);
  }

  private static boolean hasFlag (String flag, String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(flag)) {
        return true;
      }
    }

    return false;
  }
}
