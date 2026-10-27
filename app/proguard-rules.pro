# Preserve generic signatures and runtime annotations for serialization and reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve all fields annotated with @SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve fields for API data models (JSON deserialization)
-keepclassmembers class comic.api.** {
    <fields>;
    <init>(...);
}

# Preserve UI source models and cache envelopes serialized to disk via Gson in SourceCache
-keep class comic.source.Comic {
    <fields>;
    <init>(...);
}
-keep class comic.source.Chapter {
    <fields>;
    <init>(...);
}
-keep class comic.source.Page {
    <fields>;
    <init>(...);
}
-keep class comic.source.Genre {
    <fields>;
    <init>(...);
}
-keep class comic.source.Section {
    <fields>;
    <init>(...);
}
-keep class comic.source.Filter {
    <fields>;
    <init>(...);
}
-keep class comic.source.FilterOption {
    <fields>;
    <init>(...);
}
-keep class comic.source.SortOption {
    <fields>;
    <init>(...);
}
-keep class comic.source.Paged {
    <fields>;
    <init>(...);
}
-keep class comic.source.SourceCache$DiskEnvelope {
    <fields>;
    <init>(...);
}
-keepclassmembers enum comic.source.SectionType { *; }
-keepclassmembers enum comic.source.Capability { *; }

# Suppress harmless warnings from optional dependencies
-dontwarn org.jsoup.**
-dontwarn coil.**
