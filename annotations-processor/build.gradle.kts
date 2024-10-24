plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    implementation(projects.annotations)
}
