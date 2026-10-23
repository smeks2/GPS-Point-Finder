# JAMS Navigator 4.3.1 Navigation Core Test Source
Open the project in Android Studio. Build debug APK. If dependency resolution fails, inspect app/build.gradle and your SDK/Gradle setup. Do not distribute publicly until on-device tests pass.

Run independent desktop navigation math tests:
`mkdir -p tests/classes && javac -d tests/classes app/src/main/java/com/jams/gpspointfinder/NavigationMath.java tests/NavigationMathTest.java && java -cp tests/classes NavigationMathTest`

See TEST_REPORT.md for executed tests and unverified items.
