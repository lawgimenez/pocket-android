package utils

import com.android.build.api.dsl.BaseFlavor

fun BaseFlavor.buildStringField(name: String, value: String) {
    buildConfigField("String", name, "\"$value\"")
}

fun BaseFlavor.buildBooleanField(name: String, value: Boolean) {
    buildConfigField("boolean", name, if (value) "true" else "false")
}

fun BaseFlavor.resString(name: String, value: String) {
    resValue("string", name, value)
}