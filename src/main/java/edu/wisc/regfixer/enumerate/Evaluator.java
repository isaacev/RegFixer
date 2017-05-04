package edu.wisc.regfixer.enumerate;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import static edu.wisc.regfixer.enumerate.SearchEngine.getMatchingStrings;

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

  public Evaluator (List<Range> originalRanges, List<Range> selectedRanges, String corpus) {
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

  public boolean passesDotTest (PartialTree tree) {
    Pattern pattern = tree.toPattern(new CharDotNode());
    return Evaluator.matchesAllExamples(pattern, this.positiveExamples);
  }

  public boolean passesDotStarTest (PartialTree tree) {
    Pattern pattern = tree.toPattern(new StarNode(new CharDotNode()));
    return Evaluator.matchesAllExamples(pattern, this.positiveExamples);
  }

  /**
   * TODO: To simulate an "empty set" within the regular expression,
   * the test inserts some character that is known to not exist within the
   * corpus and thus cannot expand any regular expression. Currently a
   * hard-coded "!" character is used instead of a more rigorous approach for
   * finding a character not in the corpus. Some sort of searching system should
   * be used to find character candidates instead.
   */
  public boolean passesEmptySetTest (PartialTree tree) {
    Pattern pattern = tree.toPattern(new CharLiteralNode('!'));
    return Evaluator.matchesAllExamples(pattern, this.negativeExamples);
  }

  public boolean passesPositiveExampleTest (RegexNode regex) {
    return Evaluator.matchesAllExamples(regex, this.positiveExamples);
  }

  public boolean passesNegativeExampleTest (RegexNode regex) {
    return Evaluator.matchesAllExamples(regex, this.negativeExamples) == false;
  }

  public List<PartialTree> pruneForest (List<PartialTree> forest) {
    List<PartialTree> pruned = new LinkedList<PartialTree>();

    for (PartialTree tree : forest) {
      boolean dotStarResult = this.passesDotStarTest(tree);
      boolean emptySetResult = this.passesEmptySetTest(tree);

      if (dotStarResult && emptySetResult) {
        pruned.add(tree);
      }
    }

    return pruned;
  }

  public static boolean matchesAllExamples (RegexNode regex, List<String> examples) {
    // Add start and end regular expression anchors (`^` and `$`) to ensure that
    // the regular expression is tested against the entire example and not some
    // substring of the example.
    Pattern pt = Pattern.compile("^" + regex.toString() + "$");
    return matchesAllExamples(pt, examples);
  }

  public static boolean matchesAllExamples (Pattern pattern, List<String> examples) {
    for (String example : examples) {
      if (!pattern.matcher(example).matches()) {
        return false;
      }
    }

    return true;
  }
}
