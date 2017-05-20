package edu.wisc.regfixer.enumerate;

import java.io.IOException;
import java.util.*;

import edu.wisc.regfixer.automata.Automaton;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.synthesize.Synthesis;
import edu.wisc.regfixer.util.ReportStream;
import org.sat4j.specs.TimeoutException;

public class Main {
  public static void main (String[] args) {
    Job job = null;

    try {
      job = Benchmark.readFromFile("benchmarks/repeat.txt");
    } catch (IOException ex) {
      System.err.println("could not read benchmark file");
      System.exit(1);
    }

    Corpus corpus = job.getCorpus();
    Enumerants enumerants = new Enumerants(job.getTree(), corpus);
    Enumerant enumerant = null;
    Synthesis synthesis = null;


    Scanner stdin = new Scanner(System.in);

    while ((enumerant = enumerants.poll()) != null) {
      stdin.nextLine();
      System.out.printf("%d\t%s", enumerant.getCost(), enumerant);

      /*
      if (corpus.passesDotTest(enumerant)) {
        if ((synthesis = enumerant.synthesize(corpus)) != null) {
          if (corpus.noUnexpectedMatches(synthesis)) {
            System.out.println("=== FIN ===");
            System.out.println(synthesis);
            System.out.println("=== === ===");
          } else {
            enumerants.restart(corpus.findUnexpectedMatches(synthesis));
          }
        }
      }
      */

      // Testing Begins here

      // if putting a DOT into a hole is possible, return it with that

      // else, start dotStar and emptySet test
      if(!corpus.passesDotStarTest(enumerant)) {
        // TODO: handle Empty Set test later with negative examples
        continue;
      } else {
        enumerant.toPattern(HoleNode.FillType.Dot);


        RegexNode regexNode = synthesize(job);
        System.out.println("New regex is " + regexNode.toString());
      }

    }
    stdin.close();
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
