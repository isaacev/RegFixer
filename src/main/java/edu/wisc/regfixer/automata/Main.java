package edu.wisc.regfixer.automata;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.wisc.regfixer.enumerate.Job;
import edu.wisc.regfixer.enumerate.Benchmark;
import edu.wisc.regfixer.enumerate.HoleNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.synthesize.CharClassSolver;
import edu.wisc.regfixer.synthesize.SynthesisFailure;

public class Main {
  public static void main (String[] args) throws Exception {
    RegexNode regex = new ConcatNode(Arrays.asList(
      new CharLiteralNode('a'),
      new PlusNode(
        new ConcatNode(Arrays.asList(
          new HoleNode(),
          new HoleNode(),
          new HoleNode()
        ))
      )
    ));

    Job job = Benchmark.readFromFile("benchmarks/repeat.txt");
    System.out.printf("pos: %d\n", job.getCorpus().getPositiveExamples().size());
    for (String pos : job.getCorpus().getPositiveExamples()) {
      System.out.println(pos);
    }

    System.out.printf("neg: %d\n", job.getCorpus().getNegativeExamples().size());
    for (String neg : job.getCorpus().getNegativeExamples()) {
      System.out.println(neg);
    }

    /*
    Set<String> positives = new HashSet<>();
    positives.add("axyz");

    Set<String> negatives = new HashSet<>();
    negatives.add("azyzyyz");
    negatives.add("axzzxxz");
    negatives.add("axyyxyx");

    experiment(regex, positives, negatives);
    */
  }

  private static void experiment (RegexNode regex, Collection<String> pos, Collection<String> neg) throws Exception {
    Automaton automaton = new Automaton(regex);
    List<String> posList = new LinkedList<>(pos);
    List<String> negList = new LinkedList<>(neg);
    List<Set<Route>> posRoutes = new LinkedList<>();
    for (String str : posList) { posRoutes.add(automaton.trace(str)); }
    List<Set<Route>> negRoutes = new LinkedList<>();
    for (String str : negList) { negRoutes.add(automaton.trace(str)); }

    System.out.println("=== REGEX ===");
    System.out.println(regex);
    System.out.println();

    System.out.println("=== TRACES ===");
    for (int i = 0; i < pos.size(); i++) { printRouteSet(posList.get(i), posRoutes.get(i)); }
    for (int i = 0; i < neg.size(); i++) { printRouteSet(negList.get(i), negRoutes.get(i)); }

    CharClassSolver.solve(posRoutes, negRoutes);

    Map<Integer, String> solution = null;

    try {
      solution = CharClassSolver.solve(posRoutes, negRoutes);
    } catch (SynthesisFailure ex) {
      System.out.println("=== FAILURE ===");
    }

    System.out.println("=== SUCCESS ===");
    System.out.println(solution);
  }

  private static void printRouteSet (String source, Set<Route> routes) {
    if (routes == null) {
      System.out.println("ROUTES == NULL");
      return;
    }

    System.out.printf("\"%s\"\n", source);
    int i = 0;
    for (Route route : routes) {
      System.out.printf("R%d", i++);
      System.out.println(route);
    }
    System.out.println();
  }
}
