package monto.service.python;

import monto.service.MontoService;
import monto.service.ast.*;
import monto.service.message.*;
import monto.service.outline.Outline;
import monto.service.outline.Outlines;
import monto.service.region.Region;

import org.zeromq.ZContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

public class PythonOutliner extends MontoService {

    private static final Product AST = new Product("ast");
    private static final Product OUTLINE = new Product("outline");
    private static final Language PYTHON = new Language("python");

	public PythonOutliner(ZContext context, String address, String registrationAddress, String serviceID) {
		super(context, 
				address, 
				registrationAddress, 
				serviceID, 
				"Outline service for Python", 
				"An outline service for Python", 
				OUTLINE, 
				PYTHON, 
				new String[]{"Source","ast/python"});
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
        
        NonTerminal root = (NonTerminal) ASTs.decode(ast);

        OutlineTrimmer trimmer = new OutlineTrimmer(version.getContent());
        root.accept(trimmer);

        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                OUTLINE,
                PYTHON,
                Outlines.encode(trimmer.getConverted()),
                new ProductDependency(ast));
	}
	
	@Override
	public void onConfigurationMessage(List<Message> arg0) throws Exception {
		
	}
	
	/**
     * Traverses the AST and removes unneeded information.
     */
    private static class OutlineTrimmer implements ASTVisitor {

        private Deque<Outline> converted = new ArrayDeque<>();
        private TreeSet<String> variableNamesAlreadyOutlined = new TreeSet<String>();
        private final Contents content;

        public Outline getConverted() {
            return converted.getFirst();
        }
        
        public OutlineTrimmer(Contents content){
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
                	structureDeclaration(node, "function", IconType.PUBLIC);
                    break;
                    
                case "classdef":
                	structureDeclaration(node, "class", IconType.CLASS);
                	break;
                	
                case "global_stmt":
                	structureDeclaration(node, "global", IconType.PRIVATE);
                	break;
                	
                case "expr_stmt":
                	checkExpr_stmt(node, "var", IconType.PRIVATE);
                	break;

                default:
                    node.getChildren().forEach(child -> child.accept(this));
            }
        }

        @Override
        public void visit(Terminal token) {

        }
        
        private void structureDeclaration(NonTerminal node, String name, String icon) {
            node.getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .limit(2)
                    .reduce((previous, current) -> current)
                    .ifPresent(ident -> {
                        Outline structure = new Outline(name, ident, icon);
                        converted.peek().addChild(structure);
                        converted.push(structure);
                        node.getChildren().forEach(child -> child.accept(this));
                        converted.pop();
                    });
        }
        
        private void checkExpr_stmt(NonTerminal node, String name, String icon) {
         
        	TerminalFinder finder = new TerminalFinder();
        	
        	node.getChildren()
                	.stream()
                	.filter(ast -> ast instanceof NonTerminal)
                	.forEach(ident -> ident.accept(finder));
        	
        	
        	String caption1stTerminalChild;
        	if(finder.getFoundTerminal() != null){
        		caption1stTerminalChild = content.extract(finder.getFoundTerminal()).toString();
        		
        		node.getChildren()
        		.stream()
        		.filter(ast -> ast instanceof Terminal)
        		.limit(2).reduce((previous, current) -> current)
        		.ifPresent(ident -> {
        			String secondChild = content.extract(ident).toString();
        			
        			if (!variableNamesAlreadyOutlined.contains(caption1stTerminalChild)&&secondChild.equals("=") ){
        				
        				NonTerminal reducedNode = new NonTerminal(secondChild, node.getChildren().get(0));
        				Outline variable = new Outline(name, reducedNode, icon);
        				converted.peek().addChild(variable);
        				converted.push(variable);
        				converted.pop();
        				variableNamesAlreadyOutlined.add(caption1stTerminalChild);
        			}
        		});
        	} 
        }
        
        private class TerminalFinder implements ASTVisitor {
        	
        	private Terminal foundTerminal = null;


			@Override
			public void visit(NonTerminal node) {
				if(foundTerminal == null){
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




}
