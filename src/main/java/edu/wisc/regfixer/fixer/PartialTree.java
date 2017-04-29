package edu.wisc.regfixer.fixer;

import java.util.Arrays;
import java.util.List;

import edu.wisc.regfixer.parser.RegexNode;

public class PartialTree implements Costable {
  private final RegexNode tree;
  private final List<HoleNode> holes;

  public PartialTree (HoleNode hole) {
    this(hole, hole);
  }

  public PartialTree (RegexNode tree, HoleNode hole) {
    this(tree, Arrays.asList(hole));
  }

  public PartialTree (RegexNode tree, List<HoleNode> holes) {
    this.tree = tree;
    this.holes = holes;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public List<HoleNode> getHoles () {
    return this.holes;
  }

  public int getRemovedNodes () {
    return this.getHoles().stream().mapToInt(HoleNode::getRemovedNodes).sum();
  }

  public int getAddedNodes () {
    return this.getHoles().stream().mapToInt(HoleNode::getAddedNodes).sum();
  }

  public int getCost () {
    return this.getHoles().stream().mapToInt(HoleNode::getCost).sum();
  }
}
