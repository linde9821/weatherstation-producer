plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.pi4j.core)
    implementation(libs.pi4j.plugin.linuxfs)
    implementation(libs.hap.java)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.simple)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "org.example.AppKt"

    // JVM options optimized for Raspberry Pi 3
    applicationDefaultJvmArgs = listOf(
        // Memory settings
        "-Xmx128m",
        "-Xms64m",

        // Garbage Collection
        "-XX:+UseG1GC",

        // JIT Compilation
        "-XX:+TieredCompilation",
        "-XX:TieredStopAtLevel=1",

        // String optimization
        "-XX:+UseStringDeduplication",
        "-XX:StringDeduplicationAgeThreshold=3",
        "-Djava.awt.headless=true",
        "-Xverify:none",  // Skip bytecode verification (faster, slightly less safe)

        // Network and system
        "-Djava.net.preferIPv4Stack=true",
        "-Djava.awt.headless=true",
        "-Dfile.encoding=UTF-8"
    )
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
