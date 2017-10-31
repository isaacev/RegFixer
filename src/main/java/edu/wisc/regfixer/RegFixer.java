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
    return RegFixer.fix(job, report, loopLimit, new Config());
  }

  public static String fix (Job job, ReportStream report, int loopLimit, Config config) throws TimeoutException {
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
            synthesis = RegFixer.synthesisLoop(job, report, enumerant, config);
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
            synthesis = RegFixer.synthesisLoop(job, report, enumerant, config);
          } catch (SynthesisFailure ex) {
            report.printEnumerantError(ex.getMessage());
            continue;
          }
        } else {
          report.printEnumerantError("failed empty set test");
        }
      } else if (expansion == HoleNode.ExpansionChoice.Plus) {
        try {
          synthesis = RegFixer.synthesisLoop(job, report, enumerant, config);
        } catch (SynthesisFailure ex) {
          report.printEnumerantError(ex.getMessage());
          continue;
        }
      } else if (expansion == HoleNode.ExpansionChoice.Optional) {
        if (job.getCorpus().passesEmptySetTest(enumerant)) {
          try {
            synthesis = RegFixer.synthesisLoop(job, report, enumerant, config);
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

  private static Synthesis synthesisLoop (Job job, ReportStream report, Enumerant enumerant, Config config) throws SynthesisFailure {
    if (job.getCorpus().passesDotTest(enumerant) == false) {
      throw new SynthesisFailure("failed dot test");
    }

    Set<Range> P = job.getCorpus().getPositiveRanges();
    Set<Range> N = job.getCorpus().getNegativeRanges();
    Synthesis synthesis = null;

    /**
     * The synthesis loop takes a regular expression template (the enumerant)
     * which has 1 or more unknown character classes embedded in it. The job of
     * this loop is--given sets of positive and negative example strings--to
     * determine if character classes can be derrived for the unknown character
     * classes that accept all the positive examples and reject all the negative
     * examples.
     */
    while (true) {
      /**
       * For each iteration of the loop, given the positive examples P and the
       * negative examples N, a SAT formula is generated to attempt to
       * synthesize character class solutions for each unknown character class
       * in the enumerant.
       */
      synthesis = enumerant.synthesize(
        job.getCorpus().getPositiveExamples(),
        job.getCorpus().getSubstrings(N),
        config);

      /**
       * It's possible that the solution synthesized by the SAT formula will not
       * match only P but also may match some unexpected values O. Because 1 or
       * more values exist in O is NOT sufficient to reject the enumerant as
       * unsatisfiable yet. By incorporating some members of O into N the SAT
       * synthesis can be retried and will eventually either reject the
       * enumerant as unsatisfiable or derrive a perfect solution.
       */
      Set<Range> O = job.getCorpus().getMatches(synthesis);

      /**
       * Not all members of O should be added to N. A member 'o' of O should NOT
       * be added to N iff there exists some 'p' of P such that 'o' == 'p' OR
       * both of the following conditions hold:
       * 1) the lower bound of 'o' > the lower bound of 'p'
       * 2) the lower bound of 'o' < the upper bound of 'p'
       */

      // Handle condition 'o' == 'p'.
      O.removeAll(job.getCorpus().getPositiveRanges());
      if (O.size() == 0) {
        return synthesis;
      }

      // Handle conditions 1 and 2.
      Set<Range> pendingN = new TreeSet<>();
      outerLoop:
      for (Range o : O) {
        for (Range p : P) {
          boolean cond1 = o.getLeftIndex() > p.getLeftIndex();
          boolean cond2 = o.getLeftIndex() < p.getRightIndex();
          if (cond1 && cond2) {
            continue outerLoop;
          }
        }

        pendingN.add(o);
      }

      /**
       * If all 'o' that are eligible to be added to N are already contained in
       * N then the synthesis loop fails because no new information can be
       * learned that will improve the synthesized solutions.
       */
      if (N.containsAll(pendingN)) {
        throw new SynthesisFailure("failed to find novel incorrect matches");
      } else {
        N.addAll(pendingN);
      }
    }
  }
}
