package monto.service.python;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.AST;
import monto.service.ast.ASTVisitor;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.gson.GsonMonto;
import monto.service.identifier.Identifier;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.registration.ProductDependency;
import monto.service.registration.ProductDescription;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.types.Languages;

public class PythonIdentifierFinder extends MontoService {

  public PythonIdentifierFinder(ZMQConfiguration zmqConfig) {
    super(
        zmqConfig,
        PythonServices.IDENTIFIER_FINDER,
        "Identifier Finder",
        "Searches AST for identifiers",
        productDescriptions(new ProductDescription(Products.IDENTIFIER, Languages.PYTHON)),
        options(),
        dependencies(
            new SourceDependency(Languages.PYTHON),
            new ProductDependency(PythonServices.PARSER, Products.AST, Languages.PYTHON)),
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

    AST root = GsonMonto.fromJson(ast, AST.class);
    IdentifierVisitor completionVisitor = new IdentifierVisitor(version.getContents());
    root.accept(completionVisitor);
    List<Identifier> identifiers = completionVisitor.getIdentifiers();

    sendProductMessage(
        version.getId(),
        version.getSource(),
        Products.IDENTIFIER,
        Languages.PYTHON,
        GsonMonto.toJsonTree(identifiers));
  }

  private class IdentifierVisitor implements ASTVisitor {

    private TreeSet<String> variableNamesAlreadyOutlined = new TreeSet<String>();
    private List<Identifier> identifiers = new ArrayList<>();
    private String content;

    public IdentifierVisitor(String content) {
      this.content = content;
    }

    @Override
    public void visit(NonTerminal node) {

      switch (node.getName()) {
        case "funcdef":
          addFuncToConverted(node, "method");
          break;

        case "classdef":
          classToConverted(node, "class");
          break;

        case "global_stmt":
          structureDeclaration(node, "field");
          break;

        case "expr_stmt":
          checkExpr_stmt(node, "variable");
          break;

        default:
          node.getChildren().forEach(child -> child.accept(this));
      }
    }

    @Override
    public void visit(Terminal token) {}

    private void structureDeclaration(NonTerminal node, String type) {
      Terminal structureIdent =
          (Terminal)
              node.getChildren()
                  .stream()
                  .filter(ast -> ast instanceof Terminal)
                  .reduce((previous, current) -> current)
                  .get();
      String identifier = structureIdent.extract(content);
      identifiers.add(new Identifier(identifier, type));
      node.getChildren().forEach(child -> child.accept(this));
    }

    private void checkExpr_stmt(NonTerminal node, String type) {

      TerminalFinder finder = new TerminalFinder();

      node.getChildren()
          .stream()
          .filter(ast -> ast instanceof NonTerminal)
          .forEach(ident -> ident.accept(finder));

      String caption1stTerminalChild;
      if (finder.getFoundTerminal() != null) {
        caption1stTerminalChild = finder.getFoundTerminal().extract(content);

        node.getChildren()
            .stream()
            .filter(ast -> ast instanceof Terminal)
            .limit(2)
            .reduce((previous, current) -> current)
            .ifPresent(
                ident -> {
                  String secondChild = ident.extract(content);

                  if (!variableNamesAlreadyOutlined.contains(caption1stTerminalChild)
                      && secondChild.equals("=")) {
                    identifiers.add(new Identifier(caption1stTerminalChild, type));
                    variableNamesAlreadyOutlined.add(caption1stTerminalChild);
                  }
                });
      }
      node.getChildren().forEach(child -> child.accept(this));
    }

    private void addFuncToConverted(NonTerminal node, String type) {
      Object[] terminalChildren =
          node.getChildren().stream().filter(ast -> ast instanceof Terminal).toArray();
      if (terminalChildren.length > 1) {
        Terminal structureIdent = (Terminal) terminalChildren[1];
        String identifier = structureIdent.extract(content);
        identifiers.add(new Identifier(identifier, type));
      }
      node.getChildren().forEach(child -> child.accept(this));
    }

    private void classToConverted(NonTerminal node, String type) {
      Object[] terminalChildren =
          node.getChildren().stream().filter(ast -> ast instanceof Terminal).toArray();
      if (terminalChildren.length > 1) {
        Terminal structureIdent = (Terminal) terminalChildren[0];
        String identifier = structureIdent.extract(content);
        identifiers.add(new Identifier(identifier, type));
      }
      node.getChildren().forEach(child -> child.accept(this));
    }

    public List<Identifier> getIdentifiers() {
      return identifiers;
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
