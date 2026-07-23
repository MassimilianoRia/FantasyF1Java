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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

val javaFxVersion = "23.0.2"

dependencies {
    implementation("org.openjfx:javafx-base:$javaFxVersion:win")
    implementation("org.openjfx:javafx-graphics:$javaFxVersion:win")
    implementation("org.openjfx:javafx-controls:$javaFxVersion:win")
    implementation("com.mysql:mysql-connector-j:9.7.0")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2:2.3.232")
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

tasks.register<JavaExec>("smokeApp") {
    group = "verification"
    description = "Verifica il punto di accesso unificato senza collegarsi al database"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("it.unibo.fantasyf1.App")
    args("--smoke")
}

tasks.test {
    useJUnitPlatform()
}
