import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    `module-info-compile`
    id("me.champeau.jmh")
    jacoco
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(toolLibs.errorprone.annotations)

    compileOnly(projects.annotations)
    annotationProcessor(projects.annotationsProcessor)

    testImplementation(testLibs.darklaf.core)
    testImplementation(testLibs.junit.api)
    testRuntimeOnly(testLibs.junit.engine)
    testCompileOnly(libs.nullabilityAnnotations)
    testCompileOnly(toolLibs.errorprone.annotations)
    testImplementation(testLibs.svgSalamander)
    testImplementation(testLibs.batik)
    testImplementation(testLibs.imageCompare)
    testImplementation(testLibs.sizeof)

    jmh(testLibs.svgSalamander)
    jmh(testLibs.batik)
}
tasks {

    compileTestJava {
        sourceCompatibility = JavaVersion.VERSION_21.toString()
        targetCompatibility = JavaVersion.VERSION_21.toString()
    }

    test {
        dependsOn(jar)
        doFirst {
            workingDir = File(project.rootDir, "build/ref_test").also { it.mkdirs() }
        }
        environment("RESVG_TEST_SUITE_PATH" to File(project.rootDir, "resvg-test-suite/tests").absolutePath)
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
