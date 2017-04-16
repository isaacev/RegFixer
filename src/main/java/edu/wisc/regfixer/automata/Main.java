package edu.wisc.regfixer.automata;

import java.util.LinkedList;
import java.util.List;
import automata.sfa.SFA;
import edu.wisc.regfixer.parser.*;
import theory.characters.CharPred;

public class Main {
  public static void main (String[] args) throws Exception {
    RegexNode phi = edu.wisc.regfixer.parser.Main.parse(args[0]);

    System.out.println("===");
    System.out.println(phi.toString());
    System.out.println("===");

    SFA<CharPred, Character> sfa = Converter.regexToSFA(phi);

    System.out.println(sfa.toString());
    System.out.println("===");
  }
}
