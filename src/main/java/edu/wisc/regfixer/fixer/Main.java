package edu.wisc.regfixer.fixer;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.List;

import edu.wisc.regfixer.automata.Automaton;
import edu.wisc.regfixer.parser.*;
import edu.wisc.regfixer.enumerate.HoleNode;

/**
 * Created by sangyunpark on 5/19/17.
 */
public class Main {
    public static void main (String[] args) throws Exception {
        RegexNode regex = new ConcatNode(Arrays.asList(
                new HoleNode(),
                new StarNode(new CharEscapedNode('w'))
        ));
        RegexNode regex2 = new ConcatNode(Arrays.asList(
                new CharLiteralNode('a'),
                new HoleNode(),
                new StarNode(new HoleNode())
        ));

        Automaton automaton = new Automaton(regex2);
        System.out.println("Current Regex = " + regex2.toString());

//        String pos1 = "a";
        String pos2 = "axyz";
        String pos3 = "axyzxyz";
        String pos4 = "axyzxyzxyz";
        String neg1 = "abbbb";

//        List<Map<Integer, Set<Character>>> runs1 = automaton.computeRuns(pos1);
        List<Map<Integer, Set<Character>>> runs2 = automaton.computeRuns(pos2);
        List<Map<Integer, Set<Character>>> runs3 = automaton.computeRuns(pos3);
        List<Map<Integer, Set<Character>>> runs4 = automaton.computeRuns(pos4);
        List<Map<Integer, Set<Character>>> runs5 = automaton.computeRuns(neg1);

        print(regex2.toString(),runs2, pos2);
        print(regex2.toString(),runs3, pos3);
//        print(regex2.toString(),runs4, pos4);
        print(regex2.toString(),runs5, neg1);

        SATSolver solver = new SATSolver(regex2);
//        if(!runs1.isEmpty()) {
//            solver.makeFormula(runs1, true);
//        }
        if(!runs2.isEmpty()) {
            solver.makeFormula(runs2, true);
        }
        if(!runs3.isEmpty()) {
            solver.makeFormula(runs3, true);
        }
//        if(!runs4.isEmpty()) {
//            solver.makeFormula(runs4, true);
//        }
        if(!runs5.isEmpty()) {
            solver.makeFormula(runs5, false);
        }
//        System.out.printf("\nPositives: %s %s %s %s\n", pos1, pos2, pos3, pos4);
        System.out.printf("\nPositives: %s %s %s\n", pos2, pos3, pos4);
        System.out.printf("Negatives: %s\n", neg1);
        RegexNode newRegex = solver.solveFormula();
    }


    public static void print(String regex, List<Map<Integer, Set<Character>>> runs, String ex) {
        System.out.printf("\n%s apply \"%s\"\n", regex, ex);
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
