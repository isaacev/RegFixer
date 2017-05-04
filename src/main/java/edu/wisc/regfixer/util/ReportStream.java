package edu.wisc.regfixer.util;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import edu.wisc.regfixer.enumerate.Range;
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

  public ReportStream printf (String fmt, Object ...args) {
    this.print(String.format(fmt, args));
    return this;
  }

  public void greenPrintf (String fmt, Object ...args) {
    this.print(Ansi.Green.sprintf("  " + fmt + "\n", args));
  }

  public void redPrintf (String fmt, Object ...args) {
    this.print(Ansi.Red.sprintf("  " + fmt + "\n", args));
  }

  public void printRegex (RegexNode regex) {
    this.printf(Ansi.Cyan.sprintf("  %s\n", regex));
  }

  public void printMatchStatus (boolean isOK, Range range, String match) {
    if (isOK) {
      this.greenPrintf("✓ %-8s %s", range, match);
    } else {
      this.redPrintf("✗ %-8s %s", range, match);
    }
  }

  public void printRegexStatus (boolean isOK, RegexNode regex) {
    if (isOK) {
      this.greenPrintf("✓ %s", regex);
    } else {
      this.redPrintf("✗ %s", regex);
    }
  }
}
