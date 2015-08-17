package org.oc.orchestra.parser;


import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.oc.orchestra.constraint.Constraint;
import org.oc.orchestra.parser.RulesLexer;
import org.oc.orchestra.parser.RulesParser;

import java.io.FileInputStream;
import java.io.InputStream;

public class Cli {
	public static void main(String[] args) throws Exception {
		String inputFile = null;
		if ( args.length>0 ) inputFile = args[0];
		InputStream is = System.in;
		if ( inputFile!=null ) is = new FileInputStream(inputFile);
		ANTLRInputStream input = new ANTLRInputStream(is);
		RulesLexer lexer = new RulesLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		RulesParser parser = new RulesParser(tokens);
		ParseTree tree = parser.prog(); // parse; start at prog
		ConstraintsVisitor visitor = new ConstraintsVisitor();
		Constraint cons = visitor.visit(tree);
		System.out.println(cons);
		System.out.println(visitor.getConstraints().size());
		System.out.println(tree.toStringTree()); // print tree as text
	}
}
