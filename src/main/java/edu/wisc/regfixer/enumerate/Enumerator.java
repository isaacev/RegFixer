package edu.wisc.regfixer.enumerate;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.PriorityQueue;

import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;

public class Enumerator {
  private final Job job;
  private final PriorityQueue<PartialTree> queue;

  public Enumerator (Job job) {
    this.job = job;
    this.queue = new PriorityQueue<PartialTree>();
    this.queue.addAll(Partials.slice(job.getOriginalRegex()));
  }

  public boolean isEmpty () {
    return this.queue.isEmpty();
  }

  public PartialTree next () {
    if (this.isEmpty()) {
      return null;
    }

    PartialTree next = this.queue.remove();
    this.queue.addAll(this.expand(next));

    return next;
  }

  public List<PartialTree> expand (PartialTree parent) {
    if (parent.getHoles().size() == 0) {
      throw new RuntimeException("cannot expand tree with no holes");
    }

    List<PartialTree> expansions = new LinkedList<>();

    for (HoleNode hole : parent.getHoles()) {
      expansions.add(expandUnion(parent, hole));
      expansions.add(expandConcat(parent, hole));
      expansions.add(expandOptional(parent, hole));
      expansions.add(expandPlus(parent, hole));
      expansions.add(expandStar(parent, hole));
    }

    return expansions;
  }

  private PartialTree expandUnion (PartialTree parent, HoleNode hole) {
    int removed = parent.getRemovedNodes();
    int added = parent.getAddedNodes() + 1;
    HoleNode left = new HoleNode();
    HoleNode right = new HoleNode();
    List<HoleNode> holes = Arrays.asList(left, right);
    PartialTree twig = new PartialTree(new UnionNode(left, right), holes, removed, added);
    return Grafter.graft(parent, hole, twig);
  }

  private PartialTree expandConcat (PartialTree parent, HoleNode hole) {
    int removed = parent.getRemovedNodes();
    int added = parent.getAddedNodes() + 1;
    HoleNode left = new HoleNode();
    HoleNode right = new HoleNode();
    List<HoleNode> holes = Arrays.asList(left, right);
    List<RegexNode> children = new LinkedList<>(holes);
    PartialTree twig = new PartialTree(new ConcatNode(children), holes, removed, added);
    return Grafter.graft(parent, hole, twig);
  }

  private PartialTree expandOptional (PartialTree parent, HoleNode hole) {
    int removed = parent.getRemovedNodes();
    int added = parent.getAddedNodes() + 1;
    HoleNode child = new HoleNode();
    PartialTree twig = new PartialTree(new OptionalNode(child), child, removed, added);
    return Grafter.graft(parent, hole, twig);
  }

  private PartialTree expandPlus (PartialTree parent, HoleNode hole) {
    int removed = parent.getRemovedNodes();
    int added = parent.getAddedNodes() + 1;
    HoleNode child = new HoleNode();
    PartialTree twig = new PartialTree(new PlusNode(child), child, removed, added);
    return Grafter.graft(parent, hole, twig);
  }

  private PartialTree expandStar (PartialTree parent, HoleNode hole) {
    int removed = parent.getRemovedNodes();
    int added = parent.getAddedNodes() + 1;
    HoleNode child = new HoleNode();
    PartialTree twig = new PartialTree(new StarNode(child), child, removed, added);
    return Grafter.graft(parent, hole, twig);
  }
}
