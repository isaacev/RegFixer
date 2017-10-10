package edu.wisc.regfixer.enumerate;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import edu.wisc.regfixer.parser.RegexNode;

public class Enumerants {
  private final RegexNode original;
  private final Corpus corpus;
  private Set<String> history;
  private Queue<Enumerant> queue;

  public Enumerants (RegexNode original, Corpus corpus) {
    this.original = original;
    this.corpus = corpus;
    this.init();
  }

  public Enumerant poll () {
    if (this.queue.isEmpty()) {
      return null;
    }

    Enumerant enumerant = this.queue.remove();

    for (Enumerant expansion : enumerant.expand()) {
      if (false == this.history.contains(expansion.toString())) {
        this.history.add(expansion.toString());
        this.queue.add(expansion);
      }
    }

    if (enumerant.getExpansionChoice() == HoleNode.ExpansionChoice.Union) {
      return this.poll();
    } else {
      return enumerant;
    }
  }

  private void init () {
    this.history = new HashSet<>();
    this.queue = new PriorityQueue<>();

    for (Enumerant expansion : Slicer.slice(this.original)) {
      this.history.add(expansion.toString());
      this.queue.add(expansion);
    }
  }

  public void restart (Set<Range> negatives) {
    this.corpus.addNegativeMatches(negatives);
    this.init();
  }
}
