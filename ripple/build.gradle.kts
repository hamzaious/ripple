plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.github.ripple.effect"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Trim the library: no BuildConfig (saves a class). Android resources stay
    // enabled by default so `attrs.xml` is still packaged into the AAR.
    buildFeatures {
        buildConfig = false
    }

    // Generate a sources jar alongside the AAR for publishing.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Intentionally zero runtime dependencies to keep the AAR tiny.
}

// Update these to match your GitHub repository before publishing.
val libGroup = "com.github.ripple.effect"
val libArtifact = "ripple"
val libVersion = "1.0.0"
val libGitOrg = "hamzaious"              // GitHub user that hosts the repo
val libRepoName = "ripple"
val libUrl = "https://github.com/$libGitOrg/$libRepoName"

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = libGroup
                artifactId = libArtifact
                version = libVersion

                pom {
                    name.set("RippleEffect")
                    description.set(
                        "A lightweight, Kotlin-first ripple background view for Android. " +
                                "Built-in circle, star, arrow, diamond and moon shapes, " +
                                "plus an auto match-view mode that ripples in the exact " +
                                "shape of any Button / ImageView / Card / FAB."
                    )
                    url.set(libUrl)
                    inceptionYear.set("2026")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set(libGitOrg)
                            name.set("RippleEffect")
                            url.set("https://github.com/$libGitOrg")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/$libGitOrg/$libRepoName.git")
                        developerConnection.set("scm:git:ssh://github.com/$libGitOrg/$libRepoName.git")
                        url.set(libUrl)
                    }
                    issueManagement {
                        system.set("GitHub Issues")
                        url.set("$libUrl/issues")
                    }
                }
            }
        }
    }
}
