# GPS Point Finder PRO v3.3 — GPS Recovery BETA

Fixes relative to v3.2:
- Main screen subscribes to GPS directly while visible; location, accuracy, altitude, bearing and map can update before tracking service starts.
- Android runtime location permission requested on resume.
- Target coordinates persist across app restarts.
- Find and Smart Beep require a valid target; recording remains independent.
- User-visible GPS waiting state; mode buttons are not proof of successful service start.
- Service refuses disabled GPS provider.

**Not verified on an Android phone or compiled in this environment.** Run GitHub Actions and test. Foreground service GPS status remains imperfect; follow-up should add an explicit service health acknowledgment. Map is OpenStreetMap 2D, not satellite or 3D.
