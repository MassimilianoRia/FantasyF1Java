plugins {
    java
    application
    id("com.gradleup.shadow") version "9.6.0"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val javaFxVersion = "23.0.2"
val javaFXModules = listOf("base", "controls", "fxml", "graphics")
val supportedPlatforms = listOf("win")

dependencies {
    implementation("org.openjfx:javafx:$javaFxVersion")

    for (platform in supportedPlatforms) {
        for (module in javaFXModules) {
            implementation(
                "org.openjfx:javafx-$module:$javaFxVersion:$platform"
            )
        }
    }

    implementation("com.mysql:mysql-connector-j:9.7.0")

    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("it.unibo.fantasyf1.App")
}

tasks.register<JavaExec>("checkDatabaseConnection") {
    group = "verification"
    description = "Verifica la connessione JDBC al database fantasy_f1"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("it.unibo.fantasyf1.DatabaseConnectionCheck")
}
