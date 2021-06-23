val externalLibraryVersion: String? by extra
val executionServiceVersion: String? by extra

val junitVersion: String? by extra
val hamkrestVersion: String? by extra
val mockkVersion: String? by extra
val guiceVersion: String? by extra

dependencies {
    implementation("il.ac.technion.cs.softwaredesign", "primitive-storage-layer", externalLibraryVersion)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("com.google.inject", "guice", guiceVersion)

    testImplementation("com.natpryce", "hamkrest", hamkrestVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testImplementation("io.mockk", "mockk", mockkVersion)
}