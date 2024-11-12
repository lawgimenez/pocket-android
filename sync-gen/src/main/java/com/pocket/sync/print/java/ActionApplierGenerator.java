package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.print.figment.FigmentPrinterKt;
import com.pocket.sync.type.Action;
import com.pocket.sync.type.Segment;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Modifier;

/**
 * Generates a helper class that has a method for each action with their docs and types.
 * See the generated classes java doc for more details.
 */
public class ActionApplierGenerator extends ClassGenerator {
	
	public static ClassName classname(Config config) {
		return config.helper(config, "Applier");
	}
	
	public ActionApplierGenerator(Config config, Figments figments) {
		super(classname(config), config, figments);
		typeSpec.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
				.addSuperinterface(ClassNames.APPLIER)
				.addJavadoc("A helper class for applying actions in a spec. " +
						"In your spec create an implementation of this class and delegate your spec's apply() method to this classes apply() method." +
						"Then this class provides a method for every action, just override them to write your implementation." +
						"Actions that have an effect defined in the schema will be abstract and must be overridden." +
						"Actions that don't have an effect will have a default implementation that doesn't do anything, but can be overridden.");
		
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
		
		MethodSpec.Builder redirect = MethodSpec.methodBuilder("apply")
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ClassNames.ACTION, "action")
				.addParameter(ClassNames.SPACE, "space")
				.beginControlFlow("switch (action.action())");


		List<Action> actions = new ArrayList<>(config.mode.removeSkips(figments.actions()));
		Iterator<Action> it = actions.iterator();
		while (it.hasNext()) if (it.next().isInterface()) it.remove(); // Remove interfaces
		for (Action action : actions) {
			ClassName actionClass = config.action(action);
			String methodName = config.actionMethodName(action);
			
			MethodSpec.Builder m = MethodSpec.methodBuilder(methodName)
					.addModifiers(Modifier.PROTECTED)
					.addJavadoc(JavaDescriptionPrinter.print(action.getDescription(), config))
					.addJavadoc("\n")
					.addJavadoc(effectDoc(action))
					.addParameter(config.action(action), "action")
					.addParameter(ClassNames.SPACE, "space");
			if (!action.getEffect().isEmpty()) {
				// Force an implementation to be written
				m.addModifiers(Modifier.ABSTRACT);
			}
			typeSpec.addMethod(m.build());
			
			redirect.addStatement("case $T.ACTION_NAME: $N(($T) action, space); break",
						actionClass, methodName, actionClass);
		}
		
		redirect.addStatement("default: unknown(action, space); break");
		redirect.endControlFlow();
		typeSpec.addMethod(redirect.build());

		typeSpec.addMethod(MethodSpec.methodBuilder("unknown")
				.addModifiers(Modifier.PROTECTED)
				.addJavadoc("This action doesn't match any known actions and was not routed to a specific method.")
				.addParameter(ClassNames.ACTION, "action")
				.addParameter(ClassNames.SPACE, "space")
				.build());
	}

	private CodeBlock effectDoc(Action action) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("<h3>Effects</h3>\n");
		code.add("<pre>\n");
		for (Segment e : action.getEffect().getSelf()) {
			code.add(FigmentPrinterKt.toFigment(e));
		}
		code.add("</pre>\n");
		return code.build();
	}

}
