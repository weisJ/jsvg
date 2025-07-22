plugins {
    `java-library`
    id("biz.aQute.bnd.builder")
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    compileOnly(libs.osgiAnnotations)
    implementation(projects.annotations)
}

tasks {
    jar {
        bundle {
            bnd(
                bndFile(
                    moduleName = "com.github.weisj.jsvg.annotations.processor",
                    requiredModules =
                        listOf(
                            Requires("com.github.weisj.jsvg.annotations"),
                            Requires("org.jetbrains.annotations", static = true),
                            Requires("org.osgi.annotation.bundle", static = true),
                        ),
                ),
            )
        }
    }
}
