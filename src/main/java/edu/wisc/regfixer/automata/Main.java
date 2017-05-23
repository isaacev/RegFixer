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
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.synthesize.Formula;
import edu.wisc.regfixer.synthesize.CharClassSolver;
import edu.wisc.regfixer.synthesize.SynthesisFailure;

public class Main {
  public static void main (String[] args) throws Exception {
    RegexNode regex = new ConcatNode(Arrays.asList(
      new CharLiteralNode('a'),
      new StarNode(new ConcatNode(Arrays.asList(
        new HoleNode(),
        new HoleNode(),
        new HoleNode()
      )))
    ));

    Set<String> positives = new HashSet<>();
    positives.add("axyb");
    positives.add("axyzxyz");
    positives.add("axyzxyzxyz");

    Set<String> negatives = new HashSet<>();
    negatives.add("azyzyyz");
    negatives.add("axzzxxz");
    negatives.add("axyyxyx");

    experiment(regex, positives, negatives);
  }

  private static void experiment (RegexNode regex, Collection<String> pos, Collection<String> neg) throws Exception {
    Automaton automaton = new Automaton(regex);
    List<String> posList = new LinkedList<>(pos);
    List<String> negList = new LinkedList<>(neg);
    List<Set<Route>> posRoutes = new LinkedList<>();
    for (String str : posList) { posRoutes.add(automaton.trace(str)); }
    List<Set<Route>> negRoutes = new LinkedList<>();
    for (String str : negList) { negRoutes.add(automaton.trace(str)); }

    System.out.printf("Current Regex = %s\n", regex);
    System.out.println();

    // Print positive ranges
    for (int i = 0; i < pos.size(); i++) {
      System.out.printf("%s apply \"%s\"\n", regex, posList.get(i));
      printRouteSet(posRoutes.get(i));
    }

    // Print negative ranges
    for (int i = 0; i < neg.size(); i++) {
      System.out.printf("%s apply \"%s\"\n", regex, negList.get(i));
      printRouteSet(negRoutes.get(i));
    }

    // Print positive and negative examples
    System.out.printf("Positives:%s\n", posList.stream().reduce("", (a, s) -> a + " " + s));
    System.out.printf("Negatives:%s\n", negList.stream().reduce("", (a, s) -> a + " " + s));
    System.out.println();

    Formula formula = Formula.build(posRoutes, negRoutes);
    Map<Integer, CharClass> solution = null;

    try {
      solution = CharClassSolver.solve(formula);
    } catch (SynthesisFailure ex) {
      System.out.println("Failed to synthesize a result");
      System.exit(1);
    }

    System.out.println(solution);
  }

  private static void printRouteSet (Set<Route> routes) {
    if (routes == null) {
      System.out.println("ROUTES == NULL");
      return;
    }

    int i = 0;
    for (Route route : routes) {
      System.out.printf("R%d", i++);
      System.out.println(route);
    }
    System.out.println();
  }
}
