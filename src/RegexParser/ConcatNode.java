package RegexParser;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import RegexParser.RegexNode;

public class ConcatNode extends RegexNode {
	public ConcatNode(List<RegexNode> S) {
		myConcateList = S;
	}

	public List<RegexNode> getList() {
		return myConcateList;
	}


	public void toString(StringBuilder s) {
		Iterator<RegexNode> it = myConcateList.iterator();
		try {
			
			while (it.hasNext()) {
				s.append("(");
				((RegexNode) it.next()).toString(s);
				s.append(")");
			}
			
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.toString");
			System.exit(-1);
		}
	}

	@Override
	public String toString () {
		String str = "";

		Iterator<RegexNode> iter = myConcateList.iterator();
		try {
			while (iter.hasNext()) {
				str += "(" + ((RegexNode)iter.next()).toString() + ")";
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

		Iterator<RegexNode> iter = myConcateList.iterator();
		try {
			while (iter.hasNext()) {
				str += ((RegexNode)iter.next()).toCleanString();
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.toString");
			System.exit(-1);
		}

		return str;
	}

	public void unparse(PrintWriter p) {
		Iterator<RegexNode> it = myConcateList.iterator();
		try {
			
			while (it.hasNext()) {
				p.print("(");
				((RegexNode) it.next()).unparse(p);
				p.print(")");
			}
			
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in RegexListNode.unparse");
			System.exit(-1);
		}

	}

	// two kids
	private List<RegexNode> myConcateList;
}
