import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    jacoco
    id("biz.aQute.bnd.builder")
    id("org.openjfx.javafxplugin")
}

javafx {
    version = properties["javafx.version"].toString()
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    compileOnly(projects.jsvg)
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(toolLibs.errorprone.annotations)

    testImplementation(projects.jsvg)
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
                """
                Bundle-SymbolicName: com.github.weisj.jsvg-javafx
                -exportcontents: \
                  com.github.weisj.jsvg,\
                  com.github.weisj.jsvg.renderer,\
                  com.github.weisj.jsvg.renderer.jfx,\
                  com.github.weisj.jsvg.ui,\
                  com.github.weisj.jsvg.ui.jfx,\

                Import-Package: !com.google.errorprone.annotations,\
                  *

                -jpms-module-info:
                -jpms-module-info-options: \
                  com.google.errorprone.annotations;static="true";transitive="false",\
                  org.jetbrains.annotations;static="true";transitive="false",\
                  com.github.weisj.jsvg.annotations;static="true";transitive="false",

                -removeheaders: Private-Package,Tool
            """,
            )
        }
    }

    test {
        dependsOn(jar)
        doFirst {
            workingDir = File(project.rootDir, "build/ref_test").also { it.mkdirs() }
        }
        environment("JAVAFX_TEST_SVG_PATH" to File(project.rootDir, "jsvg/src/test/resources").absolutePath)
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
