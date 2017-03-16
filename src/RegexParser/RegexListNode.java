package RegexParser;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import RegexParser.FormulaNode;


public class RegexListNode extends FormulaNode{
	private List<RegexNode> myRegexList;

	public RegexListNode(List<RegexNode> S) {
		myRegexList = S;
	}
	
	public void addRegex(RegexNode r){
		myRegexList.add(r);
	}

	public List<RegexNode> getList() {
		return myRegexList;
	}

	public void toString(StringBuilder s) {
		Iterator<RegexNode> it = myRegexList.iterator();
		try {
			while (it.hasNext()) {
				((RegexNode) it.next()).toString(s);
				s.append(System.getProperty("line.separator"));
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.toString");
			System.exit(-1);
		}
	}

	@Override
	public String toString () {
		String str = "";

		Iterator<RegexNode> iter = myRegexList.iterator();
		try {
			while (iter.hasNext()) {
				str += ((RegexNode)iter.next()).toString() + System.getProperty("line.seperator");
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.toString");
			System.exit(-1);
		}

		return str;
	}

	@Override
	public String toCleanString () {
		String str = "";

		Iterator<RegexNode> iter = myRegexList.iterator();
		try {
			while (iter.hasNext()) {
				str += ((RegexNode)iter.next()).toCleanString() + System.getProperty("line.seperator");
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.toCleanString");
			System.exit(-1);
		}

		return str;
	}

	public void unparse(PrintWriter p) {
		Iterator<RegexNode> it = myRegexList.iterator();
		try {
			while (it.hasNext()) {
				((RegexNode) it.next()).unparse(p);
				p.print(System.getProperty("line.separator"));
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.unparse");
			System.exit(-1);
		}

	}

}
