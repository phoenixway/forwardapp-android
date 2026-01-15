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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all data model classes used by Room from being obfuscated.
-keep class com.romankozak.forwardappmobile.data.database.models.** { *; }

# Hilt / Dagger generated code (aggregators, components, sentinels).
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class dagger.hilt.internal.processedrootsentinel.codegen.** { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Kotlinx Serialization: keep generated serializers and metadata.
-keepclassmembers class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Retrofit/OkHttp: keep service interfaces and avoid warnings from optional deps.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
-dontwarn javax.script.**
-keep class retrofit2.** { *; }
-keep class com.romankozak.forwardappmobile.**Service { *; }

# LuaJ: keep libs (bit32 and friends) to avoid reflection instantiation issues after shrinking.
-keep class org.luaj.vm2.lib.** { *; }
