plugins {
    `java-library`
    `module-info-compile`
    id("me.champeau.jmh")
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(toolLibs.errorprone.annotations)

    testImplementation(testLibs.darklaf.core)
    testImplementation(testLibs.junit.api)
    testRuntimeOnly(testLibs.junit.engine)
    testCompileOnly(libs.nullabilityAnnotations)
    testImplementation(testLibs.svgSalamander)
    testImplementation(testLibs.batik)
    testImplementation(testLibs.imageCompare)

    jmh(testLibs.svgSalamander)
    jmh(testLibs.batik)
}

tasks.test {
    doFirst {
        workingDir = File(project.rootDir, "build/ref_test").also { it.mkdirs() }
    }
    useJUnitPlatform()
}
