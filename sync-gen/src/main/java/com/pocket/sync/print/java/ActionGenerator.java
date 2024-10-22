package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.Action;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.FieldType;
import com.pocket.sync.type.ListType;
import com.pocket.sync.type.Priority;
import com.pocket.sync.type.PriorityFlag;
import com.pocket.sync.usage.UsageMode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

/**
 * Generates an Action class for an action.
 */
public class ActionGenerator extends ClassGenerator {
	
	public ActionGenerator(Action action, Config config, Figments figments) {
		super(config.action(action), config, figments);
		final String name = action.getName();
		final ClassName builderClassName = GenUtil.createInnerClassName(className, "Builder");
		final ClassName declaredClassName = GenUtil.createInnerClassName(className, "Declared");
		final ClassName declaredMutableClassName = GenUtil.createInnerClassName(className, "DeclaredMutable");
		typeSpec.addModifiers(Modifier.PUBLIC)
				.addJavadoc(JavaDescriptionPrinter.print(action.getDescription(), config));
		if (action.getDeprecated()) {
			typeSpec.addAnnotation(Deprecated.class);
		}

		List<Field> fields = new ArrayList<>();
		for (Field field : action.getFields().getAll()) {
			if (field.getName().equals("action")) continue; // This is a special field that we always want to set to the name of the action, don't handle it like a normal field
			if (config.mode.mode(field) == UsageMode.SKIP) continue;
			fields.add(field);
		}

		GraphQlGeneratorKt.addGraphQlSupport(action, typeSpec, config);
		SyncableGenerator.addAuthType(typeSpec, action.getAuth().getAuth(), config, figments);
		SyncableGenerator.addFields(action, fields, config, typeSpec);
		SyncableGenerator.addConstructor(fields, builderClassName, declaredClassName, typeSpec);
		SyncableGenerator.addDeclared(fields, declaredClassName, declaredMutableClassName, typeSpec);
		SyncableGenerator.addFromJson(fields, className, builderClassName, config, typeSpec);
		SyncableGenerator.addToJson(action, fields, config, typeSpec, CodeBlock.builder().addStatement("json.put($S, $S)", "action", action.getName()).build());
		SyncableGenerator.addToMap(fields, config, typeSpec, CodeBlock.builder().addStatement("map.put($S, $S)", "action", action.getName()).build());
		SyncableGenerator.addToString("ACTION_NAME", typeSpec);
		SyncableGenerator.addBuilder(fields, config, className, builderClassName, declaredClassName, declaredMutableClassName, ClassNames.ACTION_BUILDER, typeSpec);
		SyncableGenerator.addCreator(typeSpec, className);
		SyncableGenerator.addRemoteInfo(action, fields, typeSpec, config);
		OpenTypes.setupForSyncable(typeSpec, config, action);

		getTypeBuilder().addSuperinterface(ClassNames.ACTION);

		getTypeBuilder().addField(FieldSpec.builder(ClassName.get(String.class), "ACTION_NAME", Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
				.initializer("$S", name)
				.build());
		
		PriorityFlag mergedPriority = action.getPriority().getMerged();
		Priority actionPriority;
		if (mergedPriority != null) {
			actionPriority = mergedPriority.getPriority();
		} else if (action.getRemote().getRemote() != null && action.getRemote().getRemote().getName().equalsIgnoreCase("Local")) {
			// TODO clean up the priority stuff now that styles are generic?
			actionPriority = Priority.LOCAL;
		} else if (action.getEndpoint().getMerged() != null) {
			actionPriority = Priority.REMOTE;
		} else {
			actionPriority = Priority.WHENEVER;
		}
		CodeBlock.Builder priority = CodeBlock.builder();
		switch (actionPriority) {
			case ASAP: priority.add("$T.SOON", ClassNames.REMOTE_PRIORITY); break;
			case WHENEVER: priority.add("$T.WHENEVER", ClassNames.REMOTE_PRIORITY); break;
			case REMOTE: priority.add("$T.REMOTE", ClassNames.REMOTE_PRIORITY); break;
			case REMOTE_RETRYABLE: priority.add("$T.REMOTE_RETRYABLE", ClassNames.REMOTE_PRIORITY); break;
			case LOCAL: priority.add("$T.LOCAL", ClassNames.REMOTE_PRIORITY); break;
			default: throw new RuntimeException("unknown priority on action " + action);
		}
		getTypeBuilder().addField(FieldSpec.builder(ClassNames.REMOTE_PRIORITY, "PRIORITY", Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
				.initializer(priority.build())
				.build());
		getTypeBuilder().addMethod(MethodSpec.methodBuilder("priority")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.REMOTE_PRIORITY)
				.addStatement("return PRIORITY")
				.build());

		getTypeBuilder().addMethod(MethodSpec.methodBuilder("action")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(String.class)
				.addStatement("return ACTION_NAME")
				.build());

		getTypeBuilder().addMethod(MethodSpec.methodBuilder("time")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(config.time != null ? config.value(figments.get(config.time)).java() : ClassNames.TIME)
				.addStatement("return time")
				.build());

		String hashcodeResultVar = "_result";
		MethodSpec.Builder hashCode = MethodSpec.methodBuilder("hashCode")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(int.class)
				.addStatement("int $N = 0", hashcodeResultVar)
				.addStatement("$1T e = $1T.STATE", ClassNames.EQUALITY);
		for (Field field : fields) {
			SyncableGenerator.addHashCodeFor(hashCode, field, hashcodeResultVar);
		}
		hashCode.addStatement("return $N", hashcodeResultVar);
		typeSpec.addMethod(hashCode.build());
		
		MethodSpec.Builder equals = MethodSpec.methodBuilder("equals")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(boolean.class)
				.addParameter(Object.class, "o")
				.addStatement("if (this == o) return true")
				.addStatement("if (o == null || getClass() != o.getClass()) return false")
				.addStatement("$1T that = ($1T) o", className)
				.addStatement("$1T e = $1T.STATE", ClassNames.EQUALITY);
		for (Field field : fields) {
			SyncableGenerator.addEqualsFor(equals, field);
		}
		equals.addStatement("return true");
		typeSpec.addMethod(equals.build());


		/*
			Add the resolved interface.  This will create code of one of these shapes:

			// When the resolved type is a thing:
				public static final ActionResolved<Item> RESOLVED = new ActionResolved<>(Item.JSON_CREATOR, Item.STREAMING_JSON_CREATOR);

			// When the resolved type is a collection:
				public static final ActionResolved<List<Item>> RESOLVED = new ActionResolved<>(
					(value, config, allowed) -> Modeller.asList(value, Item.JSON_CREATOR, config, allowed),
					(parser, config, allowed) -> Modeller.asList(parser, Item.STREAMING_JSON_CREATOR, config)
				);

			// When the resolved type is not a Thing or Variety:
				public static final ActionResolved<String> RESOLVED = new ActionResolved<>(
					(value, config, allowed) -> Modeller.STRING_CREATOR.create(value),
					(value, config, allowed) -> Modeller.STRING_STREAMING_CREATOR.create(value)
				);

			// Another example with an enum:
				public static final ActionResolved<ItemStatus> RESOLVED = new ActionResolved<>(
					(value, config, allowed) -> ItemStatus.JSON_CREATOR.create(value),
					(parser, config, allowed) -> ItemStatus.STREAMING_JSON_CREATOR.create(parser)
				);

				@Override
				public ActionResolved resolver() {
					return RESOLVED;
				}

			Or if no resolved value, just the interface method returning null
		 */
		MethodSpec.Builder resolved = MethodSpec.methodBuilder("resolved")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class);
		if (action.getResolves().getMerged() != null) {
			FieldType type = action.getResolves().getMerged().getType();
			TypeName fieldClass = GenUtil.toTypeName(type, config);
			TypeName resolvedClass = ParameterizedTypeName.get(ClassNames.ACTION_RESOLVED, fieldClass);

			resolved.returns(resolvedClass);

			CodeBlock.Builder init = CodeBlock.builder()
					.add("new $T<>(", ClassNames.ACTION_RESOLVED);

			WhenType.is(type)
				.collection(c -> {
					//(value, config, allowed) -> Modeller.asList(value, Item.JSON_CREATOR, config, allowed)
					init.add("(value, config, allowed) -> $.as$N(value, ", c instanceof ListType ? "List" : "Map")
						.add(GenUtil.creatorFromJsonCode(c.getInner(), config))
						.add(", config, allowed)")
						.add(", ")
						.add("(value, config, allowed) -> $.as$N(value, ", c instanceof ListType ? "List" : "Map")
						.add(GenUtil.streamingCreatorFromJsonCode(c.getInner(), config))
						.add(", config, allowed)");
				})
				.otherwise(t -> {
					boolean wrap = WhenType.is(t, boolean.class)
							.enumm(e -> true)
							.value(v -> true)
							.otherwise(o -> false);
					init.add(creator(GenUtil.creatorFromJsonCode(type, config), wrap))
							.add(", ")
							.add(creator(GenUtil.streamingCreatorFromJsonCode(type, config), wrap));
				});
			init.add(")");

			typeSpec.addField(FieldSpec.builder(resolvedClass, "RESOLVED", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
					.initializer(init.build())
					.build());

			resolved.addStatement("return RESOLVED");

		} else {
			resolved.returns(ClassNames.ACTION_RESOLVED);
			resolved.addStatement("return null");
		}
		typeSpec.addMethod(resolved.build());


	}

	private CodeBlock creator(CodeBlock codeBlock, boolean wrap) {
		if (wrap) {
			return CodeBlock.builder()
					.add("(value, config, allowed) -> ")
					.add(codeBlock)
					.add(".create(value)")
					.build();
		} else {
			return codeBlock;
		}
	}
}
