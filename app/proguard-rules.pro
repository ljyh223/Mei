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


