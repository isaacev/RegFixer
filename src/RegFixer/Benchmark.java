package regfixer;

import java.util.*;
import java.io.*;
import RegexParser.RegexNode;
import static RegFixer.CorpusSearchEngine.*;

public class Benchmark {
  String corpus;
  RegexNode originalRegex;
  List<Range> originalRanges;
  List<Range> selectedRanges;

  static String boundary = "---";

  public Benchmark (RegexNode originalRegex, List<Range> selectedRanges, String corpus) {
    this.corpus = corpus;
    this.originalRegex = originalRegex;
    this.originalRanges = getMatchingRanges(corpus, originalRegex);
    this.selectedRanges = selectedRanges;
  }

  public String getCorpus () {
    return this.corpus;
  }

  public RegexNode getOriginalRegex () {
    return this.originalRegex;
  }

  public List<Range> getOriginalRanges () {
    return this.originalRanges;
  }

  public List<Range> getSelectedRanges () {
    return this.selectedRanges;
  }

  public static Benchmark readFromFile (String filename) throws IOException {
    RegexNode originalRegex = null;
    List<Range> selectedRanges = new LinkedList<Range>();
    String corpus = "";

    Scanner sc = new Scanner(new File(filename));
    int lineNum = 1;

    if (sc.hasNextLine()) {
      String line = sc.nextLine();
      lineNum++;

      try {
        originalRegex = RegexParser.Main.parse(line);
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
          selectedRanges.add(new Range(line));
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

    return new Benchmark(originalRegex, selectedRanges, corpus);
  }

  public static void saveToFile (Benchmark bm, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(filename, "UTF-8");

    // Print regex string.
    pw.println(bm.getOriginalRegex());

    // Print range indices.
    pw.println(boundary);
    for (Range m : bm.getSelectedRanges()) {
      pw.println(m.toString());
    }

    // Print full corpus.
    pw.println(boundary);
    pw.println(bm.getCorpus());

    pw.close();
  }
}
