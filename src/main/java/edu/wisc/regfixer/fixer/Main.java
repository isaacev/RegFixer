package edu.wisc.regfixer.fixer;

import java.util.List;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.util.ReportStream;

public class Main {
  public static RegexNode synthesize (Job job) {
    List<IncompleteTree> candidates = Analyzer.analyze(job);
    RegexNode candidate = Synthesizer.synthesize(job, candidates);

    return candidate;
  }

  public static RegexNode synthesize (Job job, ReportStream report) {
    List<IncompleteTree> candidates = Analyzer.analyze(job, report);
    RegexNode candidate = Synthesizer.synthesize(job, candidates, report);

    if (candidate != null) {
      report.printHeader("Results in the expression:");
      report.printRegex(candidate);
      report.printHeader("That matches the strings:");
      for (Range range : SearchEngine.getMatchingRanges(job.getCorpus(), candidate)) {
        String match = SearchEngine.getMatchingString(job.getCorpus(), range);
        report.printMatchStatus(true, range, match);
      }
    }

    report.printHeader("All done");
    return candidate;
  }

  public static void main (String[] args) throws Exception {
    boolean useColor = false;
    String filename = null;

    if (args.length == 0) {
      System.err.println("Usage: regfixer [--color] <benchmark file>");
      System.exit(1);
    } else if (args.length == 1) {
      useColor = false;
      filename = args[0];
    } else if (args.length >= 2) {
      useColor = args[0].equals("--color");
      filename = args[1];
    }

    Job job = Benchmark.readFromFile(filename);
    ReportStream report = new ReportStream(System.out, useColor);
    synthesize(job, report);
  }
}
