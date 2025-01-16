plugins {
    `java-library`
    id("biz.aQute.bnd.builder")
}

dependencies {
    compileOnly(libs.nullabilityAnnotations)
    implementation(projects.annotations)
}

tasks {
    jar {
        bundle {
            bnd(
                """
                Bundle-SymbolicName: com.github.weisj.jsvg.annotations.processor

                -jpms-module-info:
                -removeheaders: Private-Package,Tool
            """,
            )
        }
    }
}
