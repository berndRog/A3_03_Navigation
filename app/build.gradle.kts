plugins {
   alias(libs.plugins.android.application)
   alias(libs.plugins.kotlin.android)
   alias(libs.plugins.kotlin.compose)
   alias(libs.plugins.google.devtools.ksp)
   alias(libs.plugins.kotlin.serialization)
}

android {
   namespace = "de.rogallab.mobile"
   compileSdk = 36

   defaultConfig {
      applicationId = "de.rogallab.mobile"
      minSdk = 26
      targetSdk = 36
      versionCode = 1
      versionName = "1.0"

      //   testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      testInstrumentationRunner = "de.rogallab.mobile.androidTest.TestRunner"

   }

   testOptions{
      animationsDisabled = true
      unitTests.isIncludeAndroidResources = true    // Robolectric
   }

   buildTypes {
      release {
         isMinifyEnabled = false
         proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      }
   }
   compileOptions {
      sourceCompatibility = JavaVersion.VERSION_21
      targetCompatibility = JavaVersion.VERSION_21
   }
   buildFeatures {
      compose = true
   }
}

kotlin {
   jvmToolchain(21)
}

dependencies {
   // Gradle version catalo
   // https://www.youtube.com/watch?v=MWw1jcwPK3Q

   // Kotlin
   // https://developer.android.com/jetpack/androidx/releases/core
   implementation(libs.androidx.core.ktx)
   // Kotlin Coroutines
   // https://kotlinlang.org/docs/releases.html
   implementation (libs.kotlinx.coroutines.core)
   implementation (libs.kotlinx.coroutines.android)

   // Ui Activity
   // https://developer.android.com/jetpack/androidx/releases/activity
   implementation(libs.androidx.activity.compose)
   implementation(libs.androidx.compose.foundation.layout)
   // Ui Compose
   // https://developer.android.com/jetpack/compose/bom/bom-mapping
   val composeBom = platform(libs.androidx.compose.bom)
   implementation(composeBom)
   testImplementation(composeBom)
   androidTestImplementation(composeBom)
   implementation(libs.androidx.ui)
   implementation(libs.androidx.ui.graphics)
   implementation(libs.androidx.ui.tooling)
   implementation(libs.androidx.ui.tooling.preview)
   implementation(libs.androidx.ui.text.google.fonts)
   implementation(libs.androidx.animation)
   implementation(libs.androidx.material3)
   implementation(libs.androidx.material.icons.extended)
   implementation(libs.androidx.material3.adaptive)
   implementation(libs.androidx.material3.windowsizeclass)

   // Ui Lifecycle
   // https://developer.android.com/jetpack/androidx/releases/lifecycle
   // val archVersion = "2.2.0"
   // ViewModel utilities for Compose
   implementation(libs.androidx.lifecycle.viewmodel.compose)
   // Lifecycle utilities for Compose
   implementation (libs.androidx.lifecycle.runtime.compose)
   // https://developer.android.com/jetpack/androidx/releases/lifecycle
   implementation(libs.androidx.lifecycle.viewmodel.navigation3)

   // Ui Navigation
   // https://developer.android.com/jetpack/androidx/releases/navigation
   // Jetpack Compose Integration
   // implementation(libs.androidx.navigation.compose) implementation(libs.androidx.navigation3.runtime)
   // https://developer.android.com/jetpack/androidx/releases/navigation3
   implementation(libs.androidx.navigation3.runtime)
   implementation(libs.androidx.navigation3.ui)

   // Room
   implementation(libs.androidx.room.ktx)
   implementation(libs.androidx.room.runtime)
   ksp(libs.androidx.room.compiler)

   // Image loading
   // https://coil-kt.github.io/coil/
   implementation(libs.coil.compose)

   // Koin
   // https://insert-koin.io/docs/3.2.0/getting-started/android/
   //implementation(platform(libs.koin.bom))
   implementation(libs.koin.core)
   implementation(libs.koin.android)
   implementation(libs.koin.androidx.compose)

   // Ktor/Kotlin JSON Serializer
   implementation(libs.kotlinx.serialization.json)

   // Retrofit
   implementation(libs.gson.json)
   implementation(libs.retrofit2.core)
   implementation(libs.retrofit2.gson)
   implementation(libs.retrofit2.logging)

   // Google Play Services Location
   implementation(libs.gplay.location)

   // TESTS -----------------------
   testImplementation(libs.junit)

   // androidx-test-core
   testImplementation(libs.androidx.test.core)
   testImplementation(libs.androidx.test.core.ktx)

   // Koin
   testImplementation(libs.koin.test)
   testImplementation(libs.koin.test.junit4)

   // Coroutines, Flow, StateFlow Testing
   testImplementation(libs.kotlinx.coroutines.test)
   testImplementation(libs.turbine.test)

   // Roboelectric
   testImplementation(libs.robolectric.test)

   // ANDROID TESTS ---------------
   // https://developer.android.com/jetpack/androidx/releases/test
   // Coroutines Testing
   androidTestImplementation(libs.kotlinx.coroutines.test)
   androidTestImplementation(libs.androidx.ui.test.junit4)

   // To use the androidx.test.core APIs
   //androidx-test-core
   androidTestImplementation(libs.androidx.test.core)
   androidTestImplementation(libs.androidx.test.core.ktx)

   // To use the JUnit Extension APIs
   androidTestImplementation(libs.androidx.test.ext.junit)
   androidTestImplementation(libs.androidx.test.ext.junit.ktx)
   androidTestImplementation(libs.androidx.test.ext.truth)
   androidTestImplementation(libs.androidx.test.runner)

   // To use Compose Testing
   androidTestImplementation(platform(libs.androidx.compose.bom))
   androidTestImplementation(libs.androidx.ui.test.junit4)

   // Navigation Testing
   // androidTestImplementation(libs.androidx.navigation.testing)

   // Room Testing
   androidTestImplementation(libs.androidx.room.testing)
   androidTestImplementation(libs.androidx.arch.core.testing)

   // Koin Test features
   androidTestImplementation(libs.koin.test)
   androidTestImplementation(libs.koin.test.junit4)
   androidTestImplementation(libs.koin.android.test)
   androidTestImplementation(libs.koin.androidx.compose)

   // Espresso To use the androidx.test.espresso
   androidTestImplementation(libs.androidx.test.espresso.core)

   // Mockito
   androidTestImplementation(libs.mockito.core)
   androidTestImplementation(libs.mockito.android)
   androidTestImplementation(libs.mockito.kotlin)

   debugImplementation(libs.androidx.ui.tooling)
   debugImplementation(libs.androidx.ui.test.manifest)

}