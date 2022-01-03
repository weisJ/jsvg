enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

rootProject.name = "jsvg-root"

pluginManagement {
    plugins {
        fun String.v() = extra["$this.version"].toString()
        fun idv(id: String, key: String = id) = id(id) version key.v()
        idv("com.diffplug.spotless")
        idv("com.github.vlsi.crlf", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.gradle-extensions", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.license-gather", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.stage-vote-release", "com.github.vlsi.vlsi-release-plugins")
        idv("net.ltgt.errorprone")
        idv("me.champeau.jmh")
        idv("org.sonarqube")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        fun VersionCatalogBuilder.idv(name: String, coordinates: String, versionRef: String = name) {
            val parts = coordinates.split(':', limit = 2)
            alias(name).to(parts[0], parts[1]).version(extra["$versionRef.version"].toString())
        }
        class VersionBundle(val bundleName: String, val builder: VersionCatalogBuilder) {
            val libs = mutableListOf<String>()
            fun idv(name: String, coordinates: String, versionRef: String = bundleName) =
                builder.idv("$bundleName-$name".also { libs.add(it) }, coordinates, versionRef)
        }
        fun VersionCatalogBuilder.bundle(name: String, init: VersionBundle.() -> Unit) = VersionBundle(name, this).run {
            init()
            bundle(name, libs)
        }

        create("libs") {
            idv("javaxAnnotations", "javax.annotation:javax.annotation-api")
            idv("nullabilityAnnotations", "org.jetbrains:annotations")
        }
        create("testLibs") {
            bundle("junit") {
                idv("api", "org.junit.jupiter:junit-jupiter-api")
                idv("engine", "org.junit.jupiter:junit-jupiter-engine")
            }
            bundle("darklaf") {
                idv("core", "com.github.weisj:darklaf-core", "darklaf")
            }
            bundle("swingExtensions") {
                idv("dsl", "com.github.weisj:swing-extensions-dsl")
            }
            idv("svgSalamander", "com.formdev:svgSalamander")
            idv("batik", "org.apache.xmlgraphics:batik-all")
            idv("imageCompare", "com.github.romankh3:image-comparison")
        }
        create("toolLibs") {
            bundle("errorprone") {
                idv("core", "com.google.errorprone:error_prone_core")
                idv("annotations", "com.google.errorprone:error_prone_annotations")
                idv("javac", "com.google.errorprone:javac", "errorprone.compiler")
                idv("guava", "com.google.guava:guava-beta-checker", "guava")
            }
            bundle("autoservice") {
                idv("annotations", "com.google.auto.service:auto-service-annotations")
                idv("processor", "com.google.auto.service:auto-service")
            }
        }
    }
}

include(
    "jsvg"
)
