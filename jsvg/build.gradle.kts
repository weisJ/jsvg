import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    `java-test-fixtures`
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

    testFixturesCompileOnly(libs.nullabilityAnnotations)
    testFixturesCompileOnly(toolLibs.errorprone.annotations)
    testFixturesImplementation(testLibs.junit.api)
    testFixturesImplementation(testLibs.bundles.batik)
    testFixturesImplementation(testLibs.imageCompare)

    testImplementation(testLibs.darklaf.core)
    testImplementation(testLibs.junit.api)
    testImplementation(testLibs.bundles.batik)
    testImplementation(testLibs.svgSalamander)
    testImplementation(testLibs.sizeof)
    testImplementation(gradleApi())

    testRuntimeOnly(testLibs.junit.engine)

    testCompileOnly(libs.nullabilityAnnotations)
    testCompileOnly(toolLibs.errorprone.annotations)
}

components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
    withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}

tasks {

    compileTestJava {
        options.release.set(21)
    }

    compileTestFixturesJava {
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
        environment(
            "W3C_SVG_11_TEST_SUITE_PATH" to
                rootProject.file("w3c-svg-11-test-suite/W3C_SVG_11_TestSuite").absolutePath,
        )
        // Track optional fixtures so initializing or removing a submodule reruns the tests.
        inputs.files(
            rootProject.fileTree("resvg-test-suite") {
                include("tests/**", "fonts/**")
            },
            rootProject.fileTree("w3c-svg-11-test-suite/W3C_SVG_11_TestSuite") {
                include("svg/**", "png/**", "images/**", "resources/**")
            },
        )
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    register<JavaExec>("resvgTestAudit") {
        group = "verification"
        description = "Reports excluded resvg reference tests that now pass."
        dependsOn(testClasses, jar)
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("com.github.weisj.jsvg.ReSvgTestAudit")
        environment("RESVG_TEST_SUITE_PATH", rootProject.file("resvg-test-suite/tests").absolutePath)
        val reportDirectory = layout.buildDirectory.dir("reports/resvg-audit")
        systemProperty("resvg.audit.reportDir", reportDirectory.get().asFile.absolutePath)
        doFirst {
            delete(reportDirectory)
        }
    }

    register<JavaExec>("SVGViewer") {
        group = "application"
        description = "Runs the SVG Viewer application."
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("com.github.weisj.jsvg.SVGViewer")
    }
}
