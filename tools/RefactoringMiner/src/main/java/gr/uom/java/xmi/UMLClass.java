package gr.uom.java.xmi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gr.uom.java.xmi.diff.StringDistance;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@JsonDeserialize(using = UMLClassDeserializer.class)
public class UMLClass extends UMLAbstractClass implements Comparable<UMLClass>, Serializable, LocationInfoProvider {
	private String qualifiedName;
    private String sourceFile;
    private String sourceFolder;
    private Visibility visibility;
    private boolean isFinal;
    private boolean isStatic;
	private boolean isAbstract;
	private boolean isInterface;
	private boolean isEnum;
	private boolean isAnnotation;
	private boolean isRecord;
	private boolean topLevel;
    private List<UMLTypeParameter> typeParameters;
    private UMLJavadoc javadoc;
    private UMLJavadoc packageDeclarationJavadoc;
    private List<UMLComment> packageDeclarationComments;
    
    public UMLClass(String packageName, String name, LocationInfo locationInfo, boolean topLevel, List<UMLImport> importedTypes) {
    	super(packageName, name, locationInfo, importedTypes);
        if(packageName.equals(""))
        	this.qualifiedName = name;
    	else
    		this.qualifiedName = packageName + "." + name;
        
        this.sourceFile = getSourceFile();
        this.sourceFolder = "";
        if(packageName.equals("")) {
        	int index = sourceFile.indexOf(name);
        	if(index != -1) {
    			this.sourceFolder = sourceFile.substring(0, index);
    		}
        }
        else {
        	if(topLevel) {
        		int index = sourceFile.indexOf(packageName.replace('.', '/'));
        		if(index != -1) {
        			this.sourceFolder = sourceFile.substring(0, index);
        		}
        	}
        	else {
        		int index = -1;
        		if(packageName.contains(".")) {
        			String realPackageName = packageName.substring(0, packageName.lastIndexOf('.'));
        			index = sourceFile.indexOf(realPackageName.replace('.', '/'));
        		}
        		else {
        			index = sourceFile.indexOf(packageName);
        		}
        		if(index != -1) {
        			this.sourceFolder = sourceFile.substring(0, index);
        		}
        	}
        }
        this.isAbstract = false;
        this.isInterface = false;
        this.topLevel = topLevel;
        this.typeParameters = new ArrayList<UMLTypeParameter>();
        this.packageDeclarationComments = new ArrayList<UMLComment>();
    }

    public String getTypeDeclarationKind() {
    	if(isInterface)
    		return "interface";
    	else if(isEnum)
    		return "enum";
    	else if(isAnnotation)
    		return "annotation";
    	else if(isRecord)
    		return "record";
    	else
    		return "class";
    }

    public List<UMLTypeParameter> getTypeParameters() {
		return typeParameters;
	}

    public List<String> getTypeParameterNames() {
    	List<String> typeParameterNames = new ArrayList<String>();
		for(UMLTypeParameter typeParameter : typeParameters) {
			typeParameterNames.add(typeParameter.getName());
		}
		return typeParameterNames;
	}

	public void addTypeParameter(UMLTypeParameter typeParameter) {
    	typeParameters.add(typeParameter);
    }

    public String getName() {
    	return this.qualifiedName;
    }

    public boolean isTopLevel() {
		return topLevel;
	}

	public void setTopLevel(boolean topLevel) {
		this.topLevel = topLevel;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public boolean isEnum() {
		return isEnum;
	}

	public void setEnum(boolean isEnum) {
		this.isEnum = isEnum;
	}

	public boolean isAnnotation() {
		return isAnnotation;
	}

	public void setAnnotation(boolean isAnnotation) {
		this.isAnnotation = isAnnotation;
	}

	public boolean isRecord() {
		return isRecord;
	}

	public void setRecord(boolean isRecord) {
		this.isRecord = isRecord;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public UMLJavadoc getJavadoc() {
		return javadoc;
	}

	public void setJavadoc(UMLJavadoc javadoc) {
		this.javadoc = javadoc;
	}

    public UMLJavadoc getPackageDeclarationJavadoc() {
		return packageDeclarationJavadoc;
	}

	public void setPackageDeclarationJavadoc(UMLJavadoc packageJavadoc) {
		this.packageDeclarationJavadoc = packageJavadoc;
	}

	public List<UMLComment> getPackageDeclarationComments() {
		return packageDeclarationComments;
	}

    public UMLOperation matchOperation(UMLOperation otherOperation) {
    	ListIterator<UMLOperation> operationIt = operations.listIterator();
    	while(operationIt.hasNext()) {
    		UMLOperation operation = operationIt.next();
    		if(operation.getName().equals(otherOperation.getName())) {
    			if(operation.getParameters().size() == otherOperation.getParameters().size()) {
    				boolean match = true;
    				int i = 0;
    				for(UMLParameter parameter : operation.getParameters()) {
    					UMLParameter otherParameter = otherOperation.getParameters().get(i);
    					String thisParameterType = parameter.getType().getClassType();
    					String otherParameterType = otherParameter.getType().getClassType();
    					int thisArrayDimension = parameter.getType().getArrayDimension();
    					int otherArrayDimension = otherParameter.getType().getArrayDimension();
    					String thisParameterTypeComparedString = null;
    	    			if(thisParameterType.contains("."))
    	    				thisParameterTypeComparedString = thisParameterType.substring(thisParameterType.lastIndexOf(".")+1);
    	    			else
    	    				thisParameterTypeComparedString = thisParameterType;
    	    			String otherParameterTypeComparedString = null;
    	    			if(otherParameterType.contains("."))
    	    				otherParameterTypeComparedString = otherParameterType.substring(otherParameterType.lastIndexOf(".")+1);
    	    			else
    	    				otherParameterTypeComparedString = otherParameterType;
    	    			if(!thisParameterTypeComparedString.equals(otherParameterTypeComparedString) || thisArrayDimension != otherArrayDimension) {
    						match = false;
    						break;
    					}
    					i++;
    				}
    				if(match)
    					return operation;
    			}
    		}
    	}
    	return null;
    }

    public boolean hasSameNameAndKind(UMLClass umlClass) {
    	if(!this.name.equals(umlClass.name))
    		return false;
    	if(!hasSameKind(umlClass))
    		return false;
    	return true;
    }

    public boolean hasSameKind(UMLClass umlClass) {
    	if(this.isInterface != umlClass.isInterface)
    		return false;
    	if(!equalTypeParameters(umlClass))
    		return false;
    	return true;
    }

	private boolean equalTypeParameters(UMLClass umlClass) {
		return this.typeParameters.equals(umlClass.typeParameters) || this.getTypeParameterNames().equals(umlClass.getTypeParameterNames()) ||
				this.renamedParameterizedType(umlClass);
	}

	private boolean renamedParameterizedType(UMLClass umlClass) {
		for(UMLOperation operation1 : this.operations) {
			List<UMLParameter> parameterized1 = operation1.getParameterizedTypesInSignature();
			if(!parameterized1.isEmpty()) {
				for(UMLOperation operation2 : umlClass.operations) {
					if(operation1.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation2)) {
						List<UMLParameter> parameterized2 = operation2.getParameterizedTypesInSignature();
						if(parameterized1.size() == parameterized2.size()) {
							int renamed = 0;
							for(int i=0; i<parameterized1.size(); i++) {
								UMLType type1 = parameterized1.get(i).getType();
								UMLType type2 = parameterized2.get(i).getType();
								if(type1.getTypeArguments().toString().equals(this.typeParameters.toString()) &&
										type2.getTypeArguments().toString().equals(umlClass.typeParameters.toString())) {
									renamed++;
								}
							}
							if(renamed == parameterized1.size()) {
								return true;
							}
						}
						break;
					}
				}
			}
		}
		return false;
	}

    public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLClass) {
    		UMLClass umlClass = (UMLClass)o;
    		return this.packageName.equals(umlClass.packageName) && this.name.equals(umlClass.name) && this.sourceFile.equals(umlClass.sourceFile);
    	}
    	return false;
    }

    public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		return result;
	}

    public String toString() {
    	return getName();
    }

	public int compareTo(UMLClass umlClass) {
		return this.toString().compareTo(umlClass.toString());
	}

	public double normalizedNameDistance(UMLClass c) {
		String s1 = name.toLowerCase();
		String s2 = c.name.toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public double normalizedPackageNameDistance(UMLClass c) {
		String s1 = packageName.toLowerCase();
		String s2 = c.packageName.toLowerCase();
		if(s1.length() == 0 && s2.length() == 0) {
			return 0;
		}
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}
	
	public double normalizedSourceFolderDistance(UMLClass c) {
		String s1 = sourceFolder.toLowerCase();
		String s2 = c.sourceFolder.toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean isSingleAbstractMethodInterface() {
		return isInterface && operations.size() == 1;
	}

	public boolean isSingleMethodClass() {
		if(!isInterface && !isEnum && !isAnnotation && !isRecord) {
			int counter = 0;
			for(UMLOperation operation : operations) {
				if(!operation.isConstructor()) {
					counter++;
				}
			}
			return counter == 1;
		}
		return false;
	}

	//json
	public UMLClass(
			int id,
			LocationInfo locationInfo,
			String packageName,
			String name,
			List<UMLOperation> operations,
			List<UMLAttribute> attributes,
			UMLType superclass,
			List<UMLType> implementedInterfaces,
			List<UMLImport> importedTypes,
			List<UMLModifier> modifiers,
			List<UMLEnumConstant> enumConstants,
			String qualifiedName,
			String sourceFile,
			String sourceFolder,
			boolean isFinal,
			boolean isAbstract,
			boolean isEnum,
			List<UMLTypeParameter> typeParameters
	) {
		super(id, locationInfo, packageName, name, operations, attributes, superclass, implementedInterfaces, importedTypes, modifiers, enumConstants);

		this.qualifiedName = qualifiedName;
		this.sourceFile = sourceFile;
		this.sourceFolder = sourceFolder;
		this.visibility = Visibility.PUBLIC;
		this.isFinal = isFinal;
		this.isAbstract = isAbstract;
		this.isEnum = isEnum;
		this.typeParameters = typeParameters;

		this.packageDeclarationComments = new ArrayList<UMLComment>();
	}
}

class UMLClassDeserializer extends JsonDeserializer<UMLClass> {
	@Override
	public UMLClass deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		ObjectMapper om = new ObjectMapper();
		JsonNode node = ctxt.readTree(p);

		return new UMLClass(
				node.get("id").asInt(),
				om.readValue(node.get("locationInfo").traverse(), LocationInfo.class),
				node.get("packageName").textValue(),
				node.get("name").textValue(),
				om.readValue(node.get("operations").traverse(), ctxt.getTypeFactory().constructCollectionType(List.class, UMLOperation.class)),
				om.readValue(node.get("attributes").traverse(), ctxt.getTypeFactory().constructCollectionType(List.class, UMLAttribute.class)),
				om.readValue(node.get("superclass").traverse(), UMLType.class),
				om.readValue(node.get("implementedInterfaces").traverse(), ctxt.getTypeFactory().constructCollectionType(List.class, UMLType.class)),
				om.readValue(node.get("importedTypes").traverse(), ctxt.getTypeFactory().constructCollectionType(List.class, UMLImport.class)),
				om.readValue(node.get("modifiers").traverse(), ctxt.getTypeFactory().constructCollectionType(List.class, UMLModifier.class)),
				om.readValue(node.get("enumConstants").traverse(), ctxt.getTypeFactory().constructCollectionType(List.class, UMLEnumConstant.class)),
				node.get("qualifiedName").textValue(),
				node.get("sourceFile").textValue(),
				node.get("sourceFolder").textValue(),
				node.get("isFinal").asBoolean(),
				node.get("isAbstract").asBoolean(),
				node.get("isEnum").asBoolean(),
				om.readValue(node.get("typeParameters").traverse(), ctxt.getTypeFactory().constructCollectionType(List.class, UMLTypeParameter.class))
		);
	}
}