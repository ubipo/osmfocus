# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

-keepattributes *Annotation*
#-keep class com.google.common.eventbus.** { *; }

-keep class net.pfiers.osmfocus.service.osmapi.ElementsRes { *; }
-keep class net.pfiers.osmfocus.service.osmapi.Element { *; }
-keep class net.pfiers.osmfocus.service.osmapi.Node { *; }
-keep class net.pfiers.osmfocus.service.osmapi.Way { *; }
-keep class net.pfiers.osmfocus.service.osmapi.Relation { *; }
-keep class net.pfiers.osmfocus.service.osmapi.OsmApiRelationMember { *; }

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
