import com.mikepenz.aboutlibraries.plugin.StrictMode

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
}

// Only permissive licenses are acceptable for anything that ends up in the APK. The build fails
// if a dependency with another license sneaks in.
aboutLibraries {
    license {
        strictMode = StrictMode.FAIL
        allowedLicenses.addAll("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "ISC")
    }
}

// Ships the repository's LICENSE file inside the APK so the About page shows the real text.
abstract class CopyLicenseTask : DefaultTask() {
    @get:InputFile
    abstract val licenseFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        licenseFile.get().asFile.copyTo(outputDir.get().file("LICENSE.txt").asFile, overwrite = true)
    }
}

val copyLicense = tasks.register<CopyLicenseTask>("copyLicense") {
    licenseFile.set(rootProject.layout.projectDirectory.file("LICENSE"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyLicense, CopyLicenseTask::outputDir)
    }
}

android {
    namespace = "io.github.romanfabio.meditate"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.romanfabio.meditate"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
