plugins {
    `java-library`
    id("biz.aQute.bnd.builder")
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
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
                        ),
                    exports =
                        listOf(
                            "com.github.weisj.jsvg.annotations",
                        ),
                ),
            )
        }
    }
}
