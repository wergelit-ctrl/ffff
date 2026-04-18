# ──────────────────────────────────────────────────────────────
# FinFocus ProGuard rules
# ──────────────────────────────────────────────────────────────

# ── Kotlin / Coroutines ────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Hilt / Dagger ─────────────────────────────────────────────
# Hilt generates component classes — keep all generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}

# ── Kotlinx Serialization ─────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep @Serializable classes and their companions
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class **$serializer {
    *** INSTANCE;
    *** descriptor;
    <methods>;
}
-keep,includedescriptorclasses class com.finfocus.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Vico Charts ───────────────────────────────────────────────
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ── Timber ────────────────────────────────────────────────────
# In release Timber.DebugTree is not planted but the class must not be removed
# because the companion object is referenced at compile time.
-dontwarn timber.log.Timber$Tree
-keep class timber.log.Timber { *; }

# ── AndroidX / Lifecycle ──────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

# ── WorkManager ───────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── DataStore ─────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ── Domain / Data models ──────────────────────────────────────
# Keep all model classes used in JSON serialization
-keep class com.finfocus.app.domain.model.** { *; }
-keep class com.finfocus.app.data.contract.model.** { *; }

# ── Enum classes ─────────────────────────────────────────────
# Kotlinx serialization needs enum names to be stable
-keepclassmembers enum com.finfocus.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── General ──────────────────────────────────────────────────
# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── MongoDB BSON (Sprint 1.3) ──────────────────────────────────
-keep class org.bson.** { *; }
-dontwarn org.bson.**
-keep class com.mongodb.** { *; }
-dontwarn com.mongodb.**
