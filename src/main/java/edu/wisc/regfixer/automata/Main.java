package edu.wisc.regfixer.automata;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.List;

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

    List<Map<Integer, Set<Character>>> runs = automaton.runs("foo123");

    System.out.printf("%s apply \"%s\"\n", regex, "foo123");
    for (int i = 0; i < runs.size(); i++) {
      System.out.printf("R%d", i);
      Map<Integer, Set<Character>> run = runs.get(i);
      for (Integer key : run.keySet()) {
        System.out.printf("\tH%d {", key);

        for (Character val : run.get(key)) {
          System.out.printf(" %c", val);
        }
        System.out.println(" }");
      }
    }
  }
}
