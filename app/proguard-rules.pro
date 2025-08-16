# Firebase / Kotlinx Serialization keep
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * { @kotlinx.serialization.SerialName <fields>; }
-keepclassmembers class * { @kotlinx.serialization.Serializable *; }
