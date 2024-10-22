package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.Action;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Modifier;

/**
 * Generates a class that can be used as a Spec.actions() implementation.
 * See the generated files javadoc for more details.
 */
public class ActionsSpecGenerator extends ClassGenerator {
	
	public static ClassName classname(Config config) {
		return config.helper(config, "Actions");
	}

	public ActionsSpecGenerator(Config config, Figments figments) {
		super(classname(config), config, figments);
		typeSpec.addSuperinterface(ClassNames.SPEC_ACTIONS)
				.addModifiers(Modifier.PUBLIC)
				.addJavadoc("A helper class for implementing Spec.actions()." +
						"In your Spec, create an final instance of this class and return it from Spec.actions()." +
						"It will have a method for each action, that returns a builder for that action." +
						"Each method will also have a java doc about that action.");

		List<Action> actions = new ArrayList<>(config.mode.removeSkips(figments.actions()));
		Iterator<Action> it = actions.iterator();
		while (it.hasNext()) if (it.next().isInterface()) it.remove(); // Remove interfaces
		
		for (Action a : actions) {
			ClassName builder = GenUtil.createInnerClassName(config.action(a), "Builder");
			
			MethodSpec.Builder method = MethodSpec.methodBuilder(config.actionMethodName(a))
					.addModifiers(Modifier.PUBLIC)
					.addStatement("return new $T()", builder)
					.addJavadoc(JavaDescriptionPrinter.print(a.getDescription(), config))
					.returns(builder);

			if (a.getDeprecated()) {
				method.addAnnotation(Deprecated.class);
			}

			typeSpec.addMethod(method.build());
		}
		
		MethodSpec.Builder parse = MethodSpec.methodBuilder("action")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.ACTION)
				.addParameter(ClassNames.OBJECT_NODE, "json")
				.addParameter(ClassNames.JSON_CONFIG, "_config")
				.beginControlFlow("switch (json.get($S).asText())", "action");
		for (Action action : actions) {
			ClassName actionClass = config.action(action);
			parse.addStatement("case $T.ACTION_NAME: return $T.from(json, _config)",
					actionClass, actionClass);
		}
		parse.addStatement("default: return null");
		parse.endControlFlow();
		typeSpec.addMethod(parse.build());
	}

}
