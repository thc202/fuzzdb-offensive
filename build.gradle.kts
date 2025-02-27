import org.zaproxy.gradle.addon.AddOnStatus
import org.zaproxy.gradle.addon.internal.model.ProjectInfo
import org.zaproxy.gradle.addon.internal.model.ReleaseState
import org.zaproxy.gradle.addon.internal.tasks.GenerateReleaseStateLastCommit
import org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml

plugins {
    `java-library`
    eclipse
    id("com.diffplug.spotless")
    id("org.zaproxy.common")
    id("org.zaproxy.add-on") version "0.13.1"
    id("org.zaproxy.crowdin") version "0.6.0"
}

eclipse {
    classpath {
        // Prevent compilation of zapHomeFiles.
        sourceSets = listOf()
    }
}

description = (
    "FuzzDB web backdoors and attack files which can be used with the ZAP fuzzer or for manual penetration testing - contains files " +
        "that may well be flagged by anti-virus tools"
)

java {
    val javaVersion = JavaVersion.VERSION_17
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

zapAddOn {
    addOnId.set(project.name.replace("-", ""))
    addOnName.set("FuzzDB Offensive")
    addOnStatus.set(AddOnStatus.RELEASE)
    zapVersion.set("2.16.0")

    releaseLink.set("https://github.com/zaproxy/fuzzdb-offensive/compare/v@PREVIOUS_VERSION@...v@CURRENT_VERSION@")
    unreleasedLink.set("https://github.com/zaproxy/fuzzdb-offensive/compare/v@CURRENT_VERSION@...HEAD")

    manifest {
        author.set("ZAP Dev Team")
        url.set("https://www.zaproxy.org/docs/desktop/addons/fuzzdb-offensive/")
        repo.set("https://github.com/zaproxy/fuzzdb-offensive/")
        changesFile.set(tasks.named<ConvertMarkdownToHtml>("generateManifestChanges").flatMap { it.html })

        helpSet {
            baseName.set("help%LC%.helpset")
            localeToken.set("%LC%")
        }
    }
}

crowdin {
    credentials {
        token.set(System.getenv("CROWDIN_AUTH_TOKEN"))
    }

    configuration {
        file.set(file("gradle/crowdin.yml"))
        tokens.set(mutableMapOf("%addOnId%" to zapAddOn.addOnId.get()))
    }
}

spotless {
    kotlinGradle {
        ktlint()
    }
}

val projectInfo = ProjectInfo.from(project)
val generateReleaseStateLastCommit by tasks.registering(GenerateReleaseStateLastCommit::class) {
    projects.set(listOf(projectInfo))
}

val releaseAddOn by tasks.registering {
    if (ReleaseState.read(projectInfo).isNewRelease()) {
        dependsOn("createRelease")
        dependsOn("handleRelease")
        dependsOn("createPullRequestNextDevIter")
        dependsOn("crowdinUploadSourceFiles")
    }
}
