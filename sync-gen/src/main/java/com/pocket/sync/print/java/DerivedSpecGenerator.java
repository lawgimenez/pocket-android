package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.print.figment.FigmentPrinterKt;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.Instructions;
import com.pocket.sync.type.Thing;
import com.pocket.sync.usage.UsageMode;
import com.pocket.sync.util.FigmentUtilsKt;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

/**
 * Creates a helper class for deriving things.
 * See generated class java doc
 */
public class DerivedSpecGenerator extends ClassGenerator {
	
	public static ClassName classname(Config config) {
		return config.helper(config, "Derives");
	}
	
	public DerivedSpecGenerator(Config config, Figments figments) {
		super(classname(config), config, figments);
		typeSpec.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
				.addSuperinterface(ClassNames.SPEC_DERIVE)
				.addJavadoc("Creates an abstract structure for handling each derived Thing as its own method.\n" +
						"Subclass and implement the abstract methods.\n" +
						"Then point your {@link $T} to this classes' {@link #rederive(Thing, Collection, Diff, Space.Selector)} method.",
							ClassNames.SPEC)
				.addMethod(MethodSpec.constructorBuilder()
						.addModifiers(Modifier.PROTECTED)
						.addParameter(ArrayTypeName.get(String[].class), "fullDeriveSupport")
						.varargs()
						.addJavadoc("@param fullDeriveSupport A list of thing types that this implementation supports fully deriving (vs just re-deriving)")
						.addStatement("$T.addAll(this.fullDeriveSupport, fullDeriveSupport)", ClassName.get(Collections.class))
						.build());

		typeSpec.addField(BaseSpecGenerator.classname(config), "spec", Modifier.PRIVATE);
		typeSpec.addMethod(MethodSpec.methodBuilder("setSpec")
				.addModifiers(Modifier.PROTECTED)
				.addParameter(BaseSpecGenerator.classname(config), "spec")
				.addStatement("if (this.spec != null) throw new RuntimeException($S)", "Spec should be effectively final. Cannot be changed.")
				.addStatement("this.spec = spec")
				.build());
		typeSpec.addMethod(MethodSpec.methodBuilder("spec")
				.addModifiers(Modifier.PROTECTED)
				.addStatement("return spec")
				.returns(BaseSpecGenerator.classname(config))
				.build());

		TypeVariableName t = TypeVariableName.get("T", ClassNames.THING);
		typeSpec.addField(FieldSpec.builder(ParameterizedTypeName.get(Set.class, String.class), "fullDeriveSupport", Modifier.PRIVATE, Modifier.FINAL)
				.initializer("new $T<>()", ClassName.get(HashSet.class)).build());
		typeSpec.addMethod(MethodSpec.methodBuilder("derive")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.addTypeVariable(t)
				.addParameter(t, "thing")
				.addParameter(ClassNames.SPACE_SELECTOR, "selector")
				.returns(t)
				.beginControlFlow("if (fullDeriveSupport.contains(thing.type()))")
					.addStatement("return rederive(thing, null, null, selector)")
				.nextControlFlow("else")
					.addStatement("return null")
				.endControlFlow()
				.build());

		List<Thing> reactives = figments.reactives()
				.stream()
				.filter(thing -> !thing.isInterface())
				.filter(thing -> config.mode.mode(thing) != UsageMode.SKIP)
				.collect(Collectors.toList());
		rederive(reactives);
		rederiveSpecifics(reactives);
	}
	
	private void rederive(List<Thing> reactives) {
		TypeVariableName t = TypeVariableName.get("T", ClassNames.THING);
		MethodSpec.Builder method = MethodSpec.methodBuilder("rederive")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addTypeVariable(t)
				.returns(t)
				.addParameter(t, "thing")
				.addParameter(ParameterizedTypeName.get(Collection.class, String.class), "reactions")
				.addParameter(ClassNames.RICH_DIFF, "diff")
				.addParameter(ClassNames.SPACE_SELECTOR, "selector");
		
		method.beginControlFlow("switch (thing.type())");
		for (Thing thing : reactives) {
			method.addCode("case $T.THING_TYPE: ", config.thing(thing));
			method.addCode("return (T)");
			method.addCode("$N(($T) thing, reactions, diff, selector);\n", GenUtil.toValidMethodName(thing.getName()), config.thing(thing));
		}
		method.endControlFlow();
		method.addStatement("return thing");
		
		typeSpec.addMethod(method.build());
	}
	
	private void rederiveSpecifics(List<Thing> reactives) {
		for (Thing thing : reactives) {
			ClassName thingClass = config.thing(thing);
			ClassName builderType = GenUtil.createInnerClassName(thingClass, "Builder");
			MethodSpec.Builder method = MethodSpec.methodBuilder(GenUtil.toValidMethodName(thing.getName()))
					.addModifiers(Modifier.PUBLIC)
					.returns(config.thing(thing))
					.addParameter(config.thing(thing), "t")
					.addParameter(ParameterizedTypeName.get(Collection.class, String.class), "reactions")
					.addParameter(ClassNames.RICH_DIFF, "diff")
					.addParameter(ClassNames.SPACE_SELECTOR, "selector");
			
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement("$T b = new $T(t)", builderType, builderType);
			
			List<Field> derives = new ArrayList<>();
			for (Field field : thing.getFields().getAll()) {
				if (!field.getDerives().isDerived()) continue;
				if (field.getDerives().getRemap() != null) continue; // TODO looking at its previous vs new contents it can reorg as needed, but this is probably not needed?
				if (config.mode.mode(field) == UsageMode.SKIP) continue;
				derives.add(field);
			}
			derives = FigmentUtilsKt.sortByDependencies(derives);
			for (Field field : derives) {
				String fieldname = GenUtil.toValidFieldName(field);
				TypeName fieldtype = GenUtil.toTypeName(field, config);
				
				code.beginControlFlow("if (!t.declared.$N || reactions == null || reactions.contains($S))", GenUtil.toValidFieldName(field), field.getName());
				
				if (field.getDerives().getFirstAvailable() != null) {
					code.addStatement("$T.$N(b)", ThingGenerator.deriveClass(config, thing), fieldname);
					
				} else if (!field.getDerives().getInstructions().isEmpty() || !field.getDerives().getReactives().isEmpty()) {
					code.add("$T v = ", fieldtype);
					String deriveMethod = deriveMethod(thingClass, field);
					MethodSpec.Builder b = MethodSpec.methodBuilder(deriveMethod)
							.addModifiers(Modifier.PUBLIC)
							.returns(fieldtype)
							.addJavadoc("<pre>\n");
					for (Instructions i : field.getDerives().getInstructions()) {
						 b.addJavadoc(FigmentPrinterKt.toFigment(i));
					}
					b.addJavadoc("</pre>\n")
					.addParameter(config.thing(thing), "t")
					.addStatement("return t.$N", fieldname);
					
					code.addStatement("$N(t, diff, selector)", deriveMethod);
					b.addParameter(ClassNames.RICH_DIFF, "diff")
						.addParameter(ClassNames.SPACE_SELECTOR, "selector");
					typeSpec.addMethod(b.build());
					code.addStatement("b.$N(v)", fieldname);
					
				} else if (field.getDerives().getRemap() != null) {
					// TODO looking at its previous vs new contents it can reorg as needed, but this is probably not needed?
				}
				code.endControlFlow();
			}
			
			code.addStatement("return b != null ? b.build() : t");
			
			typeSpec.addMethod(method.addCode(code.build()).build());
		}
		
	}
	
	public static String deriveMethod(ClassName thingClass, Field field) {
		return "derive__"+thingClass.simpleName()+"__"+GenUtil.toValidFieldName(field);
	}
	
}
