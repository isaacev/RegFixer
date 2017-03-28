package RegFixer;

import java.util.*;
import java.io.*;
import RegexParser.RegexNode;

class Benchmark {
  RegexNode regex;
  List<Range> posMatches;
  List<Range> negMatches;
  String corpus;

  static String boundary = "---";

  Benchmark (RegexNode regex, List<Range> posMatches, List<Range> negMatches, String corpus) {
    this.regex = regex;
    this.posMatches = posMatches;
    this.negMatches = negMatches;
    this.corpus = corpus;
  }

  RegexNode getRegex () {
    return this.regex;
  }

  List<Range> getPosMatches () {
    return this.posMatches;
  }

  List<Range> getNegMatches () {
    return this.negMatches;
  }

  String getCorpus () {
    return this.corpus;
  }

  static Benchmark readFromFile (String filename) throws IOException {
    RegexNode regex = null;
    List<Range> posMatches = new LinkedList<Range>();
    List<Range> negMatches = new LinkedList<Range>();
    String corpus = "";

    Scanner sc = new Scanner(new File(filename));
    int lineNum = 1;

    if (sc.hasNextLine()) {
      String line = sc.nextLine();
      lineNum++;

      try {
        regex = RegexParser.Main.parse(line);
      } catch (Exception ex) {
        throw new IOException(ex.toString());
      }

      // Digest boundary.
      if (!sc.hasNextLine() || !sc.nextLine().equals(boundary)) {
        lineNum++;
        String fmt = "Expected boundary on line %d of '%s'";
        throw new IOException(String.format(fmt, lineNum, filename));
      }
    }

    while (sc.hasNextLine()) {
      String line = sc.nextLine();
      lineNum++;

      if (line.equals(boundary)) {
        // Break loop since boundary was encountered.
        break;
      } else {
        try {
          posMatches.add(new Range(line));
        } catch (BadRangeException ex) {
          String fmt = "Expected index pair or boundary on line %d of '%s'";
          throw new IOException(String.format(fmt, lineNum, filename));
        }
      }
    }

    while (sc.hasNextLine()) {
      String line = sc.nextLine();
      lineNum++;

      if (line.equals(boundary)) {
        // Break loop since boundary was encountered.
        break;
      } else {
        try {
          negMatches.add(new Range(line));
        } catch (BadRangeException ex) {
          String fmt = "Expected index pair or boundary on line %d of '%s'";
          throw new IOException(String.format(fmt, lineNum, filename));
        }
      }
    }

    while (sc.hasNextLine()) {
      corpus += sc.nextLine() + (sc.hasNextLine() ? "\n" : "");
      lineNum++;
    }

    return new Benchmark(regex, posMatches, negMatches, corpus);
  }

  static void saveToFile (Benchmark bm, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(filename, "UTF-8");

    // Print regex string.
    pw.println(bm.getRegex());

    // Print match indices.
    pw.println(boundary);
    for (Range m : bm.getPosMatches()) {
      pw.println(m.toString());
    }

    // Print match indices.
    pw.println(boundary);
    for (Range m : bm.getNegMatches()) {
      pw.println(m.toString());
    }

    // Print full corpus.
    pw.println(boundary);
    pw.println(bm.getCorpus());

    pw.close();
  }
}
