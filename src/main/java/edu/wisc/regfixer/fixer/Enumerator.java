package edu.wisc.regfixer.fixer;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.PriorityQueue;

import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;

public class Enumerator {
  private final Job job;
  private final Comparator<Costable> comparator;
  private final PriorityQueue<PartialTree> queue;

  public Enumerator (Job job) {
    this.job = job;
    this.comparator = new CostableComparator();
    this.queue = new PriorityQueue<PartialTree>(10, this.comparator);
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
      // expansions.add(expandUnion(parent, hole));
      expansions.add(expandConcat(parent, hole));
      expansions.add(expandOptional(parent, hole));
      expansions.add(expandPlus(parent, hole));
      expansions.add(expandStar(parent, hole));
    }

    return this.job.getEvaluator().pruneForest(expansions);
  }

  private PartialTree expandUnion (PartialTree parent, HoleNode hole) {
    int removed = hole.getRemovedNodes();
    int added = hole.getAddedNodes();
    HoleNode leftHole = new HoleNode(removed, added + 1);
    HoleNode rightHole = new HoleNode(removed, added + 1);
    List<HoleNode> holes = Arrays.asList(leftHole, rightHole);

    PartialTree twig = new PartialTree(new UnionNode(leftHole, rightHole), holes);
    return Grafter.graft(parent, hole, twig);
  }

  private PartialTree expandConcat (PartialTree parent, HoleNode hole) {
    int removed = hole.getRemovedNodes();
    int added = hole.getAddedNodes();
    HoleNode leftHole = new HoleNode(removed, added + 1);
    HoleNode rightHole = new HoleNode(removed, added + 1);
    List<HoleNode> holes = Arrays.asList(leftHole, rightHole);

    PartialTree twig = new PartialTree(new ConcatNode(new LinkedList<RegexNode>(holes)), holes);
    return Grafter.graft(parent, hole, twig);
  }

  private PartialTree expandOptional (PartialTree parent, HoleNode hole) {
    int removed = hole.getRemovedNodes();
    int added = hole.getAddedNodes();
    HoleNode childHole = new HoleNode(removed, added + 1);

    PartialTree twig = new PartialTree(new OptionalNode(childHole), childHole);
    return Grafter.graft(parent, hole, twig);
  }

  private PartialTree expandPlus (PartialTree parent, HoleNode hole) {
    int removed = hole.getRemovedNodes();
    int added = hole.getAddedNodes();
    HoleNode childHole = new HoleNode(removed, added + 1);

    PartialTree twig = new PartialTree(new PlusNode(childHole), childHole);
    return Grafter.graft(parent, hole, twig);
  }

  private PartialTree expandStar (PartialTree parent, HoleNode hole) {
    int removed = hole.getRemovedNodes();
    int added = hole.getAddedNodes();
    HoleNode childHole = new HoleNode(removed, added + 1);

    PartialTree twig = new PartialTree(new StarNode(childHole), childHole);
    return Grafter.graft(parent, hole, twig);
  }
}
