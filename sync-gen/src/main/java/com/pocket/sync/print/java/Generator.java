package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.Action;
import com.pocket.sync.type.Enum;
import com.pocket.sync.type.Interface;
import com.pocket.sync.type.OpenType;
import com.pocket.sync.type.Thing;
import com.pocket.sync.type.Variety;
import com.pocket.sync.usage.UsageFile;
import com.pocket.sync.usage.UsageMode;
import com.pocket.sync.usage.UsageModeCalculator;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;

import okio.FileSystem;
import okio.Path;

/**
 * Generates Things, Actions and other classes needed for a sync engine implementation.
 * Create an instance and run {@link #generate()}
 *
 * <h2>Generation Design</h2>
 *
 * Code that generates other code, especially when it produces a lot of different types of code,
 * that all reference and work with each other... can be a bit tricky to wrap your head around.
 *
 * This doc will help try to provide the high level overview of what is produced and how it relates to the schema and the sync engine.
 *
 * <h3>Classes, Names and References</h3>
 *
 * When you need to reference a class type, use one of these to do it:
 * <ul>
 *     <li>{@link ClassNames} - Use these static constants for preexisting classes that aren't being code generated. Such as classes in the sync engine or java libraries.</li>
 *     <li>{@link Config} methods like {@link Config#thing(Thing)} - Use these to reference definition classes that are created by code generation itself. These methods help provide a single standard for these class names and package structure.</li>
 *     <li>{@link GenUtil#toTypeName(com.pocket.sync.type.FieldType, Config)} - Use this when typing a value from a field.</li>
 * </ul>
 *
 * When you need to convert a name to a java one, use {@link GenUtil} methods such as:
 * <ul>
 *     <li>{@link GenUtil#toValidFieldName(String)}</li>
 *     <li>{@link GenUtil#toValidClassName(String)}</li>
 * </ul>
 *
 * When you need to reference some inner value of a generated class such as a method or field,
 * these are typically exposed as static methods from the various code owners of that output, such as:
 * <ul>
 *     <li>{@link ThingGenerator#builderClass(Config, Thing)}</li>
 *     <li>{@link GenUtil#creatorFromJsonCode(com.pocket.sync.type.FieldType, Config)}</li>
 *     <li>etc..</li>
 * </ul>
 * In general, if something might be referenced by something else, pick a code owner and expose a method that returns the type/name.
 *
 *
 *
 * TODO flush out more of the high level generation design.
 *
 *
 * <h4>Variety</h4>
 * The main code owner for handling varieties is {@link OpenTypes}.
 *
 * Each variety will be represented by an empty java interface, and all of the types in that variety will implement it.
 * The package name and class name of the interface follows the same rules as things,
 * and {@link Config#variety(Variety)} uses {@link Config#thing(Thing)}.
 *
 * For example:
 *
 * <pre>
 * variety Pet : Dog, Cat
 * thing Home {
 *     pets : [Pet]
 * }
 * </pre>
 *
 * Code generation will create java interface class for Pet.
 * It will also create a concrete Thing class for Home.
 *
 * In java, Dog and Cat will implement Pet, and Pet will implement Thing.
 *
 * The interface doesn't have any methods, it is just used to help with type safety.
 * It means "Dog" and "Cat" will be accepted anywhere using this variety as its type.
 *
 * The interface will have a static constant creator that other parts of code generation can reference when it needs a creator.
 * Use the various static methods on {@link OpenTypes} such as {@link OpenTypes#creatorReference(OpenType, Config)} to reference them in code.
 *
 *
 * <h4>Interface</h4>
 * The main code owners for handling interfaces is {@link OpenTypes} and {@link InterfaceGenerator}.
 *
 * Each GraphQL interface have its own java interface.
 * The package name and class name of interfaces follows the same rules as normal things and actions,
 * and also use {@link Config#thing(Thing)} or {@link Config#action(Action)}.
 *
 * The interface will either extend Thing or Action.
 *
 * It will have an interface method for each of its fields.
 *
 * It will also have a static constant creator that other parts of code generation can reference when it needs a creator.
 * See {@link OpenTypes#creatorReference(OpenType, Config)} to reference it in code.
 *
 * Things that implement this interface in GraphQL will similarly have their java classes implement its java interface.
 *
 * For example:
 *
 * <pre>
 * interface thing Animal {
 *     species : String
 * }
 * interface thing Bird : Animal {
 *     wingspan : Integer
 * }
 * interface thing Feathered : Animal {
 *     feather_color : Color
 * }
 * thing Chicken : Bird {
 *     name : String
 * }
 * </pre>
 *
 * Code generation will create java interface classes for Animal, Bird and Feathered.
 * It will also create a concrete Thing class for Chicken.
 *
 * In java, Chicken will implement Bird,
 * Feathered and Bird will implement Animal,
 * and Animal will implement Thing.
 *
 * Animal will have a "species" method,
 * Bird will also have a "wingspan" method,
 * Feathered will also have a "feather_color" method.
 * 
 * Chicken will have both fields and methods for "species", "wingspan", "feather_color" and those methods will return the field value.
 * It will also have a "name" field.
 *
 * <h4>Base Actions</h4>
 * Base Actions are treated just like action interfaces.
 *
 * <h4>Open Type Serialization</h4>
 * Main code owner for this logic is {@link SyncableGenerator} and {@link CompressGenerator}.
 *
 * Anytime it is serializing an open type, to JSON, or to binary, it will include some type identifier.
 * For JSON it includes a special field "_type" (See the constant in the sync engine's Thing class for the official key)
 * For compression, it writes out the type name before the data of the thing.
 *
 * When it is parsing JSON it expects this special field is present. If it isn't, an error will occur.
 * Likewise, when decompressing binary, it expects the type string before the thing data.
 *
 * It only adds/expects these special identifiers when it knows it is an open type (interface or variety).
 * This avoids noise and overhead when it is obvious what type it is expecting, which is likely the vast majority of the time.
 *
 * For parsing, it relies on the {@link ClassNames#OPEN_CREATOR} class in the sync engine, which knows how to find the type identifier and then route to the correct parser,
 * based on the type it finds.
 */
public class Generator {

	private final Figments figments;
	private final Config config;

	public Generator(Figments figments, Config config) {
		this.figments = figments;
		this.config = config;
	}

	public void generate() throws IOException {
		UsageModeCalculator mode = new UsageModeCalculator(figments, new UsageFile(FileSystem.SYSTEM, config.usageFile != null ? Path.get(config.usageFile) : null));
		config.mode = mode;

		// Start with Modeller, so it fails early in case of anything missing in Config
		// with a clear error message instead of a cryptic exception.
		write(new ModellerGenerator(config, figments).getTypeSpec(), config.outPackage);

		for (Enum value : figments.enums()) {
			if (mode.mode(value) == UsageMode.SKIP) continue;
			TypeSpec classs = new EnumGenerator(value, figments, config).getTypeSpec();
			write(classs, config.enumsPackage);
		}
		for (Thing value : figments.things()) {
			if (mode.mode(value) == UsageMode.SKIP) continue;
			TypeSpec clazz;
			if (value.isInterface()) {
				clazz = new InterfaceGenerator((Interface) value, figments, config).create();
			} else {
			 	clazz = new ThingGenerator(value, figments, config).setup().getTypeSpec();
			}
			write(clazz, config.thingPackage);
		}
		for (Action value : figments.actions()) {
			if (mode.mode(value) == UsageMode.SKIP && !value.isBase()) continue;
			TypeSpec clazz;
			if (value.isInterface()) {
				clazz = new InterfaceGenerator((Interface) value, figments, config).create();
			} else {
				clazz = new ActionGenerator(value, config, figments).getTypeSpec();
			}
			write(clazz, config.actionPackage);
		}
		for (Variety value : OpenTypes.uniqueVarieties(figments, mode)) {
			// Varieties are not tracked in the usage file.
			// Instead OpenTypes.uniqueVarieties() filters out unused varieties.
			TypeSpec clazz = new VarietyGenerator(value, config).create();
			write(clazz, config.thingPackage);
		}
		write(new ActionApplierGenerator(config, figments).getTypeSpec(), config.outPackage);
		write(new ThingsSpecGenerator(config, figments).getTypeSpec(), config.outPackage);
		write(new ActionsSpecGenerator(config, figments).getTypeSpec(), config.outPackage);
		write(new DerivedSpecGenerator(config, figments).getTypeSpec(), config.outPackage);
		write(new BaseSpecGenerator(config, figments).getTypeSpec(), config.outPackage);
		write(new AuthTypeGenerator(config, figments).getTypeSpec(), config.outPackage);
		write(new RemoteStyleGenerator(config, figments).getTypeSpec(), config.outPackage);

		mode.commitUsageFile();
	}

	private void write(TypeSpec clazz, String packageName) throws IOException {
		JavaFile.builder(packageName, clazz)
				.indent("    ")
				.build()
				.writeTo(config.outDirectory);
	}

}

