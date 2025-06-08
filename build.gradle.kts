plugins {
    kotlin("jvm") version "1.9.0"
    application
}

val ktorVersion = "2.3.7" // Define Ktor version
val mockkVersion = "1.13.10" // Define MockK version
val googleApiClientVersion = "2.2.0" // Updated to a more recent version
val googleOauthClientVersion = "1.35.0" // Updated to a more recent version
val googleCalendarApiVersion = "v3-rev20240225-2.0.0" // Updated to a more recent version

group = "com.example"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson-jvm:$ktorVersion")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("com.h2database:h2:2.1.214") // Main H2 dependency

    // Google API Libraries
    implementation("com.google.api-client:google-api-client:$googleApiClientVersion")
    implementation("com.google.oauth-client:google-oauth-client-jetty:$googleOauthClientVersion")
    implementation("com.google.apis:google-api-services-calendar:$googleCalendarApiVersion")

    // Testing Dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    // testImplementation("com.h2database:h2:2.1.214") // Optionally, make H2 test-specific if needed
}

application {
    mainClass.set("com.example.taskmanager.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
}
