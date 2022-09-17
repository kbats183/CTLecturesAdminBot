java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withSourcesJar()
}

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    application
}

group "ru.kbats.youtube.broadcastscheduler"
version = "0.1"

application {
    mainClass.set("ru.kbats.youtube.broadcastscheduler.MainKt")
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(
            listOf("compileJava", "compileKotlin", "processResources")
        ) // We need this for Gradle optimization to work
//        archiveClassifier.set("standalone") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}
//kotlin {
//    sourceSets {
//        all {
//            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
//            languageSettings.optIn("kotlinx.coroutines.FlowPreview")
//            languageSettings.optIn("kotlin.RequiresOptIn")
//        }
//    }
//}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.0.7")
    implementation("org.litote.kmongo:kmongo-coroutine:4.7.1")
    compile("com.google.api-client:google-api-client:1.31.5")
    compile("com.google.apis:google-api-services-youtube:v3-rev182-1.22.0")
    compile("com.google.http-client:google-http-client-jackson2:1.20.0")
    compile("com.google.oauth-client:google-oauth-client-jetty:1.20.0")
//    implementation("org.apache.xmlgraphics:xmlgraphics-commons:2.7")
//    implementation("org.apache.xmlgraphics:batik-transcoder:1.14")
//    implementation("org.apache.xmlgraphics:batik-codec:1.14")
}
