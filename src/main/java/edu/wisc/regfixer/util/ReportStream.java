package edu.wisc.regfixer.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import edu.wisc.regfixer.enumerate.Range;
import edu.wisc.regfixer.parser.RegexNode;

public class ReportStream extends PrintStream {
  public ReportStream () {
    this(new NullOutputStream());
  }

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

  public void printSearchTableHeader () {
    this.println("  Order:  Cost:   Enumerant:      Repair:         Error:");
    this.println("  --------------------------------------------------------");
  }

  public void printEnumerant (int order, int cost, String enumerant) {
    this.print(Ansi.White.sprintf("  %-8d%-8d", order, cost));
    this.print(Ansi.Cyan.sprintf("%-16s", enumerant));
  }

  public void printEnumerantRepair (boolean newline, String repair) {
    this.print(Ansi.Cyan.sprintf("%-16s", repair));

    if (newline) {
      this.printf("\n");
    }
  }

  public void printEnumerantError (boolean padded, String message) {
    if (padded) {
      this.printf("%16s", "");
    }

    this.print(Ansi.White.sprintf("%s\n", message));
  }

  public void printEnumerantBadMatch (Range range, String example) {
    this.print(Ansi.Red.sprintf("  %32s%-8s\"%s\"\n", "", range, example));
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

  private static class NullOutputStream extends OutputStream {
    public NullOutputStream () {}
    public void write (final int i) throws IOException {}
    public void write (final byte[] bytes) throws IOException {}
    public void write (final byte[] bytes, final int off, final int len) throws IOException {}
  }
}
