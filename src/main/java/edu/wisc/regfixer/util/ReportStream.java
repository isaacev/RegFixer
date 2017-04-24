package edu.wisc.regfixer.util;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import edu.wisc.regfixer.fixer.Range;
import edu.wisc.regfixer.parser.RegexNode;

public class ReportStream extends PrintStream {
  public ReportStream (OutputStream out) {
    this(out, false);
  }

  public ReportStream (OutputStream out, boolean useColor) {
    super(out);

    if (useColor) {
      Ansi.enableColor();
    } else {
      Ansi.disableColor();
    }
  }

  public void printHeader (String message) {
    this.printf("\n%s\n\n", message);
  }

  public void printRegex (RegexNode regex) {
    this.printf(Ansi.Cyan.sprintf("  %s\n", regex));
  }

  public void printMatchStatus (boolean isOK, Range range, String match) {
    if (isOK) {
      this.print(Ansi.Green.sprintf("  ✓ %-8s %s\n", range, match));
    } else {
      this.printf(Ansi.Red.sprintf("  ✗ %-8s %s\n", range, match));
    }
  }

  public void printRegexStatus (boolean isOK, RegexNode regex) {
    if (isOK) {
      this.printf(Ansi.Green.sprintf("  ✓ %s\n", regex));
    } else {
      this.printf(Ansi.Red.sprintf("  ✗ %s\n", regex));
    }
  }
}
