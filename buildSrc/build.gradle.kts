plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleApi())
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("module-info-compile") {
            id = "module-info-compile"
            implementationClass = "ModuleInfoCompilePlugin"
        }
    }
}
