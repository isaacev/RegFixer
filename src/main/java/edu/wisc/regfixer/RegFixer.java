package edu.wisc.regfixer;

import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.wisc.regfixer.diagnostic.Diagnostic;
import edu.wisc.regfixer.enumerate.Enumerant;
import edu.wisc.regfixer.enumerate.Enumerants;
import edu.wisc.regfixer.enumerate.Expansion;
import edu.wisc.regfixer.enumerate.Job;
import edu.wisc.regfixer.enumerate.Range;
import edu.wisc.regfixer.enumerate.UnknownChar;
import edu.wisc.regfixer.synthesize.Synthesis;
import edu.wisc.regfixer.synthesize.SynthesisFailure;

public class RegFixer {
  public static class Result {
    public final int     templates;
    public final boolean hasSolution;
    public final int     cost;
    public final String  solution;
    public final int     failedDotTest;
    public final int     failedDotStarTest;
    public final int     failedEmptySetTest;

    public Result (int templates, int cost, String solution, int failedDotTest, int failedDotStarTest, int failedEmptySetTest) {
      this.templates          = templates;
      this.hasSolution        = true;
      this.cost               = cost;
      this.solution           = solution;
      this.failedDotTest      = failedDotTest;
      this.failedDotStarTest  = failedDotStarTest;
      this.failedEmptySetTest = failedEmptySetTest;
    }

    public Result (int templates, int failedDotTest, int failedDotStarTest, int failedEmptySetTest) {
      this.templates          = templates;
      this.hasSolution        = false;
      this.cost               = 0;
      this.solution           = null;
      this.failedDotTest      = failedDotTest;
      this.failedDotStarTest  = failedDotStarTest;
      this.failedEmptySetTest = failedEmptySetTest;
    }

    public String toString () {
      return String.format("%d,%s,\"%s\",%d,%d,%d",
        this.templates,
        (this.hasSolution) ? Integer.toString(this.cost) : "",
        (this.hasSolution) ? this.solution : "",
        this.failedDotTest,
        this.failedDotStarTest,
        this.failedEmptySetTest);
    }
  }

  public static Result fix (Job job) throws TimeoutException {
    return RegFixer.fix(job, new Diagnostic());
  }

  public static Result fix (Job job, Diagnostic diag) throws TimeoutException {
    return RegFixer.fix(job, 1000, diag);
  }

  public static Result fix (Job job, int loopLimit) throws TimeoutException {
    return RegFixer.fix(job, loopLimit, new Diagnostic());
  }

  public static Result fix (Job job, int loopLimit, Diagnostic diag) throws TimeoutException {
    int    templates          = 0;
    int    cost               = 0;
    Map<String, Integer> solutions = new HashMap<>();
    int    failedDotTest      = 0;
    int    failedDotStarTest  = 0;
    int    failedEmptySetTest = 0;

    diag.output().printSectionHeader("Given the regular expression:");
    diag.output().printIndent(job.getTree().toString());

    diag.output().printSectionHeader("That that should match the strings:");
    for (Range range : job.getCorpus().getPositiveRanges()) {
      String example = job.getCorpus().getSubstring(range);
      diag.output().printExample(true, range, example);
    }

    diag.output().printSectionHeader("And reject the strings:");
    for (Range range : job.getCorpus().getNegativeRanges()) {
      String example = job.getCorpus().getSubstring(range);
      diag.output().printExample(false, range, example);
    }

    diag.output().printSectionHeader("Search through possible transformations:");
    diag.output().printHeader();

    Enumerants enumerants = new Enumerants(job.getTree(), job.getCorpus());
    Enumerant enumerant = null;
    int COST_CUTOFF = Integer.MAX_VALUE;

    int i = 0;
    while ((enumerant = enumerants.poll()) != null) {
      templates++;
      if (i++ >= loopLimit) {
        throw new TimeoutException("enumeration loop limit reached");
      }

      if (enumerant.getCost() > COST_CUTOFF) {
        break;
      }

      Synthesis synthesis = null;
      diag.output().printPartialRow(enumerant.getCost(), enumerant.toString());
      Expansion expansion = enumerant.getLatestExpansion();

      if (expansion == Expansion.Concat) {
        if (job.getCorpus().passesDotTest(enumerant)) {
          try {
            synthesis = RegFixer.synthesisLoop(job, enumerant, diag);
          } catch (SynthesisFailure ex) {
            diag.output().finishRow(ex.getMessage());
            continue;
          }
        } else {
          failedDotTest++;
          diag.output().finishRow("failed dot test");
        }
      } else if (expansion == Expansion.Star) {
        if (job.getCorpus().passesEmptySetTest(enumerant)) {
          try {
            synthesis = RegFixer.synthesisLoop(job, enumerant, diag);
          } catch (SynthesisFailure ex) {
            diag.output().finishRow(ex.getMessage());
            continue;
          }
        } else {
          failedEmptySetTest++;
          diag.output().finishRow("failed empty set test");
        }
      } else if (expansion == Expansion.Plus) {
        try {
          synthesis = RegFixer.synthesisLoop(job, enumerant, diag);
        } catch (SynthesisFailure ex) {
          diag.output().finishRow(ex.getMessage());
          continue;
        }
      } else if (expansion == Expansion.Optional) {
        if (job.getCorpus().passesEmptySetTest(enumerant)) {
          try {
            synthesis = RegFixer.synthesisLoop(job, enumerant, diag);
          } catch (SynthesisFailure ex) {
            diag.output().finishRow(ex.getMessage());
            continue;
          }
        } else {
          failedEmptySetTest++;
          diag.output().finishRow("failed empty set test");
        }
      } else if (expansion == Expansion.Repeat) {
        try {
          synthesis = RegFixer.synthesisLoop(job, enumerant, diag);
        } catch (SynthesisFailure ex) {
          diag.output().finishRow(ex.getMessage());
          continue;
        }
      } else {
        throw new RuntimeException("unknown expansion type");
      }

      if (synthesis != null) {
        String sol = synthesis.toString();
        int fit = synthesis.getFitness();
        solutions.put(sol, fit);

        diag.output().finishRow(sol);
        COST_CUTOFF = enumerant.getCost();
      }
    }

    if (solutions.size() > 0) {
      String solution = null;
      diag.output().printSectionHeader("Finds the following solutions (and the corresponding fitness):");
      for (Map.Entry<String, Integer> entry : solutions.entrySet()) {
        diag.output().printIndent(String.format("%4d %s", entry.getValue(), entry.getKey()));

        if (solution == null) {
          solution = entry.getKey();
        } else if (solutions.get(solution) < entry.getValue()) {
          solution = entry.getKey();
        }
      }

      // diag.output().printSectionHeader("Results in the expression:");
      // diag.output().printIndent(synthesis.getTree().toString());
      // diag.output().printSectionHeader("With a specificity of:");
      // diag.output().printIndent(Integer.toString(synthesis.getFitness()));
      diag.output().printSectionHeader("Computed in:");
      diag.output().printSectionHeader("All done");
      return new Result(templates, cost, solution, failedDotTest, failedDotStarTest, failedEmptySetTest);
    } else {
      diag.output().printSectionHeader("Unable to compute a repair");
      diag.output().printSectionHeader("All done");
      return new Result(templates, failedDotTest, failedDotStarTest, failedEmptySetTest);
    }
  }

  private static Synthesis synthesisLoop (Job job, Enumerant enumerant, Diagnostic diag) throws SynthesisFailure {
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
        diag);

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

      // Handle condition len('o') == 0.
      for (Iterator<Range> iter = O.iterator(); iter.hasNext();) {
        if (iter.next().length() == 0) {
          iter.remove();
        }
      }

      if (O.size() == 0) {
        return synthesis;
      }

      // Handle conditions 1 and 2.
      Set<Range> pendingN = new TreeSet<>();
      outerLoop:
      for (Range o : O) {
        if (o.length() == 0) {
          continue;
        }

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
      if (N.size() > 0 && N.containsAll(pendingN)) {
        throw new SynthesisFailure("failed to find novel incorrect matches");
      } else {
        N.addAll(pendingN);
      }
    }
  }
}
