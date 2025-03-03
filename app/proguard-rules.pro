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


# 保留 ExoPlayer 相关类
-keep class androidx.media3.exoplayer.** { *; }
-dontwarn androidx.media3.exoplayer.**

# 保留 SimpleCache 和其依赖类
-keep class androidx.media3.datasource.cache.SimpleCache { *; }
-keep class androidx.media3.database.StandaloneDatabaseProvider { *; }

# 防止 R8 删除缓存相关的字段和方法
-keepclassmembers class androidx.media3.datasource.cache.SimpleCache {
    private *;
}

# 防止 R8 删除 OkHttp 相关类
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator
-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.ComponentEntryPoint class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.GeneratedEntryPoint class *
-keep,allowshrinking class * extends androidx.work.ListenableWorker
-keep class androidx.work.WorkerParameters
-keep,allowshrinking class * extends androidx.work.InputMerger
-keep,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement
-keep class * extends androidx.room.RoomDatabase
-keep class io.ktor.client.engine.** implements io.ktor.client.HttpClientEngineContainer
-keep,allowobfuscation,allowshrinking,allowoptimization class <1>
-keep,allowshrinking class * extends androidx.startup.Initializer
-keep class * implements androidx.versionedparcelable.VersionedParcelable
-keep public class androidx.versionedparcelable.ParcelImpl
-keep,allowobfuscation @interface androidx.annotation.Keep
-keep,allowobfuscation interface <1>
-keep,allowobfuscation interface * extends <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking,allowoptimization class <3>
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowshrinking class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# 保留 ServiceConnection 实现类
-keep class com.ljyh.music.playback.MusicService$* { *; }

-keep class android.content.ServiceConnection { *; }
# 防止 R8 删除或混淆 MusicService 的生命周期方法
-keepclassmembers class com.ljyh.music.playback.MusicService {
    public void onCreate();
    public void onDestroy();
}




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