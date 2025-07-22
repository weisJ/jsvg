plugins {
    `java-library`
    id("biz.aQute.bnd.builder")
}

dependencies {
    compileOnly(projects.jsvg)
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(libs.osgiAnnotations)
    compileOnly(libs.bndAnnotations)
    compileOnly(libs.slf4jApi)
}

tasks {
    jar {
        bundle {
            bnd(
                bndFile(
                    moduleName = "com.github.weisj.jsvg.logging.slf4j",
                    requiredModules =
                        listOf(
                            Requires("com.github.weisj.jsvg", static = true),
                            Requires("org.slf4j", static = true),
                            Requires("org.jetbrains.annotations", static = true),
                            Requires("org.osgi.annotation.bundle", static = true),
                            Requires("biz.aQute.bndlib", static = true),
                        ),
                ),
            )
        }
    }
}
