package utils

import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.gradle.api.Project
import java.util.*

fun getGitSha(): String =
    ProcessGroovyMethods.getText(
        ProcessGroovyMethods.execute("git rev-parse --short HEAD")
    ).trim()

/**
 * Get a secret value.
 *
 * First checks environment variables, then checks secrets.properties file.
 */
fun Project.getSecret(key: String): String {
    val envValue = System.getenv(key)
    if (envValue != null) return envValue

    val secretProperties = Properties()
    try {
        secretProperties.load(
            ResourceGroovyMethods.newDataInputStream(
                rootProject.file("secrets/secret.properties")
            )
        )
    } catch (e: Exception) {
        throw RuntimeException("Missing secrets. Run ./secrets/decrypt.sh", e)
    }

    val propertiesValue = secretProperties.getProperty(key)
    if (propertiesValue != null) return secretProperties.getProperty(key)

    throw RuntimeException("Missing $key from secret.properties. Maybe you need to re-run ./secrets/decrypt.sh to refresh the secrets?")
}
