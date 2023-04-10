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
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }

    test {
        doFirst {
            workingDir = File(project.rootDir, "build/ref_test").also { it.mkdirs() }
        }
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
