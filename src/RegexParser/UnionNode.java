package RegexParser;

import java.io.PrintWriter;
import RegexParser.RegexNode;

public class UnionNode extends RegexNode {
	public UnionNode(RegexNode r1, RegexNode r2) {
		myRegex1 = r1;
		myRegex2 = r2;
	}

	public RegexNode getMyRegex1() {
		return myRegex1;
	}

	public RegexNode getMyRegex2() {
		return myRegex2;
	}

	@Override
	public void unparse(PrintWriter p) {
		p.print("(");
		myRegex1.unparse(p);
		p.print(" | ");
		myRegex2.unparse(p);
		p.print(")");
	}

	@Override
	public void toString(StringBuilder s) {
		s.append("(");
		myRegex1.toString(s);
		s.append(" | ");
		myRegex2.toString(s);
		s.append(")");
	}

	@Override
	public String toString () {
		return "(" + myRegex1.toString() + " | " + myRegex2.toString() + ")";
	}

	@Override
	public String toCleanString () {
		return myRegex1.toCleanString() + " | " + myRegex2.toCleanString();
	}

	// two kids
	protected RegexNode myRegex1;
	protected RegexNode myRegex2;
}
