package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.Action;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.Interface;
import com.pocket.sync.type.Syncable;
import com.pocket.sync.type.Thing;
import com.pocket.sync.usage.UsageMode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.Collection;

import javax.lang.model.element.Modifier;


/**
 * Creates a java interface for a figment interface
 */
public class InterfaceGenerator {

	protected final Interface definition;
	protected final TypeSpec.Builder typeSpec;
	protected final ClassName className;
	protected final Figments figments;
	protected final Config config;

	public InterfaceGenerator(Interface definition, Figments figments, Config config) {
		this(definition, figments, config, config.interface_(definition));
	}

	public InterfaceGenerator(Interface definition, Figments figments, Config config, ClassName className) {
		this.typeSpec = TypeSpec.interfaceBuilder(className);
		this.className = className;
		this.figments = figments;
		this.config = config;
		this.definition = definition;
	}

	public TypeSpec create() {
		typeSpec.addModifiers(Modifier.PUBLIC)
				.addJavadoc(JavaDescriptionPrinter.print(definition.getDescription(), config));

		if (definition instanceof Thing) {
			typeSpec.addSuperinterface(ClassNames.THING);
			flat();
		} else {
			typeSpec.addSuperinterface(ClassNames.ACTION);
		}

		Syncable asSyncable = (Syncable) definition;

		if (definition.getDeprecated()) typeSpec.addAnnotation(Deprecated.class);

		OpenTypes.setupForSyncable(typeSpec, config, asSyncable);

		OpenTypes.setupForInterface(typeSpec, definition, config);

		Collection<Field> fields = asSyncable.getFields().getAll();
		for (Field field : fields) {
			if (field.getName().equals("action") && asSyncable instanceof Action) continue; // This is a special field always set to the name of the action. TODO separate out this convention in someway, otherwise this is a field name actions can never have and could become an issue for some apis.
			if (config.mode.mode(field) == UsageMode.SKIP) continue;
			typeSpec.addMethod(fieldMethod(field, config)
					.addModifiers(Modifier.ABSTRACT)
					.build());
		}
		return typeSpec.build();
	}

	public static MethodSpec.Builder fieldMethod(Field field, Config config) {
		return MethodSpec.methodBuilder("_"+GenUtil.toValidFieldName(field))
				.addModifiers(Modifier.PUBLIC)
				.returns(GenUtil.toTypeName(field, config));
	}

	private void flat() {
		typeSpec.addMethod(
				MethodSpec.methodBuilder("flat")
						.addAnnotation(Override.class)
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.returns(className)
						.build()
		);
	}
}
