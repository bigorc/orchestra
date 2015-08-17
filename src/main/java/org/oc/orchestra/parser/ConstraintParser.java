package org.oc.orchestra.parser;

import java.util.Scanner;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.oc.orchestra.constraint.Constraint;

public class ConstraintParser {

	public static Constraint parse(String str) {
		ANTLRInputStream input = new ANTLRInputStream(str);
		RulesLexer lexer = new RulesLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		RulesParser parser = new RulesParser(tokens);
		ParseTree tree = parser.prog(); // parse; start at prog
		ConstraintsVisitor visitor = new ConstraintsVisitor();
		Constraint cons = visitor.visit(tree);
		return cons;
	}

	public static void main(String[] args) {
		startCli();
	}
	

	private static void startCli() {
		String rule = prompt(">");
		while(!rule.equals("exit")) {
			System.out.println(rule);
			Constraint cons = ConstraintParser.parse(rule + "\n");
			cons.enforce();
			System.out.println(cons.getClass().getName());
			rule = prompt(">");
		}
	}

	private static String prompt(String output) {
		System.out.print(output);
		Scanner input = new Scanner(System.in, "utf-8");
		
		return input.nextLine();
	}

}
