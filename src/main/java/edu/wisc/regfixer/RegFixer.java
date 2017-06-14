package edu.wisc.regfixer;

import java.util.concurrent.TimeoutException;
import java.util.regex.PatternSyntaxException;

import edu.wisc.regfixer.enumerate.Enumerant;
import edu.wisc.regfixer.enumerate.Enumerants;
import edu.wisc.regfixer.enumerate.Job;
import edu.wisc.regfixer.enumerate.Range;
import edu.wisc.regfixer.synthesize.Synthesis;
import edu.wisc.regfixer.synthesize.SynthesisFailure;
import edu.wisc.regfixer.util.ReportStream;

public class RegFixer {
  public static String fix (Job job) throws TimeoutException {
    return RegFixer.fix(job, new ReportStream());
  }

  public static String fix (Job job, ReportStream report) throws TimeoutException {
    return RegFixer.fix(job, report, 1000);
  }

  public static String fix (Job job, int loopLimit) throws TimeoutException {
    return RegFixer.fix(job, new ReportStream(), loopLimit);
  }

  public static String fix (Job job, ReportStream report, int loopLimit) throws TimeoutException {
    report.printHeader("Given the regular expression:");
    report.printRegex(job.getTree());

    report.printHeader("That that should match the strings:");
    for (Range range : job.getCorpus().getPositiveRanges()) {
      String example = job.getCorpus().getSubstring(range);
      report.printMatchStatus(true, range, example);
    }

    report.printHeader("And reject the strings:");
    for (Range range : job.getCorpus().getNegativeRanges()) {
      String example = job.getCorpus().getSubstring(range);
      report.printMatchStatus(false, range, example);
    }

    report.printHeader("Search through possible transformations:");
    report.printSearchTableHeader();

    Enumerants enumerants = new Enumerants(job.getTree(), job.getCorpus());
    Enumerant enumerant = null;
    Synthesis synthesis = null;

    int i = 0;
    while ((enumerant = enumerants.poll()) != null) {
      report.printEnumerant(++i, enumerant.getCost(), enumerant.toString());
      if (job.getCorpus().passesDotTest(enumerant)) {
        try {
          synthesis = enumerant.synthesize(job.getCorpus());
        } catch (SynthesisFailure ex) {
          report.printEnumerantError(true, ex.getMessage());
          continue;
        }
        try {
          if (job.getCorpus().isPerfectMatch(synthesis)) {
            report.printEnumerantRepair(true, synthesis.toString());
            break;
          } else {
            report.printEnumerantRepair(false, synthesis.toString());
            report.printEnumerantError(false, "matched incorrectly");

            for (Range range : job.getCorpus().getBadMatches(synthesis)) {
              String example = job.getCorpus().getSubstring(range);
              report.printEnumerantBadMatch(range, example);
            }
          }
        } catch (PatternSyntaxException e) {  // catching illegal syntax (e.g. [23-0], [23 ])
          report.printEnumerantError(false, "Illegal Syntax of Regex");
        }

      } else {
        report.printEnumerantError(true, "failed dot test");
      }

      if (i >= loopLimit) {
        String fmt = "TIMEOUT: enumeration loop limit reached (%d)";
        report.redPrintf(fmt, loopLimit);
        throw new TimeoutException(String.format(fmt, loopLimit));
      }
    }

    if (synthesis != null) {
      report.printHeader("Results in the expression:");
      report.printRegex(synthesis.getTree());
      report.printHeader("All done");
      return synthesis.getTree().toString();
    } else {
      report.printHeader("Unable to compute a repair");
      report.printHeader("All done");
      return null;
    }
  }
}
