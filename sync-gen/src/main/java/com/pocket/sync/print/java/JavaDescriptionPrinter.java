package com.pocket.sync.print.java;

import com.pocket.sync.type.Action;
import com.pocket.sync.type.Auth;
import com.pocket.sync.type.Description;
import com.pocket.sync.type.Enum;
import com.pocket.sync.type.Feature;
import com.pocket.sync.type.Link;
import com.pocket.sync.type.Remote;
import com.pocket.sync.type.Segment;
import com.pocket.sync.type.Slice;
import com.pocket.sync.type.Thing;
import com.pocket.sync.type.Value;
import com.pocket.sync.type.Variety;
import com.pocket.sync.type.path.Reference;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

/**
 * Helper for printing java docs from {@link Description}s
 */
public class JavaDescriptionPrinter {

	public static CodeBlock print(Description description, Config config) {
		final CodeBlock.Builder javadoc = CodeBlock.builder();
		for (Segment segment : description.getAll()) {
			int substringStart = 0;
			for (Link link : segment.getLinks()) {
				Reference typed = link.getReference().getCurrent();

				javadoc.add("$L", segment.getText().substring(substringStart, link.getStart())
						.replace("\n\n", "\n<p>\n"));
				if (typed.getType() instanceof Value) {
					javadoc.add("{@link $T}", config.value(((Value) typed.getType())).java());
					
				} else if (typed.getType() instanceof Enum) {
					ClassName clazz = config.enumm((Enum) typed.getType());
					if (typed.getPath() != null && typed.getPath().getEnum() != null) {
						javadoc.add("{@link $T#" + EnumGenerator.valueFieldName(typed.getPath().getEnum()) + "}", clazz);
					} else {
						javadoc.add("{@link $T}", clazz);
					}

				} else if (typed.getType() instanceof Feature || typed.getType() instanceof Auth || typed.getType() instanceof Remote || typed.getType() instanceof Slice) {
					// We could create empty classes just to have documentation on them? But for now ignore.
					javadoc.add("{$L}", typed);
					
				} else {
					final ClassName clazz;
					if (typed.getType() instanceof Action) {
						clazz = config.action((Action) typed.getType());
					} else if (typed.getType() instanceof Variety) {
						clazz = config.variety((Variety) typed.getType());
					} else {
						clazz = config.thing((Thing) typed.getType());
					}
					
					if (typed.getPath() == null || typed.getPath().getSyncable().isEmpty()) {
						javadoc.add("{@link $T}", clazz);
						
					} else if (typed.getPath().getSyncable().size() == 1) {
						javadoc.add("{@link $T#" + GenUtil.toValidFieldName(typed.getPath().getSyncable().get(0).getField()) + "}", clazz);
						
					} else {
						// We can't do deep links in java, so just leave reference as is as text.
						javadoc.add("{$L}", typed);
					}
				}
				substringStart = link.getEnd();
			}
			if (substringStart < segment.getText().length()) {
				String content = segment.getText().substring(substringStart)
						.replace("\n\n", "\n<p>\n");
				javadoc.add("$L", content);
			}
			if (segment.getText().length() == 0) javadoc.add("<p>");
			javadoc.add("\n");
		}
		return javadoc.build();
	}
}
