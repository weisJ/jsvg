import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

open class ModuleInfoExtension {
    var version: JavaVersion = JavaVersion.VERSION_1_9
    var extraArgs: List<String> = emptyList()
}

class ModuleInfoCompilePlugin : Plugin<Project> {

    override fun apply(target: Project) = target.run {
        val infoExtension = target.extensions.create("moduleInfo", ModuleInfoExtension::class.java)
        if (!JavaVersion.current().isJava9Compatible
            || project.findProperty("skipModuleInfo") in listOf("", "true")) return@run

        val moduleInfoFile = file("src/main/module/module-info.java")
        if (moduleInfoFile.exists()) {
            val compileJava = tasks.named<JavaCompile>("compileJava")
            val compileModuleInfoJava by tasks.registering(JavaCompile::class) {
                val javaCompile = compileJava.get()
                classpath = files()
                source("src/main/module/module-info.java")
                source(javaCompile.source)
                destinationDirectory.set(layout.buildDirectory.dir("classes/module"))
                check(infoExtension.version.isJava9Compatible)
                options.compilerArgs.addAll(listOf("--module-path", javaCompile.classpath.asPath))
                if (infoExtension.extraArgs.isNotEmpty()) {
                    options.compilerArgs.addAll(infoExtension.extraArgs)
                    sourceCompatibility = infoExtension.version.majorVersion
                    targetCompatibility = infoExtension.version.majorVersion
                } else {
                    options.compilerArgs.addAll(listOf("--release", infoExtension.version.majorVersion))
                }
            }
            val copyModuleInfo by tasks.registering(Copy::class) {
                dependsOn(compileModuleInfoJava)
                from(layout.buildDirectory.dir("classes/module/module-info.class"))
                into(layout.buildDirectory.dir("classes/java/main"))
            }
            compileJava.configure {
                dependsOn(copyModuleInfo)
            }
        }
    }

}
