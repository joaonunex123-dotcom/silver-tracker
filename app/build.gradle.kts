import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Le local.properties (fora do git) para manter URL e chave da API fora do codigo.
val propriedadesLocais = Properties().apply {
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}

// Valor em branco conta como ausente: baseUrl vazia quebraria o Retrofit no boot.
fun propriedade(chave: String, padrao: String): String =
    propriedadesLocais.getProperty(chave)?.takeIf { it.isNotBlank() }
        ?: System.getenv(chave.replace('.', '_').uppercase())?.takeIf { it.isNotBlank() }
        ?: padrao

android {
    namespace = "com.stacking.tracker"
    // 37 e exigido pelas AndroidX atuais: androidx.core 1.19.0 declara
    // minCompileSdk=37 no metadado do AAR. targetSdk segue em 36, que e o
    // Android mais recente em que o app foi de fato exercitado.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.stacking.tracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "METALS_BASE_URL",
            "\"${propriedade("metals.api.baseUrl", "https://api.metals.dev/v1/")}\"",
        )
        buildConfigField(
            "String",
            "METALS_API_KEY",
            "\"${propriedade("metals.api.key", "")}\"",
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}
