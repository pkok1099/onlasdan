# Preserve generic signatures and runtime annotations for serialization and reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve all fields annotated with @SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve fields for API data models (JSON deserialization)
-keepclassmembers class com.onlasdan.comic.api.** {
    <fields>;
    <init>(...);
}

# Preserve UI source models and cache envelopes serialized to disk via Gson in SourceCache
-keep class com.onlasdan.comic.ui.source.Comic {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.Chapter {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.Page {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.Genre {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.Section {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.Filter {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.FilterOption {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.SortOption {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.Paged {
    <fields>;
    <init>(...);
}
-keep class com.onlasdan.comic.ui.source.SourceCache$DiskEnvelope {
    <fields>;
    <init>(...);
}
-keepclassmembers enum com.onlasdan.comic.ui.source.SectionType { *; }
-keepclassmembers enum com.onlasdan.comic.ui.source.Capability { *; }

# Suppress harmless warnings from optional dependencies
-dontwarn org.jsoup.**
-dontwarn coil.**
