plugins {
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    implementation(projects.annotations)
}
