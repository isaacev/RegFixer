package edu.wisc.regfixer;

import java.io.IOException;

import edu.wisc.regfixer.enumerate.Benchmark;
import edu.wisc.regfixer.enumerate.Job;
import edu.wisc.regfixer.util.ReportStream;

public class CLI {
  public static void main (String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: regfixer <benchmark>");
      System.exit(1);
    }

    Job job = null;

    try {
      job = Benchmark.readFromFile(args[0]);
    } catch (IOException ex) {
      System.err.println("unable to read benchmark file");
      System.exit(1);
    }

    ReportStream report = new ReportStream(System.out, true);
    RegFixer.fix(job, report);
  }
}
