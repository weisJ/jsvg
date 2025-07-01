import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.properties.dsl.props
import com.github.vlsi.gradle.properties.dsl.stringProperty
import com.github.vlsi.gradle.properties.dsl.toBool
import com.github.vlsi.gradle.publishing.dsl.simplifyXml
import com.github.vlsi.gradle.publishing.dsl.versionFromResolution
import net.ltgt.gradle.errorprone.errorprone
import nmcp.NmcpExtension
import org.gradle.kotlin.dsl.provideDelegate
import java.time.Duration

plugins {
    idea
    id("org.sonarqube")
    id("com.diffplug.spotless")
    id("com.github.vlsi.crlf")
    id("com.github.vlsi.gradle-extensions")
    id("com.gradleup.nmcp") apply false
    id("net.ltgt.errorprone") apply false
}

val skipJavadoc by props()
val enableMavenLocal by props(false)
val enableGradleMetadata by props()
val enableErrorProne by props()
val skipSpotless by props(false)
val isRelease = props.bool(name = "release", default = false)
val snapshotName by props("")

if (isRelease && !JavaVersion.current().isJava9Compatible) {
    throw GradleException("Java 9 compatible compiler is needed for release builds")
}

val String.v: String get() = rootProject.extra["$this.version"] as String
val projectVersion = "jsvg".v

val snapshotIdentifier = if (!isRelease && snapshotName.isNotEmpty()) "-$snapshotName" else ""
val buildVersion = "$projectVersion$snapshotIdentifier" + (if (isRelease) "" else "-SNAPSHOT")

println("Building: JSVG $buildVersion")
println("     JDK: " + System.getProperty("java.home"))
println("  Gradle: " + gradle.gradleVersion)

sonarqube {
    properties {
        properties["sonar.issue.ignore.multicriteria"] = "e1,e2,e3,e4,e5"

        // java:S115: Disable checking of constants names: We follow the nomenclature of the standard which
        // results in enums being camel case instead of upper snake case
        properties["sonar.issue.ignore.multicriteria.e1.ruleKey"] = "java:S115"
        properties["sonar.issue.ignore.multicriteria.e1.resourceKey"] = "**/*.java"

        // java:S107: Some of the context classes need all of their content passed in a constructor.
        properties["sonar.issue.ignore.multicriteria.e2.ruleKey"] = "java:S107"
        properties["sonar.issue.ignore.multicriteria.e2.resourceKey"] = "**/*.java"

        // javaarchitecture:S7091,javaarchitecture:S7027: Circularity in classes is allowed
        properties["sonar.issue.ignore.multicriteria.e3.ruleKey"] = "javaarchitecture:S7091"
        properties["sonar.issue.ignore.multicriteria.e3.resourceKey"] = "**/*.java"
        properties["sonar.issue.ignore.multicriteria.e4.ruleKey"] = "javaarchitecture:S7027"
        properties["sonar.issue.ignore.multicriteria.e4.resourceKey"] = "**/*.java"

        // java:S6548: Singleton detection. We use singletons in some cases.
        properties["sonar.issue.ignore.multicriteria.e5.ruleKey"] = "java:S6548"
        properties["sonar.issue.ignore.multicriteria.e5.resourceKey"] = "**/*.java"
    }
}

allprojects {
    group = "com.github.weisj"
    version = buildVersion

    repositories {
        if (enableMavenLocal) {
            mavenLocal()
        }
        if (!isRelease) {
            maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        }
        mavenCentral()
    }

    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }

    if (!skipSpotless) {
        apply(plugin = "com.diffplug.spotless")
        spotless {
            val spotlessRatchet by props(default = true)
            if (spotlessRatchet) {
                ratchetFrom("origin/master")
            }
            kotlinGradle {
                ktlint("ktlint".v)
            }
            format("markdown") {
                target("**/*.md")
                endWithNewline()
                trimTrailingWhitespace()
            }
            format("svg") {
                target("**/*.svg")
                targetExclude("**/brokenUpCharContent.svg")
                eclipseWtp(EclipseWtpFormatterStep.XML)
            }
            plugins.withType<JavaPlugin>().configureEach {
                java {
                    importOrder("java", "javax", "org", "com")
                    removeUnusedImports()
                    eclipse().configFile("${project.rootDir}/config/java.eclipseformat.xml")
                    licenseHeaderFile("${project.rootDir}/config/LICENSE_HEADER.txt")
                }
            }
        }
    }

    plugins.withType<JacocoPlugin> {
        the<JacocoPluginExtension>().toolVersion = "jacoco".v
        tasks.withType<JacocoReport>().configureEach {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        // Ensure builds are reproducible
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        dirPermissions {
            user {
                read = true
                write = true
                execute = true
            }
            group {
                read = true
                write = true
                execute = true
            }
            other {
                read = true
                write = false
                execute = true
            }
        }
        filePermissions {
            user {
                read = true
                write = true
                execute = false
            }
            group {
                read = true
                write = false
                execute = false
            }
            other {
                read = true
                write = false
                execute = false
            }
        }
    }

    if (!enableGradleMetadata) {
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            withSourcesJar()
            if (!skipJavadoc && isRelease) {
                withJavadocJar()
            }
        }

        apply(plugin = "maven-publish")
        apply(plugin = "com.gradleup.nmcp")
        apply(plugin = "signing")

        val useInMemoryKey by props(default = false)
        val centralPortalPublishingType by props(default = "USER_MANAGED")
        val centralPortalPublishingTimeout by props(default = 1)

        configure<NmcpExtension> {
            centralPortal {
                username.set(
                    project.stringProperty("centralPortalUsername")
                        ?: providers.environmentVariable("CENTRAL_PORTAL_USERNAME").orNull,
                )
                password.set(
                    project.stringProperty("centralPortalPassword")
                        ?: providers.environmentVariable("CENTRAL_PORTAL_PASSWORD").orNull,
                )
                publishingType = centralPortalPublishingType
                verificationTimeout = Duration.ofMinutes(centralPortalPublishingTimeout.toLong())
            }
        }

        if (!isRelease) {
            configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "centralSnapshots"
                        url = uri("https://central.sonatype.com/repository/maven-snapshots")
                        credentials(PasswordCredentials::class)
                    }
                }
            }
        } else {
            configure<SigningExtension> {
                sign(extensions.getByType<PublishingExtension>().publications)
                if (!useInMemoryKey) {
                     useGpgCmd()
                } else {
                    useInMemoryPgpKeys(
                        project.stringProperty("signing.inMemoryKey")?.replace("#", "\n"),
                        project.stringProperty("signing.password"),
                    )
                }
            }
        }

        if (enableErrorProne) {
            apply(plugin = "net.ltgt.errorprone")
            dependencies {
                "errorprone"(toolLibs.errorprone.core)
                "annotationProcessor"(toolLibs.errorprone.guava)
                if (!JavaVersion.current().isJava9Compatible) {
                    "errorproneJavac"(toolLibs.errorprone.javac)
                }
            }
            tasks.withType<JavaCompile>().configureEach {
                options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000", "-Xmaxwarns", "10000"))
                if (props.bool("Werror", false)) {
                    options.compilerArgs.add("-Werror")
                }
                options.errorprone {
                    errorproneArgs.add("-XepExcludedPaths:.*/javacc/.*")
                    disableWarningsInGeneratedCode.set(true)
                    disable(
                        "StringSplitter",
                        "InlineMeSuggester",
                        "MissingSummary",
                        "MultipleNullnessAnnotations",
                    )
                }
            }
        }

        tasks {
            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
                options.release.set(8)
            }

            withType<ProcessResources>().configureEach {
                from(source) {
                    include("**/*.properties")
                    filteringCharset = "UTF-8"
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                    // apply native2ascii conversion since Java 8 expects properties to have ascii symbols only
                    filter(org.apache.tools.ant.filters.EscapeUnicode::class)
                }
            }

            withType<Jar>().configureEach {
                manifest {
                    attributes["Bundle-License"] = "MIT"
                    attributes["Implementation-Title"] = project.name
                    attributes["Implementation-Version"] = project.version
                    attributes["Specification-Vendor"] = "JSVG"
                    attributes["Specification-Version"] = project.version
                    attributes["Specification-Title"] = "JSVG"
                    attributes["Implementation-Vendor"] = "JSVG"
                    attributes["Implementation-Vendor-Id"] = "com.github.weisj"
                }

                CrLfSpec(LineEndings.LF).run {
                    into("META-INF") {
                        filteringCharset = "UTF-8"
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                        // This includes either project-specific license or a default one
                        if (file("$projectDir/LICENSE").exists()) {
                            textFrom("$projectDir/LICENSE")
                        } else {
                            textFrom("$rootDir/LICENSE")
                        }
                    }
                }
            }

            withType<Javadoc>().configureEach {
                (options as StandardJavadocDocletOptions).apply {
                    quiet()
                    locale = "en"
                    docEncoding = "UTF-8"
                    charSet = "UTF-8"
                    encoding = "UTF-8"
                    docTitle = "JSVG ${project.name} API"
                    windowTitle = "JSVG ${project.name} API"
                    header = "<b>JSVG</b>"
                    addBooleanOption("Xdoclint:none", true)
                    addStringOption("source", "8")
                    if (JavaVersion.current().isJava9Compatible) {
                        addBooleanOption("html5", true)
                    }
                }
            }
        }

        configure<PublishingExtension> {
            if (project.path in listOf(":", ":annotations", ":annotations-processor")) {
                return@configure
            }

            publications {
                create<MavenPublication>(project.name) {
                    artifactId = "${project.name}$snapshotIdentifier"
                    version = buildVersion
                    description = project.description
                    from(project.components["java"])
                }
                withType<MavenPublication> {
                    // Use the resolved versions in pom.xml
                    // Gradle might have different resolution rules, so we set the versions
                    // that were used in Gradle build/test.
                    versionFromResolution()
                    pom {
                        simplifyXml()

                        description.set(
                            project.description
                                ?: "A lightweight Java2D SVG renderer",
                        )
                        name.set(
                            (project.findProperty("artifact.name") as? String)
                                ?: project.name.replaceFirstChar { it.uppercase() }.replace("-", " "),
                        )
                        url.set("https://github.com/weisJ/jsvg")
                        organization {
                            name.set("com.github.weisj")
                            url.set("https://github.com/weisj")
                        }
                        issueManagement {
                            system.set("GitHub")
                            url.set("https://github.com/weisJ/jsvg/issues")
                        }
                        licenses {
                            license {
                                name.set("MIT")
                                url.set("https://github.com/weisj/jsvg/blob/master/LICENSE")
                                distribution.set("repo")
                            }
                        }
                        scm {
                            url.set("https://github.com/weisJ/jsvg")
                            connection.set("scm:git:git://github.com/weisJ/jsvg.git")
                            developerConnection.set("scm:git:ssh://git@github.com:weisj/jsvg.git")
                        }
                        developers {
                            developer {
                                name.set("Jannis Weis")
                            }
                        }
                    }
                }
            }
        }
    }
}
