package edu.wisc.regfixer.enumerate;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Benchmark {
  public final static String boundary = "---";

  public static Job readFromFile (String filename) throws IOException {
    String regex = "";
    Set<Range> selectedRanges = new HashSet<Range>();
    Set<Range> negativeRanges = new HashSet<Range>();
    String corpus = "";

    Scanner sc = new Scanner(new File(filename));
    int lineNum = 1;

    if (sc.hasNextLine()) {
      String line = sc.nextLine();
      lineNum++;

      regex = line;

      // Digest boundary.
      if (!sc.hasNextLine()) {
        lineNum++;
        String fmt = "Expected boundary on line %d of '%s'";
        throw new IOException(String.format(fmt, lineNum, filename));
      }

      line = sc.nextLine();
      lineNum++;
      if (!line.equals(boundary)) {
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

    return new JobImplicit(filename, regex, corpus, selectedRanges);
  }

  public static void saveToFile (Job job, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(filename, "UTF-8");
    pw.print(job.toString());
    pw.close();
  }
}
