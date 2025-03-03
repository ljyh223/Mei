# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.awt.image.BufferedImage
-dontwarn javax.imageio.ImageIO
-dontwarn javax.imageio.stream.ImageInputStream

# If class has fields with `@SerializedName` annotation keep its constructors
-if class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep class <1> {
  <init>(...);
}

-keepclasseswithmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# 保留所有带有 @SerializedName 注解的字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留类的所有构造函数
-keepclassmembers class * {
    <init>(...);
}


-keep class * extends com.google.gson.TypeAdapter {
  <init>();
}
-keep class * implements com.google.gson.TypeAdapterFactory {
  <init>();
}
-keep class * implements com.google.gson.JsonSerializer {
  <init>();
}
-keep class * implements com.google.gson.JsonDeserializer {
  <init>();
}

-if class *
-keepclasseswithmembers,allowshrinking,allowobfuscation class <1> {
  @com.google.gson.annotations.SerializedName <fields>;
}

-if class *
-keepclasseswithmembers class <1> {
  <init>(...);
  @com.google.gson.annotations.SerializedName <fields>;
}

-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class * { void (); }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.AndroidEntryPoint class * { <init>(); }
-keep class * extends androidx.room.RoomDatabase { <init> (); }
# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn coil3.PlatformContext
-dontwarn java.awt.Graphics2D
-dontwarn java.awt.Image
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.RenderedImage
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn javax.imageio.ImageWriter
-dontwarn javax.imageio.stream.ImageOutputStream
-dontwarn javax.swing.filechooser.FileFilter
-dontwarn korlibs.crypto.encoding.Base64
-dontwarn korlibs.crypto.encoding.Base64Kt
-dontwarn korlibs.crypto.encoding.Hex
-dontwarn korlibs.crypto.encoding.HexKt

# 保留四大组件、自定义 Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

# Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留枚举和注解类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepattributes *Annotation*

-keepattributes Signature

# 保持 Log 方法不被移除
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** e(...);
}

# Dagger Hilt 注解相关规则
-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class * { <init>(); }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class * { <init>(); }
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.ComponentEntryPoint class * { <init>(); }
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.GeneratedEntryPoint class * { <init>(); }


# WorkManager 相关规则
-keep,allowshrinking class * extends androidx.work.ListenableWorker { <init>(); }
-keep class androidx.work.WorkerParameters { <init>(); }
-keep,allowshrinking class * extends androidx.work.InputMerger { <init>(); }

# Navigation 相关规则
-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator { <init>(); }

# AndroidX Startup 相关规则
-keep,allowshrinking class * extends androidx.startup.Initializer { <init>(); }

# OkHttp 相关规则
-keep,allowshrinking class okhttp3.internal.publicsuffix.PublicSuffixDatabase { <init>(); }

# Retrofit 相关规则
-keep,allowobfuscation interface *
-keep,allowobfuscation interface * extends *
-keep,allowobfuscation,allowshrinking class retrofit2.Response { <init>(); }

# Ktor 相关规则
-keep class io.ktor.client.engine.** implements io.ktor.client.HttpClientEngineContainer { <init>(); }

# VersionedParcelable 相关规则
-keep class * implements androidx.versionedparcelable.VersionedParcelable { <init>(); }
-keep public class androidx.versionedparcelable.ParcelImpl { <init>(); }

# AndroidX Annotations 相关规则
-keep,allowobfuscation @interface androidx.annotation.Keep

# Compose 相关规则
-keep,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement { <init>(); }