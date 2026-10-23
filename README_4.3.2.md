# JAMS Navigator v4.3.2 — Safe UI beta

## Changes
- Floating search and bottom controls respect Android system bar and display cutout insets.
- Side margins increased to 16dp and bottom toolbar sits 16dp above the system navigation inset.
- A separate, visible START NAVIGATION / STOP NAVIGATION control is displayed above the bottom toolbar after a road route is calculated.
- The old START and STOP controls inside the long Field Mode menu are hidden to prevent confusion.
- App version and GitHub Actions artifact name updated to v4.3.2.

## Limitations
- Search still uses public Photon endpoint, which can time out or return irrelevant results. No search service failover is implemented in this patch.
- OSRM public demo routing is not a production service. No live traffic.
- Java navigation math tests run on JVM. Android SDK/Gradle not available in this environment, so APK compilation, emulator tests, physical GPS and voice tests were NOT performed.
- Upload the contents of this ZIP to repository root and run GitHub Actions to build a debug APK.
