# GPS Point Finder PRO v3.0 BETA — source build

## Implemented in this package
- Mark Point 60-second GPS sampling with quality filter and weighted average; saved permanently locally.
- Find saved point, GPS heading/course/bearing/turn, altitude, distance and accuracy.
- Foreground GPS service with variable audio beep and background track logging.
- Autosave latest track approximately every 15 seconds; simple schematic offline track viewer.
- GPX as shareable XML text (not an attached .gpx file).
- GitHub Actions workflow to build DEBUG APK.

## NOT YET IMPLEMENTED — phase 2
- Licensed downloadable offline satellite map packs, digital elevation terrain, 3D terrain renderer, offline hillshade and elevation at target.
- Proper offline map viewer with MBTiles/PMTiles. Current track viewer is schematic ONLY, without real map layers.
- Backtrack turn-by-turn navigation and multi-track history.
- Commercial license/activation, release signing, rigorous field testing.

## Important
- This code has NOT been compiled or tested on an Android device here. GitHub Actions must be used to check the build.
- Start the tracking service while the app is visible; phone battery settings may restrict background use.
- Marking for 60 seconds does not guarantee 1–2m accuracy or a legally surveyed boundary.
- On some devices Android 13+ requires granting notification permission to show notifications normally.
- Track recording stores recent points in preferences, intended for short field trials, not long production trips.
- Upload contents of this ZIP into the ROOT of a NEW private repository; preserve folder hierarchy.
- Do not redistribute Google Earth imagery without authorization.
