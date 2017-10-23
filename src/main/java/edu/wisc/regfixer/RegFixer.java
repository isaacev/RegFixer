package edu.wisc.regfixer;

import java.util.concurrent.TimeoutException;
import java.util.Set;
import java.util.TreeSet;

import edu.wisc.regfixer.enumerate.Enumerant;
import edu.wisc.regfixer.enumerate.Enumerants;
import edu.wisc.regfixer.enumerate.HoleNode;
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
      synthesis = null;
      report.printEnumerant(++i, enumerant.getCost(), enumerant.toString());
      HoleNode.ExpansionChoice expansion = enumerant.getExpansionChoice();

      if (expansion == HoleNode.ExpansionChoice.Concat) {
        if (job.getCorpus().passesDotTest(enumerant)) {
          try {
            synthesis = RegFixer.synthesisLoop(job, report, enumerant);
          } catch (SynthesisFailure ex) {
            report.printEnumerantError(ex.getMessage());
            continue;
          }
        } else {
          report.printEnumerantError("failed dot test");
        }
      } else if (expansion == HoleNode.ExpansionChoice.Star) {
        if (job.getCorpus().passesEmptySetTest(enumerant)) {
          try {
            synthesis = RegFixer.synthesisLoop(job, report, enumerant);
          } catch (SynthesisFailure ex) {
            report.printEnumerantError(ex.getMessage());
            continue;
          }
        } else {
          report.printEnumerantError("failed empty set test");
        }
      } else if (expansion == HoleNode.ExpansionChoice.Plus) {
        try {
          synthesis = RegFixer.synthesisLoop(job, report, enumerant);
        } catch (SynthesisFailure ex) {
          report.printEnumerantError(ex.getMessage());
          continue;
        }
      } else if (expansion == HoleNode.ExpansionChoice.Optional) {
        if (job.getCorpus().passesEmptySetTest(enumerant)) {
          try {
            synthesis = RegFixer.synthesisLoop(job, report, enumerant);
          } catch (SynthesisFailure ex) {
            report.printEnumerantError(ex.getMessage());
            continue;
          }
        } else {
          report.printEnumerantError("failed empty set test");
        }
      }

      if (synthesis != null) {
        report.printEnumerantRepair(synthesis.toString());
        report.clearPending();
        report.println();
        break;
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

  private static Synthesis synthesisLoop (Job job, ReportStream report, Enumerant enumerant) throws SynthesisFailure {
    if (job.getCorpus().passesDotTest(enumerant) == false) {
      throw new SynthesisFailure("failed dot test");
    }

    Set<String> p = job.getCorpus().getPositiveExamples();
    Set<String> n = job.getCorpus().getNegativeExamples();
    Synthesis synthesis = null;

    while (true) {
      synthesis = enumerant.synthesize(p, n);

      /**
       * M = get all matches
       * B []
       *
       *
       * (l,u) in M is a bad match if:
       * 1) there exists no positive match l' and u' such that l' < l <= u'
       * 2) there exists a positive match l' and u' such that l = l' and u > u'
       *
       * if (l,u) satisfies both conditions then
       *   add (l,u) to B
       *
       *
       * if size of B > 0
       *   n = n U B
       * else
       *   return synthesis
       */

      Set<Range> ranges = job.getCorpus().getMatches(synthesis);
      Set<Range> badMatches = new TreeSet<>();

      for (Range r : ranges) {
        boolean cond1 = false;
        boolean cond2 = false;
        for (Range rPrime : job.getCorpus().getPositiveRanges()) {
          if (!(rPrime.getLeftIndex() < r.getLeftIndex() && r.getLeftIndex() <= rPrime.getRightIndex())) {
            cond1 = true;
          }

          if (r.getLeftIndex() == rPrime.getLeftIndex() && r.getRightIndex() > rPrime.getRightIndex()) {
            cond2 = true;
          }
        }

        if (cond1 && cond2) {
          badMatches.add(r);
        }
      }

      if (badMatches.size() > 0) {
        for (Range r : badMatches) {
          n.add(job.getCorpus().getSubstring(r));
        }
      } else {
        return synthesis;
      }
    }
  }
}
