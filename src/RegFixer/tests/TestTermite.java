package RegFixer;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import RegexParser.*;
import RegFixer.*;

public class TestTermite {
  @Test
  public void testDigestOptional () {
    testDigestMethod('a?', new String[]{
      "(🔮 )?",
      "🔮 ",
    });
  }

  @Test
  public void testDigestStar () {
    List<String> expected = new LinkedList<String>();
    expected.add("(🔮 )*");
    expected.add("🔮 ");

    StarNode node = new StarNode(new CharNode('a'));
    List<RegexNode> found = Termite.digestStar(node);

    assertEquals(expected.size(), found.size());

    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i), found.get(i).toString());
    }
  }

  @Test
  public void testDigestPlus () {
    List<String> expected = new LinkedList<String>();
    expected.add("(🔮 )+");
    expected.add("🔮 ");

    PlusNode node = new PlusNode(new CharNode('a'));
    List<RegexNode> found = Termite.digestPlus(node);

    assertEquals(expected.size(), found.size());

    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i), found.get(i).toString());
    }
  }

  private void testDigestMethod(String pattern, String[] expected) throws Exception {
    RegexNode regex = RegexParser.Main.parse(pattern);
    List<RegexNode> found = Termite.digest(regex);
  }
}
