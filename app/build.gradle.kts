import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Le local.properties (fora do git) para permitir sobrescrever a URL da API.
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
        versionCode = 6
        versionName = "1.2"

        // A AwesomeAPI nao pede chave, entao nao ha segredo a embutir no APK.
        // A URL segue configuravel para apontar a um proxy ou a um mock.
        buildConfigField(
            "String",
            "COTACAO_BASE_URL",
            "\"${propriedade("cotacao.api.baseUrl", "https://economia.awesomeapi.com.br/")}\"",
        )
    }

    // Keystore fixa e versionada. Sem isto, o Gradle gera uma chave de debug nova
    // a cada build; como os runners do CI sao descartaveis, cada release saia com
    // assinatura diferente e o Android recusava atualizar por cima ("app nao
    // instalado"). Nao e segredo: sao exatamente os parametros publicos da debug
    // keystore padrao do Android Studio. NUNCA usar para publicar na Play Store.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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

// Sem bloco kotlin { compilerOptions { jvmTarget } }: com o Kotlin embutido do
// AGP 9 o alvo da JVM vem de compileOptions acima.

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
