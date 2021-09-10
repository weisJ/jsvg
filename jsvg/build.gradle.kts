plugins {
    `java-library`
    id("me.champeau.jmh")
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(libs.tools.errorprone.annotations)

    testImplementation(libs.test.darklaf.core)
    testImplementation(libs.test.junit.api)
    testRuntimeOnly(libs.test.junit.engine)
    testCompileOnly(libs.nullabilityAnnotations)
    testImplementation(libs.test.svgSalamander)
    testImplementation(libs.test.batik)
    testImplementation(libs.test.imageCompare)

    jmh(libs.test.svgSalamander)
    jmh(libs.test.batik)
}

tasks.test {
    doFirst {
        workingDir = File(project.rootDir, "build/ref_test")
        workingDir.mkdirs()
    }
    useJUnitPlatform()
}
