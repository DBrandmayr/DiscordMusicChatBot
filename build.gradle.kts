plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
    id("com.gradleup.shadow") version "9.3.2"
}

group = "io.github.dbrandmayr.bot"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.lavalink.dev/snapshots")
}
dependencies {
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.3")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    // Database
    val exposedVersion = "1.1.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.53.0.0")

    implementation("org.slf4j:slf4j-nop:2.0.17")

    implementation(kotlin("stdlib"))
    implementation("dev.kord:kord-core:0.18.0")
    implementation("dev.schlaubi.lavakord:kord:9.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.github.dbrandmayr.bot.BotMainKt")
}
tasks {
    shadowJar {
        archiveFileName.set("dmcbot.jar")
        destinationDirectory.set(file("build/jars"))
        archiveClassifier.set("")
        manifest {
            attributes(
                "Main-Class" to "io.github.dbrandmayr.bot.BotMainKt"
            )
        }
    }
}