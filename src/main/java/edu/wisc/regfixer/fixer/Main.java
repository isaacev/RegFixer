package edu.wisc.regfixer.fixer;

import java.util.List;
import java.util.Scanner;
import java.io.IOException;

import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.util.ReportStream;

public class Main {
  public static void main (String[] args) {
    Job job = null;

    try {
      job = Benchmark.readFromFile("benchmarks/repeat.txt");
      System.out.println(">> read benchmark file");
    } catch (IOException ex) {
      System.out.println(">> cannot read from benchmark file");
      return;
    }

    Enumerator enumerator = new Enumerator(job);
    Scanner stdin = new Scanner(System.in);
    System.out.printf(">>\t%s", job.getOriginalRegex().toString());

    while (stdin.hasNextLine()) {
      stdin.nextLine();

      if (enumerator.isEmpty()) {
        break;
      } else {
        PartialTree partial = enumerator.next();
        System.out.printf("%d\t%s", partial.getCost(), partial.toString());
      }
    }

    stdin.close();
  }

  public static RegexNode synthesize (Job job) {
    return null;
  }
}
