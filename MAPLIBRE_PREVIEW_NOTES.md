# JAIMS MapLibre preview — experimental source

This is a source-level integration, NOT a compiled or field-tested APK.
- Distance >15 m: MapLibre raster OSM view tilted at 55 degrees.
- Distance <=10 m: original osmdroid 2D precision map. Hysteresis 10/15 m.
- MapLibre view shows target and GPS markers, camera bearing toward the target.
- Existing multi-pin, track, mark, and 2D navigation code preserved.
- MapLibre view uses public OSM raster tile endpoint online: do not bulk-download; comply with tile usage policy.
- Tilt is NOT real 3D terrain, and there is no offline MapLibre tile pack, UTM grid or anti-drift implementation.
- MapLibre distant view has markers, but the directional arrow is NOT yet implemented on that view.
- No Android SDK or Gradle build was available in this environment. GitHub Actions compilation is REQUIRED.
- Review the GitHub Actions build log before attempting installation.
