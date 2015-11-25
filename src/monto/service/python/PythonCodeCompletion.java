package monto.service.python;

import monto.service.MontoService;
import monto.service.ast.*;
import monto.service.completion.Completion;
import monto.service.completion.Completions;
import monto.service.message.*;
import monto.service.region.IRegion;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PythonCodeCompletion extends MontoService {
	
    private static final Product AST = new Product("ast");
    private static final Product COMPLETIONS = new Product("completions");
    private static final Language PYTHON = new Language("python");

	public PythonCodeCompletion(ZContext context, String address, String registrationAddress, String serviceID) {
        super(context, 
        		address, 
        		registrationAddress, 
        		serviceID, 
        		"Code Completion service for Python", 
        		"A code completion service for Python",  
        		COMPLETIONS, 
        		PYTHON, 
        		new String[]{"Source","ast/python"});
	}

	@Override
	public void onConfigurationMessage(List<Message> arg0) throws Exception {
		
	}

	@Override
	public ProductMessage onVersionMessage(List<Message> messages) throws Exception {
        VersionMessage version = Messages.getVersionMessage(messages);
        if (!version.getLanguage().equals(PYTHON)) {
            throw new IllegalArgumentException("wrong language in version message");
        }
        ProductMessage ast = Messages.getProductMessage(messages, AST, PYTHON);
        if (!ast.getLanguage().equals(PYTHON)) {
            throw new IllegalArgumentException("wrong language in ast product message");
        }
        
        if (version.getSelections().size() > 0) {
            AST root = ASTs.decode(ast);
            List<Completion> allcompletions = allCompletions(version.getContent(), root);
            List<AST> selectedPath = selectedPath(root, version.getSelections().get(0));

//            if (selectedPath.size() > 0 && last(selectedPath) instanceof Terminal) {
                Terminal terminalToBeCompleted = (Terminal) selectedPath.get(0);
                String text = version.getContent().extract(terminalToBeCompleted).toString();
                
                if (terminalToBeCompleted.getEndOffset() >= version.getSelections().get(0).getStartOffset() && terminalToBeCompleted.getStartOffset() <= version.getSelections().get(0).getStartOffset()) {
                    int vStart = version.getSelections().get(0).getStartOffset();
                    int tStart = terminalToBeCompleted.getStartOffset();
                    text = text.substring(0, vStart - tStart);
                }
                String toBeCompleted = text;
                
                Stream<Completion> relevant =
                        allcompletions
                                .stream()
                                .filter(comp -> comp.getReplacement().startsWith(toBeCompleted))
                                .map(comp -> new Completion(
                                        comp.getDescription() + ": " + comp.getReplacement(),
                                        comp.getReplacement().substring(toBeCompleted.length()),
                                        version.getSelections().get(0).getStartOffset(),
                                        comp.getIcon()));

                return new ProductMessage(
                        version.getVersionId(),
                        new LongKey(1),
                        version.getSource(),
                        COMPLETIONS,
                        PYTHON,
                        Completions.encode(relevant),
                        new ProductDependency(ast));
//            }
//            throw new IllegalArgumentException(String.format("Last token in selection path is not a terminal: %s", selectedPath));
        }
        throw new IllegalArgumentException("Code completion needs selection");
	}
	
    private static List<Completion> allCompletions(Contents contents, AST root) {
        AllCompletions completionVisitor = new AllCompletions(contents);
        root.accept(completionVisitor);
        return completionVisitor.getCompletions();
    }
	
	private static class AllCompletions implements ASTVisitor {

        private List<Completion> completions = new ArrayList<>();
        private Contents content;

        public AllCompletions(Contents content) {
            this.content = content;
        }

        @Override
        public void visit(NonTerminal node) {
            switch (node.getName()) {

                case "funcdef":
                	structureDeclaration(node, "def", IconType.NO_IMG);
                    break;

                case "classdef":
                    structureDeclaration(node, "class", IconType.CLASS);
                    break;
                    
                case "global_stmt":
                	structureDeclaration(node, "global", IconType.NO_IMG);
                	break;

                default:
                    node.getChildren().forEach(child -> child.accept(this));
            }
        }

        @Override
        public void visit(Terminal token) {

        }

        private void structureDeclaration(NonTerminal node, String name, String icon) {
            Terminal structureIdent = (Terminal) node
                    .getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .reduce((previous, current) -> current).get();
            completions.add(new Completion(name, content.extract(structureIdent).toString(), icon));
            node.getChildren().forEach(child -> child.accept(this));
        }

        private void leaf(NonTerminal node, String name, String icon) {
            AST ident = node
                    .getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .findFirst().get();
            completions.add(new Completion(name, content.extract(ident).toString(), icon));
        }
        

        public List<Completion> getCompletions() {
            return completions;
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

    private static <A> A last(List<A> list) {
        return list.get(list.size() - 1);
    }

}
