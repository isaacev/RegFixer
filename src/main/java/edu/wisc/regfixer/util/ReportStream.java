package edu.wisc.regfixer.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import edu.wisc.regfixer.enumerate.Range;
import edu.wisc.regfixer.parser.RegexNode;

public class ReportStream extends PrintStream {
  private boolean liveStatus;
  private String pending;

  public ReportStream () {
    this(new NullOutputStream());
  }

  public ReportStream (OutputStream out) {
    this(out, false, false);
  }

  public ReportStream (OutputStream out, boolean liveStatus, boolean useColor) {
    super(out);
    this.liveStatus = liveStatus;

    if (useColor) {
      Ansi.enableColor();
    } else {
      Ansi.disableColor();
    }
  }

  public void clearPending () {
    if (this.liveStatus == false) { return; }

    this.print("\r");
    this.print(pending);
  }

  public void printHeader (String message) {
    this.printf("\n%s\n\n", message);
  }

  public void printSearchTableHeader () {
    this.println("  Order:  Cost:   Enumerant:      Status:");
    this.println("  -----------------------------------------");
  }

  public void printEnumerant (int order, int cost, String enumerant) {
    this.pending = "";
    this.pending += Ansi.White.sprintf("  %-8d%-8d", order, cost);
    this.pending += Ansi.Cyan.sprintf("%-16s", enumerant);
    this.clearPending();
  }

  public void printStatus (String status) {
    if (this.liveStatus == false) { return; }

    this.clearPending();
    this.print(Ansi.Green.sprintf("pending: %s", status));
  }

  public void printEnumerantRepair (String repair) {
    this.clearPending();
    this.print(Ansi.Cyan.sprintf("%-16s\n", repair));
    this.pending = "";
  }

  public void printEnumerantError (String message) {
    this.clearPending();
    this.print(Ansi.White.sprintf("failed: %s\n", message));
    this.pending = "";
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
