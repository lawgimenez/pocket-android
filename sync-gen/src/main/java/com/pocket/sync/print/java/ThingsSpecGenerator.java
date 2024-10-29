package com.pocket.sync.print.java;

import com.fasterxml.jackson.core.JsonParser;
import com.pocket.sync.Figments;
import com.pocket.sync.type.Thing;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Modifier;

/**
 * Generates a class that can be used as a Spec.things() implementation.
 * See the generated files javadoc for more details.
 */
public class ThingsSpecGenerator extends ClassGenerator {
	
	public static ClassName classname(Config config) {
		return config.helper(config, "Things");
	}

	public ThingsSpecGenerator(Config config, Figments figments) {
		super(classname(config), config, figments);
		typeSpec.addSuperinterface(ClassNames.SPEC_THINGS)
				.addModifiers(Modifier.PUBLIC)
				.addJavadoc("A helper class for implementing Spec.things()." +
						"In your Spec, create an final instance of this class and return it from Spec.things()." +
						"It will have a method for each thing, that returns a builder for that thing." +
						"Each method will also have a java doc about that thing.");
		
		List<Thing> things = new ArrayList<>(config.mode.removeSkips((figments.things())));
		Iterator<Thing> it = things.iterator();
		while (it.hasNext()) if (it.next().isInterface()) it.remove(); // Remove interfaces

		for (Thing s : things) {
			ClassName builder = GenUtil.createInnerClassName(config.thing(s), "Builder");
			
			MethodSpec.Builder method = MethodSpec.methodBuilder(GenUtil.toValidMethodName(s.getName()))
					.addModifiers(Modifier.PUBLIC)
					.addStatement("return new $T()", builder)
					.addJavadoc(JavaDescriptionPrinter.print(s.getDescription(), config))
					.returns(builder);

			if (s.getDeprecated()) {
				method.addAnnotation(Deprecated.class);
			}

			typeSpec.addMethod(method.build());
		}
		
		MethodSpec.Builder parse = MethodSpec.methodBuilder("thing")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.THING)
				.addParameter(String.class, "type")
				.addParameter(ClassNames.OBJECT_NODE, "json")
				.addParameter(ClassNames.JSON_CONFIG, "_config")
				.beginControlFlow("switch (type)");
		
		for (Thing thing : things) {
			parse.addStatement("case $T.THING_TYPE: return $T.from(json, _config)",
					config.thing(thing), config.thing(thing));
		}
		parse.addStatement("default: return null");
		parse.endControlFlow();
		typeSpec.addMethod(parse.build());
		
		MethodSpec.Builder parser = MethodSpec.methodBuilder("thing")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.THING)
				.addParameter(String.class, "type")
				.addParameter(JsonParser.class, "parser")
				.addParameter(ClassNames.JSON_CONFIG, "_config")
				.addException(IOException.class)
				.beginControlFlow("switch (type)");
		
		for (Thing thing : things) {
			parser.addStatement("case $1T.THING_TYPE: return $1T.from(parser, _config)",
					config.thing(thing));
		}
		parser.addStatement("default: return null");
		parser.endControlFlow();
		typeSpec.addMethod(parser.build());
		
		
		MethodSpec.Builder buffer = MethodSpec.methodBuilder("thing")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.THING)
				.addParameter(String.class, "type")
				.addParameter(ClassNames.BYTE_READER, "in")
				.beginControlFlow("switch (type)");
		
		for (Thing thing : things) {
			buffer.addStatement("case $1T.THING_TYPE: return $1T.uncompress(in)",
					config.thing(thing));
		}
		buffer.addStatement("default: return null");
		buffer.endControlFlow();
		typeSpec.addMethod(buffer.build());
	}

}
