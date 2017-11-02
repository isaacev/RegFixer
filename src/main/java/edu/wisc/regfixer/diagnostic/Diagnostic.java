package edu.wisc.regfixer.diagnostic;

public class Diagnostic {
  private ReportStream out;
  private Registry reg;

  public Diagnostic () {
    this.out = new ReportStream(System.out);
    this.reg = new Registry();
  }

  public Diagnostic (ReportStream out, Registry reg) {
    this.out = out;
    this.reg = reg;
  }

  public ReportStream output () {
    return this.out;
  }

  public boolean getBool (String name) {
    return this.reg.getBool(name);
  }

  public int getInt (String name) {
    return this.reg.getInt(name);
  }

  public String getStr (String name) {
    return this.reg.getStr(name);
  }
}
