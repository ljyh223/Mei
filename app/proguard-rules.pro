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
