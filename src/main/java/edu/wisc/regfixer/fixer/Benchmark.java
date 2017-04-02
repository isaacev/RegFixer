package edu.wisc.regfixer.fixer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import edu.wisc.regfixer.parser.Main;
import edu.wisc.regfixer.parser.RegexNode;
import static edu.wisc.regfixer.fixer.SearchEngine.getMatchingRanges;

public class Benchmark {
  static String boundary = "---";

  public static Job readFromFile (String filename) throws IOException {
    RegexNode originalRegex = null;
    List<Range> selectedRanges = new LinkedList<Range>();
    String corpus = "";

    Scanner sc = new Scanner(new File(filename));
    int lineNum = 1;

    if (sc.hasNextLine()) {
      String line = sc.nextLine();
      lineNum++;

      try {
        originalRegex = Main.parse(line);
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

    return new Job(originalRegex, selectedRanges, corpus);
  }

  public static void saveToFile (Job job, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(filename, "UTF-8");

    // Print regex string.
    pw.println(job.getOriginalRegex());

    // Print range indices.
    pw.println(boundary);
    for (Range m : job.getSelectedRanges()) {
      pw.println(m.toString());
    }

    // Print full corpus.
    pw.println(boundary);
    pw.println(job.getCorpus());

    pw.close();
  }
}
