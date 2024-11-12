package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;

/**
 * Creates a BaseSpec implementation that is almost ready to use, just needs the concrete implementations of any actions or derived things/fields.
 */
public class BaseSpecGenerator extends ClassGenerator {
	
	public static ClassName classname(Config config) {
		return config.helper(config, "BaseSpec");
	}
	
	public BaseSpecGenerator(Config config, Figments figments) {
		super(classname(config), config, figments);
		
		ClassName things = ThingsSpecGenerator.classname(config);
		ClassName actions = ActionsSpecGenerator.classname(config);
		ClassName deriver = DerivedSpecGenerator.classname(config);
		ClassName applier = ActionApplierGenerator.classname(config);
		
		typeSpec.addModifiers(Modifier.PUBLIC)
				.addSuperinterface(ClassNames.SPEC)
				.addField(FieldSpec.builder(things, "things").initializer("new $T()", things).build())
				.addField(FieldSpec.builder(actions, "actions").initializer("new $T()", actions).build())
				.addField(deriver, "deriver")
				.addField(applier, "applier")
				.addJavadoc("A helper for building a {@link $T} for $N. \n" +
							"It contains everything that was generated from schema, but just needs implementations of how to apply actions and derive fields.\n" +
							"To use, create your spec as subclass that passes in implementations for {@link $T} and {@link $T} into the constructor.\n",
							ClassNames.SPEC, config.name, deriver, applier);
		
		typeSpec.addMethod(MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PROTECTED)
				.addParameter(deriver, "deriver")
				.addParameter(applier, "applier")
				.addStatement("this.deriver = deriver")
				.addStatement("this.applier = applier")
				.addStatement("this.deriver.setSpec(this)")
				.addStatement("this.applier.setSpec(this)")
				.build());
		typeSpec.addMethod(MethodSpec.methodBuilder("things")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addStatement("return things")
				.returns(things)
				.build());
		typeSpec.addMethod(MethodSpec.methodBuilder("actions")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addStatement("return actions")
				.returns(actions)
				.build());
		typeSpec.addMethod(MethodSpec.methodBuilder("derive")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addStatement("return deriver")
				.returns(deriver)
				.build());
		typeSpec.addMethod(MethodSpec.methodBuilder("apply")
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ClassNames.ACTION, "action")
				.addParameter(ClassNames.SPACE, "space")
				.addAnnotation(Override.class)
				.addStatement("applier.apply(action, space)")
				.build());
		
	}
	
}
