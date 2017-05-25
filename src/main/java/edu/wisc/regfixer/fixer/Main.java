package edu.wisc.regfixer.fixer;

import java.util.*;

import edu.wisc.regfixer.automata.Automaton;
import edu.wisc.regfixer.parser.*;
import edu.wisc.regfixer.enumerate.HoleNode;
import org.sat4j.specs.TimeoutException;

/**
 * Created by sangyunpark on 5/19/17.
 */
public class Main {
    public static void main (String[] args) throws Exception {
//        testRepeat();
//        testTwoHoles();
        testDigitAndLetters();
    }

    public static void testDigitAndLetters() {
        String reg = "(❑)+a(❑)+";
        RegexNode regex = new ConcatNode(Arrays.asList(
                new PlusNode(new HoleNode()),
                new CharLiteralNode('a'),
                new PlusNode(new HoleNode())
        ));
        List<String> posExamples = new ArrayList<>();
        posExamples.add("123a23");
        posExamples.add("abca23");

        List<String> negExamples = new ArrayList<>();
        negExamples.add("ccca1");
        negExamples.add("bbba1");

        performTest(regex, posExamples, negExamples);
    }

    public static void testRepeat() {
        String reg = "a(❑)*";
        RegexNode regex = new ConcatNode(Arrays.asList(
                new CharLiteralNode('a'),
//                new HoleNode(),
                new StarNode(new HoleNode())
        ));
        List<String> posExamples = new ArrayList<>();
        posExamples.add("a");
        posExamples.add("azyx");
        posExamples.add("axyzxyz");
        posExamples.add("axyzxyzxyz");

        List<String> negExamples = new ArrayList<>();
        negExamples.add("a123");

        performTest(regex, posExamples, negExamples);
    }

    public static void testTwoHoles() {
        String reg = "a❑(❑)*";
        try {
//            RegexNode regex= edu.wisc.regfixer.parser.Main.parse(reg);
        RegexNode regex = new ConcatNode(Arrays.asList(
                new CharLiteralNode('a'),
                new HoleNode(),
                new StarNode(new HoleNode())
        ));
            List<String> posExamples = new ArrayList<>();
//            posExamples.add("a4");
            posExamples.add("a2zxy");
            posExamples.add("a3yyzxyz");
            posExamples.add("a4zyzxyzxyz");

            List<String> negExamples = new ArrayList<>();
            negExamples.add("awww");

            performTest(regex, posExamples, negExamples);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void performTest(RegexNode regex, List<String> posExamples, List<String> negExamples) {

        try {
            System.out.println("Current Regex = " + regex.toString());

            Automaton automaton = new Automaton(regex);
            List<List<Map<Integer, Set<Character>>>> allRunsPos = new ArrayList<>();
            List<List<Map<Integer, Set<Character>>>> allRunsNeg = new ArrayList<>();

            System.out.print("Positives: ");
            for(int i = 0; i < posExamples.size(); i ++) {
                String pos =  posExamples.get(i);
                allRunsPos.add(automaton.computeRuns(pos));
                print(regex.toString(), allRunsPos.get(i), pos);
            }
            System.out.print("\nNegatives: ");
            for(int i = 0; i < negExamples.size(); i ++) {
                String neg  = negExamples.get(i);
//                System.out.print(neg + " ");
                allRunsNeg.add(automaton.computeRuns(neg));
                print(regex.toString(), allRunsNeg.get(i), neg);
            }
            System.out.println();
            SATSolver solver = new SATSolver(regex);
            for(List<Map<Integer, Set<Character>>> posRun : allRunsPos) {
                if(!posRun.isEmpty()) {
                    solver.makeFormula(posRun, true);
                }
            }
            for(List<Map<Integer, Set<Character>>> negRun : allRunsNeg) {
                if(!negRun.isEmpty()) {
                    solver.makeFormula(negRun, false);
                }
            }
            solver.encodePredConstraint();
            RegexNode newRegex = solver.solveFormula();
        } catch(Exception e) {
            e.printStackTrace();
        }


    }
    public static void print(String regex, List<Map<Integer, Set<Character>>> runs, String ex) {
        if (runs.isEmpty())
            return;
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
