import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions

object AndroidConfigs {
    const val CompileSdkVersion = 35
    const val MinSdkVersion = 23
    const val TargetSdkVersion = 34
}

object KotlinConfigs {
    val jvmTarget : JvmTarget = JvmTarget.JVM_11
    const val FreeCompilerArgs = "-Xjvm-default=all"
}

object JavaConfigs {
    val javaVersion = JavaVersion.VERSION_11
}

/**
 * Set android configurations for a [BaseExtension].
 * [BaseExtension] is a parent class of [LibraryExtension] and [BaseAppModuleExtension] so this
 * method can be used within an 'android {}' block from an android app and an android library.
 */
fun BaseExtension.setDefaultConfigs() {
    compileSdkVersion(AndroidConfigs.CompileSdkVersion)
    defaultConfig {
        minSdk = AndroidConfigs.MinSdkVersion
        targetSdk = AndroidConfigs.TargetSdkVersion
    }
    compileOptions {
        sourceCompatibility = JavaConfigs.javaVersion
        targetCompatibility = JavaConfigs.javaVersion
    }
}

fun HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions>.setDefaultConfigs() {
    compilerOptions {
        jvmTarget.set(KotlinConfigs.jvmTarget)
        freeCompilerArgs.add(KotlinConfigs.FreeCompilerArgs)
    }
}
