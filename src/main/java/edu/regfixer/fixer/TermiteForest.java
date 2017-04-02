package edu.wisc.regfixer.fixer;

import java.util.*;
import edu.wisc.regfixer.parser.RegexNode;

public class TermiteForest {
  private RegexNode tree;
  private List<TermiteTree> trees;

  public TermiteForest (RegexNode tree, List<TermiteTree> trees) {
    this.tree = tree;
    this.trees = trees;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public List<TermiteTree> getTrees () {
    return this.trees;
  }
}
