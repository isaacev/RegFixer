package edu.wisc.regfixer.automata;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.List;

import edu.wisc.regfixer.fixer.SATSolver;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.enumerate.HoleNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;

public class Main {
  public static void main (String[] args) throws Exception {
    RegexNode regex = new ConcatNode(Arrays.asList(
      new PlusNode(new CharEscapedNode('w')),
      new PlusNode(new HoleNode()),
      new HoleNode()
    ));
    Automaton automaton = Converter.regexToAutomaton(regex);
    String str1 = "aaa443";
    String str2 = "aaa222";
    String str3 = "aaa444";
    List<Map<Integer, Set<Character>>> runs = automaton.runs(str1);
    List<Map<Integer, Set<Character>>> runs2 = automaton.runs(str2);
    List<Map<Integer, Set<Character>>> runs3 = automaton.runs(str3);

    // TODO: create a Encoding class and add encoding step here
    SATSolver.makeFormula(runs, true); // second argument indicating it's a positive example
    SATSolver.makeFormula(runs2, true); // second argument indicating it's a positive example
    SATSolver.makeFormula(runs3, false); // second argument indicating it's a negative example
    SATSolver.solveFormula();

    printing(regex, runs, str1);
    printing(regex, runs2, str2);
    printing(regex, runs3, str3);
  }

  private static void printing(RegexNode regex, List<Map<Integer, Set<Character>>> runs3, String str) {
    System.out.printf("%s apply \"%s\"\n", regex, str);
    for (int i = 0; i < runs3.size(); i++) {
      System.out.printf("R%d", i);
      Map<Integer, Set<Character>> run = runs3.get(i);
      for (Integer key : run.keySet()) {
        System.out.printf("\tH%d {", key);

        for (Character val : run.get(key)) {
          System.out.printf(" %c", val);
        }
        System.out.println(" }");
      }
    }
    System.out.println();
  }
}
