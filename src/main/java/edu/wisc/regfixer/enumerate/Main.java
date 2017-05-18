package edu.wisc.regfixer.enumerate;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.synthesize.Synthesis;
import edu.wisc.regfixer.util.ReportStream;

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
      System.out.printf("%d\t%s", enumerant.getCost(), enumerant);
      stdin.nextLine();

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
    }

    stdin.close();
  }

  public static RegexNode synthesize (Job job) {
    return null;
  }
}
