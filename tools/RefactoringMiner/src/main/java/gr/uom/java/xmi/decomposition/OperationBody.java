package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.decomposition.Visitor.stringify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class OperationBody {

	private CompositeStatementObject compositeStatement;
	private List<String> stringRepresentation;
	private boolean containsAssertion;
	private Set<VariableDeclaration> activeVariableDeclarations;
	private VariableDeclarationContainer container;
	private int bodyHashCode;

	public OperationBody(CompilationUnit cu, String filePath, Block methodBody, VariableDeclarationContainer container, List<UMLAttribute> attributes) {
		this.compositeStatement = new CompositeStatementObject(cu, filePath, methodBody, 0, CodeElementType.BLOCK);
		this.container = container;
		this.bodyHashCode = stringify(methodBody).hashCode();
		this.activeVariableDeclarations = new HashSet<VariableDeclaration>();
		for(UMLAttribute attribute : attributes) {
			activeVariableDeclarations.add(attribute.getVariableDeclaration());
		}
		this.activeVariableDeclarations.addAll(container != null ? container.getParameterDeclarationList() : Collections.emptyList());
		if(container.isDeclaredInAnonymousClass()) {
			UMLAnonymousClass anonymousClassContainer = container.getAnonymousClassContainer().get();
			for(VariableDeclarationContainer parentContainer : anonymousClassContainer.getParentContainers()) {
				for(VariableDeclaration parameterDeclaration : parentContainer.getParameterDeclarationList()) {
					if(parameterDeclaration.isFinal()) {
						this.activeVariableDeclarations.add(parameterDeclaration);
					}
				}
			}
		}
		List<Statement> statements = methodBody.statements();
		for(Statement statement : statements) {
			processStatement(cu, filePath, compositeStatement, statement);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	public double builderStatementRatio() {
		List<AbstractStatement> fragments = compositeStatement.getStatements();
		int builderCount = 0;
		for(AbstractCodeFragment fragment : fragments) {
			AbstractCall invocation = fragment.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = fragment.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation instanceof OperationInvocation) {
				OperationInvocation inv = (OperationInvocation)invocation;
				if(inv.numberOfSubExpressions() > 3) {
					builderCount++;
				}
			}
		}
		//fragments.size() == 1 corresponds to a single-statement method
		if(fragments.size() > 1)
			return (double)builderCount/(double)fragments.size();
		return 0;
	}

	public int statementCount() {
		return compositeStatement.statementCount();
	}

	public CompositeStatementObject getCompositeStatement() {
		return compositeStatement;
	}

	public boolean containsAssertion() {
		return containsAssertion;
	}

	public List<CompositeStatementObject> getSynchronizedStatements() {
		List<CompositeStatementObject> synchronizedStatements = new ArrayList<CompositeStatementObject>();
		for(CompositeStatementObject innerNode : compositeStatement.getInnerNodes()) {
			if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT)) {
				synchronizedStatements.add(innerNode);
			}
		}
		return synchronizedStatements;
	}

	public List<AnonymousClassDeclarationObject> getAllAnonymousClassDeclarations() {
		return new ArrayList<AnonymousClassDeclarationObject>(compositeStatement.getAllAnonymousClassDeclarations());
	}

	public List<AbstractCall> getAllOperationInvocations() {
		return new ArrayList<AbstractCall>(compositeStatement.getAllMethodInvocations());
	}

	public List<AbstractCall> getAllCreations() {
		return new ArrayList<AbstractCall>(compositeStatement.getAllCreations());
	}

	public List<LambdaExpressionObject> getAllLambdas() {
		return new ArrayList<LambdaExpressionObject>(compositeStatement.getAllLambdas());
	}

	public List<String> getAllVariables() {
		return new ArrayList<String>(compositeStatement.getAllVariables());
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		return new ArrayList<VariableDeclaration>(compositeStatement.getAllVariableDeclarations());
	}

	public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
		return new ArrayList<VariableDeclaration>(compositeStatement.getVariableDeclarationsInScope(location));
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		return compositeStatement.getVariableDeclaration(variableName);
	}

	private void processStatement(CompilationUnit cu, String filePath, CompositeStatementObject parent, Statement statement) {
		if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, block, parent.getDepth()+1, CodeElementType.BLOCK);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			for(Statement blockStatement : blockStatements) {
				processStatement(cu, filePath, child, blockStatement);
			}
		}
		else if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, ifStatement, parent.getDepth()+1, CodeElementType.IF_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, ifStatement.getExpression(), CodeElementType.IF_STATEMENT_CONDITION, container);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, ifStatement.getThenStatement());
			if(ifStatement.getElseStatement() != null) {
				processStatement(cu, filePath, child, ifStatement.getElseStatement());
			}
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, forStatement, parent.getDepth()+1, CodeElementType.FOR_STATEMENT);
			parent.addStatement(child);
			List<Expression> initializers = forStatement.initializers();
			for(Expression initializer : initializers) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, initializer, CodeElementType.FOR_STATEMENT_INITIALIZER, container);
				child.addExpression(abstractExpression);
			}
			Expression expression = forStatement.getExpression();
			if(expression != null) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, expression, CodeElementType.FOR_STATEMENT_CONDITION, container);
				child.addExpression(abstractExpression);
			}
			List<Expression> updaters = forStatement.updaters();
			for(Expression updater : updaters) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, updater, CodeElementType.FOR_STATEMENT_UPDATER, container);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			this.activeVariableDeclarations.addAll(variableDeclarations);
			processStatement(cu, filePath, child, forStatement.getBody());
			this.activeVariableDeclarations.removeAll(variableDeclarations);
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, enhancedForStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT);
			parent.addStatement(child);
			SingleVariableDeclaration variableDeclaration = enhancedForStatement.getParameter();
			VariableDeclaration vd = new VariableDeclaration(cu, filePath, variableDeclaration, container);
			child.addVariableDeclaration(vd);
			AbstractExpression variableDeclarationName = new AbstractExpression(cu, filePath, variableDeclaration.getName(), CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME, container);
			child.addExpression(variableDeclarationName);
			if(variableDeclaration.getInitializer() != null) {
				AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container);
				child.addExpression(variableDeclarationInitializer);
			}
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, enhancedForStatement.getExpression(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION, container);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			this.activeVariableDeclarations.addAll(variableDeclarations);
			processStatement(cu, filePath, child, enhancedForStatement.getBody());
			this.activeVariableDeclarations.removeAll(variableDeclarations);
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, whileStatement.getExpression(), CodeElementType.WHILE_STATEMENT_CONDITION, container);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, whileStatement.getBody());
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, doStatement, parent.getDepth()+1, CodeElementType.DO_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, doStatement.getExpression(), CodeElementType.DO_STATEMENT_CONDITION, container);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, doStatement.getBody());
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, expressionStatement, parent.getDepth()+1, CodeElementType.EXPRESSION_STATEMENT, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, switchStatement, parent.getDepth()+1, CodeElementType.SWITCH_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, switchStatement.getExpression(), CodeElementType.SWITCH_STATEMENT_CONDITION, container);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			List<Statement> switchStatements = switchStatement.statements();
			for(Statement switchStatement2 : switchStatements)
				processStatement(cu, filePath, child, switchStatement2);
		}
		else if(statement instanceof SwitchCase) {
			SwitchCase switchCase = (SwitchCase)statement;
			StatementObject child = new StatementObject(cu, filePath, switchCase, parent.getDepth()+1, CodeElementType.SWITCH_CASE, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof AssertStatement) {
			AssertStatement assertStatement = (AssertStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, assertStatement, parent.getDepth()+1, CodeElementType.ASSERT_STATEMENT, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement)statement;
			SimpleName label = labeledStatement.getLabel();
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, labeledStatement, parent.getDepth()+1, CodeElementType.LABELED_STATEMENT.setName(label.getIdentifier()));
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, labeledStatement.getBody());
		}
		else if(statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, returnStatement, parent.getDepth()+1, CodeElementType.RETURN_STATEMENT, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, synchronizedStatement, parent.getDepth()+1, CodeElementType.SYNCHRONIZED_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, synchronizedStatement.getExpression(), CodeElementType.SYNCHRONIZED_STATEMENT_EXPRESSION, container);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, synchronizedStatement.getBody());
		}
		else if(statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, throwStatement, parent.getDepth()+1, CodeElementType.THROW_STATEMENT, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)statement;
			TryStatementObject child = new TryStatementObject(cu, filePath, tryStatement, parent.getDepth()+1);
			parent.addStatement(child);
			List<Expression> resources = tryStatement.resources();
			for(Expression resource : resources) {
				AbstractExpression expression = new AbstractExpression(cu, filePath, resource, CodeElementType.TRY_STATEMENT_RESOURCE, container);
				child.addExpression(expression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			this.activeVariableDeclarations.addAll(variableDeclarations);
			List<Statement> tryStatements = tryStatement.getBody().statements();
			for(Statement blockStatement : tryStatements) {
				processStatement(cu, filePath, child, blockStatement);
			}
			this.activeVariableDeclarations.removeAll(variableDeclarations);
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				Block catchClauseBody = catchClause.getBody();
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(cu, filePath, catchClause, parent.getDepth()+1, CodeElementType.CATCH_CLAUSE);
				child.addCatchClause(catchClauseStatementObject);
				parent.addStatement(catchClauseStatementObject);
				catchClauseStatementObject.setTryContainer(child);
				SingleVariableDeclaration variableDeclaration = catchClause.getException();
				VariableDeclaration vd = new VariableDeclaration(cu, filePath, variableDeclaration, container);
				catchClauseStatementObject.addVariableDeclaration(vd);
				AbstractExpression variableDeclarationName = new AbstractExpression(cu, filePath, variableDeclaration.getName(), CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME, container);
				catchClauseStatementObject.addExpression(variableDeclarationName);
				if(variableDeclaration.getInitializer() != null) {
					AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container);
					catchClauseStatementObject.addExpression(variableDeclarationInitializer);
				}
				addStatementInVariableScopes(catchClauseStatementObject);
				List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
				this.activeVariableDeclarations.addAll(catchClauseVariableDeclarations);
				List<Statement> blockStatements = catchClauseBody.statements();
				for(Statement blockStatement : blockStatements) {
					processStatement(cu, filePath, catchClauseStatementObject, blockStatement);
				}
				this.activeVariableDeclarations.removeAll(catchClauseVariableDeclarations);
			}
			Block finallyBlock = tryStatement.getFinally();
			if(finallyBlock != null) {
				CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(cu, filePath, finallyBlock, parent.getDepth()+1, CodeElementType.FINALLY_BLOCK);
				child.setFinallyClause(finallyClauseStatementObject);
				parent.addStatement(finallyClauseStatementObject);
				finallyClauseStatementObject.setTryContainer(child);
				addStatementInVariableScopes(finallyClauseStatementObject);
				List<Statement> blockStatements = finallyBlock.statements();
				for(Statement blockStatement : blockStatements) {
					processStatement(cu, filePath, finallyClauseStatementObject, blockStatement);
				}
			}
		}
		else if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, variableDeclarationStatement, parent.getDepth()+1, CodeElementType.VARIABLE_DECLARATION_STATEMENT, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			this.activeVariableDeclarations.addAll(child.getVariableDeclarations());
		}
		else if(statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation)statement;
			StatementObject child = new StatementObject(cu, filePath, constructorInvocation, parent.getDepth()+1, CodeElementType.CONSTRUCTOR_INVOCATION, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation)statement;
			StatementObject child = new StatementObject(cu, filePath, superConstructorInvocation, parent.getDepth()+1, CodeElementType.SUPER_CONSTRUCTOR_INVOCATION, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, breakStatement, parent.getDepth()+1, CodeElementType.BREAK_STATEMENT, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, continueStatement, parent.getDepth()+1, CodeElementType.CONTINUE_STATEMENT, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof EmptyStatement) {
			EmptyStatement emptyStatement = (EmptyStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, emptyStatement, parent.getDepth()+1, CodeElementType.EMPTY_STATEMENT, container);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
	}

	private void addStatementInVariableScopes(AbstractStatement statement) {
		for(VariableDeclaration variableDeclaration : activeVariableDeclarations) {
			variableDeclaration.addStatementInScope(statement);
			if(container != null) {
				for(AnonymousClassDeclarationObject anonymous : statement.getAnonymousClassDeclarations()) {
					UMLAnonymousClass anonymousClass = container.findAnonymousClass(anonymous);
					for(UMLOperation operation : anonymousClass.getOperations()) {
						if(operation.getBody() != null) {
							CompositeStatementObject composite = operation.getBody().getCompositeStatement();
							for(AbstractStatement anonymousStatement : composite.getInnerNodes()) {
								variableDeclaration.addStatementInScope(anonymousStatement);
							}
							for(AbstractCodeFragment anonymousStatement : composite.getLeaves()) {
								variableDeclaration.addStatementInScope(anonymousStatement);
							}
						}
					}
				}
				for(LambdaExpressionObject lambda : statement.getLambdas()) {
					OperationBody lambdaBody = lambda.getBody();
					if(lambdaBody != null) {
						CompositeStatementObject composite = lambdaBody.getCompositeStatement();
						for(AbstractStatement lambdaStatement : composite.getInnerNodes()) {
							variableDeclaration.addStatementInScope(lambdaStatement);
						}
						for(AbstractCodeFragment lambdaStatement : composite.getLeaves()) {
							variableDeclaration.addStatementInScope(lambdaStatement);
						}
					}
					AbstractExpression lambdaExpression = lambda.getExpression();
					if(lambdaExpression != null) {
						variableDeclaration.addStatementInScope(lambdaExpression);
					}
				}
			}
		}
	}

	public Map<String, Set<String>> aliasedVariables() {
		return compositeStatement.aliasedVariables();
	}

	public Map<String, Set<String>> aliasedAttributes() {
		return compositeStatement.aliasedAttributes();
	}

	public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
		return compositeStatement.loopWithVariables(currentElementName, collectionName);
	}

	public int getBodyHashCode() {
		return bodyHashCode;
	}

	public List<String> stringRepresentation() {
		if(stringRepresentation == null) {
			stringRepresentation = compositeStatement.stringRepresentation();
		}
		return stringRepresentation;
	}

	@JsonCreator
	public OperationBody(
			@JsonProperty("compositeStatement")CompositeStatementObject compositeStatement,
			@JsonProperty("container")int container,
			@JsonProperty("stringified")String stringified
	) {
		this.compositeStatement = compositeStatement;
		this.container = VariableDeclarationContainer.get(container);
		this.bodyHashCode = stringified.hashCode();
	}
}
