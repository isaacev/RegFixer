package edu.wisc.regfixer.enumerate;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import edu.wisc.regfixer.enumerate.Range;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.synthesize.Synthesis;
import edu.wisc.regfixer.synthesize.SynthesisFailure;
import edu.wisc.regfixer.util.ReportStream;
import org.sat4j.specs.TimeoutException;

public class Main {
  public static void main (String[] args) {
    Job job = null;

    String filename = "benchmarks/repeat.txt";

    try {
      job = Benchmark.readFromFile(args.length > 0 ? args[0] : filename);
    } catch (IOException ex) {
      System.err.println("could not read benchmark file");
      System.exit(1);
    }

    Corpus corpus = job.getCorpus();
    Enumerants enumerants = new Enumerants(job.getTree(), corpus);
    Enumerant enumerant = null;
    Synthesis synthesis = null;

    final int costPadding = 8;
    final int enumerantPadding = 16;
    final int repairPadding = 16;

    String enumerantFmt = "%-8d%-16s";
    String repairFmt = "%-16s";
    String errorFmt = "%16s%s\n";
    String badMatchFmt = "%40s%-8s\"%s\"\n";

    System.out.println("Cost:   Enumerant:      Repair:         Error:");
    System.out.println("--------------------------------------------------------");
    while ((enumerant = enumerants.poll()) != null && enumerant.getCost() < 4) {
      System.out.printf(enumerantFmt, enumerant.getCost(), enumerant);

      if (corpus.passesDotTest(enumerant)) {
        try {
          synthesis = enumerant.synthesize(corpus);
        } catch (SynthesisFailure ex) {
          System.out.printf(errorFmt, "", ex.getMessage());
          continue;
        }

        System.out.printf(repairFmt, synthesis.toString());

        if (corpus.isPerfectMatch(synthesis)) {
          System.out.printf("perfect match\n");
          return;
        } else {
          System.out.printf("matched more than positive examples:\n");
          for (Range match : corpus.getBadMatches(synthesis)) {
            System.out.printf(badMatchFmt, "", match, corpus.getSubstring(match));
          }
          continue;
        }
      } else {
        System.out.printf(errorFmt, "", "failed dot test");
        continue;
      }
    }
  }

  public static RegexNode synthesize (Job job) {

    // TODO: need to work on this
    Corpus corpus = job.getCorpus();
    RegexNode regex = job.getTree();
    try {

      Automaton automaton = new Automaton(regex);
      System.out.println(automaton.toString());

      automaton.computeRuns("axyz");
      Set<String> posExamples = corpus.getPositiveExamples();
      //
      Set<String> negExamples = corpus.getNegativeExamples();

      Map<String, List<Map<Integer, Set<Character>>>> runsAll = automaton.computeRuns(posExamples);
      Set<String> keys = runsAll.keySet();
      Iterator<String> itr = keys.iterator();
      while(itr.hasNext()) {
        String posExKey = itr.next();
        List<Map<Integer, Set<Character>>> runs = runsAll.get(posExKey);



      }

    } catch(TimeoutException ex) {

    }


    return null;
  }
}
