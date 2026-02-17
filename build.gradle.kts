plugins {
    java
}

group = "ascendant.core"
version = "1.1.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Project ID: 1431415 HyUI
    // Project ID: 1414678 LevelingCore
    // Project ID: 1431313 MMOSkillTree
    // Project ID: 1441945 Elite Mobs
    implementation(project.fileTree("libs") { include("*.jar") })
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.register<Copy>("installModJar") {
    dependsOn(tasks.named("jar"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })

    into(file("A:/Spiele/Hytale/UserData/Saves/Test/mods"))
}

tasks.named("build") {
    dependsOn("installModJar")
}
