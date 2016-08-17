package org.hl7.fhir.dstu3.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.Context;

import org.hl7.fhir.dstu3.elementmodel.Property;
import org.hl7.fhir.dstu3.exceptions.DefinitionException;
import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.dstu3.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.dstu3.model.ConceptMap.TargetElementComponent;
import org.hl7.fhir.dstu3.model.Constants;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.DecimalType;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.ElementDefinition.ElementDefinitionMappingComponent;
import org.hl7.fhir.dstu3.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Enumerations.ConceptMapEquivalence;
import org.hl7.fhir.dstu3.model.ExpressionNode;
import org.hl7.fhir.dstu3.model.ExpressionNode.CollectionStatus;
import org.hl7.fhir.dstu3.model.ExpressionNode.TypeDetails;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceFactory;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionContactComponent;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionMappingComponent;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionSnapshotComponent;
import org.hl7.fhir.dstu3.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.dstu3.model.StructureMap;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpander.ValueSetExpansionOutcome;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapContactComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapGroupComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapGroupInputComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapGroupRuleComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapGroupRuleDependentComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapGroupRuleSourceComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapGroupRuleTargetComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapGroupRuleTargetParameterComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapInputMode;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapListMode;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapModelMode;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapStructureComponent;
import org.hl7.fhir.dstu3.model.StructureMap.StructureMapTransform;
import org.hl7.fhir.dstu3.utils.FHIRLexer.FHIRLexerException;
import org.hl7.fhir.dstu3.validation.ValidationMessage.Source;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

public class StructureMapUtilities {

	public static final String MAP_WHERE_CHECK = "map.where.check";
	public static final String MAP_WHERE_EXPRESSION = "map.where.expression";
	public static final String MAP_EXPRESSION = "map.transform.expression";
  private static final boolean RENDER_MULTIPLE_TARGETS_ONELINE = false;

	public interface ITransformerServices {
		//    public boolean validateByValueSet(Coding code, String valuesetId);
		public Coding translate(Object appInfo, Coding source, String conceptMapUrl) throws FHIRException;
		//    public Coding translate(Coding code)
		//    ValueSet validation operation
		//    Translation operation
		//    Lookup another tree of data
		//    Create an instance tree
		//    Return the correct string format to refer to a tree (input or output)

	}

	private IWorkerContext worker;
	private FluentPathEngine fpe;
	private Map<String, StructureMap> library;
	private ITransformerServices services;

	public StructureMapUtilities(IWorkerContext worker, Map<String, StructureMap> library, ITransformerServices services) {
		super();
		this.worker = worker;
		this.library = library;
		this.services = services;
		fpe = new FluentPathEngine(worker);
	}


	public String render(StructureMap map) throws FHIRException {
		StringBuilder b = new StringBuilder();
		b.append("map \"");
		b.append(map.getUrl());
		b.append("\" = \"");
		b.append(Utilities.escapeJava(map.getName()));
		b.append("\"\r\n\r\n");

		renderUses(b, map);
		renderImports(b, map);
		for (StructureMapGroupComponent g : map.getGroup())
			renderGroup(b, g);
		return b.toString();
	}

	private void renderUses(StringBuilder b, StructureMap map) {
		for (StructureMapStructureComponent s : map.getStructure()) {
			b.append("uses \"");
			b.append(s.getUrl());
			b.append("\" as ");
			b.append(s.getMode().toCode());
			b.append("\r\n");
			renderDoco(b, s.getDocumentation());
		}
		if (map.hasStructure())
			b.append("\r\n");
	}

	private void renderImports(StringBuilder b, StructureMap map) {
		for (UriType s : map.getImport()) {
			b.append("imports \"");
			b.append(s.getValue());
			b.append("\"\r\n");
		}
		if (map.hasImport())
			b.append("\r\n");
	}

	private void renderGroup(StringBuilder b, StructureMapGroupComponent g) throws FHIRException {
		b.append("group ");
		b.append(g.getName());
		if (g.hasExtends()) {
			b.append(" extends ");
			b.append(g.getExtends());
		}
		if (g.hasDocumentation()) 
			renderDoco(b, g.getDocumentation());
		b.append("\r\n");
		for (StructureMapGroupInputComponent gi : g.getInput()) {
			b.append("  input ");
			b.append(gi.getName());
			if (gi.hasType()) {
				b.append(" : ");
				b.append(gi.getType());
			}
			b.append(" as ");
			b.append(gi.getMode().toCode());
			b.append(";\r\n");
		}
		if (g.hasInput())
			b.append("\r\n");
		for (StructureMapGroupRuleComponent r : g.getRule()) {
			renderRule(b, r, 2);
		}
		b.append("\r\nendgroup\r\n");
	}

	private void renderRule(StringBuilder b, StructureMapGroupRuleComponent r, int indent) throws FHIRException {
		for (int i = 0; i < indent; i++)
			b.append(' ');
		b.append(r.getName());
		b.append(": for ");
		boolean first = true;
		for (StructureMapGroupRuleSourceComponent rs : r.getSource()) {
			if (first)
				first = false;
			else
				b.append(", ");
			renderSource(b, rs);
		}
		if (r.getTarget().size() > 1) {
			b.append(" make ");
			first = true;
			for (StructureMapGroupRuleTargetComponent rt : r.getTarget()) {
				if (first)
					first = false;
				else
					b.append(", ");
				if (RENDER_MULTIPLE_TARGETS_ONELINE)
	        b.append(' ');
				else {
				  b.append("\r\n");
				  for (int i = 0; i < indent+4; i++)
				    b.append(' ');
				}
				renderTarget(b, rt);
			}
		} else if (r.hasTarget()) { 
			b.append(" make ");
			renderTarget(b, r.getTarget().get(0));
		}
		if (r.hasRule()) {
			b.append(" then {\r\n");
			renderDoco(b, r.getDocumentation());
			for (StructureMapGroupRuleComponent ir : r.getRule()) {
			  renderRule(b, ir, indent+2);
			}
      for (int i = 0; i < indent; i++)
        b.append(' ');
			b.append("}\r\n");
		} else {
			if (r.hasDependent()) {
				first = true;
				for (StructureMapGroupRuleDependentComponent rd : r.getDependent()) {
					if (first)
						first = false;
					else
						b.append(", ");
					b.append(rd.getName());
					b.append("(");
					boolean ifirst = true;
					for (StringType rdp : rd.getVariable()) {
						if (ifirst)
							ifirst = false;
						else
							b.append(", ");
						b.append(rd.getVariable());
					}
				}
			}
			renderDoco(b, r.getDocumentation());
			b.append("\r\n");
		}

	}

	private void renderSource(StringBuilder b, StructureMapGroupRuleSourceComponent rs) {
		if (!rs.getRequired())
			b.append("optional ");
		b.append(rs.getContext());
		if (rs.hasElement()) {
			b.append('.');
			b.append(rs.getElement());
		}
		if (rs.hasListMode()) {
			b.append(" ");
			if (rs.getListMode() == StructureMapListMode.SHARE)
				b.append("only_one");
			else
				b.append(rs.getListMode().toCode());
		}
		if (rs.hasVariable()) {
			b.append(" as ");
			b.append(rs.getVariable());
		}
		if (rs.hasCondition())  {
			b.append(" where ");
			b.append(rs.getCondition());
		}
		if (rs.hasCheck())  {
			b.append(" check ");
			b.append(rs.getCheck());
		}
	}

	private void renderTarget(StringBuilder b, StructureMapGroupRuleTargetComponent rt) throws FHIRException {
	  if (rt.hasContext()) {
	    b.append(rt.getContext());
	    if (rt.hasElement())  {
	      b.append('.');
	      b.append(rt.getElement());
	    }
	  }
		if (rt.hasTransform()) {
	    if (rt.hasContext()) 
  			b.append(" = ");
			if (rt.getTransform() == StructureMapTransform.COPY && rt.getParameter().size() == 1) {
				renderTransformParam(b, rt.getParameter().get(0));
			} else if (rt.getTransform() == StructureMapTransform.EVALUATE && rt.getParameter().size() == 2) {
				b.append(rt.getTransform().toCode());
				b.append("(");
				b.append(((IdType) rt.getParameter().get(0).getValue()).asStringValue());
				b.append(((StringType) rt.getParameter().get(1).getValue()).asStringValue());
				b.append(")");
			} else {
				b.append(rt.getTransform().toCode());
				b.append("(");
				boolean first = true;
				for (StructureMapGroupRuleTargetParameterComponent rtp : rt.getParameter()) {
					if (first)
						first = false;
					else
						b.append(", ");
					renderTransformParam(b, rtp);
				}
				b.append(")");
			}
		}
		if (rt.hasVariable()) {
			b.append(" as ");
			b.append(rt.getVariable());
		}
		for (Enumeration<StructureMapListMode> lm : rt.getListMode()) {
			b.append(" ");
			b.append(lm.getValue().toCode());
			if (lm.getValue() == StructureMapListMode.SHARE) {
				b.append(" ");
				b.append(rt.getListRuleId());
			}
		}
	}

	private void renderTransformParam(StringBuilder b, StructureMapGroupRuleTargetParameterComponent rtp) throws FHIRException {
		if (rtp.hasValueBooleanType())
			b.append(rtp.getValueBooleanType().asStringValue());
		else if (rtp.hasValueDecimalType())
			b.append(rtp.getValueDecimalType().asStringValue());
		else if (rtp.hasValueIdType())
			b.append(rtp.getValueIdType().asStringValue());
		else if (rtp.hasValueDecimalType())
			b.append(rtp.getValueDecimalType().asStringValue());
		else if (rtp.hasValueIntegerType())
			b.append(rtp.getValueIntegerType().asStringValue());
		else 
			b.append(Utilities.escapeJava(rtp.getValueStringType().asStringValue()));
	}

	private void renderDoco(StringBuilder b, String doco) {
		if (Utilities.noString(doco))
			return;
		b.append(" // ");
		b.append(doco.replace("\r\n", " ").replace("\r", " ").replace("\n", " "));
	}

	public StructureMap parse(String text) throws FHIRException {
		FHIRLexer lexer = new FHIRLexer(text);
		if (lexer.done())
			throw lexer.error("Map Input cannot be empty");
		lexer.skipComments();
		lexer.token("map");
		StructureMap result = new StructureMap();
		result.setUrl(lexer.readConstant("url"));
		lexer.token("=");
		result.setName(lexer.readConstant("name"));
		lexer.skipComments();

		while (lexer.hasToken("conceptmap"))
			parseConceptMap(result, lexer);

		while (lexer.hasToken("uses"))
			parseUses(result, lexer);
		while (lexer.hasToken("imports"))
			parseImports(result, lexer);

		parseGroup(result, lexer);

		while (!lexer.done()) {
			parseGroup(result, lexer);    
		}

		return result;
	}

	private void parseConceptMap(StructureMap result, FHIRLexer lexer) throws FHIRLexerException {
		lexer.token("conceptmap");
		ConceptMap map = new ConceptMap();
		String id = lexer.readConstant("map id");
		if (!id.startsWith("#"))
			lexer.error("Concept Map identifier must start with #");
		map.setId(id.substring(1));
		result.getContained().add(map);
		lexer.token("{");
		lexer.skipComments();
		//	  lexer.token("source");
		//	  map.setSource(new UriType(lexer.readConstant("source")));
		//	  lexer.token("target");
		//	  map.setSource(new UriType(lexer.readConstant("target")));
		Map<String, String> prefixes = new HashMap<String, String>();
		while (lexer.hasToken("prefix")) {
			lexer.token("prefix");
			String n = lexer.take();
			lexer.token("=");
			String v = lexer.readConstant("prefix url");
			prefixes.put(n, v);
		}
		while (!lexer.hasToken("}")) {
		  String srcs = readPrefix(prefixes, lexer);
      lexer.token(":");
      String sc = lexer.take();
		  ConceptMapEquivalence eq = readEquivalence(lexer);
		  String tgts = (eq != ConceptMapEquivalence.UNMATCHED) ? readPrefix(prefixes, lexer) : "";
		  ConceptMapGroupComponent g = getGroup(map, srcs, tgts);
			SourceElementComponent e = g.addElement();
			e.setCode(sc);
			TargetElementComponent tgt = e.addTarget();
			if (eq != ConceptMapEquivalence.EQUIVALENT)
			  tgt.setEquivalence(eq);
			if (tgt.getEquivalence() != ConceptMapEquivalence.UNMATCHED) {
				lexer.token(":");
				tgt.setCode(lexer.take());
			}
			if (lexer.hasComment())
				tgt.setComments(lexer.take().substring(2).trim());
		}
		lexer.token("}");
	}


	private ConceptMapGroupComponent getGroup(ConceptMap map, String srcs, String tgts) {
	  for (ConceptMapGroupComponent grp : map.getGroup()) {
	    if (grp.getSource().equals(srcs) && grp.getTarget().equals(tgts))
	      return grp;
	  }
	  ConceptMapGroupComponent grp = map.addGroup(); 
    grp.setSource(srcs);
    grp.setTarget(tgts);
    return grp;
  }


  private String readPrefix(Map<String, String> prefixes, FHIRLexer lexer) throws FHIRLexerException {
		String prefix = lexer.take();
		if (!prefixes.containsKey(prefix))
			throw lexer.error("Unknown prefix '"+prefix+"'");
		return prefixes.get(prefix);
	}


	private ConceptMapEquivalence readEquivalence(FHIRLexer lexer) throws FHIRLexerException {
		String token = lexer.take();
		if (token.equals("="))
			return ConceptMapEquivalence.EQUAL;
		if (token.equals("=="))
			return ConceptMapEquivalence.EQUIVALENT;
		if (token.equals("!="))
			return ConceptMapEquivalence.DISJOINT;
		if (token.equals("--"))
			return ConceptMapEquivalence.UNMATCHED;
		if (token.equals("<="))
			return ConceptMapEquivalence.WIDER;
		if (token.equals("<-"))
			return ConceptMapEquivalence.SUBSUMES;
		if (token.equals(">="))
			return ConceptMapEquivalence.NARROWER;
		if (token.equals(">-"))
			return ConceptMapEquivalence.SPECIALIZES;
		if (token.equals("~"))
			return ConceptMapEquivalence.INEXACT;
		throw lexer.error("Unknown equivalence token '"+token+"'");
	}


	private void parseUses(StructureMap result, FHIRLexer lexer) throws FHIRException {
		lexer.token("uses");
		StructureMapStructureComponent st = result.addStructure();
		st.setUrl(lexer.readConstant("url"));
		lexer.token("as");
		st.setMode(StructureMapModelMode.fromCode(lexer.take()));
		lexer.skipToken(";");
		if (lexer.hasComment()) {
			st.setDocumentation(lexer.take().substring(2).trim());
		}
		lexer.skipComments();
	}

	private void parseImports(StructureMap result, FHIRLexer lexer) throws FHIRException {
		lexer.token("imports");
		result.addImport(lexer.readConstant("url"));
		lexer.skipToken(";");
		if (lexer.hasComment()) {
			lexer.next();
		}
		lexer.skipComments();
	}

	private void parseGroup(StructureMap result, FHIRLexer lexer) throws FHIRException {
		lexer.token("group");
		StructureMapGroupComponent group = result.addGroup();
		group.setName(lexer.take());
		if (lexer.hasToken("extends")) {
			lexer.next();
			group.setExtends(lexer.take());
		}
		lexer.skipComments();
		while (lexer.hasToken("input")) 
			parseInput(group, lexer);
		while (!lexer.hasToken("endgroup")) {
			if (lexer.done())
				throw lexer.error("premature termination expecting 'endgroup'");
			parseRule(group.getRule(), lexer);
		}
		lexer.next();
		lexer.skipComments();
	}

	private void parseInput(StructureMapGroupComponent group, FHIRLexer lexer) throws FHIRException {
		lexer.token("input");
		StructureMapGroupInputComponent input = group.addInput();
		input.setName(lexer.take());
		if (lexer.hasToken(":")) {
			lexer.token(":");
			input.setType(lexer.take());
		}
		lexer.token("as");
		input.setMode(StructureMapInputMode.fromCode(lexer.take()));
		if (lexer.hasComment()) {
			input.setDocumentation(lexer.take().substring(2).trim());
		}
		lexer.skipToken(";");
		lexer.skipComments();
	}

	private void parseRule(List<StructureMapGroupRuleComponent> list, FHIRLexer lexer) throws FHIRException {
		StructureMapGroupRuleComponent rule = new StructureMapGroupRuleComponent(); 
		list.add(rule);
		rule.setName(lexer.takeDottedToken());
		lexer.token(":");
		lexer.token("for");
		boolean done = false;
		while (!done) {
			parseSource(rule, lexer);
			done = !lexer.hasToken(",");
			if (!done)
				lexer.next();
		}
		if (lexer.hasToken("make")) {
			lexer.token("make");
			done = false;
			while (!done) {
				parseTarget(rule, lexer);
				done = !lexer.hasToken(",");
				if (!done)
					lexer.next();
			}
		}
		if (lexer.hasToken("then")) {
			lexer.token("then");
			if (lexer.hasToken("{")) {
				lexer.token("{");
				if (lexer.hasComment()) {
					rule.setDocumentation(lexer.take().substring(2).trim());
				}
				lexer.skipComments();
				while (!lexer.hasToken("}")) {
					if (lexer.done())
						throw lexer.error("premature termination expecting '}' in nested group");
					parseRule(rule.getRule(), lexer);
				}      
				lexer.token("}");
			} else {
				done = false;
				while (!done) {
					parseRuleReference(rule, lexer);
					done = !lexer.hasToken(",");
					if (!done)
						lexer.next();
				}
			}
		} else if (lexer.hasComment()) {
			rule.setDocumentation(lexer.take().substring(2).trim());
		}
		lexer.skipComments();
	}

	private void parseRuleReference(StructureMapGroupRuleComponent rule, FHIRLexer lexer) throws FHIRLexerException {
		StructureMapGroupRuleDependentComponent ref = rule.addDependent();
		ref.setName(lexer.take());
		lexer.token("(");
		boolean done = false;
		while (!done) {
			ref.addVariable(lexer.take());
			done = !lexer.hasToken(",");
			if (!done)
				lexer.next();
		}
		lexer.token(")");
	}

	private void parseSource(StructureMapGroupRuleComponent rule, FHIRLexer lexer) throws FHIRException {
		StructureMapGroupRuleSourceComponent source = rule.addSource();
		if (lexer.hasToken("optional")) 
			lexer.next();
		else
			source.setRequired(true);
		source.setContext(lexer.take());
		if (lexer.hasToken(".")) {
			lexer.token(".");
			source.setElement(lexer.take());
		}
		if (lexer.hasToken(":")) {
		  // type and cardinality
		  lexer.token(":");
		  String type = lexer.takeDottedToken();
		  String min = lexer.take();
      lexer.token("..");
		  String max = lexer.take();
		  // now ignore 
		}
		if (Utilities.existsInList(lexer.getCurrent(), "first", "last", "only_one"))
			if (lexer.getCurrent().equals("only_one")) { 
				source.setListMode(StructureMapListMode.SHARE);
				lexer.take();
			} else 
				source.setListMode(StructureMapListMode.fromCode(lexer.take()));

		if (lexer.hasToken("as")) {
			lexer.take();
			source.setVariable(lexer.take());
		}
		if (lexer.hasToken("where")) {
			lexer.take();
			ExpressionNode node = fpe.parse(lexer);
			source.setUserData(MAP_WHERE_EXPRESSION, node);
			source.setCondition(node.toString());
		}
		if (lexer.hasToken("check")) {
			lexer.take();
			ExpressionNode node = fpe.parse(lexer);
			source.setUserData(MAP_WHERE_CHECK, node);
			source.setCheck(node.toString());
		}
	}

	private void parseTarget(StructureMapGroupRuleComponent rule, FHIRLexer lexer) throws FHIRException {
		StructureMapGroupRuleTargetComponent target = rule.addTarget();
		String start = lexer.take();
		if (lexer.hasToken(".")) {
	    target.setContext(start);
	    start = null;
			lexer.token(".");
			target.setElement(lexer.take());
		}
		String name;
		boolean isConstant = false;
		if (lexer.hasToken("=")) {
		  if (start != null)
	      target.setContext(start);
			lexer.token("=");
			isConstant = lexer.isConstant(true);
			name = lexer.take();
		} else 
		  name = start;
		
		if (lexer.hasToken("(")) {
		  target.setTransform(StructureMapTransform.fromCode(name));
		  lexer.token("(");
		  if (target.getTransform() == StructureMapTransform.EVALUATE) {
		    parseParameter(target, lexer);
		    lexer.token(",");
		    ExpressionNode node = fpe.parse(lexer);
		    target.setUserData(MAP_EXPRESSION, node);
		    target.addParameter().setValue(new StringType(node.toString()));
		  } else { 
		    while (!lexer.hasToken(")")) {
		      parseParameter(target, lexer);
		      if (!lexer.hasToken(")"))
		        lexer.token(",");
		    }       
		  }
		  lexer.token(")");
		} else if (name != null) {
		  target.setTransform(StructureMapTransform.COPY);
		  if (!isConstant) {
		    String id = name;
		    while (lexer.hasToken(".")) {
		      id = id + lexer.take() + lexer.take();
		    }
		    target.addParameter().setValue(new IdType(id));
		  }
		  else 
		    target.addParameter().setValue(readConstant(name, lexer));
		}
		if (lexer.hasToken("as")) {
			lexer.take();
			target.setVariable(lexer.take());
		}
		while (Utilities.existsInList(lexer.getCurrent(), "first", "last", "share", "only_one")) {
			if (lexer.getCurrent().equals("share")) {
				target.addListMode(StructureMapListMode.SHARE);
				lexer.next();
				target.setListRuleId(lexer.take());
			} else if (lexer.getCurrent().equals("first")) 
				target.addListMode(StructureMapListMode.FIRST);
			else
				target.addListMode(StructureMapListMode.LAST);
			lexer.next();
		}
	}


	private void parseParameter(StructureMapGroupRuleTargetComponent target, FHIRLexer lexer) throws FHIRLexerException {
		if (!lexer.isConstant(true)) {
			target.addParameter().setValue(new IdType(lexer.take()));
		} else if (lexer.isStringConstant())
			target.addParameter().setValue(new StringType(lexer.readConstant("??")));
		else {
			target.addParameter().setValue(readConstant(lexer.take(), lexer));
		}
	}

	private Type readConstant(String s, FHIRLexer lexer) throws FHIRLexerException {
		if (Utilities.isInteger(s))
			return new IntegerType(s);
		else if (Utilities.isDecimal(s))
			return new DecimalType(s);
		else if (Utilities.existsInList(s, "true", "false"))
			return new BooleanType(s.equals("true"));
		else 
			return new StringType(lexer.processConstant(s));        
	}


	public enum VariableMode {
		INPUT, OUTPUT
	}

	public class Variable {
		private VariableMode mode;
		private String name;
		private Base object;
		public Variable(VariableMode mode, String name, Base object) {
			super();
			this.mode = mode;
			this.name = name;
			this.object = object;
		}
		public VariableMode getMode() {
			return mode;
		}
		public String getName() {
			return name;
		}
		public Base getObject() {
			return object;
		}
	}

	public class Variables {
		private List<Variable> list = new ArrayList<Variable>();

		public void add(VariableMode mode, String name, Base object) {
			Variable vv = null;
			for (Variable v : list) 
				if ((v.mode == mode) && v.getName().equals(name))
					vv = v;
			if (vv != null)
				list.remove(vv);
			list.add(new Variable(mode, name, object));
		}

		public Variables copy() {
			Variables result = new Variables();
			result.list.addAll(list);
			return result;
		}

		public Base get(VariableMode mode, String name) {
			for (Variable v : list) 
				if ((v.mode == mode) && v.getName().equals(name))
					return v.getObject();
			return null;
		}
	}

	public class TransformContext {
		private Object appInfo;

		public TransformContext(Object appInfo) {
			super();
			this.appInfo = appInfo;
		}

		public Object getAppInfo() {
			return appInfo;
		}

	}

	private void log(String cnt) {
		System.out.println(cnt);
	}

	/**
	 * Given an item, return all the children that conform to the pattern described in name
	 * 
	 * Possible patterns:
	 *  - a simple name (which may be the base of a name with [] e.g. value[x])
	 *  - a name with a type replacement e.g. valueCodeableConcept
	 *  - * which means all children
	 *  - ** which means all descendents
	 *  
	 * @param item
	 * @param name
	 * @param result
	 * @throws FHIRException 
	 */
	protected void getChildrenByName(Base item, String name, List<Base> result) throws FHIRException {
		for (Base v : item.listChildrenByName(name, true))
			if (v != null)
				result.add(v);
	}

	public Base transform(Base source, StructureMap map) {
		return null;
	}

	public void transform(Object appInfo, Base source, StructureMap map, Base target) throws Exception {
		TransformContext context = new TransformContext(appInfo);
		Variables vars = new Variables();
		vars.add(VariableMode.INPUT, "source", source);
		vars.add(VariableMode.OUTPUT, "target", target);

		log("Start Transform "+map.getUrl());
		executeGroup("", context, map, vars, map.getGroup().get(0));
	}

	private void executeGroup(String indent, TransformContext context, StructureMap map, Variables vars, StructureMapGroupComponent group) throws Exception {
		log(indent+"Group : "+group.getName());
		// todo: extends
		// todo: check inputs
		for (StructureMapGroupRuleComponent r : group.getRule()) {
			executeRule(indent+"  ", context, map, vars, group, r);
		}
	}

	private void executeRule(String indent, TransformContext context, StructureMap map, Variables vars, StructureMapGroupComponent group, StructureMapGroupRuleComponent rule) throws Exception {
		log(indent+"rule : "+rule.getName());
		Variables srcVars = vars.copy();
		if (rule.getSource().size() != 1)
			throw new Exception("not handled yet");
		List<Variables> source = analyseSource(context, srcVars, rule.getSource().get(0));
		if (source != null) {
			for (Variables v : source) {
				for (StructureMapGroupRuleTargetComponent t : rule.getTarget()) {
					processTarget(context, v, map, t);
				}
				if (rule.hasRule()) {
					for (StructureMapGroupRuleComponent childrule : rule.getRule()) {
						executeRule(indent +"  ", context, map, v, group, childrule);
					}
				} else if (rule.hasDependent()) {
					for (StructureMapGroupRuleDependentComponent dependent : rule.getDependent()) {
						executeDependency(indent+"  ", context, map, v, group, dependent);
					}
				}
			}
		}
	}

	private void executeDependency(String indent, TransformContext context, StructureMap map, Variables vin, StructureMapGroupComponent group, StructureMapGroupRuleDependentComponent dependent) throws Exception {
		StructureMap targetMap = null;
		StructureMapGroupComponent target = null;
		for (StructureMapGroupComponent grp : map.getGroup()) {
			if (grp.getName().equals(dependent.getName())) {
				if (targetMap == null) {
					targetMap = map;
					target = grp;
				} else 
					throw new FHIRException("Multiple possible matches for rule '"+dependent.getName()+"'");
			}
		}

		for (UriType imp : map.getImport()) {
			StructureMap impMap = library.get(imp.getValue());
			if (impMap == null)
				throw new FHIRException("Unable to find map "+imp.getValue());
			for (StructureMapGroupComponent grp : impMap.getGroup()) {
				if (grp.getName().equals(dependent.getName())) {
					if (targetMap == null) {
						targetMap = impMap;
						target = grp;
					} else 
						throw new FHIRException("Multiple possible matches for rule '"+dependent.getName()+"'");
				}
			}
		}
		if (target == null)
			throw new FHIRException("No matches found for rule '"+dependent.getName()+"'");

		if (target.getInput().size() != dependent.getVariable().size()) {
			throw new FHIRException("Rule '"+dependent.getName()+"' has "+Integer.toString(target.getInput().size())+" but the invocation has "+Integer.toString(dependent.getVariable().size())+" variables");
		}
		Variables v = new Variables();
		for (int i = 0; i < target.getInput().size(); i++) {
			StructureMapGroupInputComponent input = target.getInput().get(i);
			StringType var = dependent.getVariable().get(i);
			VariableMode mode = input.getMode() == StructureMapInputMode.SOURCE ? VariableMode.INPUT : VariableMode.OUTPUT;
			Base vv = vin.get(mode, var.getValue());
			if (vv == null)
				throw new FHIRException("Rule '"+dependent.getName()+"' "+mode.toString()+" variable '"+input.getName()+"' has no value");
			v.add(mode, input.getName(), vv);    	
		}
		executeGroup(indent+"  ", context, targetMap, v, target);
	}


	private List<Variables> analyseSource(TransformContext context, Variables vars, StructureMapGroupRuleSourceComponent src) throws Exception {
		Base b = vars.get(VariableMode.INPUT, src.getContext());
		if (b == null)
			throw new FHIRException("Unknown input variable "+src.getContext());

		if (src.hasCondition()) {
			ExpressionNode expr = (ExpressionNode) src.getUserData(MAP_WHERE_EXPRESSION);
			if (expr == null) {
				expr = fpe.parse(src.getCondition());
				//        fpe.check(context.appInfo, ??, ??, expr)
				src.setUserData(MAP_WHERE_EXPRESSION, expr);
			}
			if (!fpe.evaluateToBoolean(null, b, expr))
				return null;
		}

		if (src.hasCheck()) {
			ExpressionNode expr = (ExpressionNode) src.getUserData(MAP_WHERE_CHECK);
			if (expr == null) {
				expr = fpe.parse(src.getCheck());
				//        fpe.check(context.appInfo, ??, ??, expr)
				src.setUserData(MAP_WHERE_CHECK, expr);
			}
			if (!fpe.evaluateToBoolean(null, b, expr))
				throw new Exception("Check condition failed");
		} 

		List<Base> items = new ArrayList<Base>();
		if (!src.hasElement()) 
			items.add(b);
		else 
			getChildrenByName(b, src.getElement(), items);
		List<Variables> result = new ArrayList<Variables>();
		for (Base r : items) {
			Variables v = vars.copy();
			if (src.hasVariable())
				v.add(VariableMode.INPUT, src.getVariable(), r);
			result.add(v); 
		}
		return result;
	}


	private void processTarget(TransformContext context, Variables vars, StructureMap map, StructureMapGroupRuleTargetComponent tgt) throws Exception {
	  Base dest = null;
	  if (tgt.hasContext()) {
  		dest = vars.get(VariableMode.OUTPUT, tgt.getContext());
  		if (dest == null)
  			throw new Exception("target context not known: "+tgt.getContext());
  		if (!tgt.hasElement())
  			throw new Exception("Not supported yet");
	  }
		Base v = null;
		if (tgt.hasTransform()) {
			v = runTransform(context, map, tgt, vars);
			if (v != null && dest != null)
				dest.setProperty(tgt.getElement().hashCode(), tgt.getElement(), v);
		} else if (dest != null) 
			v = dest.makeProperty(tgt.getElement().hashCode(), tgt.getElement());
		if (tgt.hasVariable() && v != null)
			vars.add(VariableMode.OUTPUT, tgt.getVariable(), v);
	}

	private Base runTransform(TransformContext context, StructureMap map, StructureMapGroupRuleTargetComponent tgt, Variables vars) throws FHIRException {
		switch (tgt.getTransform()) {
		case CREATE :
			Base res = ResourceFactory.createResourceOrType(getParamString(vars, tgt.getParameter().get(0)));
			res.setIdBase(UUID.randomUUID().toString().toLowerCase());
      return res;
		case COPY : 
			return getParam(vars, tgt.getParameter().get(0));
		case EVALUATE :
			ExpressionNode expr = (ExpressionNode) tgt.getUserData(MAP_EXPRESSION);
			if (expr == null) {
				expr = fpe.parse(getParamString(vars, tgt.getParameter().get(1)));
				tgt.setUserData(MAP_WHERE_EXPRESSION, expr);
			}
			List<Base> v = fpe.evaluate(null, null, getParam(vars, tgt.getParameter().get(0)), expr);
			if (v.size() != 1)
				throw new FHIRException("evaluation of "+expr.toString()+" returned "+Integer.toString(v.size())+" objects");
			return v.get(0);

		case TRUNCATE : 
			String src = getParamString(vars, tgt.getParameter().get(0));
			String len = getParamString(vars, tgt.getParameter().get(1));
			if (Utilities.isInteger(len)) {
				int l = Integer.parseInt(len);
				if (src.length() > l)
					src = src.substring(0, l);
			}
			return new StringType(src);
		case ESCAPE : 
			throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
		case CAST :
			throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
		case APPEND : 
			throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
		case TRANSLATE : 
			return translate(context, map, vars, tgt.getParameter());
		case REFERENCE :
      Base b = getParam(vars, tgt.getParameter().get(0));
      if (b == null)
        throw new FHIRException("Unable to find parameter "+((IdType) tgt.getParameter().get(0).getValue()).asStringValue());
      if (!b.isResource())
        throw new FHIRException("Transform engine cannot point at an element of type "+b.fhirType());
      else {
        String id = b.getIdBase();
        if (id == null) {
          id = UUID.randomUUID().toString().toLowerCase();
          b.setIdBase(id);
        }
        return new Reference().setReference(b.fhirType()+"/"+id);
      }
		case DATEOP :
			throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
		case UUID :
			return new IdType(UUID.randomUUID().toString());
		case POINTER :
			b = getParam(vars, tgt.getParameter().get(0));
			if (b instanceof Resource)
				return new UriType("urn:uuid:"+((Resource) b).getId());
			else
				throw new FHIRException("Transform engine cannot point at an element of type "+b.fhirType());
		case CC:
		  CodeableConcept cc = new CodeableConcept();
		  cc.addCoding(buildCoding(getParamString(vars, tgt.getParameter().get(0)), getParamString(vars, tgt.getParameter().get(1))));
		  return cc;
		case C: 
      Coding c = buildCoding(getParamString(vars, tgt.getParameter().get(0)), getParamString(vars, tgt.getParameter().get(1)));
      return c;
		default:
			throw new Error("Transform Unknown: "+tgt.getTransform().toCode());
		}
	}


	private Coding buildCoding(String uri, String code) throws FHIRException {
	  // if we can get this as a valueSet, we will
	  String system = null;
	  ValueSet vs = worker.fetchResource(ValueSet.class, uri);
	  if (vs != null) {
	    ValueSetExpansionOutcome vse = worker.expandVS(vs, true, false);
	    if (vse.getError() != null)
	      throw new FHIRException(vse.getError());

	    for (ValueSetExpansionContainsComponent t : vse.getValueset().getExpansion().getContains()) {
	      if (code.equals(t.getCode()) && t.hasSystem()) {
	        system = t.getSystem();
	        break;
	      }
	    }
	    if (system == null)
	      throw new FHIRException("The code '"+code+"' is not in the value set '"+uri+"'");
	  } else
	    system = uri;
    return new Coding().setSystem(system).setCode(code);
  }


  private String getParamString(Variables vars, StructureMapGroupRuleTargetParameterComponent parameter) {
		Base b = getParam(vars, parameter);
		if (b == null || !b.hasPrimitiveValue())
			return null;
		return b.primitiveValue();
	}


	private Base getParam(Variables vars, StructureMapGroupRuleTargetParameterComponent parameter) {
		Type p = parameter.getValue();
		if (!(p instanceof IdType))
			return p;
		else { 
			Base b = vars.get(VariableMode.INPUT, ((IdType) p).asStringValue());
			if (b == null)
				b = vars.get(VariableMode.OUTPUT, ((IdType) p).asStringValue());
			return b;
		}
	}


	private Base translate(TransformContext context, StructureMap map, Variables vars, List<StructureMapGroupRuleTargetParameterComponent> parameter) throws FHIRException {
		Base src = getParam(vars, parameter.get(0));
		String id = getParamString(vars, parameter.get(1));
		String fld = parameter.size() > 2 ? getParamString(vars, parameter.get(2)) : null;
		return translate(context, map, src, id, fld);
	}

	private class SourceElementComponentWrapper {
	  private ConceptMapGroupComponent group;
    private SourceElementComponent comp;
    public SourceElementComponentWrapper(ConceptMapGroupComponent group, SourceElementComponent comp) {
      super();
      this.group = group;
      this.comp = comp;
    }
	}
	public Base translate(TransformContext context, StructureMap map, Base source, String conceptMapUrl, String fieldToReturn) throws FHIRException {
		Coding src = new Coding();
		if (source.isPrimitive()) {
			src.setCode(source.primitiveValue());
		} else if ("Coding".equals(source.fhirType())) {
			Base[] b = source.getProperty("system".hashCode(), "system", true);
			if (b.length == 1)
				src.setSystem(b[0].primitiveValue());
			b = source.getProperty("code".hashCode(), "code", true);
			if (b.length == 1)
				src.setCode(b[0].primitiveValue());
		} else if ("CE".equals(source.fhirType())) {
			Base[] b = source.getProperty("codeSystem".hashCode(), "codeSystem", true);
			if (b.length == 1)
				src.setSystem(b[0].primitiveValue());
			b = source.getProperty("code".hashCode(), "code", true);
			if (b.length == 1)
				src.setCode(b[0].primitiveValue());
		} else
			throw new FHIRException("Unable to translate source "+source.fhirType());

		if (conceptMapUrl.equals("http://hl7.org/fhir/ConceptMap/special-oid2uri")) {
			String uri = worker.oid2Uri(src.getCode());
			if (uri == null)
				uri = "urn:oid:"+src.getCode();
			if ("uri".equals(fieldToReturn))
				return new UriType(uri);
			else
				throw new FHIRException("Error in return code");
		} else {
			ConceptMap cmap = null;
			if (conceptMapUrl.startsWith("#")) {
				for (Resource r : map.getContained()) {
					if (r instanceof ConceptMap && ((ConceptMap) r).getId().equals(conceptMapUrl.substring(1)))
						cmap = (ConceptMap) r;
				}
			} else
				cmap = worker.fetchResource(ConceptMap.class, conceptMapUrl);
			Coding outcome = null;
			boolean done = false;
			String message = null;
			if (cmap == null) {
				if (services == null) 
					message = "No map found for "+conceptMapUrl;
				else {
					outcome = services.translate(context.appInfo, src, conceptMapUrl);
					done = true;
				}
			} else {
			  List<SourceElementComponentWrapper> list = new ArrayList<SourceElementComponentWrapper>();
			  for (ConceptMapGroupComponent g : cmap.getGroup()) {
			    for (SourceElementComponent e : g.getElement()) {
			      if (!src.hasSystem() && src.getCode().equals(e.getCode())) 
			        list.add(new SourceElementComponentWrapper(g, e));
			      else if (src.hasSystem() && src.getSystem().equals(g.getSource()) && src.getCode().equals(e.getCode()))
			        list.add(new SourceElementComponentWrapper(g, e));
			    }
			  }
				if (list.size() == 0)
					done = true;
				else if (list.get(0).comp.getTarget().size() == 0)
					message = "Concept map "+conceptMapUrl+" found no translation for "+src.getCode();
				else {
					for (TargetElementComponent tgt : list.get(0).comp.getTarget()) {
						if (tgt.getEquivalence() == ConceptMapEquivalence.EQUAL || tgt.getEquivalence() == ConceptMapEquivalence.EQUIVALENT || tgt.getEquivalence() == ConceptMapEquivalence.WIDER) {
							if (done) {
								message = "Concept map "+conceptMapUrl+" found multiple matches for "+src.getCode();
								done = false;
							} else {
								done = true;
								outcome = new Coding().setCode(tgt.getCode()).setSystem(list.get(0).group.getTarget());
							}
						} else if (tgt.getEquivalence() == ConceptMapEquivalence.UNMATCHED) {
							done = true;
						}
					}
					if (!done)
						message = "Concept map "+conceptMapUrl+" found no usable translation for "+src.getCode();
				}
			}
			if (!done) 
				throw new FHIRException(message);
			if (outcome == null)
				return null;
			if ("code".equals(fieldToReturn))
				return new CodeType(outcome.getCode());
			else
				return outcome; 
		}
	}


	public Map<String, StructureMap> getLibrary() {
	  return library;
	}

	public class PropertyWithType {
	  private Property property;
	  private TypeDetails types;
    public PropertyWithType(Property property, TypeDetails types) {
      super();
      this.property = property;
      this.types = types;
    }
    public Property getProperty() {
      return property;
    }
    public TypeDetails getTypes() {
      return types;
    }
	}
	
	public class VariableForProfiling {
	    private VariableMode mode;
	    private String name;
	    private PropertyWithType property;
	    
	    public VariableForProfiling(VariableMode mode, String name, PropertyWithType property) {
	      super();
	      this.mode = mode;
	      this.name = name;
	      this.property = property;
	    }
	    public VariableMode getMode() {
	      return mode;
	    }
	    public String getName() {
	      return name;
	    }
      public PropertyWithType getProperty() {
        return property;
      }
	  }

  public class VariablesForProfiling {
    private List<VariableForProfiling> list = new ArrayList<VariableForProfiling>();
    private boolean optional;
    private boolean repeating;

    public VariablesForProfiling(boolean optional, boolean repeating) {
      this.optional = optional;
      this.repeating = repeating;
    }

    public void add(VariableMode mode, String name, Property property, TypeDetails types) {
      add(mode, name, new PropertyWithType(property, types));
    }
    
    public void add(VariableMode mode, String name, PropertyWithType property) {
      VariableForProfiling vv = null;
      for (VariableForProfiling v : list) 
        if ((v.mode == mode) && v.getName().equals(name))
          vv = v;
      if (vv != null)
        list.remove(vv);
      list.add(new VariableForProfiling(mode, name, property));
    }

    public VariablesForProfiling copy(boolean optional, boolean repeating) {
      VariablesForProfiling result = new VariablesForProfiling(optional, repeating);
      result.list.addAll(list);
      return result;
    }

    public VariablesForProfiling copy() {
      VariablesForProfiling result = new VariablesForProfiling(optional, repeating);
      result.list.addAll(list);
      return result;
    }

    public PropertyWithType get(VariableMode mode, String name) {
      for (VariableForProfiling v : list) 
        if ((v.mode == mode) && v.getName().equals(name))
          return v.getProperty();
      return null;
    }
  }

	/**
	 * Given a structure map, return a set of profiles for what it will create
	 * 
	 * @param appInfo
	 * @param map
	 * @return
	 * @throws Exception
	 */
  public List<StructureDefinition> profileTransform(Object appInfo, StructureMap map) throws Exception {
    StructureMapGroupComponent start = map.getGroup().get(0);
    VariablesForProfiling vars = new VariablesForProfiling(false, false);
    List<StructureDefinition> profiles = new ArrayList<StructureDefinition>();
    
    for (StructureMapGroupInputComponent t : start.getInput())
      if (t.getMode() == StructureMapInputMode.SOURCE)
        vars.add(VariableMode.INPUT, t.getName(), resolveType(map, t.getType(), t.getMode()));
      else 
        vars.add(VariableMode.OUTPUT, t.getName(), createProfile(map, profiles, resolveType(map, t.getType(), t.getMode())));

    executeGroupProfile("", appInfo, map, vars, start);

    return profiles;
  }


  private void executeGroupProfile(String indent, Object appInfo, StructureMap map, VariablesForProfiling vars, StructureMapGroupComponent group) throws Exception {
    log(indent+"Profile Group : "+group.getName());
    // todo: extends
    // todo: check inputs
    for (StructureMapGroupRuleComponent r : group.getRule()) {
      executeRuleProfile(indent+"  ", appInfo, map, vars, group, r);
    }    
  }


  private void executeRuleProfile(String indent, Object appInfo, StructureMap map, VariablesForProfiling vars, StructureMapGroupComponent group, StructureMapGroupRuleComponent rule) throws Exception {
    log(indent+"Profile rule : "+rule.getName());
    VariablesForProfiling srcVars = vars.copy();
    if (rule.getSource().size() != 1)
      throw new Exception("not handled yet");
    VariablesForProfiling source = analyseSourceProfile(appInfo, srcVars, rule.getSource().get(0));
    if (source != null) {
      for (StructureMapGroupRuleTargetComponent t : rule.getTarget()) {
        processTargetProfile(appInfo, source, map, t);
      }
//        if (rule.hasRule()) {
//          for (StructureMapGroupRuleComponent childrule : rule.getRule()) {
//            executeRule(indent +"  ", context, map, v, group, childrule);
//          }
//        } else if (rule.hasDependent()) {
//          for (StructureMapGroupRuleDependentComponent dependent : rule.getDependent()) {
//            executeDependency(indent+"  ", context, map, v, group, dependent);
//          }
//        }
//      }
    }
  }

  private void processTargetProfile(Object appInfo, VariablesForProfiling vars, StructureMap map, StructureMapGroupRuleTargetComponent tgt) throws Exception {
    PropertyWithType dest = vars.get(VariableMode.OUTPUT, tgt.getContext());
    if (dest == null)
      throw new Exception("target context not known: "+tgt.getContext());
    if (!tgt.hasElement())
      throw new Exception("Not supported yet");
    TypeDetails td = runTransformProfile(appInfo, map, tgt, vars);
//    Property prop = updateProfile(tgt.getElement().hashCode(), tgt.getElement(), td);
//    if (tgt.hasVariable())
//      vars.add(VariableMode.OUTPUT, tgt.getVariable(), prop);
  }

//  private Property updateProfile(int hashCode, String element, TypeDetails td) {
////    !// TODO Auto-generated method stub
////    return null;
//  }


  private TypeDetails runTransformProfile(Object appInfo, StructureMap map, StructureMapGroupRuleTargetComponent tgt, VariablesForProfiling vars) throws FHIRException {
    switch (tgt.getTransform()) {
    case CREATE :
      return new TypeDetails(CollectionStatus.SINGLETON, getParamString(vars, tgt.getParameter().get(0)));
    case COPY : 
      return getParam(vars, tgt.getParameter().get(0));
//    case EVALUATE :
//      ExpressionNode expr = (ExpressionNode) tgt.getUserData(MAP_EXPRESSION);
//      if (expr == null) {
//        expr = fpe.parse(getParamString(vars, tgt.getParameter().get(1)));
//        tgt.setUserData(MAP_WHERE_EXPRESSION, expr);
//      }
//      List<Base> v = fpe.evaluate(null, null, getParam(vars, tgt.getParameter().get(0)), expr);
//      if (v.size() != 1)
//        throw new FHIRException("evaluation of "+expr.toString()+" returned "+Integer.toString(v.size())+" objects");
//      return v.get(0);
//
//    case TRUNCATE : 
//      String src = getParamString(vars, tgt.getParameter().get(0));
//      String len = getParamString(vars, tgt.getParameter().get(1));
//      if (Utilities.isInteger(len)) {
//        int l = Integer.parseInt(len);
//        if (src.length() > l)
//          src = src.substring(0, l);
//      }
//      return new StringType(src);
//    case ESCAPE : 
//      throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
//    case CAST :
//      throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
//    case APPEND : 
//      throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
//    case TRANSLATE : 
//      return translate(context, map, vars, tgt.getParameter());
//    case REFERENCE :
//      throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
//    case DATEOP :
//      throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
//    case UUID :
//      return new IdType(UUID.randomUUID().toString());
//    case POINTER :
//      Base b = getParam(vars, tgt.getParameter().get(0));
//      if (b instanceof Resource)
//        return new UriType("urn:uuid:"+((Resource) b).getId());
//      else
//        throw new FHIRException("Transform engine cannot point at an element of type "+b.fhirType());
    default:
      throw new Error("Transform Unknown or not handled yet: "+tgt.getTransform().toCode());
    }
  }

  private String getParamString(VariablesForProfiling vars, StructureMapGroupRuleTargetParameterComponent parameter) {
    Type p = parameter.getValue();
    if (p == null || p instanceof IdType)
      return null;
    if (!p.hasPrimitiveValue())
      return null;
    return p.primitiveValue();
  }


  private TypeDetails getParam(VariablesForProfiling vars, StructureMapGroupRuleTargetParameterComponent parameter) {
    Type p = parameter.getValue();
    if (!(p instanceof IdType))
      return new TypeDetails(CollectionStatus.SINGLETON, p.fhirType());
    else { 
      PropertyWithType b = vars.get(VariableMode.INPUT, ((IdType) p).asStringValue());
      if (b == null)
        b = vars.get(VariableMode.OUTPUT, ((IdType) p).asStringValue());
      return b.getTypes();
    }
  }


  private VariablesForProfiling analyseSourceProfile(Object appInfo, VariablesForProfiling vars, StructureMapGroupRuleSourceComponent src) throws Exception {
    PropertyWithType prop = vars.get(VariableMode.INPUT, src.getContext());
    if (prop == null)
      throw new FHIRException("Unknown input variable "+src.getContext());

    boolean optional = false;
    boolean repeating = false;
    
    if (src.hasCondition()) {
      optional = true;
    }

    if (src.hasElement()) {
      Property element = prop.getProperty().getChild(prop.types.getType(), src.getElement());
      if (element == null)
        throw new Exception("unknown element name "+src.getElement());
      if (element.getDefinition().getMin() == 0)
        optional = true;
      if (element.getDefinition().getMax().equals("*"))
        repeating = true;
      VariablesForProfiling result = vars.copy(optional, repeating);
      TypeDetails type = new TypeDetails(CollectionStatus.SINGLETON);
      for (TypeRefComponent tr : element.getDefinition().getType()) {
        type.addType(tr.getCode());
      }      
      result.add(VariableMode.INPUT, src.getVariable(), new PropertyWithType(element, type));
      return result;
    } else
      return vars.copy(optional, repeating);
  }


  private PropertyWithType createProfile(StructureMap map, List<StructureDefinition> profiles, PropertyWithType prop) throws Exception {
    if (prop.getProperty().getDefinition().getPath().contains(".")) 
      throw new Exception("Unable to process entry point");
    StructureDefinition profile = new StructureDefinition();
    profiles.add(profile);
    profile.setDerivation(TypeDerivationRule.CONSTRAINT);
    profile.setType(prop.getProperty().getDefinition().getPath());
    profile.setBaseDefinition(prop.getProperty().getStructure().getUrl());
    profile.setName("Profile on "+profile.getType()+" for map "+map.getName());
    profile.setUrl(map.getUrl().replace("StructureMap", "StructureDefinition")+"-"+profile.getType());
    profile.setId(map.getId()+"-"+profile.getType());
    profile.setStatus(map.getStatus());
    profile.setExperimental(map.getExperimental());
    profile.setDescription("Generated automatically from the mapping by the Java Reference Implementation");
    for (StructureMapContactComponent c : map.getContact()) {
      StructureDefinitionContactComponent p = profile.addContact();
      p.setName(c.getName());
      for (ContactPoint cc : c.getTelecom()) 
        p.addTelecom(cc);
    }
    profile.setDate(map.getDate());
    profile.setCopyright(map.getCopyright());
    profile.setFhirVersion(Constants.VERSION);
    profile.setKind(prop.getProperty().getStructure().getKind());
    profile.setAbstract(false);
    ElementDefinition ed = profile.getDifferential().addElement();
    ed.setPath(prop.getProperty().getDefinition().getPath());
    return new PropertyWithType(new Property(worker, ed, profile), new TypeDetails(CollectionStatus.SINGLETON, profile.getType()));
  }


  private PropertyWithType resolveType(StructureMap map, String type, StructureMapInputMode mode) throws Exception {
    for (StructureMapStructureComponent imp : map.getStructure()) {
      if ((imp.getMode() == StructureMapModelMode.SOURCE && mode == StructureMapInputMode.SOURCE) || 
          (imp.getMode() == StructureMapModelMode.TARGET && mode == StructureMapInputMode.TARGET)) {
        StructureDefinition sd = worker.fetchResource(StructureDefinition.class, imp.getUrl());
        if (sd == null)
          throw new Exception("Import "+imp.getUrl()+" cannot be resolved");
        if (sd.getId().equals(type)) {
          return new PropertyWithType(new Property(worker, sd.getSnapshot().getElement().get(0), sd), new TypeDetails(CollectionStatus.SINGLETON, sd.getId()));
        }
      }
    }
    throw new Exception("Unable to find structure definition for "+type+" in imports");
  }


  public StructureMap generateMapFromMappings(StructureDefinition sd) throws IOException, FHIRException {
    String id = getLogicalMappingId(sd);
    if (id == null) 
        return null;
    // we build this by text. Any element that has a mapping, we put it's mappings inside it....
    StringBuilder b = new StringBuilder();
    b.append("map \""+sd.getUrl().replace("StructureDefinition", "StructureMap")+"-map\" = \"Map for "+sd.getId()+"\"\r\n");
    b.append("\r\n");
    b.append("uses \""+sd.getUrl()+"\" as source\r\n\r\n");
    ElementDefinition root = sd.getSnapshot().getElementFirstRep();
    b.append(getMapping(root, id)+"\r\n\r\n");
    addChildMappings(b, id, "", sd, root, false);
    b.append("endgroup\r\n");
    TextFile.stringToFile(b.toString(), "c:\\temp\\test.map");
    StructureMap map = parse(b.toString());
    map.setId(sd.getId()+"-map");
    map.getText().setStatus(NarrativeStatus.GENERATED);
    map.getText().setDiv(new XhtmlNode(NodeType.Element));
    map.getText().getDiv().addTag("pre").addText(render(map));
    return map;
  }


  private void addChildMappings(StringBuilder b, String id, String indent, StructureDefinition sd, ElementDefinition ed, boolean inner) throws DefinitionException {
    boolean first = true;
    List<ElementDefinition> children = ProfileUtilities.getChildMap(sd, ed);
    for (ElementDefinition child : children) {
      if (first && inner) {
        b.append(" then {\r\n");
        first = false;
      }
      String map = getMapping(child, id);
      if (map != null) {
        b.append(indent+"  "+child.getPath()+": "+map);
        addChildMappings(b, id, indent+"  ", sd, child, true);
        b.append("\r\n");
      }
    }
    if (!first && inner)
      b.append(indent+"}");
    
  }


  private String getMapping(ElementDefinition ed, String id) {
    for (ElementDefinitionMappingComponent map : ed.getMapping())
      if (id.equals(map.getIdentity()))
        return map.getMap();
    return null;
  }


  private String getLogicalMappingId(StructureDefinition sd) {
    String id = null;
    for (StructureDefinitionMappingComponent map : sd.getMapping()) {
      if ("http://hl7.org/fhir/logical".equals(map.getUri()))
        id = map.getIdentity();
    }
    if (id == null)
      return null;
    for (ElementDefinitionMappingComponent map : sd.getSnapshot().getElementFirstRep().getMapping())
      if (id.equals(map.getIdentity()))
        return id;
    return null;
  }
	
}
