package edu.wisc.regfixer;

import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.HashSet;
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

  public static Result fix (Job job, int loopCutoff) throws TimeoutException {
    return RegFixer.fix(job, loopCutoff, new Diagnostic());
  }

  public static Result fix (Job job, int loopCutoff, Diagnostic diag) throws TimeoutException {
    diag.timing().startTiming("whole");

    // Keep track of all solutions found. Each solution is mapped to its
    // fitness score which is a count of how many single character classes are
    // included in its synthesized character classes.
    Map<String, Integer> solutions = new HashMap<>();

    // Statistics tracked during the enumeration search. Timing statistics are
    // tracked from within the Diagnostic object.
    int templatesTotal           = 0;
    int templatesToFirstSolution = 0;
    int testDotStarTotal         = 0;
    int testDotStarRejections    = 0;
    int testEmptySetTotal        = 0;
    int testEmptySetRejections   = 0;
    int testDotTotal             = 0;
    int testDotRejections        = 0;

    // Print the report header which describes the initial inputs to the
    // search algorithm including the initial regular expression, any explicit
    // positive examples and any inferred negative examples.
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

    // Once the first solution is found, the algorithm can be configured to
    // keep searching in order to find a better solution. In this case, the
    // alrogithm will only keep searching as long as there are more templates
    // that have a cost equal-to or less-than the cost of the first solution.
    // This variable tracks the maximum cost allowed to search. Before a
    // solution is found, the cutoff is huge to allow any template. After the
    // first solution is found the cutoff is set to the cost of the first
    // solution.
    int costCutoff = Integer.MAX_VALUE;

    int i = 0;
    while ((enumerant = enumerants.poll()) != null) {
      // Stop the loop if the cost of the current template is greater than
      // cutoff or if the number of templates searched is greater than the
      // loop cutoff.
      if (enumerant.getCost() > costCutoff) {
        break;
      } else if (templatesTotal++ >= loopCutoff) {
        templatesTotal--;

        if (solutions.size() == 0) {
          throw new TimeoutException("enumeration loop limit reached");
        } else {
          break;
        }
      }

      // Print some information about the current template.
      diag.output().printPartialRow(enumerant.getCost(), enumerant.toString());

      Synthesis synthesis = null;
      Expansion expansion = enumerant.getLatestExpansion();

      boolean passesTests = true;

      switch (expansion) {
      case Concat:
        if (diag.getBool("test-all") || diag.getBool("test-dot")) {
          passesTests = job.getCorpus().passesDotTest(enumerant);

          // Increment appropriate counters.
          testDotTotal++;
          if (passesTests == false) {
            testEmptySetRejections++;
          }
        }
        break;
      case Star:
      case Optional:
        if (diag.getBool("test-all") || diag.getBool("test-emptyset")) {
          passesTests = job.getCorpus().passesEmptySetTest(enumerant);

          // Increment appropriate counters.
          testEmptySetTotal++;
          if (passesTests == false) {
            testEmptySetRejections++;
          }
        }
        break;
      }

      if (passesTests) {
        try {
          synthesis = RegFixer.synthesisLoop(job, enumerant, diag);
        } catch (SynthesisFailure ex) {
          diag.output().finishRow(ex.getMessage());
          continue;
        }
      }

      if (synthesis != null) {
        if (solutions.size() == 0) {
          templatesToFirstSolution = templatesTotal;
        }

        String sol = synthesis.toString();
        int fit = synthesis.getFitness();
        solutions.put(sol, fit);

        diag.output().finishRow(sol);
        costCutoff = enumerant.getCost();
      }
    }

    diag.timing().stopTimingAndAdd("whole");

    if (solutions.size() > 0) {
      String solution = null;
      diag.output().printSectionHeader("Finds the following solutions (and the corresponding fitness):");
      for (Map.Entry<String, Integer> entry : solutions.entrySet()) {
        diag.output().printIndent(String.format("%-4d %s", entry.getValue(), entry.getKey()));

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
      diag.output().printIndent(String.format("%dms", Math.round(diag.timing().getTiming("whole") / 1e6)));

      if (diag.getBool("debug-stats")) {
        diag.output().printSectionHeader("Statistics:");
        diag.output().printIndent("Templates:");
        diag.output().printIndent(String.format("  Total:      %d", templatesTotal));
        diag.output().printIndent(String.format("  Before sol: %d", templatesToFirstSolution));
        diag.output().println();
        diag.output().printIndent("Tests:");
        diag.output().printIndent("  Dot Star:");
        diag.output().printIndent(String.format("    Total:    %d", testDotStarTotal));
        diag.output().printIndent(String.format("    Rejected: %d", testDotStarRejections));
        diag.output().printIndent("  Empty Set:");
        diag.output().printIndent(String.format("    Total:    %d", testEmptySetTotal));
        diag.output().printIndent(String.format("    Rejected: %d", testEmptySetRejections));
        diag.output().printIndent("  Dot:");
        diag.output().printIndent(String.format("    Total:    %d", testDotTotal));
        diag.output().printIndent(String.format("    Rejected: %d", testDotRejections));
        diag.output().println();
        diag.output().printIndent("Timings:");
        diag.output().printIndent(String.format("  Whole: %d", diag.timing().getTiming("whole")));
      }

      diag.output().printSectionHeader("All done");

      Result result = new Result(
        templatesTotal,
        costCutoff,
        solution,
        testDotTotal,
        testDotStarTotal,
        testEmptySetTotal);

      if (diag.getBool("output-csv")) {
        diag.output().println(result.toString());
      } else if (diag.getBool("output-solution") || diag.getBool("output-none") == false) {
        diag.output().println(solution);
      }

      return result;
    } else {
      diag.output().printSectionHeader("Unable to compute a repair");
      diag.output().printSectionHeader("All done");
      Result result = new Result(
        templatesTotal,
        testDotTotal,
        testDotStarTotal,
        testEmptySetTotal);

      if (diag.getBool("output-csv")) {
        diag.output().println(result.toString());
      }

      return result;
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

      boolean matchesAllP = O.containsAll(job.getCorpus().getPositiveRanges());

      // Handle condition 'o' == 'p'.
      O.removeAll(job.getCorpus().getPositiveRanges());

      // Handle condition len('o') == 0.
      for (Iterator<Range> iter = O.iterator(); iter.hasNext();) {
        if (iter.next().length() == 0) {
          iter.remove();
        }
      }

      if (O.size() == 0 && matchesAllP) {
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
      if (pendingN.size() == 0 || N.containsAll(pendingN)) {
        throw new SynthesisFailure("failed to find novel incorrect matches");
      } else {
        N.addAll(pendingN);
      }
    }
  }
}
