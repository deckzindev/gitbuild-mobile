# ProGuard / R8 Rules for GitBuilder

# Moshi and Reflection
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.example.data.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonClass <fields>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.example.data.db.** { *; }

# OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
