package RegFixer;

import java.util.*;
import java.io.*;
import RegexParser.RegexNode;

class Benchmark {
  RegexNode regex;
  List<Range> matches;
  String corpus;

  static String boundary = "---";

  Benchmark (RegexNode regex, List<Range> matches, String corpus) {
    this.regex = regex;
    this.matches = matches;
    this.corpus = corpus;
  }

  RegexNode getRegex () {
    return this.regex;
  }

  List<Range> getMatches () {
    return this.matches;
  }

  String getCorpus () {
    return this.corpus;
  }

  static Benchmark readFromFile (String filename) throws IOException {
    RegexNode regex = null;
    List<Range> matches = new ArrayList<Range>();
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
          matches.add(new Range(line));
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

    return new Benchmark(regex, matches, corpus);
  }

  static void saveToFile (Benchmark bm, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(filename, "UTF-8");

    // Print regex string.
    pw.println(bm.getRegex());

    // Print match indices.
    pw.println(boundary);
    for (Range m : bm.getMatches()) {
      pw.println(m.toString());
    }

    // Print full corpus.
    pw.println(boundary);
    pw.println(bm.getCorpus());

    pw.close();
  }
}
