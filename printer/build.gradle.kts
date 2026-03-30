import com.android.build.api.dsl.androidLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {

//    alias(libs.plugins.android.kotlin.multiplatform.library)

    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)

}

kotlin {
    jvm()

    androidLibrary {
        namespace = "io.printer.kmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(
                    JvmTarget.JVM_21
                )
            }
        }
    }
//    iosX64()
//    iosArm64()
//    iosSimulatorArm64()
//    linuxX64()

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-network:3.4.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)


            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")



        }

        androidMain.dependencies {
            implementation(libs.core.ktx)
        }

        jvmMain.dependencies {
        }

    }
}


group = "io.github.mamon-aburawi" // this group name in maven central repository
version = "1.0.0" // version of library

mavenPublishing {

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release", "debug"),
        )
    )


    coordinates(
        groupId = group.toString(),
        version = version.toString(),
        artifactId = "nexus-player-kmp"
    )

    pom {
        name = "Nexus Player KMP"
        description = "A high-performance, Kotlin Multiplatform (KMP) video player library designed for Compose Multiplatform. Providing a seamless media experience across Android, iOS, Desktop (JVM), and Web (Wasm), this library simplifies cross-platform video integration with a unified API."
        inceptionYear = "2026"
        url = "https://github.com/mamon-aburawi/Nexus-Player-KMP"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                name = "Mamon Aburawi"
                email = "mamon.aburawi@gmail.com"
            }
        }
        scm {
            url = "https://github.com/mamon-aburawi/Nexus-Player-KMP"
        }
    }

    publishToMavenCentral()

    signAllPublications()
}