import java.nio.file.Files
import java.nio.file.Path
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

plugins {
    java
}

group = "io.github.leonstange"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApi = "io.papermc.paper:paper-api:26.3.build.190-stable"

dependencies {
    compileOnly(paperApi)
    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

val pluginVersion = version.toString()

tasks.processResources {
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") { expand("version" to pluginVersion) }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("BHD-CraftAttack")
}

val integration by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
}
configurations[integration.compileOnlyConfigurationName].extendsFrom(configurations.compileOnly.get())

val integrationJar by tasks.registering(Jar::class) {
    archiveFileName.set("BHD-CraftAttack-Verification.jar")
    destinationDirectory.set(layout.buildDirectory.dir("integration"))
    from(integration.output)
}

val testJava = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}
val suppliedPaper = providers.gradleProperty("paperJar")

tasks.register("integrationTest") {
    group = "verification"
    description = "Tests the plugin on an isolated local Paper 26.3 server."
    dependsOn(tasks.jar, integrationJar)
    doLast {
        val root = layout.buildDirectory.dir("integration").get().asFile.toPath()
        Files.createDirectories(root)
        val paper = if (suppliedPaper.isPresent) Path.of(suppliedPaper.get()) else root.resolve("paper.jar")
        if (!Files.exists(paper)) {
            URI("https://fill-data.papermc.io/v1/objects/db0fd32588e732fd0de57cc95a2076624b0e73c1cb2dba87dc5f5f3340bcbb1b/paper-26.3-190.jar")
                .toURL().openStream().use { Files.copy(it, paper) }
        }
        val checksum = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(paper))
            .joinToString("") { "%02x".format(it) }
        check(checksum == "db0fd32588e732fd0de57cc95a2076624b0e73c1cb2dba87dc5f5f3340bcbb1b") { "Paper SHA-256 mismatch" }
        val server = Files.createTempDirectory(root, "server-")
        Files.createDirectories(server.resolve("plugins"))
        Files.copy(tasks.jar.get().archiveFile.get().asFile.toPath(), server.resolve("plugins/BHD-CraftAttack.jar"))
        Files.copy(integrationJar.get().archiveFile.get().asFile.toPath(), server.resolve("plugins/Verification.jar"))
        Files.writeString(server.resolve("eula.txt"), "eula=true\n")
        Files.writeString(server.resolve("server.properties"),
            "server-ip=127.0.0.1\nserver-port=0\nonline-mode=true\nview-distance=2\nsimulation-distance=2\nlevel-type=minecraft:flat\ngenerator-settings={\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1}],\"biome\":\"minecraft:plains\"}\ngenerate-structures=false\n")
        val log = root.resolve("latest.log")
        val process = ProcessBuilder(testJava.get().executablePath.asFile.absolutePath,
            "-Dterminal.jline=false", "-Dterminal.ansi=false", "-Xms256m", "-Xmx1G",
            "-jar", paper.toAbsolutePath().toString(), "--nogui", "--noconsole")
            .directory(server.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start()
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroy()
            if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly()
            error("Paper test timed out. See $log")
        }
        val output = Files.readString(log)
        check(process.exitValue() == 0 && output.contains("BHD_CRAFTATTACK_TESTS_OK")
                && !output.contains("BHD_CRAFTATTACK_TEST_FAILED")) { "Paper test failed. See $log" }
        println(output.lineSequence().filter { it.contains("BHD_TEST:") || it.contains("BHD_CRAFTATTACK_TESTS_OK") }.joinToString("\n"))
        println("Paper integration log: $log")
    }
}
