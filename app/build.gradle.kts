import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.drivepark.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.drivepark.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 13
        versionName = "0.2.11"
        // 실제 차량(arm64)과 Pleos 에뮬레이터(x86_64)용 네이티브 라이브러리만 넣는다(32비트·mips 제외로 용량 감소).
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    // release 서명: 저장소 밖 폴더(프로젝트 옆 waveby-signing/)의 keystore.properties에서 읽는다.
    // 다른 위치면 환경변수 WAVEBY_SIGNING_DIR로 지정한다. 파일이 없으면 서명 없이 빌드된다.
    // ⚠ 서명 키(.jks)와 비밀번호(keystore.properties)는 GitHub에 절대 올리지 않는다.
    //   저장소 안으로 복사하지 말고, 이 코드나 README에 비밀번호를 적지 않는다. 새면 즉시 새 키로 교체해야 한다.
    val signingDir = System.getenv("WAVEBY_SIGNING_DIR")?.let(::File) ?: rootDir.parentFile.resolve("waveby-signing")
    val signingProps = signingDir.resolve("keystore.properties")
    if (signingProps.exists()) {
        val p = Properties().apply { signingProps.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                storeFile = signingDir.resolve(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // 개발용은 패키지 이름 뒤에 .debug를 붙여 제출용(release)과 한 기기에 나란히 깔 수 있게 한다.
        debug {
            applicationIdSuffix = ".debug" // 앱 이름은 src/debug/res/values/strings.xml의 "Waveby (개발)"
        }
        release {
            // 코드·리소스 축소(APK 용량). SDK 보존 규칙은 proguard-rules.pro. 바꾸면 에뮬레이터 시연 순서를 처음부터 다시 확인한다.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.fusedlocation)
    implementation(libs.pleos.tts)
    implementation(libs.pleos.navihelper)
    implementation(libs.pleos.llm)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
