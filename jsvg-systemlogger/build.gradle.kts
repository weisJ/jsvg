plugins {
    `java-library`
    id("biz.aQute.bnd.builder")
}

dependencies {
    compileOnly(projects.jsvg)
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(libs.osgiAnnotations)
    compileOnly(libs.bndAnnotations)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(9)
    }
    jar {
        bundle {
            bnd(
                bndFile(
                    moduleName = "com.github.weisj.jsvg.logging.systemlogger",
                    requiredModules =
                        listOf(
                            Requires("com.github.weisj.jsvg", static = true),
                            Requires("org.jetbrains.annotations", static = true),
                            Requires("org.osgi.annotation.bundle", static = true),
                            Requires("biz.aQute.bndlib", static = true),
                        ),
                ),
            )
        }
    }
}
