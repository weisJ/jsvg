import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    jacoco
    id("biz.aQute.bnd.builder")
    id("org.openjfx.javafxplugin")
}

javafx {
    version = rootProject.extra["javafx.version"].toString()
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    compileOnly(projects.jsvg)
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(toolLibs.errorprone.annotations)
    compileOnly(libs.osgiAnnotations)

    testImplementation(projects.jsvg)
    testImplementation(testLibs.junit.api)
    testImplementation(testLibs.imageCompare)
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
                    moduleName = "com.github.weisj.jsvg.javafx",
                    requiredModules =
                        listOf(
                            Requires("com.github.weisj.jsvg"),
                            Requires("org.jetbrains.annotations", static = true),
                            Requires("com.google.errorprone.annotations", static = true),
                            Requires("org.osgi.annotation.bundle", static = true),
                        ),
                ),
            )
        }
    }

    withType<JavaExec> {
        environment("JAVAFX_TEST_SVG_PATH" to File(project.rootDir, "jsvg/src/test/resources").absolutePath)
    }

    test {
        dependsOn(jar)
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
