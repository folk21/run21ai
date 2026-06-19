plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ai.run21.runtime.restapi.RestApiServerKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":runtime:local"))
    implementation(project(":tools:formula"))

    implementation("io.ktor:ktor-server-core-jvm:3.1.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.1.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.1.3")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.1.3")
    implementation("io.ktor:ktor-serialization-jackson-jvm:3.1.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.1.3")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:3.1.3")
    testImplementation("io.ktor:ktor-serialization-jackson-jvm:3.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
