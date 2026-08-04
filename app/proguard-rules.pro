# Mantem os DTOs anotados com @Serializable (kotlinx.serialization gera serializers estaticos).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.stacking.tracker.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.stacking.tracker.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
