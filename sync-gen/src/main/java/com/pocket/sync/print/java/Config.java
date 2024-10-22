package com.pocket.sync.print.java;

import com.pocket.sync.type.Action;
import com.pocket.sync.type.Enum;
import com.pocket.sync.type.Interface;
import com.pocket.sync.type.Syncable;
import com.pocket.sync.type.Thing;
import com.pocket.sync.type.Value;
import com.pocket.sync.type.Variety;
import com.pocket.sync.usage.UsageModeCalculator;
import com.squareup.javapoet.ClassName;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration of what and where to output files when generating code.
 */
public class Config {

    public static class Builder {
		private final File outDirectory;
		private final String outPackage;
		private final Map<String, ValueModeller> modellers = new HashMap<>();
		private File compatFile;
		private String name;
		private String time;
		private boolean graphQl = false;

		public Builder(String name, File outDirectory, String outPackage) {
			this.name = name;
			this.outDirectory = outDirectory;
			this.outPackage = outPackage;
		}

		public Builder enableGraphQl() {
			this.graphQl = true;
			return this;
		}
		
		public Builder compatFile(File file) {
			this.compatFile = file;
			return this;
		}
		
		public Builder modelValue(String name, ValueModeller modeller) {
			modellers.put(name, modeller);
			return this;
		}
		
		/** If you want your action's to return a different Time subclass, provide the value of its value type here. */
		public Builder timeValue(String name) {
			time = name;
			return this;
		}
		
		public Config build() {
			return new Config(this);
		}
		
	}
	

	/** The directory to output generated files and directories into. */
	public final File outDirectory;
	/** The base package name to use for generated files. */
	public final String outPackage;
	/** The package name for generated things. */
	public final String thingPackage;
	/** The package name for generated things. */
	public final String enumsPackage;
	/** The package name for generated actions. */
	public final String actionPackage;
	/** The location of where a {@link com.pocket.sync.usage.UsageFile} for this project can be found/saved. */
	public final File usageFile;
	/** A prefix name to use for helper classes generated with this config */
	public final String name;
	/** If non-null, use this value type instead of the default Time class for actions. */
	public final String time;
	/** If true, include graphql support in generated code */
	public final boolean graphQl;
	/** How to handle values in code. */
	private final Map<String, ValueModeller> modellers = new HashMap<>();
	
	/** Access for querying gen modes. Set by the {@link Generator}. Set on the config object since it is passed around and is an easy place to reference it. */
	public UsageModeCalculator mode;
	
	public Config(Builder builder) {
		this.outDirectory = builder.outDirectory;
		this.outPackage = builder.outPackage;
		this.thingPackage = outPackage + ".thing";
		this.enumsPackage = outPackage + ".enums";
		this.actionPackage = outPackage + ".action";
		this.modellers.putAll(builder.modellers);
		this.usageFile = builder.compatFile;
		this.name = builder.name;
		this.time = builder.time;
		this.graphQl = builder.graphQl;
	}
	
	public ValueModeller value(Value value) {
		return modellers.get(value.getName());
	}
	
	/** @return A {@link ClassName} for a generated helper class. */
	public ClassName helper(Config config, String name) {
		return ClassName.get(outPackage, config.name + name);
	}
	
	/** @return A {@link ClassName} for a generated thing. */
	public ClassName thing(String name) {
		return ClassName.get(thingPackage, GenUtil.toValidClassName(name));
	}

	/** @return A {@link ClassName} for a generated thing. */
	public ClassName thing(Thing value) {
		return thing(value.getName());
	}
	
	/** @return A {@link ClassName} for a generated enum. */
	public ClassName enumm(String name) {
		return ClassName.get(enumsPackage, GenUtil.toValidClassName(name));
	}
	
	/** @return A {@link ClassName} for a generated enum. */
	public ClassName enumm(Enum value) {
		return enumm(value.getName());
	}
	
	/** @return A {@link ClassName} for a generated action. */
	public ClassName action(Action action) {
		return action(action.getName());
	}

	/** @return A {@link ClassName} for a generated action. */
	public ClassName action(String name) {
		return ClassName.get(actionPackage, GenUtil.toValidClassName(name));
	}
	
	public String actionMethodName(Action action) {
		return GenUtil.toValidFieldName(action.getName());
	}

	public ClassName syncable(Syncable definition) {
		if (definition instanceof Thing) return thing((Thing) definition);
		else if (definition instanceof Action) return action((Action) definition);
		throw new RuntimeException("Unexpected syncable type " + definition);
	}

	public ClassName interface_(Interface type) {
		return syncable((Syncable) type);
	}


	public ClassName modeller() {
		return ClassName.get(outPackage, "Modeller");
	}

	public ClassName variety(Variety variety) {
		return thing(variety.getName());
	}
}