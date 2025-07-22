plugins {
    `java-library`
    id("biz.aQute.bnd.builder")
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(libs.osgiAnnotations)
}

tasks {
    jar {
        bundle {
            bnd(
                bndFile(
                    moduleName = "com.github.weisj.jsvg.annotations",
                    requiredModules =
                        listOf(
                            Requires("org.jetbrains.annotations", static = true),
                            Requires("org.osgi.annotation.bundle", static = true),
                        ),
                ),
            )
        }
    }
}
