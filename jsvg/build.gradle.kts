import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    `module-info-compile`
    id("me.champeau.jmh")
    jacoco
    id("biz.aQute.bnd.builder")
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

    jar {
        bundle {
            bnd(
                """
                Bundle-SymbolicName: com.github.weisj.jsvg
                -exportcontents: \
                  com.github.weisj.jsvg,\
                  com.github.weisj.jsvg.animation,\
                  com.github.weisj.jsvg.attributes,\
                  com.github.weisj.jsvg.attributes.font,\
                  com.github.weisj.jsvg.attributes.paint,\
                  com.github.weisj.jsvg.geometry.size,\
                  com.github.weisj.jsvg.nodes,\
                  com.github.weisj.jsvg.parser,\
                  com.github.weisj.jsvg.parser.css,\
                  com.github.weisj.jsvg.parser.resources,\
                  com.github.weisj.jsvg.renderer,\
                  com.github.weisj.jsvg.renderer.awt,\
                  com.github.weisj.jsvg.ui,\
                
                Import-Package: !com.google.errorprone.annotations,\
                  *
                
                -jpms-module-info
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
