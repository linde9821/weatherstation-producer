plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.pi4j:pi4j-core:3.0.3")
    implementation("com.pi4j:pi4j-plugin-linuxfs:3.0.3")
    implementation("io.github.hap-java:hap:2.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
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
        "-XX:+UseSerialGC",
        "-XX:MaxGCPauseMillis=100",
        "-XX:GCTimeRatio=19",

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
