
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class RunSentence {
    public static void main(String[] args) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(System.in);
        SentenceLexer lexer = new SentenceLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SentenceParser parser = new SentenceParser(tokens);
        ParseTree tree = parser.sentence();

//        CalculatorBaseVisitorImpl calcVisitor = new CalculatorBaseVisitorImpl();
//        Double result = calcVisitor.visit(tree);
//        System.out.println("Result: " + result);
    }
}
