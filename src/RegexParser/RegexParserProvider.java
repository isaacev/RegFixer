package RegexParser;

import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//import LTLparser.LTLNode;
import java_cup.runtime.Symbol;

/**
 * 
 * @author Fang Wang May/30/2016
 *
 *         Parser for regular expressions, unit test file is in
 *         testRegex/testRegex Detailed structure of the nodes is in
 *         FormulaNode.java No longer needs to use ExtractUsableLines.java, the
 *         provider filters parse-able input lines for you.
 */
public class RegexParserProvider {

	FileReader inFile;
	private static PrintWriter outFile;
	private static boolean isFile; // true for file, false for string
	private String inputAsString;
	public static RegexParserProvider test;

	public RegexParserProvider(String regex) {
		// check for command-line args
		inputAsString = regex;
		// open input file
	}

	public RegexParserProvider(FileReader reader) {
		isFile = true;
		inFile = reader;
	}

	private Symbol parseRegex(String line) {
		try {
			parser P = new parser(new Yylex(new StringReader(line)));
			return P.parse();

		} catch (Exception e) {
			return null;
		}
	}

	private RegexNode filterModifiers(String line) {
		RegexNode node = null;
		Symbol formula = null;
		boolean hasModifier = false;
		int pos = 0;
		// possibly has modifier
		if (line.length() >= 3 && line.charAt(0) == '/') {
			if (line.charAt(line.length() - 1) == '/') {
				// e.g. /a/ only means 'a'
				hasModifier = false;
				line = line.substring(1, line.length() - 1);
			} else {
				for (pos = line.length() - 1; pos > 0; pos--) {
					char tempChar = line.charAt(pos);
					if (tempChar == '/') {
						hasModifier = true;
						break;
					} else {
						if ((tempChar != 'm') && (tempChar != 'i') && (tempChar != 'H')&& (tempChar != 'U')&& (tempChar != 's')&& (tempChar != 'R')&& (tempChar != 'P')) {
							break;
						}
					}
				}
			}
		}

		if (hasModifier) {
			formula = parseRegex(line.substring(1, pos));
			return new ModifierNode((RegexNode) formula.value, line.substring(pos + 1, line.length()));
		} else {
			formula = parseRegex(line);
			node = (RegexNode) formula.value;
		}

		return node;
	}

	public RegexNode process() {
		return filterModifiers(inputAsString);
	}

	public static void toFile(RegexListNode root) {
		if (isFile) {
			root.unparse(outFile);
			System.out.println("Unparsing finished.");
			outFile.close();
		} else {
			System.err.println("This is a String, use toStringBuilder() instead");
		}
	}

	/**
	 * If you have something to do with the String output, use this method
	 * 
	 * @param boolean
	 *            is can be set to true if you want to print the output in the
	 *            console
	 */
	public static StringBuilder toStringBuilder(RegexNode root, boolean printString) {
		if (!isFile) {
			StringBuilder s = new StringBuilder();
			root.toString(s);
			if (printString) {
				String result = s.toString();
				System.out.println(result);
			}
			return s;
		} else {
			System.err.println("This is a File, use toFile() instead");
			return null;
		}
	}

	public static void main(String[] args) {


	}

}
