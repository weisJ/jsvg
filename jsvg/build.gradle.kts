import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    jacoco
    id("biz.aQute.bnd.builder")
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(toolLibs.errorprone.annotations)
    compileOnly(projects.annotations)
    compileOnly(libs.bndAnnotations)
    compileOnly(libs.osgiAnnotations)

    annotationProcessor(projects.annotationsProcessor)

    testImplementation(testLibs.darklaf.core)
    testImplementation(testLibs.junit.api)
    testImplementation(testLibs.svgSalamander)
    testImplementation(testLibs.batik)
    testImplementation(testLibs.imageCompare)
    testImplementation(testLibs.sizeof)
    testImplementation(gradleApi())

    testRuntimeOnly(testLibs.junit.engine)

    testCompileOnly(libs.nullabilityAnnotations)
    testCompileOnly(toolLibs.errorprone.annotations)
}
tasks {

    compileTestJava {
        options.release.set(21)
    }

    jar {
        bundle {
            bnd(
                bndFile(
                    moduleName = "com.github.weisj.jsvg",
                    requiredModules =
                        listOf(
                            Requires("com.google.errorprone.annotations", static = true),
                            Requires("org.jetbrains.annotations", static = true),
                            Requires("com.github.weisj.jsvg.annotations", static = true),
                            Requires("org.osgi.annotation.bundle", static = true),
                            Requires("biz.aQute.bndlib", static = true),
                        ),
                ),
            )
        }
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
