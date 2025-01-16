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
                """
                Bundle-SymbolicName: com.github.weisj.jsvg.annotations
                -exportcontents: \
                  com.github.weisj.jsvg.annoations

                -jpms-module-info:
                -removeheaders: Private-Package,Tool
            """,
            )
        }
    }
}
