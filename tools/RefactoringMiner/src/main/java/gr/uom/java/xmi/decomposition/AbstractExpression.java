package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.decomposition.Visitor.stringify;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class AbstractExpression extends AbstractCodeFragment {
	
	private String expression;
	private LocationInfo locationInfo;
	private CompositeStatementObject owner;
	private LambdaExpressionObject lambdaOwner;
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
    
    public AbstractExpression(CompilationUnit cu, String filePath, Expression expression, CodeElementType codeElementType, VariableDeclarationContainer container) {
    	this.locationInfo = new LocationInfo(cu, filePath, expression, codeElementType);
    	Visitor visitor = new Visitor(cu, filePath, container);
    	expression.accept(visitor);
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
		this.expression = stringify(expression);
    	this.owner = null;
    	this.lambdaOwner = null;
    }

    public void setOwner(CompositeStatementObject owner) {
    	this.owner = owner;
    }

    public CompositeStatementObject getOwner() {
    	return this.owner;
    }

	public LambdaExpressionObject getLambdaOwner() {
		return lambdaOwner;
	}

	public void setLambdaOwner(LambdaExpressionObject lambdaOwner) {
		this.lambdaOwner = lambdaOwner;
	}

	@Override
	public CompositeStatementObject getParent() {
		return getOwner();
	}

    public String getExpression() {
    	return expression;
    }

	public String getString() {
    	return toString();
    }
  
	public String toString() {
		return getExpression().toString();
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

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public VariableDeclaration searchVariableDeclaration(String variableName) {
		VariableDeclaration variableDeclaration = this.getVariableDeclaration(variableName);
		if(variableDeclaration != null) {
			return variableDeclaration;
		}
		else if(owner != null) {
			return owner.searchVariableDeclaration(variableName);
		}
		else if(lambdaOwner != null) {
			for(VariableDeclaration declaration : lambdaOwner.getParameters()) {
				if(declaration.getVariableName().equals(variableName)) {
					return declaration;
				}
			}
		}
		return null;
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return declaration;
			}
		}
		return null;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	@JsonCreator
	public AbstractExpression(
			@JsonProperty("id")int id,
			@JsonProperty("depth")int depth,
			@JsonProperty("index")int index,
			@JsonProperty("expression")String expression,
			@JsonProperty("locationInfo")LocationInfo locationInfo,
			@JsonProperty("owner")int owner,
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
		super(id, depth, index);

		this.expression = expression;
		this.locationInfo = locationInfo;
		this.owner = (CompositeStatementObject)AbstractCodeFragment.get(owner);
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

		this.lambdaOwner = null;
		this.lambdas = new ArrayList<>();
		this.anonymousClassDeclarations = new ArrayList<>();
	}
}
