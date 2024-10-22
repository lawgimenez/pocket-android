plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaConfigs.javaVersion
    targetCompatibility = JavaConfigs.javaVersion
}

kotlin {
    setDefaultConfigs()
}
