import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

// Definidos aqui, e não só no bloco mavenPublishing, porque a substituição de dependência do
// composite build (o `includeBuild("../CustomLogs")` do ChartPatternTracker) resolve por
// group:name durante a configuração — tarde demais se o group viesse depois.
group = "io.github.rodorush"
version = "0.1.0"

android {
    namespace = "br.com.rodorush.customlogs"
    compileSdk = 35

    defaultConfig {
        // Piso baixo de propósito: é library pública. O Crashlytics do BOM 33 pede 21.
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    // API pública explícita: nada vaza para o artefato sem estar marcado de propósito.
    explicitApi()

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
}

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true,
        )
    )

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()

    coordinates(group.toString(), "customlogs", version.toString())

    pom {
        name.set("CustomLogs")
        description.set(
            "Fachada de log para Android com contexto estruturado: comportamento por tipo de " +
                "build e pares chave/valor que viram custom keys no Firebase Crashlytics."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/Rodorush/CustomLogs")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("Rodorush")
                name.set("Rodolfo Pereira de Andrade")
                url.set("https://github.com/Rodorush")
            }
        }

        scm {
            url.set("https://github.com/Rodorush/CustomLogs")
            connection.set("scm:git:git://github.com/Rodorush/CustomLogs.git")
            developerConnection.set("scm:git:ssh://git@github.com/Rodorush/CustomLogs.git")
        }
    }
}
