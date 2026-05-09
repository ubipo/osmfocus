# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html


# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

-keepattributes *Annotation*
#-keep class com.google.common.eventbus.** { *; }

-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# for prettytime (see https://github.com/ubipo/osmfocus/issues/17#issuecomment-831836548)
-keep class com.ocpsoft.pretty.time.i18n.**
-keep class org.ocpsoft.prettytime.i18n.**
-keepnames class ** implements org.ocpsoft.prettytime.TimeUnit

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-printmapping build/outputs/mapping/release/mapping.txt

# protobuf-javalite runtime reflects on generated message backing fields
# (e.g. bitField0_) from metadata in dynamicMethod(BUILD_MESSAGE_INFO).
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# androidx.window reflects on OEM extension and sidecar implementations that are
# not bundled with the app on most devices. Suppress R8 warnings for those
# optional classes so release minification can proceed.
-dontwarn androidx.window.extensions.WindowExtensions
-dontwarn androidx.window.extensions.WindowExtensionsProvider
-dontwarn androidx.window.extensions.area.ExtensionWindowAreaPresentation
-dontwarn androidx.window.extensions.core.util.function.Consumer
-dontwarn androidx.window.extensions.core.util.function.Function
-dontwarn androidx.window.extensions.core.util.function.Predicate
-dontwarn androidx.window.extensions.layout.DisplayFeature
-dontwarn androidx.window.extensions.layout.FoldingFeature
-dontwarn androidx.window.extensions.layout.WindowLayoutComponent
-dontwarn androidx.window.extensions.layout.WindowLayoutInfo
-dontwarn androidx.window.sidecar.SidecarDeviceState
-dontwarn androidx.window.sidecar.SidecarDisplayFeature
-dontwarn androidx.window.sidecar.SidecarInterface$SidecarCallback
-dontwarn androidx.window.sidecar.SidecarInterface
-dontwarn androidx.window.sidecar.SidecarProvider
-dontwarn androidx.window.sidecar.SidecarWindowLayoutInfo
