package edu.wisc.regfixer.server;

import java.util.List;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.Main;
import edu.wisc.regfixer.fixer.Job;
import edu.wisc.regfixer.fixer.Range;

public class RequestPayload {
  private final String regex;
  private final List<Range> ranges;
  private final String corpus;

  public RequestPayload (String regex, List<Range> ranges, String corpus) {
    this.regex = regex;
    this.ranges = ranges;
    this.corpus = corpus;
  }

  public String getRegex () {
    return this.regex;
  }

  public List<Range> getRanges () {
    return this.ranges;
  }

  public String getCorpus () {
    return this.corpus;
  }

  public Job toJob () throws Exception {
    RegexNode parsedRegex = null;

    try {
      parsedRegex = Main.parse(this.regex);
    } catch (Exception ex) {
      throw new Exception(ex.toString());
    }

    return new Job(parsedRegex, this.ranges, this.corpus);
  }
}
