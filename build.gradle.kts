import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

val configureSharedExtensionModule = { project: Project ->
    val katariSourceApiVersion = project.providers.gradleProperty("katariSourceApiVersion")
        .orElse("local-SNAPSHOT")
        .get()
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

    project.extensions.configure<ApplicationExtension> {
        namespace = "eu.kanade.tachiyomi.extension"
        compileSdk = 36

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        defaultConfig {
            minSdk = 26
            targetSdk = 36
        }

        sourceSets {
            getByName("main") {
                manifest.srcFile("AndroidManifest.xml")
                java.setSrcDirs(listOf("src"))
                kotlin.setSrcDirs(listOf("src"))
                res.setSrcDirs(listOf("res"))
            }
        }

        buildTypes {
            getByName("debug") {
                isMinifyEnabled = false
            }
            getByName("release") {
                isMinifyEnabled = false
            }
        }

        packaging {
            resources {
                excludes += "kotlin-tooling-metadata.json"
            }
        }
    }

    project.extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    project.dependencies.add(
        "compileOnly",
        "com.github.katariapp.katari:entry-source-api:$katariSourceApiVersion",
    )
    project.dependencies.add("compileOnly", libs.findLibrary("jspecify").get().get())
}

extra["configureSharedExtensionModule"] = configureSharedExtensionModule
