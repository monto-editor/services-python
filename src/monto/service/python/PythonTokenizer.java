package monto.service.python;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.zeromq.ZContext;

import monto.service.MontoService;
import monto.service.message.*;
import monto.service.python.antlr.Python3Lexer;
import monto.service.python.antlr.Python3Parser;
import monto.service.token.Category;
import monto.service.token.Token;
import monto.service.token.Tokens;

public class PythonTokenizer extends MontoService {

	private static final Product TOKENS = new Product("tokens");
    private static final Language PYTHON = new Language("python");

    private Python3Lexer lexer = new Python3Lexer(new ANTLRInputStream());

    public PythonTokenizer(ZContext context, String address, String registrationAddress, String serviceID) {
        super(context, 
        		address, 
        		registrationAddress, 
        		serviceID, 
        		"ANTLR Tokenizer for Python", 
        		"A tokenizer for Python that uses ANTLR for tokenizing", 
        		TOKENS, 
        		PYTHON, 
        		new String[]{"Source"});
    }

	@Override
	public ProductMessage onVersionMessage(List<Message> messages) throws Exception {
		VersionMessage version = Messages.getVersionMessage(messages);
        if (!version.getLanguage().equals(PYTHON)) {
            throw new IllegalArgumentException("wrong language in version message");
        }
//        version.getContent().getReader().;
        
        lexer = new Python3Lexer(new ANTLRInputStream());
        lexer.setInputStream(new ANTLRInputStream(version.getContent().getReader()));
//        System.out.println("version.getContent().getReader() = " + version.getContent().getReader());
        
//        System.out.println("lexer.getAllTokens().size = " + lexer.getAllTokens().size());
        List<Token> tokens = lexer.getAllTokens().stream().map(token -> convertToken(token)).collect(Collectors.toList());

        System.out.println("PYTHON TOKENIZERs ONVERSIONMESSAGE: tokens.size() = " + tokens.size());
        
        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                TOKENS,
                PYTHON,
                Tokens.encode(tokens));
	}
	
	@Override
	public void onConfigurationMessage(List<Message> arg0) throws Exception {
		
	}

	
	private Token convertToken(org.antlr.v4.runtime.Token token) {
        int offset = token.getStartIndex();
        int length = token.getStopIndex() - offset + 1;

        Category category;
        
        System.out.println(token +"type: " +token.getType());
        switch (token.getType()) {
    	
       	
//            category = Category.COMMENT;
//            break;
            
        case Python3Lexer.NONE :
            category = Category.CONSTANT;
            break;
            
        case Python3Lexer.STRING_LITERAL :
            category = Category.STRING;
            break;

//            category = Category.CHARACTER;
//            break;

        case Python3Lexer.DECIMAL_INTEGER :
        case Python3Lexer.OCT_INTEGER :
        case Python3Lexer.HEX_INTEGER :
        case Python3Lexer.BIN_INTEGER :
        case Python3Lexer.IMAG_NUMBER :
        case Python3Lexer.BYTES_LITERAL :
            category = Category.NUMBER;
            break;

        case Python3Lexer.TRUE :
        case Python3Lexer.FALSE :
            category = Category.BOOLEAN;
            break;

        case Python3Lexer.FLOAT_NUMBER :
            category = Category.FLOAT;
            break;
            
        case Python3Lexer.NAME :
            category = Category.IDENTIFIER;
            break;

        case Python3Lexer.IF :
        case Python3Lexer.ELIF :
        case Python3Lexer.ELSE :
            category = Category.CONDITIONAL;
            break;

        case Python3Lexer.WHILE :
        case Python3Lexer.FOR :
        case Python3Lexer.IN :
        case Python3Lexer.CONTINUE :
        case Python3Lexer.BREAK :
        case Python3Lexer.SKIP :
            category = Category.REPEAT;
            break;

//            category = Category.LABEL;
//            break;

        case Python3Lexer.OR :
        case Python3Lexer.AND :
        case Python3Lexer.NOT :
        case Python3Lexer.OR_OP :
        case Python3Lexer.XOR :
        case Python3Lexer.AND_OP :
        case Python3Lexer.LEFT_SHIFT :
        case Python3Lexer.RIGHT_SHIFT :
        case Python3Lexer.ADD :
        case Python3Lexer.MINUS :
        case Python3Lexer.DIV :
        case Python3Lexer.MOD :
        case Python3Lexer.IDIV :
        case Python3Lexer.NOT_OP :
        case Python3Lexer.ADD_ASSIGN :
        case Python3Lexer.SUB_ASSIGN :
        case Python3Lexer.MULT_ASSIGN :
        case Python3Lexer.AT_ASSIGN :
        case Python3Lexer.DIV_ASSIGN :
        case Python3Lexer.MOD_ASSIGN :
        case Python3Lexer.AND_ASSIGN :
        case Python3Lexer.OR_ASSIGN :
        case Python3Lexer.XOR_ASSIGN :
        case Python3Lexer.LEFT_SHIFT_ASSIGN :
        case Python3Lexer.RIGHT_SHIFT_ASSIGN :
        case Python3Lexer.POWER_ASSIGN :
        case Python3Lexer.IDIV_ASSIGN :
        case Python3Lexer.LESS_THAN :
        case Python3Lexer.GREATER_THAN :
        case Python3Lexer.EQUALS :
        case Python3Lexer.GT_EQ :
        case Python3Lexer.LT_EQ :
        case Python3Lexer.NOT_EQ_1 :
        case Python3Lexer.NOT_EQ_2 :
        case Python3Lexer.IS :
        case Python3Lexer.POWER :
        case Python3Lexer.ASSIGN :
            category = Category.OPERATOR;
            break;

        case Python3Lexer.TRY :
        case Python3Lexer.FINALLY :
        case Python3Lexer.RAISE :
        case Python3Lexer.EXCEPT :
        case Python3Lexer.WITH :
            category = Category.EXCEPTION;
            break;

//            category = Category.TYPE;
//            break;
            
        case Python3Lexer.DEF :
        case Python3Lexer.GLOBAL :
        case Python3Lexer.NONLOCAL :
        case Python3Lexer.LAMBDA :
            category = Category.MODIFIER;
            break;

        case Python3Lexer.CLASS :
            category = Category.STRUCTURE;
            break;

        case Python3Lexer.RETURN :
        case Python3Lexer.ASSERT :
        case Python3Lexer.FROM :
        case Python3Lexer.IMPORT :
        case Python3Lexer.YIELD :
        case Python3Lexer.AS :
        case Python3Lexer.DEL :
        case Python3Lexer.PASS :
            category = Category.KEYWORD;
            break;
            
        case Python3Lexer.OPEN_BRACK :
        case Python3Lexer.CLOSE_BRACK :
        case Python3Lexer.OPEN_BRACE :
        case Python3Lexer.CLOSE_BRACE :
        case Python3Lexer.OPEN_PAREN :
        case Python3Lexer.CLOSE_PAREN :
        	
        case Python3Parser.INDENT :
        case Python3Parser.DEDENT :
        	
            category = Category.PARENTHESIS;
            break;

        case Python3Lexer.DOT :
        case Python3Lexer.ELLIPSIS :
        case Python3Lexer.STAR :
        case Python3Lexer.COMMA :
        case Python3Lexer.COLON :
        case Python3Lexer.SEMI_COLON :
        case Python3Lexer.ARROW :
            category = Category.DELIMITER;
            break;
            
        case Python3Lexer.AT :
            category = Category.META;
            break;
            
        case Python3Lexer.NEWLINE :
            category = Category.WHITESPACE;
            break;
            
        case Python3Lexer.UNKNOWN_CHAR :
        	category = Category.UNKNOWN;
        	break;

        default:
        	System.out.println(token +"type: " +token.getType());
            category = Category.UNKNOWN;
        }
        return new Token(offset, length, category);
	}



}