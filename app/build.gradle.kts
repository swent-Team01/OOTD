import java.io.FileInputStream
import java.util.Properties

plugins {
  //    jacoco
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsKotlinAndroid)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.sonar)
  alias(libs.plugins.compose.compiler)
  // Add the Google services Gradle plugin
  id("com.google.gms.google-services")
  id("jacoco")
  id("org.jetbrains.kotlin.plugin.serialization")
}

configurations.all { resolutionStrategy { force("org.apache.commons:commons-compress:1.26.0") } }

android {
  namespace = "com.android.ootd"
  compileSdk = 35

  // Load the API key from environment variable (for CI) or local.properties (for local dev)
  val mapsApiKey: String =
      System.getenv("MAPS_API_KEY")
          ?: run {
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
              localProperties.load(FileInputStream(localPropertiesFile))
              localProperties.getProperty("MAPS_API_KEY") ?: ""
            } else {
              ""
            }
          }

  bundle { language { enableSplit = false } }

  defaultConfig {
    applicationId = "com.android.ootd"
    minSdk = 28
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }
    manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
  }

  signingConfigs {
    create("release") {
      // CI signing config
      val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
      val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
      val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
      val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")

      if (keystorePath != null &&
          keystorePassword != null &&
          keyAlias != null &&
          keyPassword != null) {
        storeFile = file(keystorePath)
        storePassword = keystorePassword
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

      // Only use release signing config for the CI env
      if (System.getenv("ANDROID_KEYSTORE_PATH") != null) {
        signingConfig = signingConfigs.getByName("release")
      }
    }

    debug {
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
  }

  testCoverage { jacocoVersion = "0.8.11" }

  buildFeatures { compose = true }

  composeCompiler { enableStrongSkippingMode = true }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions { jvmTarget = "17" }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      merges += "META-INF/LICENSE.md"
      merges += "META-INF/LICENSE-notice.md"
      excludes += "META-INF/LICENSE-notice.md"
      excludes += "META-INF/LICENSE.md"
      excludes += "META-INF/LICENSE"
      excludes += "META-INF/LICENSE.txt"
      excludes += "META-INF/NOTICE"
      excludes += "META-INF/NOTICE.txt"
    }
    jniLibs { useLegacyPackaging = true }
    dex { useLegacyPackaging = false }
  }

  // Robolectric needs to be run only in debug. But its tests are placed in the shared source set
  // (test)
  // The next lines transfers the src/test/* from shared to the testDebug one
  //
  // This prevent errors from occurring during unit tests
  sourceSets.getByName("testDebug") {
    val test = sourceSets.getByName("test")

    java.setSrcDirs(test.java.srcDirs)
    res.setSrcDirs(test.res.srcDirs)
    resources.setSrcDirs(test.resources.srcDirs)
  }

  sourceSets.getByName("test") {
    java.setSrcDirs(emptyList<File>())
    res.setSrcDirs(emptyList<File>())
    resources.setSrcDirs(emptyList<File>())
  }
}

sonar {
  properties {
    property("sonar.projectKey", "swent-Team01_OOTD")
    property("sonar.organization", "swent-team01")
    property("sonar.projectName", "OOTD")
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.gradle.skipCompile", "true")
    // Comma-separated paths to the various directories containing the *.xml JUnit report files.
    // Each path may be absolute or relative to the project base directory.
    property(
        "sonar.junit.reportPaths",
        "${project.layout.buildDirectory.get()}/test-results/testDebugunitTest/")
    // Paths to xml files with Android Lint issues. If the main flavor is changed, this file will
    // have to be changed too.
    property(
        "sonar.androidLint.reportPaths",
        "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
    // Paths to JaCoCo XML coverage report files.
    property(
        "sonar.coverage.jacoco.xmlReportPaths",
        "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
  }
}

// When a library is used both by robolectric and connected tests, use this function
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
  androidTestImplementation(dep)
  testImplementation(dep)
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(platform(libs.compose.bom))
  implementation(libs.androidx.datastore.core)
  implementation(libs.play.services.location)
  implementation(libs.androidx.foundation.layout)
  implementation(libs.androidx.foundation)
  implementation(libs.androidx.animation)
  implementation(libs.androidx.material3)

  // Firebase
  // Use Firebase BOM from version catalog for maintainability
  val firebaseBom = libs.firebase.bom
  implementation(firebaseBom)
  // Firebase Storage KTX (provides Firebase.storage extension).
  implementation(libs.firebase.storage.ktx)
  implementation(libs.firebase.database.ktx)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth.ktx)
  implementation(libs.firebase.auth)

  // Credential Manager (for Google Sign-In)
  implementation(libs.credentials)
  implementation(libs.credentials.play.services.auth)
  implementation(libs.googleid)

  // Networking with OkHttp
  implementation(libs.okhttp)

  // Navigation
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)
  androidTestImplementation(libs.androidx.navigation.testing)

  testImplementation(libs.junit)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.mockk.agent.android)
  testImplementation(libs.mockk)

  // ------------- Jetpack Compose ------------------
  val composeBom = platform(libs.compose.bom)
  implementation(composeBom)
  globalTestImplementation(composeBom)

  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  // Material Design 3
  implementation(libs.compose.material3)

  // Coil for image loading in Compose
  implementation(libs.coil.compose)
  implementation(libs.compose.activity)

  // CameraX
  implementation(libs.camerax.core)
  implementation(libs.camerax.camera2)
  implementation(libs.camerax.lifecycle)
  implementation(libs.camerax.view)

  // ExifInterface for reading image orientation
  implementation(libs.androidx.exifinterface)

  // Accompanist for permissions
  implementation(libs.accompanist.permissions)

  // Guava for CameraX ListenableFuture
  implementation(libs.guava)

  // Integration with ViewModels
  implementation(libs.compose.viewmodel)
  // Android Studio Preview support
  implementation(libs.compose.preview)
  debugImplementation(libs.compose.tooling)
  // UI Tests
  globalTestImplementation(libs.compose.test.junit)
  debugImplementation(libs.compose.test.manifest)
  implementation(libs.compose.material.icons.extended)

  // Coil for image loading in Compose
  implementation(libs.coil.compose)
  implementation(libs.compose.activity)

  // CameraX
  implementation(libs.camerax.core)
  implementation(libs.camerax.camera2)
  implementation(libs.camerax.lifecycle)
  implementation(libs.camerax.view)

  // ExifInterface for reading image orientation
  implementation(libs.androidx.exifinterface)

  // Accompanist for permissions
  implementation(libs.accompanist.permissions)

  // Guava for CameraX ListenableFuture
  implementation(libs.guava)

  // Integration with ViewModels
  implementation(libs.compose.viewmodel)
  // Android Studio Preview support
  implementation(libs.compose.preview)
  debugImplementation(libs.compose.tooling)
  // UI Tests
  globalTestImplementation(libs.compose.test.junit)
  debugImplementation(libs.compose.test.manifest)

  // --------- Kaspresso test framework ----------
  globalTestImplementation(libs.kaspresso)
  globalTestImplementation(libs.kaspresso.compose)

  // ----------       Robolectric     ------------
  testImplementation(libs.robolectric)

  // Google Service and Maps
  implementation(libs.play.services.maps)
  implementation(libs.maps.compose)
  implementation(libs.maps.compose.utils)
  implementation(libs.play.services.auth)
  implementation("javax.annotation:javax.annotation-api:1.3.2")
  testImplementation(kotlin("test"))
}

tasks.withType<Test> {
  // Configure Jacoco for each tests
  configure<JacocoTaskExtension> {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }
}

tasks.register("jacocoTestReport", JacocoReport::class) {
  mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest")

  reports {
    xml.required = true
    html.required = true
  }

  val fileFilter =
      listOf(
          "**/R.class",
          "**/R$*.class",
          "**/BuildConfig.*",
          "**/Manifest*.*",
          "**/*Test*.*",
          "android/**/*.*",
      )

  val debugTree =
      fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
      }

  val mainSrc = "${project.layout.projectDirectory}/src/main/java"
  sourceDirectories.setFrom(files(mainSrc))
  classDirectories.setFrom(files(debugTree))
  executionData.setFrom(
      fileTree(project.layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
      })
}

configurations.forEach { configuration ->
  // Exclude protobuf-lite from all configurations
  // This fixes a fatal exception for tests interacting with Cloud Firestore
  configuration.exclude("com.google.protobuf", "protobuf-lite")
}
