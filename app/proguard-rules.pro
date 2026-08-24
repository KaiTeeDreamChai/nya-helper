-keep class com.nya.helper.MainActivity {
    public boolean isLsposedActiveDirect();
}
-keep class com.nya.helper.xposed.** { *; }
-keep class com.nya.helper.model.** { *; }
-keep class com.nya.helper.engine.** { *; }
-keep class com.nya.helper.provider.** { *; }
-keep class com.nya.helper.service.** { *; }
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**
