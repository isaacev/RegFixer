package edu.wisc.regfixer.enumerate;

import java.io.IOException;
import java.util.*;

import edu.wisc.regfixer.automata.Automaton;
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

    // Scanner stdin = new Scanner(System.in);

    System.out.println("Cost:   Enumerant:      Repair:         Error:");
    System.out.println("--------------------------------------------------------");
    while ((enumerant = enumerants.poll()) != null) {
      System.out.printf("%-7d %-16s", enumerant.getCost(), enumerant);

      if (corpus.passesDotTest(enumerant)) {
        // if ((synthesis = enumerant.synthesize(corpus)) != null) {
        //   if (corpus.noUnexpectedMatches(synthesis)) {
        //     System.out.println("=== FIN ===");
        //     System.out.println(synthesis);
        //     System.out.println("=== === ===");
        //   } else {
        //     enumerants.restart(corpus.findUnexpectedMatches(synthesis));
        //   }
        // }

        try {
          synthesis = enumerant.synthesize(corpus);
        } catch (SynthesisFailure ex) {
          System.out.printf("%16s%s", "", ex.getMessage());
          synthesis = null;
        }

        if (synthesis != null) {
          if (corpus.noUnexpectedMatches(synthesis)) {
            System.out.println("perfect match");
          } else {
            System.out.println("broken match");
            System.out.print("[");
            for (Range missing : corpus.findUnexpectedMatches(synthesis)) {
              System.out.printf(" %s", missing.toString());
            }
            System.out.println(" ]");
            System.out.print("[");
            for (Range missing : corpus.findUnexpectedMatches(synthesis)) {
              System.out.printf(" %s", corpus.getSubstring(missing));
            }
            System.out.println(" ]");
          }

          System.out.printf("%s", synthesis.toString());
        }
      } else {
        System.out.printf("%16s%s", "", "failed dot test");
      }

      if (enumerant.getCost() > 3) {
        break;
      }

      System.out.println();
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
