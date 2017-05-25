package edu.wisc.regfixer;

import java.io.IOException;

import edu.wisc.regfixer.enumerate.Benchmark;
import edu.wisc.regfixer.enumerate.Job;
import edu.wisc.regfixer.util.ReportStream;

public class CLI {
  public static void main (String[] args) {
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

    ReportStream report = new ReportStream(System.out, true);
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
