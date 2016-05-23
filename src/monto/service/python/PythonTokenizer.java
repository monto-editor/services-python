package monto.service.python;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.gson.GsonMonto;
import monto.service.product.Products;
import monto.service.python.antlr.Python3Lexer;
import monto.service.python.antlr.Python3Parser;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.token.ColorTheme;
import monto.service.token.FontStore;
import monto.service.token.Token;
import monto.service.token.TokenCategory;
import monto.service.types.Languages;
import org.antlr.v4.runtime.ANTLRInputStream;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PythonTokenizer extends MontoService {

    private Python3Lexer lexer;
    private FontStore fonts = new FontStore();
    private ColorTheme theme = ColorTheme.solarized();

    public PythonTokenizer(ZMQConfiguration zmqConfig) {
        super(zmqConfig,
                PythonServices.PYTHON_TOKENIZER,
                "Tokenizer",
                "A tokenizer for Python that uses ANTLR for tokenizing",
                Languages.PYTHON,
                Products.TOKENS,
                options(),
                dependencies(
                        new SourceDependency(Languages.PYTHON)
                )
        );
    }

    @Override
    public void onRequest(Request request) throws IOException {
        SourceMessage version = request.getSourceMessage()
                .orElseThrow(() -> new IllegalArgumentException("No version message in request"));

        lexer = new Python3Lexer(new ANTLRInputStream());
        lexer.setInputStream(new ANTLRInputStream(version.getContents()));
        List<Token> tokens = lexer.getAllTokens().stream().map(token -> convertToken(token)).collect(Collectors.toList());


        sendProductMessage(
                version.getId(),
                version.getSource(),
                Products.TOKENS,
                Languages.PYTHON,
                GsonMonto.toJsonTree(tokens)
        );
    }


    private Token convertToken(org.antlr.v4.runtime.Token token) {
        TokenCategory category;

        switch (token.getType()) {


//            category = TokenCategory.COMMENT;
//            break;

            case Python3Lexer.NONE:
                category = TokenCategory.CONSTANT;
                break;

            case Python3Lexer.STRING_LITERAL:
                category = TokenCategory.STRING;
                break;

//            category = TokenCategory.CHARACTER;
//            break;

            case Python3Lexer.DECIMAL_INTEGER:
            case Python3Lexer.OCT_INTEGER:
            case Python3Lexer.HEX_INTEGER:
            case Python3Lexer.BIN_INTEGER:
            case Python3Lexer.IMAG_NUMBER:
            case Python3Lexer.BYTES_LITERAL:
                category = TokenCategory.NUMBER;
                break;

            case Python3Lexer.TRUE:
            case Python3Lexer.FALSE:
                category = TokenCategory.BOOLEAN;
                break;

            case Python3Lexer.FLOAT_NUMBER:
                category = TokenCategory.FLOAT;
                break;

            case Python3Lexer.NAME:
                category = TokenCategory.IDENTIFIER;
                break;

            case Python3Lexer.IF:
            case Python3Lexer.ELIF:
            case Python3Lexer.ELSE:
                category = TokenCategory.CONDITIONAL;
                break;

            case Python3Lexer.WHILE:
            case Python3Lexer.FOR:
            case Python3Lexer.IN:
            case Python3Lexer.CONTINUE:
            case Python3Lexer.BREAK:
            case Python3Lexer.SKIP:
                category = TokenCategory.REPEAT;
                break;

//            category = TokenCategory.LABEL;
//            break;

            case Python3Lexer.OR:
            case Python3Lexer.AND:
            case Python3Lexer.NOT:
            case Python3Lexer.OR_OP:
            case Python3Lexer.XOR:
            case Python3Lexer.AND_OP:
            case Python3Lexer.LEFT_SHIFT:
            case Python3Lexer.RIGHT_SHIFT:
            case Python3Lexer.ADD:
            case Python3Lexer.MINUS:
            case Python3Lexer.DIV:
            case Python3Lexer.MOD:
            case Python3Lexer.IDIV:
            case Python3Lexer.NOT_OP:
            case Python3Lexer.ADD_ASSIGN:
            case Python3Lexer.SUB_ASSIGN:
            case Python3Lexer.MULT_ASSIGN:
            case Python3Lexer.AT_ASSIGN:
            case Python3Lexer.DIV_ASSIGN:
            case Python3Lexer.MOD_ASSIGN:
            case Python3Lexer.AND_ASSIGN:
            case Python3Lexer.OR_ASSIGN:
            case Python3Lexer.XOR_ASSIGN:
            case Python3Lexer.LEFT_SHIFT_ASSIGN:
            case Python3Lexer.RIGHT_SHIFT_ASSIGN:
            case Python3Lexer.POWER_ASSIGN:
            case Python3Lexer.IDIV_ASSIGN:
            case Python3Lexer.LESS_THAN:
            case Python3Lexer.GREATER_THAN:
            case Python3Lexer.EQUALS:
            case Python3Lexer.GT_EQ:
            case Python3Lexer.LT_EQ:
            case Python3Lexer.NOT_EQ_1:
            case Python3Lexer.NOT_EQ_2:
            case Python3Lexer.IS:
            case Python3Lexer.POWER:
            case Python3Lexer.ASSIGN:
                category = TokenCategory.OPERATOR;
                break;

            case Python3Lexer.TRY:
            case Python3Lexer.FINALLY:
            case Python3Lexer.RAISE:
            case Python3Lexer.EXCEPT:
            case Python3Lexer.WITH:
                category = TokenCategory.EXCEPTION;
                break;

//            category = TokenCategory.TYPE;
//            break;

            case Python3Lexer.DEF:
            case Python3Lexer.GLOBAL:
            case Python3Lexer.NONLOCAL:
            case Python3Lexer.LAMBDA:
                category = TokenCategory.MODIFIER;
                break;

            case Python3Lexer.CLASS:
                category = TokenCategory.STRUCTURE;
                break;

            case Python3Lexer.RETURN:
            case Python3Lexer.ASSERT:
            case Python3Lexer.FROM:
            case Python3Lexer.IMPORT:
            case Python3Lexer.YIELD:
            case Python3Lexer.AS:
            case Python3Lexer.DEL:
            case Python3Lexer.PASS:
                category = TokenCategory.KEYWORD;
                break;

            case Python3Lexer.OPEN_BRACK:
            case Python3Lexer.CLOSE_BRACK:
            case Python3Lexer.OPEN_BRACE:
            case Python3Lexer.CLOSE_BRACE:
            case Python3Lexer.OPEN_PAREN:
            case Python3Lexer.CLOSE_PAREN:

            case Python3Parser.INDENT:
            case Python3Parser.DEDENT:

                category = TokenCategory.PARENTHESIS;
                break;

            case Python3Lexer.DOT:
            case Python3Lexer.ELLIPSIS:
            case Python3Lexer.STAR:
            case Python3Lexer.COMMA:
            case Python3Lexer.COLON:
            case Python3Lexer.SEMI_COLON:
            case Python3Lexer.ARROW:
                category = TokenCategory.DELIMITER;
                break;

            case Python3Lexer.AT:
                category = TokenCategory.META;
                break;

            case Python3Lexer.NEWLINE:
                category = TokenCategory.WHITESPACE;
                break;

            case Python3Lexer.UNKNOWN_CHAR:
                category = TokenCategory.UNKNOWN;
                break;

            default:
                category = TokenCategory.UNKNOWN;
        }

        int offset = token.getStartIndex();
        int length = token.getStopIndex() - offset + 1;
        return new Token(offset, length, category, fonts.getFont(category.getColor(theme)));
    }


}
