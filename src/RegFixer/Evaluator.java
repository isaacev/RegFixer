package regfixer;

import java.util.*;
import java.util.regex.*;
import RegexParser.*;
import static RegFixer.CorpusSearchEngine.*;

public class Evaluator {
  private String corpus;

  // The indexes in the corpus matched by the original (imperfect) regex.
  private List<Range> originalRanges;

  // The strings corresponding to the indexes in `originalRanges`.
  private List<String> originalStrings;

  // The indexes given by the user as the desired results of some better regex.
  private List<Range> selectedRanges;

  // The strings corresponding to the indexes in `selectedRanges`.
  private List<String> selectedStrings;

  // Strings that a better regex MUST match.
  private List<String> positiveExamples;

  // Strings that a better regex MUST reject.
  private List<String> negativeExamples;

  public Evaluator (String corpus, List<Range> originalRanges, List<Range> selectedRanges) {
    this.corpus = corpus;

    this.originalRanges = originalRanges;
    this.originalStrings = getMatchingStrings(corpus, originalRanges);

    this.selectedRanges = selectedRanges;
    this.selectedStrings = getMatchingStrings(corpus, selectedRanges);

    this.positiveExamples = this.selectedStrings;
    this.negativeExamples = new LinkedList<String>(this.originalStrings);
    this.negativeExamples.removeAll(this.selectedStrings);
  }

  public List<Range> getOriginalRanges () {
    return this.originalRanges;
  }

  public List<String> getOriginalStrings () {
    return this.originalStrings;
  }

  public List<Range> getSelectedRanges () {
    return this.selectedRanges;
  }

  public List<String> getSelectedStrings () {
    return this.selectedStrings;
  }

  public List<String> getPositiveExamples () {
    return this.positiveExamples;
  }

  public List<String> getNegativeExamples () {
    return this.negativeExamples;
  }

  public boolean runDotStarTest (TermiteTree tree) {
    RegexNode twig = new StarNode(new CharDotNode());
    tree.fillHole(twig);
    boolean didPass = Evaluator.regexMatchesAllExamples(tree, this.positiveExamples);
    tree.emptyHole();

    return didPass;
  }

  /**
   * TODO: To simulate an "empty set" within the regular expression,
   * the test inserts some character that is known to not exist within the
   * corpus and thus cannot expand any regular expression. Currently a
   * hard-coded "!" character is used instead of a more rigorous approach for
   * finding a character not in the corpus. Some sort of searching system should
   * be used to find character candidates instead.
   */
  public boolean runEmptySetTest (TermiteTree tree) {
    RegexNode twig = new CharLiteralNode('!');
    tree.fillHole(twig);
    boolean didPass = (Evaluator.regexMatchesAllExamples(tree, this.negativeExamples) == false);
    tree.emptyHole();

    return didPass;
  }

  public boolean runPositiveExampleTest (RegexNode regex) {
    return Evaluator.regexMatchesAllExamples(regex, this.positiveExamples);
  }

  public boolean runNegativeExampleTest (RegexNode regex) {
    return (Evaluator.regexMatchesAllExamples(regex, this.negativeExamples) == false);
  }

  public TermiteForest pruneForest (TermiteForest forest) {
    List<TermiteTree> trees = new LinkedList<TermiteTree>();

    for (TermiteTree tree : forest.getTrees()) {
      boolean dotStarResult = this.runDotStarTest(tree);
      boolean emptySetResult = this.runEmptySetTest(tree);

      if (dotStarResult && emptySetResult) {
        trees.add(tree);
      }
    }

    return new TermiteForest(forest.getTree(), trees);
  }

  public static boolean regexMatchesAllExamples (RegexNode regex, List<String> examples) {
    // Add start and end regular expression anchors (`^` and `$`) to ensure that
    // the regular expression is tested against the entire example and not some
    // substring of the example.
    Pattern pt = Pattern.compile("^" + regex.toString() + "$");

    for (String example : examples) {
      if (!pt.matcher(example).matches()) {
        return false;
      }
    }

    return true;
  }
}
