
import io.homeassistant.companion.android.androidConfig
import io.homeassistant.companion.android.getPluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * A convention plugin that applies common configurations to Android Compose modules.
 *
 * This plugin applies the Compose compiler plugin and the screenshot test plugin.
 * It also adds the necessary Compose dependencies and configurations for both runtime and testing.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.compose.compiler.getPluginId())

            androidConfig {
                buildFeatures.compose = true

                experimentalProperties["android.experimental.enableScreenshotTest"] = true
            }

            // Screenshot test worker memory grows with test count. Increase as needed.
            // Tracking: https://issuetracker.google.com/issues/469819154
            val maxHeapSizeScreenshotTesting = "3g"

            // Most of the worker's footprint is layoutlib's native bitmaps, which sit outside
            // maxHeapSize and are only released when the JVM exits. Restarting the worker every
            // N test classes caps that growth; without it the app module reaches ~15GB on CI.
            val screenshotTestForkEvery = 10L

//            tasks.withType<PreviewScreenshotValidationTask>().configureEach {
//                maxHeapSize = maxHeapSizeScreenshotTesting
//                forkEvery = screenshotTestForkEvery
//            }
//
//            tasks.withType<PreviewScreenshotUpdateTask>().configureEach {
//                maxHeapSize = maxHeapSizeScreenshotTesting
//                forkEvery = screenshotTestForkEvery
//            }

            androidConfig {
                dependencies {
                    "implementation"(platform(libs.compose.bom))
                    "implementation"(libs.compose.foundation)
                    "implementation"(libs.compose.material3)
                    "implementation"(libs.compose.ui)
                    "implementation"(libs.compose.uiTooling)
                    "implementation"(libs.androidx.lifecycle.runtime.compose)

                    "androidTestImplementation"(platform(libs.compose.bom))
                    "androidTestImplementation"(libs.bundles.androidx.compose.ui.test)

                    "testImplementation"(platform(libs.compose.bom))
                    "testImplementation"(libs.bundles.androidx.compose.ui.test)
                }

                with(testOptions) {
                    screenshotTests.create("screenshotTest") {
                        engineVersion = libs.versions.screenshot.get()
                        imageDifferenceThreshold = 0.00025f // 0.025%
                        //targetVariants.add("fullDebug")

                        dependencies {
                            implementation.add(libs.compose.uiTooling)
                            implementation.add(libs.screenshot.validation.api)
                        }
                    }
                }
            }
        }
    }
}
