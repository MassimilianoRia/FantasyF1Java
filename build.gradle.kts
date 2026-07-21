plugins {
    java
    application
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

dependencies {
    implementation("org.openjfx:javafx-base:$javaFxVersion:win")
    implementation("org.openjfx:javafx-graphics:$javaFxVersion:win")
    implementation("org.openjfx:javafx-controls:$javaFxVersion:win")
    implementation("com.mysql:mysql-connector-j:9.7.0")
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
