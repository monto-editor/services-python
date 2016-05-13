package monto.service.python;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.*;
import monto.service.completion.Completion;
import monto.service.gson.GsonMonto;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.region.IRegion;
import monto.service.registration.ProductDependency;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.types.Languages;
import monto.service.types.Selection;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;


public class PythonCodeCompletion extends MontoService {


    public PythonCodeCompletion(ZMQConfiguration zmqConfig) {
        super(zmqConfig,
                PythonServices.PYTHON_CODE_COMPLETION,
                "Code Completion",
                "A code completion service for Python",
                Languages.PYTHON,
                Products.COMPLETIONS,
                options(),
                dependencies(new SourceDependency(Languages.PYTHON),
                        new ProductDependency(PythonServices.PYTHON_PARSER, Products.AST, Languages.PYTHON))
        );
    }


    @Override
    public ProductMessage onRequest(Request request) throws Exception {
        SourceMessage version = request.getSourceMessage()
                .orElseThrow(() -> new IllegalArgumentException("No version message in request"));
        ProductMessage ast = request.getProductMessage(Products.AST, Languages.PYTHON)
                .orElseThrow(() -> new IllegalArgumentException("No AST message in request"));

        if (version.getSelection().isPresent()) {
            AST root = ASTs.decode(ast);
            List<Completion> allcompletions = allCompletions(version.getContents(), root);
            List<AST> selectedPath = selectedPath(root, version.getSelection().get());

            Terminal terminalToBeCompleted = (Terminal) last(selectedPath);

            String text = extract(version.getContents(), terminalToBeCompleted).toString();

            if (terminalToBeCompleted.getEndOffset() >= version.getSelection().get().getStartOffset() && terminalToBeCompleted.getStartOffset() <= version.getSelection().get().getStartOffset()) {
                int vStart = version.getSelection().get().getStartOffset();
                int tStart = terminalToBeCompleted.getStartOffset();
                text = text.substring(0, vStart - tStart);
            }
            String toBeCompleted = text;

            List<Completion> relevant =
                    allcompletions
                            .stream()
                            .filter(comp -> comp.getReplacement().startsWith(toBeCompleted))
                            .map(comp -> new Completion(
                                    comp.getDescription() + ": " + comp.getReplacement(),
                                    comp.getReplacement().substring(toBeCompleted.length()),
                                    version.getSelection().get().getStartOffset(),
                                    comp.getIcon()))
                            .collect(Collectors.toList());

            return productMessage(
                    version.getId(),
                    version.getSource(),
                    Products.COMPLETIONS,
                    Languages.PYTHON,
                    GsonMonto.toJson(relevant));

        }
        throw new IllegalArgumentException("Code completion needs selection");
    }

    private List<Completion> allCompletions(String contents, AST root) {
        AllCompletions completionVisitor = new AllCompletions(contents);
        root.accept(completionVisitor);
        return completionVisitor.getCompletions();
    }

    private class AllCompletions implements ASTVisitor {

        private TreeSet<String> variableNamesAlreadyOutlined = new TreeSet<String>();
        private List<Completion> completions = new ArrayList<>();
        private String content;

        public AllCompletions(String content) {
            this.content = content;
        }

        @Override
        public void visit(NonTerminal node) {

            switch (node.getName()) {

                case "funcdef":
                    addFuncToConverted(node, "def", getResource("method-public.png"));
                    break;

                case "classdef":
                    classToConverted(node, "class", getResource("class-public.png"));
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
        public void visit(Terminal token) {

        }

        private void structureDeclaration(NonTerminal node, String name, URL url) {
            Terminal structureIdent = (Terminal) node
                    .getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .reduce((previous, current) -> current).get();
            completions.add(new Completion(name, extract(content, structureIdent).toString(), url));
            node.getChildren().forEach(child -> child.accept(this));
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
                        .limit(2).reduce((previous, current) -> current)
                        .ifPresent(ident -> {
                            String secondChild = extract(content, ident).toString();

                            if (!variableNamesAlreadyOutlined.contains(caption1stTerminalChild) && secondChild.equals("=")) {
                                completions.add(new Completion(name, caption1stTerminalChild, url));
                                variableNamesAlreadyOutlined.add(caption1stTerminalChild);
                            }
                        });
            }
            node.getChildren().forEach(child -> child.accept(this));
        }

        private void addFuncToConverted(NonTerminal node, String name, URL url) {
            Object[] terminalChildren = node.getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .toArray();
            if (terminalChildren.length > 1) {
                Terminal structureIdent = (Terminal) terminalChildren[1];
                completions.add(new Completion(name, extract(content, structureIdent).toString(), url));

            }
            node.getChildren().forEach(child -> child.accept(this));
        }

        private void classToConverted(NonTerminal node, String name, URL url) {
            Object[] terminalChildren = node.getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .toArray();
            if (terminalChildren.length > 1) {
                Terminal structureIdent = (Terminal) terminalChildren[0];
                completions.add(new Completion(name, extract(content, structureIdent).toString(), url));

            }
            node.getChildren().forEach(child -> child.accept(this));
        }


        public List<Completion> getCompletions() {
            return completions;
        }

        private class TerminalFinder implements ASTVisitor {

            private Terminal foundTerminal = null;


            @Override
            public void visit(NonTerminal node) {
                if (foundTerminal == null) {
                    node.getChildren()
                            .stream().forEach(child -> child.accept(this));
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

    private static List<AST> selectedPath(AST root, Selection sel) {
        SelectedPath finder = new SelectedPath(sel);
        root.accept(finder);
        return finder.getSelected();
    }

    private static class SelectedPath implements ASTVisitor {

        private Selection selection;
        private List<AST> selectedPath = new ArrayList<>();

        public SelectedPath(Selection selection) {
            this.selection = selection;
        }

        @Override
        public void visit(NonTerminal node) {
            if (selection.inRange(node) || rightBehind(selection, node))
                selectedPath.add(node);
            node.getChildren()
                    .stream()
                    .filter(child -> selection.inRange(child) || rightBehind(selection, child))
                    .forEach(child -> child.accept(this));
        }

        @Override
        public void visit(Terminal token) {
            if (rightBehind(selection, token))
                selectedPath.add(token);
        }

        public List<AST> getSelected() {
            return selectedPath;
        }

        private static boolean rightBehind(IRegion region1, IRegion region2) {
            try {
                return region1.getStartOffset() <= region2.getEndOffset() && region1.getStartOffset() >= region2.getStartOffset();
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static String extract(String str, AST indent) {
        return str.subSequence(indent.getStartOffset(), indent.getStartOffset() + indent.getLength()).toString();
    }

    private static <A> A last(List<A> list) {
        return list.get(list.size() - 1);
    }
}
