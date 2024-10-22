package com.pocket.sync.print.java

import com.pocket.sync.type.Variety
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class VarietyGenerator(private val definition: Variety, private val config: Config) {
    fun create(): TypeSpec {
        val type = config.variety(definition)
        val builder = TypeSpec.interfaceBuilder(type)
            .addSuperinterface(ClassNames.THING)
            .addSuperinterface(ClassNames.VARIETY)
            .addModifiers(Modifier.PUBLIC)
        OpenTypes.setupForVariety(builder, definition, config)
        return builder.build()
    }
}
