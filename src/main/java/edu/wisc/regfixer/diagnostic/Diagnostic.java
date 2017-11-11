package edu.wisc.regfixer.diagnostic;

public class Diagnostic {
  private ReportStream out;
  private Registry reg;
  private Timing tim;

  public Diagnostic () {
    this.out = new ReportStream(System.out);
    this.reg = new Registry();
    this.tim = new Timing();
  }

  public Diagnostic (ReportStream out, Registry reg, Timing tim) {
    this.out = out;
    this.reg = reg;
    this.tim = tim;
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

  public Timing timing () {
    return this.tim;
  }
}
