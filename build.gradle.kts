/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.application") version "8.13.2"
    id("com.google.protobuf") version "0.9.4"
}

android {
    namespace = "com.android.dialer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.dialer"
        minSdk = 26  // Increased to support adaptive icons
        targetSdk = 35
        versionCode = 2900000
        versionName = "23.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["appPackageName"] = applicationId ?: "com.android.dialer"
    }

    flavorDimensions += "variant"
    productFlavors {
        create("aosp") {
            dimension = "variant"
        }
        create("dev") {
            dimension = "variant"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "applicationLabel", "DEV Phone")
            manifestPlaceholders["appPackageName"] = "com.android.dialer.dev"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard.flags"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.setSrcDirs(listOf("java"))
            res.srcDirs(
                "assets/product/res",
                "assets/quantum/res",
                "java/com/android/contacts/common/res",
                "java/com/android/dialer/about/res",
                "java/com/android/dialer/app/res",
                "java/com/android/dialer/app/voicemail/error/res",
                "java/com/android/dialer/assisteddialing/res",
                "java/com/android/dialer/assisteddialing/ui/res",
                "java/com/android/dialer/blocking/res",
                "java/com/android/dialer/blockreportspam/res",
                "java/com/android/dialer/callcomposer/camera/camerafocus/res",
                "java/com/android/dialer/callcomposer/cameraui/res",
                "java/com/android/dialer/callcomposer/res",
                "java/com/android/dialer/calldetails/res",
                "java/com/android/dialer/calllog/ui/menu/res",
                "java/com/android/dialer/calllog/ui/res",
                "java/com/android/dialer/calllogutils/res",
                "java/com/android/dialer/clipboard/res",
                "java/com/android/dialer/common/preference/res",
                "java/com/android/dialer/common/res",
                "java/com/android/dialer/contactphoto/res",
                "java/com/android/dialer/contacts/displaypreference/res",
                "java/com/android/dialer/contacts/resources/res",
                "java/com/android/dialer/contactsfragment/res",
                "java/com/android/dialer/dialpadview/res",
                "java/com/android/dialer/dialpadview/theme/res",
                "java/com/android/dialer/enrichedcall/simulator/res",
                "java/com/android/dialer/glidephotomanager/impl/res",
                "java/com/android/dialer/historyitemactions/res",
                "java/com/android/dialer/interactions/res",
                "java/com/android/dialer/lettertile/res",
                "java/com/android/dialer/main/impl/bottomnav/res",
                "java/com/android/dialer/main/impl/res",
                "java/com/android/dialer/main/impl/toolbar/res",
                "java/com/android/dialer/notification/res",
                "java/com/android/dialer/oem/res",
                "java/com/android/dialer/phonenumberutil/res",
                "java/com/android/dialer/postcall/res",
                "java/com/android/dialer/precall/impl/res",
                "java/com/android/dialer/preferredsim/impl/res",
                "java/com/android/dialer/preferredsim/suggestion/res",
                "java/com/android/dialer/promotion/impl/res",
                "java/com/android/dialer/rtt/res",
                "java/com/android/dialer/searchfragment/common/res",
                "java/com/android/dialer/searchfragment/cp2/res",
                "java/com/android/dialer/searchfragment/directories/res",
                "java/com/android/dialer/searchfragment/list/res",
                "java/com/android/dialer/searchfragment/nearbyplaces/res",
                "java/com/android/dialer/searchfragment/remote/res",
                "java/com/android/dialer/shortcuts/res",
                "java/com/android/dialer/spam/promo/res",
                "java/com/android/dialer/spannable/res",
                "java/com/android/dialer/speeddial/res",
                "java/com/android/dialer/theme/base/res",
                "java/com/android/dialer/theme/common/res",
                "java/com/android/dialer/theme/hidden/res",
                "java/com/android/dialer/theme/res",
                "java/com/android/dialer/util/res",
                "java/com/android/dialer/voicemail/listui/error/res",
                "java/com/android/dialer/voicemail/listui/res",
                "java/com/android/dialer/voicemail/settings/res",
                "java/com/android/dialer/widget/res",
                "java/com/android/incallui/answer/impl/affordance/res",
                "java/com/android/incallui/answer/impl/answermethod/res",
                "java/com/android/incallui/answer/impl/hint/res",
                "java/com/android/incallui/answer/impl/res",
                "java/com/android/incallui/audioroute/res",
                "java/com/android/incallui/autoresizetext/res",
                "java/com/android/incallui/calllocation/impl/res",
                "java/com/android/incallui/callpending/res",
                "java/com/android/incallui/callscreen/res",
                "java/com/android/incallui/commontheme/res",
                "java/com/android/incallui/contactgrid/res",
                "java/com/android/incallui/disconnectdialog/res",
                "java/com/android/incallui/hold/res",
                "java/com/android/incallui/incall/impl/res",
                "java/com/android/incallui/res",
                "java/com/android/incallui/rtt/impl/res",
                "java/com/android/incallui/sessiondata/res",
                "java/com/android/incallui/spam/res",
                "java/com/android/incallui/speakerbuttonlogic/res",
                "java/com/android/incallui/telecomeventui/res",
                "java/com/android/incallui/theme/res",
                "java/com/android/incallui/video/impl/res",
                "java/com/android/incallui/video/protocol/res",
                "java/com/android/voicemail/impl/configui/res",
                "java/com/android/voicemail/impl/res"
            )
            aidl.srcDirs("java")
            assets.srcDirs("assets")
        }
        getByName("aosp") {
            java.srcDirs("java/com/android/dialer/constants/aospdialer")
        }
        getByName("dev") {
            java.srcDirs("java/com/android/dialer/constants/aospdialer")
        }
        getByName("test") {
            java.setSrcDirs(listOf("src/test/java"))
            resources.srcDirs("src/test/resources")
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    androidResources {
        noCompress += listOf()
        @Suppress("DEPRECATION")
        additionalParameters += listOf("--auto-add-overlay", "--no-resource-deduping")
        ignoreAssetsPattern = "!.svn:!.git:.*:!CVS:!thumbs.db:!picasa.ini:!*.scc:*~"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.annotation:annotation:1.8.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager:viewpager:1.0.0")
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.loader:loader:1.1.0")
    implementation("androidx.collection:collection:1.4.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.legacy:legacy-support-v13:1.0.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.interpolator:interpolator:1.0.0")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.50")
    implementation("com.googlecode.libphonenumber:geocoder:2.234")
    implementation("me.leolin:ShortcutBadger:1.1.22")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.dagger:dagger:2.52")
    annotationProcessor("com.google.dagger:dagger-compiler:2.52")
    implementation("com.google.auto.value:auto-value-annotations:1.11.0")
    annotationProcessor("com.google.auto.value:auto-value:1.11.0")
    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.google.auto:auto-common:1.2.2")
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("io.grpc:grpc-stub:1.62.2")
    implementation("io.grpc:grpc-protobuf-lite:1.62.2")
    implementation("io.grpc:grpc-okhttp:1.62.2")
    implementation("org.apache.james:apache-mime4j-core:0.8.11")
    implementation("org.apache.james:apache-mime4j-dom:0.8.11")
    implementation("commons-io:commons-io:2.15.1")
    // OpenStreetMap (osmdroid) - open source alternative to Google Maps
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("com.google.errorprone:error_prone_annotations:2.15.0")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("com.google.truth:truth:1.4.4")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

// Protobuf sources are automatically added by the protobuf plugin
