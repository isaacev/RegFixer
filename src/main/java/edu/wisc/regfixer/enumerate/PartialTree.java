package edu.wisc.regfixer.enumerate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import edu.wisc.regfixer.parser.RegexNode;

public class PartialTree implements Comparable<PartialTree> {
  private final RegexNode tree;
  private final List<HoleNode> holes;
  private int removedNodes = 0;
  private int addedNodes = 0;

  public PartialTree (HoleNode hole, int removedNodes) {
    this(hole, hole, removedNodes);
  }

  public PartialTree (RegexNode tree, HoleNode hole, int removedNodes) {
    this(tree, hole, removedNodes, 0);
  }

  public PartialTree (RegexNode tree, HoleNode hole, int removedNodes, int addedNodes) {
    this(tree, Arrays.asList(hole), removedNodes, addedNodes);
  }

  public PartialTree (RegexNode tree, List<HoleNode> holes, int removedNodes) {
    this(tree, holes, removedNodes, 0);
  }

  public PartialTree (RegexNode tree, List<HoleNode> holes, int removedNodes, int addedNodes) {
    Collections.sort(holes);
    this.tree = tree;
    this.holes = holes;
    this.removedNodes = removedNodes;
    this.addedNodes = addedNodes;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public List<HoleNode> getHoles () {
    return this.holes;
  }

  public int getRemovedNodes () {
    return this.removedNodes;
  }

  public int getAddedNodes () {
    return this.addedNodes;
  }

  public int getCost () {
    return this.getRemovedNodes() + this.getAddedNodes();
  }

  public Pattern toPattern (RegexNode fill) {
    this.holes.stream().forEach(h -> h.fill(fill));
    Pattern pattern = Pattern.compile(String.format("^%s$", this.tree));
    this.holes.stream().forEach(HoleNode::clear);
    return pattern;
  }

  @Override
  public int compareTo (PartialTree other) {
    return Integer.compare(this.getCost(), other.getCost());
  }

  public String toString () {
    return this.tree.toString();
  }
}
