plugins {
   // https://developer.android.com/build/releases/gradle-plugin
   alias(libs.plugins.android.application) apply false
   alias(libs.plugins.kotlin.android) apply false
   alias(libs.plugins.kotlin.compose) apply false
   alias(libs.plugins.kotlin.serialization) apply false
   alias(libs.plugins.google.devtools.ksp) apply false
}


