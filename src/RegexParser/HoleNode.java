package RegexParser;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class HoleNode extends RegexNode{

	private List<RegexNode> regexList;
	
	public void addRegexList(List<RegexNode> regexList) {
		this.regexList = regexList;
	}
	public void addRegex(RegexNode r) {
		regexList.add(r);
	}
	
	public List<RegexNode> getList() {
		return this.regexList;
	}
	
	
	@Override
	public void unparse(PrintWriter p) {
		Iterator<RegexNode> it = regexList.iterator();
		try {
			
			while (it.hasNext()) {
				p.print("(");
				((RegexNode) it.next()).unparse(p);
				p.print(")");
			}
			
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.unparse");
			System.exit(-1);
		}	}

	@Override
	public void toString(StringBuilder s) {
		/*Iterator<RegexNode> it = regexList.iterator();
		try {
			while (it.hasNext()) {
				((RegexNode) it.next()).toString(s);
				s.append(System.getProperty("line.separator"));
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.toString");
			System.exit(-1);
		}		
		*/
		s.append("[ ]");
	}

	@Override
	public String toString () {
		// render as 🐱 (an emoji cat)
		return "\uD83D\uDC31 ";
	}
}
