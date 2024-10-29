package com.pocket.sync.print.java

import com.pocket.sync.type.Syncable
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import javax.lang.model.element.Modifier

/*
 * Will add components similar to this:
 * ```
 * public static GraphQlSupport GRAPHQL = new GraphQl();
 * private static class GraphQl implements GraphQlSupport {
 *     @Nullable
 *     @Override
 *     public String operation() {
 *         return "query Name($count: Int) { field }";
 *     }
 * }
 * @Override
 * public GraphQlSupport graphQl() {
 *     return GRAPHQL;
 * }
 * ```
 */
fun addGraphQlSupport(definition: Syncable<*>, typeSpec: TypeSpec.Builder, config: Config) {
    if (!config.graphQl) return
    val supportClass = GenUtil.createInnerClassName(config.syncable(definition), "GraphQl")
    typeSpec.addSuperinterface(ClassNames.GRAPHQL_SYNCABLE)
    typeSpec.addType(TypeSpec.classBuilder(supportClass)
            .addSuperinterface(ClassNames.GRAPHQL_SUPPORT)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addMethod(operation(definition))
            .build())
    typeSpec.addField(FieldSpec.builder(ClassNames.GRAPHQL_SUPPORT, "GRAPHQL", Modifier.PUBLIC, Modifier.STATIC)
            .initializer("new \$T()", supportClass)
            .build())
    typeSpec.addMethod(MethodSpec.methodBuilder("graphQl")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(NotNull::class.java)
            .addAnnotation(Override::class.java)
            .returns(ClassNames.GRAPHQL_SUPPORT)
            .addStatement("return GRAPHQL")
            .build())
}

private fun operation(definition: Syncable<*>): MethodSpec {
    val operation = definition.operation
    return MethodSpec.methodBuilder("operation")
        .apply { 
            if (operation != null) {
                addAnnotation(AnnotationSpec.builder(Language::class.java)
                    .addMember("value", "\$S", "GraphQL")
                    .build())
            }
        }
        .addAnnotation(Nullable::class.java)
        .addAnnotation(Override::class.java)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassNames.STRING)
        .addStatement("return\$W\$S", operation)
        .build()
}
