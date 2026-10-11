GPS Point Finder v2.0 - Android Studio source project
This is SOURCE CODE, NOT AN APK.
Build: Android Studio -> Open project folder -> allow Gradle sync -> Build > Build APK(s).
Output: app/build/outputs/apk/debug/app-debug.apk
Requires Android SDK 35, JDK 17, Android Gradle Plugin 8.7.3.
Permissions: precise location and vibration. No web map is embedded; button opens Google Maps.
Input: decimal WGS84 latitude, longitude or Google Maps shared text containing decimal coordinates.
JTU projected grid coordinates require a separately verified CRS conversion.
The directional arrow uses a rotation-vector sensor when available; GPS works without it.
