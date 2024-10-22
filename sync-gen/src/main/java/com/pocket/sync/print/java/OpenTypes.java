package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.CollectionType;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.Interface;
import com.pocket.sync.type.OpenType;
import com.pocket.sync.type.Syncable;
import com.pocket.sync.type.Thing;
import com.pocket.sync.type.ThingInterface;
import com.pocket.sync.type.Variety;
import com.pocket.sync.type.VarietyType;
import com.pocket.sync.usage.UsageMode;
import com.pocket.sync.usage.UsageModeCalculator;
import com.pocket.sync.util.FigmentUtilsKt;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

/**
 * Helper for generating all of the structures needed to support open types in code generation.
 */
public class OpenTypes {
    
    private static Set<Variety> uniqueVarieties;

    /**
     * Invoke this for each field you generate for a Thing or Action.
     * If needed, it will make sure the interface methods, variety classes, etc are setup for it.
     * @param parentClass The class/type builder of the thing or action the field is in
     * @param parent The thing or action the field is in
     */
    public static void setupForField(TypeSpec.Builder parentClass, Syncable parent, Field field, Config config) {
        // If the parent extends an interface and this field is part of an interface,
        // add the override method that delegates to the field variable.
        if (parent.getFields().getInterfaced().contains(field)) {
            parentClass.addMethod(InterfaceGenerator.fieldMethod(field, config)
                    .addAnnotation(Override.class)
                    .addStatement("return $N", GenUtil.toValidFieldName(field))
                    .build());
        }
    }

    /**
     * Returns a set of varieties that represent all known, and used varieties.
     * This can be used to generate all of the variety classes that will be needed.
     * Can ignore the field/context on these varieties, and just focus on the options.
     */
    public static Set<Variety> uniqueVarieties(Figments figments, UsageModeCalculator mode) {
        return figments.syncables()
                .stream()
                .filter(definition -> mode.mode(definition) != UsageMode.SKIP) // keep only used syncables
                .flatMap(syncable -> syncable.getFields().getAll().stream()) // get all fields
                .filter(field -> mode.mode(field) != UsageMode.SKIP) // keep only used fields
                .map(Field::getType) // get field types
                .map(type -> type instanceof CollectionType ? ((CollectionType) type).getInner() : type) // get inner types from lists and maps
                .filter(VarietyType.class::isInstance).map(VarietyType.class::cast) // keep only variety usages
                .map(VarietyType::getDefinition) // get the original declarations
                .collect(Collectors.toSet()); // collecting to a set removes duplicates
    }

    private static Collection<Variety> uniqueVarietiesIncluding(Thing thing,
            UsageModeCalculator mode) {
        Set<Variety> varieties = uniqueVarieties(thing.getSchema(), mode);
        varieties.retainAll(FigmentUtilsKt.varietiesIncluding(thing));
        return varieties;
    }

    /**
     * Invoke this for each thing/action you generate a class for.
     * This will make sure it extends any base actions / interfaces / varieties / etc. as needed.
     *
     * @param typeSpec The class you are generating
     * @param of The thing or action the class is for
     */
    public static <I extends Interface> void setupForSyncable(TypeSpec.Builder typeSpec, Config config, Syncable<I> of) {
        // Add "implements" for any interfaces this thing or action extends.
        for (I i : of.getInterfaces()) {
            typeSpec.addSuperinterface(config.syncable((Syncable<I>) i));
        }

        // If this thing is used in any varieties anywhere, also have it implement that variety
        // This is down here so they are added at the end of the interface list
        if (of instanceof Thing) {
            for (Variety variety : uniqueVarietiesIncluding((Thing) of, config.mode)) {
                typeSpec.addSuperinterface(config.variety(variety));
            }
        }
    }

    /**
     * Invoke this for every class you generate representing an Interface
     * This will make sure it has a a parser/creator static instance.
     * @param into The class being generated
     * @param type The interface it is for
     */
    public static void setupForInterface(TypeSpec.Builder into, Interface type, Config config) {
        if (type instanceof ThingInterface) {
            setupForOpenClass(into,
                    config.interface_(type),
                    creatorFieldName(type),
                    ((ThingInterface) type).getUnknown().getName(),
                    config);
        }
    }

    /**
     * Invoke this for every class you generate representing a Variety
     * This will make sure it has a a parser/creator static instance.
     * @param into The class being generated
     * @param type The variety it is for
     */
    public static void setupForVariety(TypeSpec.Builder into, Variety type, Config config) {
        setupForOpenClass(into,
                config.variety(type),
                creatorFieldName(type),
                type.getUnknown().getName(),
                config);
    }

    /**
     * Invoke this for every class you generate representing an open type, such as an interface or a variety.
     * This will make sure it has a a parser/creator static instance.
     * @param into The class being generated
     * @param typename The open type it is for
     * @param creatorName {@link #creatorFieldName(Interface)}
     */
    private static void setupForOpenClass(TypeSpec.Builder into, TypeName typename, String creatorName, String unknownName, Config config) {
        ParameterizedTypeName creatorType = ParameterizedTypeName.get(ClassNames.OPEN_CREATOR, typename);
        into.addField(FieldSpec.builder(creatorType, creatorName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $1T($2T.THINGS, $2T.OBJECT_MAPPER, $3S)", creatorType, config.modeller(), unknownName)
                .build());
    }

    /**
     * Use this to get a piece of code that references the parser/creator for this open type.
     */
    public static CodeBlock creatorReference(OpenType type, Config config) {
        return WhenType.is(type, CodeBlock.class)
                .variety(v -> CodeBlock.builder().add("$T.$N", config.variety(v), creatorFieldName(v)).build())
                .interface_(i -> CodeBlock.builder().add("$T.$N", config.interface_(i), creatorFieldName(i)).build())
                .otherwiseFail();
    }

    /**
     * A name that can be used for the creator/parser static field.
     * It has to be specific to the type, it can't be generic like we do in normal things/actions.
     * Otherwise if something implements multiple interfaces that all have the same but conflicting static constant field name, it won't compile.
     */
    private static String creatorFieldName(Interface type) {
        return StringUtils.upperCase(GenUtil.toValidFieldName("INTERFACE_" + type.getName() + "_CREATOR"));
    }

    private static String creatorFieldName(Variety variety) {
        return StringUtils.upperCase(GenUtil.toValidFieldName("VARIETY_" + variety.getName() + "_CREATOR"));
    }
}