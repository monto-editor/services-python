package monto.service.python;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TreeSet;
import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.AST;
import monto.service.ast.ASTVisitor;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.gson.GsonMonto;
import monto.service.outline.Outline;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.registration.ProductDependency;
import monto.service.registration.ProductDescription;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.types.Languages;

public class PythonOutliner extends MontoService {

  public PythonOutliner(ZMQConfiguration zmqConfig) {
    super(
        zmqConfig,
        PythonServices.PYTHON_OUTLINER,
        "Outline",
        "An outline service for Python",
        productDescriptions(new ProductDescription(Products.OUTLINE, Languages.PYTHON)),
        options(),
        dependencies(
            new SourceDependency(Languages.PYTHON),
            new ProductDependency(PythonServices.PYTHON_PARSER, Products.AST, Languages.PYTHON)),
        commands());
  }

  @Override
  public void onRequest(Request request) throws Exception {
    SourceMessage version =
        request
            .getSourceMessage()
            .orElseThrow(() -> new IllegalArgumentException("No version message in request"));
    ProductMessage ast =
        request
            .getProductMessage(Products.AST, Languages.PYTHON)
            .orElseThrow(() -> new IllegalArgumentException("No AST message in request"));

    NonTerminal root = (NonTerminal) GsonMonto.fromJson(ast, AST.class);

    OutlineTrimmer trimmer = new OutlineTrimmer(version.getContents());
    root.accept(trimmer);

    sendProductMessage(
        version.getId(),
        version.getSource(),
        Products.OUTLINE,
        Languages.PYTHON,
        GsonMonto.toJsonTree(trimmer.getConverted()));
  }

  /**
   * Traverses the AST and removes unneeded information.
   */
  private class OutlineTrimmer implements ASTVisitor {

    private Deque<Outline> converted = new ArrayDeque<>();
    private TreeSet<String> variableNamesAlreadyOutlined = new TreeSet<String>();
    private final String content;

    public Outline getConverted() {
      return converted.getFirst();
    }

    public OutlineTrimmer(String content) {
      this.content = content;
    }

    @Override
    public void visit(NonTerminal node) {

      switch (node.getName()) {
        case "file_input":
          converted.push(new Outline("file_input", node, null));
          node.getChildren().forEach(child -> child.accept(this));
          break;

        case "funcdef":
          structureDeclaration(node, "function", getResource("method-public.png"));
          break;

        case "classdef":
          structureDeclaration(node, "class", getResource("class-public.png"));
          break;

        case "global_stmt":
          structureDeclaration(node, "global", getResource("field-public.png"));
          break;

        case "expr_stmt":
          checkExpr_stmt(node, "var", getResource("field-public.png"));
          break;

        default:
          node.getChildren().forEach(child -> child.accept(this));
      }
    }

    @Override
    public void visit(Terminal token) {}

    private void structureDeclaration(NonTerminal node, String name, URL url) {
      node.getChildren()
          .stream()
          .filter(ast -> ast instanceof Terminal)
          .limit(2)
          .reduce((previous, current) -> current)
          .ifPresent(
              ident -> {
                Outline structure = new Outline(name, ident, url);
                converted.peek().addChild(structure);
                converted.push(structure);
                node.getChildren().forEach(child -> child.accept(this));
                converted.pop();
              });
    }

    private void checkExpr_stmt(NonTerminal node, String name, URL url) {

      TerminalFinder finder = new TerminalFinder();

      node.getChildren()
          .stream()
          .filter(ast -> ast instanceof NonTerminal)
          .forEach(ident -> ident.accept(finder));

      String caption1stTerminalChild;
      if (finder.getFoundTerminal() != null) {
        caption1stTerminalChild = extract(content, finder.getFoundTerminal()).toString();

        node.getChildren()
            .stream()
            .filter(ast -> ast instanceof Terminal)
            .limit(2)
            .reduce((previous, current) -> current)
            .ifPresent(
                ident -> {
                  String secondChild = extract(content, ident).toString();

                  if (!variableNamesAlreadyOutlined.contains(caption1stTerminalChild)
                      && secondChild.equals("=")) {

                    NonTerminal reducedNode =
                        new NonTerminal(secondChild, node.getChildren().get(0));
                    Outline variable = new Outline(name, reducedNode, url);
                    converted.peek().addChild(variable);
                    converted.push(variable);
                    converted.pop();
                    variableNamesAlreadyOutlined.add(caption1stTerminalChild);
                  }
                });
      }
    }

    private String extract(String str, AST indent) {
      return str.subSequence(indent.getStartOffset(), indent.getStartOffset() + indent.getLength())
          .toString();
    }

    private class TerminalFinder implements ASTVisitor {

      private Terminal foundTerminal = null;

      @Override
      public void visit(NonTerminal node) {
        if (foundTerminal == null) {
          node.getChildren().stream().forEach(child -> child.accept(this));
        }
      }

      @Override
      public void visit(Terminal token) {
        foundTerminal = token;
      }

      public Terminal getFoundTerminal() {
        return foundTerminal;
      }
    }
  }
}
