import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser

plugins {
    id("com.github.ben-manes.versions")
}

tasks.withType<DependencyUpdatesTask> {
    resolutionStrategy {
        componentSelection { 
            all {
                if (
                    (candidate.displayName.startsWith("com.google.crypto.tink:tink-android") && candidate.version.isNewerThan("1.2.2"))
                    || (candidate.group == "com.fasterxml.jackson.core" && candidate.version.isNewerThan("2.8.6"))
                ) {
                    reject("Need sync engine code changes, so punting for now.")
                }
                if (
                    (candidate.displayName.startsWith("commons-io:commons-io") && candidate.version.isNewerThan("2.6"))
                    || (candidate.displayName.startsWith("commons-codec:commons-codec") && candidate.version.isNewerThan("1.10"))
                    || (candidate.displayName.startsWith("org.apache.commons:commons-lang3") && candidate.version.isNewerThan("3.5"))
                    || (candidate.displayName.startsWith("org.apache.james:apache-mime4j-core") && candidate.version.isNewerThan("0.8.5"))
                ) {
                    reject("We can probably just not upgrade this. It probably won't be necessary after converting everything to kotlin.")
                }
                if (candidate.group == "org.jsoup" && candidate.version.isNewerThan("1.14.3")) {
                    reject("We use JSoup only in Listen's TTS player." +
                            "It's a minor feature with very old code," +
                            "not worth running regressions over updating this dependency.")
                }
                if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                    reject("Not stable")
                }
            }
        }
    }
    gradleReleaseChannel = "current"
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val guavaAndroidVersion = version.endsWith("-android")
    val isStable = stableKeyword || regex.matches(version) || guavaAndroidVersion
    return isStable.not()
}

fun makeVersionComparator(): Comparator<String> {
    val baseComparator = DefaultVersionComparator().asVersionComparator()
    val versionParser = VersionParser()
    return Comparator { string1, string2 ->
        baseComparator.compare(versionParser.transform(string1), versionParser.transform(string2))
    }
}

private val versionComparator = makeVersionComparator()

fun String.isNewerThan(version: String) = versionComparator.compare(this, version) >= 1
fun String.isAtLeast(version: String) = versionComparator.compare(this, version) >= 0
