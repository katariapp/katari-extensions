plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

@Suppress("UNCHECKED_CAST")
val configureSharedExtensionModule = rootProject.extra["configureSharedExtensionModule"] as (Project) -> Unit

configureSharedExtensionModule(project)

android {
    defaultConfig {
        versionCode = 2
        versionName = "2.1.1"
        applicationId = "eu.kanade.tachiyomi.extension.en.novelbuddy"

        manifestPlaceholders += mapOf(
            "appName" to "Katari: NovelBuddy",
            "extClass" to ".NovelBuddyFactory",
            "nsfw" to 1,
        )
    }

    sourceSets {
        getByName("test") {
            java.setSrcDirs(listOf("test"))
        }
    }
}

dependencies {
    val katariSourceApiVersion = rootProject.extra["katariSourceApiVersion"] as String

    testImplementation("com.github.katariapp.katari:entry-source-api:$katariSourceApiVersion") {
        exclude(group = "com.github.katariapp.katari", module = "core-common")
    }
    testImplementation(libs.jsoup)
    testImplementation(libs.junit)
    testImplementation(kotlin("test-junit"))
}
