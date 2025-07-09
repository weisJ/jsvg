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
