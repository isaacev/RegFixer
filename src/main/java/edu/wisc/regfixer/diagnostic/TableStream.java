package edu.wisc.regfixer.diagnostic;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import edu.wisc.regfixer.util.StringUtil;

public class TableStream extends PrintStream {
  private static class TableCol {
    private String name;
    private int width;
    private String fmt;
    private String underline;

    public TableCol (String name, int width) {
      this.name  = name;
      this.width = width;
      this.fmt   = String.format("%%-%ds", this.width);
    }

    public String getName () {
      return this.name;
    }

    public int getWidth () {
      return this.width;
    }

    public String toString (Object val) {
      return String.format(this.fmt, val.toString());
    }
  }

  private TableCol ordinal;
  private List<TableCol> cols;
  private int counter;
  private List<Object> partialRow;

  public TableStream (OutputStream out) {
    super(out);

    this.ordinal    = null;
    this.cols       = new LinkedList<>();
    this.counter    = 0;
    this.partialRow = null;
  }

  public void addOrdinalCol (String name, int width) {
    this.ordinal = new TableCol(name, width);
  }

  public void addCol (String name, int width) {
    this.cols.add(new TableCol(name, width));
  }

  public void printBreak () {
    String row = "";

    if (this.ordinal != null) {
      row += "  ";
      row += this.ordinal.toString("");
      row += "  |";
    }

    this.println(row);
  }

  public void printHeader () {
    String names = "";
    String under = "";

    if (this.ordinal != null) {
      names += "  ";
      names += this.ordinal.toString(this.ordinal.getName());
      names += "  |";

      under += StringUtil.repeatString('-', names.length() - 1);
      under += "|";
    }

    for (TableCol col : this.cols) {
      names += "  ";
      names += col.toString(col.getName());
    }

    under += StringUtil.repeatString('-', names.length() - under.length() + 2);
    this.println(names);
    this.println(under);
  }

  public void printRow (Object... vals) {
    String row = "";

    if (this.ordinal != null) {
      row += "  ";
      row += this.ordinal.toString(++this.counter);
      row += "  |";
    }

    for (int i = 0; i < vals.length && i < this.cols.size(); i++) {
      row += "  ";
      row += this.cols.get(i).toString(vals[i]);
    }

    this.println(row);
  }

  public void printPartialRow (Object... vals) {
    String row = "";

    if (this.ordinal != null) {
      row += "  ";
      row += this.ordinal.toString(++this.counter);
      row += "  |";
    }

    for (int i = 0; i < vals.length && i < this.cols.size(); i++) {
      row += "  ";
      row += this.cols.get(i).toString(vals[i]);
    }

    this.print(row);
    this.partialRow = Arrays.asList(vals);
  }

  public void finishRow (Object... vals) {
    String row = "";
    int already = this.partialRow.size();

    for (int i = 0; i < vals.length && i < this.cols.size() - already; i++) {
      row += "  ";
      row += this.cols.get(i + already).toString(vals[i]);
    }

    this.println(row);
    this.partialRow = null;
  }

  public void printBlock (String block) {
    this.printBlock(block.split("\n"));
  }

  public void printBlock (String[] block) {
    for (int i = 0; i < block.length; i++) {
      String row = "";

      if (this.ordinal != null) {
        row += "  ";
        row += this.ordinal.toString("");
        row += "  |";
      }

      row += block[i];
      this.println(row);
    }
  }
}
