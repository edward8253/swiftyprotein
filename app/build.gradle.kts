plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.google.services)
}

android {
	namespace = "com.swiftyprotein"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.swiftyprotein"
		minSdk = 24
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			optimization {
				enable = false
			}
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	buildFeatures {
		compose = true
	}
}

dependencies {
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.compose.material.icons.extended)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.biometric.ktx)
	implementation(libs.androidx.lifecycle.process)
	implementation(libs.firebase.auth)
	implementation(libs.coil.compose)
	testImplementation(libs.junit)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(libs.androidx.junit)
	debugImplementation(platform(libs.androidx.compose.bom))
	debugImplementation(libs.androidx.compose.ui.test.manifest)
	debugImplementation(libs.androidx.compose.ui.tooling)

	// Import the Firebase BoM
	implementation(platform(libs.firebase.bom))

	// TODO: Add the dependencies for Firebase products you want to use
	// When using the BoM, don't specify versions in Firebase dependencies
	implementation(libs.firebase.analytics)

	// Add the dependencies for any other desired Firebase products
	// https://firebase.google.com/docs/android/setup#available-libraries
}