package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.diff.CodeRange;
import static gr.uom.java.xmi.decomposition.Visitor.stringify;

public class StatementObject extends AbstractStatement {
	
	private String statement;
	private LocationInfo locationInfo;
	private List<LeafExpression> variables;
	private List<String> types;
	private List<VariableDeclaration> variableDeclarations;
	private List<AbstractCall> methodInvocations;
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
	private List<LeafExpression> stringLiterals;
	private List<LeafExpression> charLiterals;
	private List<LeafExpression> numberLiterals;
	private List<LeafExpression> nullLiterals;
	private List<LeafExpression> booleanLiterals;
	private List<LeafExpression> typeLiterals;
	private List<AbstractCall> creations;
	private List<LeafExpression> infixExpressions;
	private List<String> infixOperators;
	private List<LeafExpression> arrayAccesses;
	private List<LeafExpression> prefixExpressions;
	private List<LeafExpression> postfixExpressions;
	private List<LeafExpression> thisExpressions;
	private List<LeafExpression> arguments;
	private List<LeafExpression> parenthesizedExpressions;
	private List<LeafExpression> castExpressions;
	private List<TernaryOperatorExpression> ternaryOperatorExpressions;
	private List<LambdaExpressionObject> lambdas;
	
	public StatementObject(CompilationUnit cu, String filePath, Statement statement, int depth, CodeElementType codeElementType, VariableDeclarationContainer container) {
		super();
		this.locationInfo = new LocationInfo(cu, filePath, statement, codeElementType);
		Visitor visitor = new Visitor(cu, filePath, container);
		statement.accept(visitor);
		this.variables = visitor.getVariables();
		this.types = visitor.getTypes();
		this.variableDeclarations = visitor.getVariableDeclarations();
		this.methodInvocations = visitor.getMethodInvocations();
		this.anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
		this.stringLiterals = visitor.getStringLiterals();
		this.charLiterals = visitor.getCharLiterals();
		this.numberLiterals = visitor.getNumberLiterals();
		this.nullLiterals = visitor.getNullLiterals();
		this.booleanLiterals = visitor.getBooleanLiterals();
		this.typeLiterals = visitor.getTypeLiterals();
		this.creations = visitor.getCreations();
		this.infixExpressions = visitor.getInfixExpressions();
		this.infixOperators = visitor.getInfixOperators();
		this.arrayAccesses = visitor.getArrayAccesses();
		this.prefixExpressions = visitor.getPrefixExpressions();
		this.postfixExpressions = visitor.getPostfixExpressions();
		this.thisExpressions = visitor.getThisExpressions();
		this.arguments = visitor.getArguments();
		this.parenthesizedExpressions = visitor.getParenthesizedExpressions();
		this.castExpressions = visitor.getCastExpressions();
		this.ternaryOperatorExpressions = visitor.getTernaryOperatorExpressions();
		this.lambdas = visitor.getLambdas();
		setDepth(depth);
		String statementAsString = stringify(statement);
		if(Visitor.METHOD_INVOCATION_PATTERN.matcher(statementAsString).matches()) {
			if(statement instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
				StringBuilder sb = new StringBuilder();
				List<IExtendedModifier> modifiers = variableDeclarationStatement.modifiers();
				for(IExtendedModifier modifier : modifiers) {
					sb.append(modifier.toString()).append(" ");
				}
				sb.append(stringify(variableDeclarationStatement.getType()));
				List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
				for(VariableDeclarationFragment fragment : fragments) {
					sb.append(fragment.getName().getIdentifier());
					Expression initializer = fragment.getInitializer();
					if(initializer != null) {
						sb.append(" = ");
						if(initializer instanceof MethodInvocation) {
							MethodInvocation methodInvocation = (MethodInvocation)initializer;
							sb.append(Visitor.processMethodInvocation(methodInvocation));
						}
						else if(initializer instanceof ClassInstanceCreation) {
							ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)initializer;
							sb.append(Visitor.processClassInstanceCreation(classInstanceCreation));
						}
					}
				}
				this.statement = sb.toString();
			}
			else if(statement instanceof ReturnStatement) {
				ReturnStatement returnStatement = (ReturnStatement)statement;
				StringBuilder sb = new StringBuilder();
				sb.append("return").append(" ");
				Expression expression = returnStatement.getExpression();
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					sb.append(Visitor.processMethodInvocation(methodInvocation));
				}
				else if(expression instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
					sb.append(Visitor.processClassInstanceCreation(classInstanceCreation));
				}
				this.statement = sb.toString();
			}
			else if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				StringBuilder sb = new StringBuilder();
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					sb.append(Visitor.processMethodInvocation(methodInvocation));
				}
				else if(expression instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
					sb.append(Visitor.processClassInstanceCreation(classInstanceCreation));
				}
				this.statement = sb.toString();
			}
			else {
				this.statement = statementAsString;
			}
		}
		else {
			this.statement = statementAsString;
		}
	}

	@Override
	public List<AbstractCodeFragment> getLeaves() {
		List<AbstractCodeFragment> leaves = new ArrayList<AbstractCodeFragment>();
		leaves.add(this);
		return leaves;
	}

	public String toString() {
		return statement;
	}

	@Override
	public List<LeafExpression> getVariables() {
		return variables;
	}

	@Override
	public List<String> getTypes() {
		return types;
	}

	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	@Override
	public List<AbstractCall> getMethodInvocations() {
		return methodInvocations;
	}

	@Override
	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	@Override
	public List<LeafExpression> getStringLiterals() {
		return stringLiterals;
	}

	@Override
	public List<LeafExpression> getCharLiterals() {
		return charLiterals;
	}

	@Override
	public List<LeafExpression> getNumberLiterals() {
		return numberLiterals;
	}

	@Override
	public List<LeafExpression> getNullLiterals() {
		return nullLiterals;
	}

	@Override
	public List<LeafExpression> getBooleanLiterals() {
		return booleanLiterals;
	}

	@Override
	public List<LeafExpression> getTypeLiterals() {
		return typeLiterals;
	}

	@Override
	public List<AbstractCall> getCreations() {
		return creations;
	}

	@Override
	public List<LeafExpression> getInfixExpressions() {
		return infixExpressions;
	}

	@Override
	public List<String> getInfixOperators() {
		return infixOperators;
	}

	@Override
	public List<LeafExpression> getArrayAccesses() {
		return arrayAccesses;
	}

	@Override
	public List<LeafExpression> getPrefixExpressions() {
		return prefixExpressions;
	}

	@Override
	public List<LeafExpression> getPostfixExpressions() {
		return postfixExpressions;
	}

	@Override
	public List<LeafExpression> getThisExpressions() {
		return thisExpressions;
	}

	@Override
	public List<LeafExpression> getArguments() {
		return arguments;
	}

	@Override
	public List<LeafExpression> getParenthesizedExpressions() {
		return parenthesizedExpressions;
	}

	@Override
	public List<LeafExpression> getCastExpressions() {
		return castExpressions;
	}

	@Override
	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	@Override
	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	@Override
	public int statementCount() {
		return 1;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return declaration;
			}
		}
		for(LambdaExpressionObject lambda : getLambdas()) {
			for(VariableDeclaration declaration : lambda.getParameters()) {
				if(declaration.getVariableName().equals(variableName)) {
					return declaration;
				}
			}
		}
		return null;
	}

	@Override
	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.add(this.toString());
		return stringRepresentation;
	}

	@JsonCreator
	public StatementObject(
			@JsonProperty("id")int id,
			@JsonProperty("depth")int depth,
			@JsonProperty("index")int index,
			@JsonProperty("parent")int parent,
			@JsonProperty("statement")String statement,
			@JsonProperty("locationInfo")LocationInfo locationInfo,
			@JsonProperty("variables")List<LeafExpression> variables,
			@JsonProperty("types")List<String> types,
			@JsonProperty("variableDeclarations")List<VariableDeclaration> variableDeclarations,
			@JsonProperty("methodInvocations")List<AbstractCall> methodInvocations,
			@JsonProperty("stringLiterals")List<LeafExpression> stringLiterals,
			@JsonProperty("charLiterals")List<LeafExpression> charLiterals,
			@JsonProperty("numberLiterals")List<LeafExpression> numberLiterals,
			@JsonProperty("nullLiterals")List<LeafExpression> nullLiterals,
			@JsonProperty("booleanLiterals")List<LeafExpression> booleanLiterals,
			@JsonProperty("typeLiterals")List<LeafExpression> typeLiterals,
			@JsonProperty("creations")List<AbstractCall> creations,
			@JsonProperty("infixExpressions")List<LeafExpression> infixExpressions,
			@JsonProperty("infixOperators")List<String> infixOperators,
			@JsonProperty("arrayAccesses")List<LeafExpression> arrayAccesses,
			@JsonProperty("prefixExpressions")List<LeafExpression> prefixExpressions,
			@JsonProperty("postfixExpressions")List<LeafExpression> postfixExpressions,
			@JsonProperty("thisExpressions")List<LeafExpression> thisExpressions,
			@JsonProperty("arguments")List<LeafExpression> arguments,
			@JsonProperty("parenthesizedExpressions")List<LeafExpression> parenthesizedExpressions,
			@JsonProperty("castExpressions")List<LeafExpression> castExpressions,
			@JsonProperty("ternaryOperatorExpressions")List<TernaryOperatorExpression> ternaryOperatorExpressions
	) {
		super(id, depth, index, parent);

		this.statement = statement;
		this.locationInfo = locationInfo;
		this.variables = variables;
		this.types = types;
		this.variableDeclarations = variableDeclarations;
		this.methodInvocations = methodInvocations;
		this.stringLiterals = stringLiterals;
		this.charLiterals = charLiterals;
		this.numberLiterals = numberLiterals;
		this.nullLiterals = nullLiterals;
		this.booleanLiterals = booleanLiterals;
		this.typeLiterals = typeLiterals;
		this.creations = creations;
		this.infixExpressions = infixExpressions;
		this.infixOperators = infixOperators;
		this.arrayAccesses = arrayAccesses;
		this.prefixExpressions = prefixExpressions;
		this.postfixExpressions = postfixExpressions;
		this.thisExpressions = thisExpressions;
		this.arguments = arguments;
		this.parenthesizedExpressions = parenthesizedExpressions;
		this.castExpressions = castExpressions;
		this.ternaryOperatorExpressions = ternaryOperatorExpressions;
		this.lambdas = new ArrayList<>();

		this.anonymousClassDeclarations = new ArrayList<>();
	}
}