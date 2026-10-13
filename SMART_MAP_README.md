# JAIMS GPS PRO v3.5 SMART MAP — experimental source

- MapLibre OpenGL vector map, OpenFreeMap Liberty style; internet needed for uncached tiles.
- AUTO: tilted perspective map >30m; top-down osmdroid 2D <=30m; return tilted map >45m.
- 4m zone of search is indicative, not a surveyed boundary.
- Blue location arrow uses active target bearing (rather than movement course); red pin is destination.
- Tap SMART MAP button to cycle AUTO / 2D / 3D.
- 3D is a **tilted perspective** (55 degrees), not full satellite imagery or DEM terrain relief.
- Existing track and waypoint data remain stored by the old app code.
- Not compiled or device-tested. GitHub Actions workflow builds the APK; verify MapLibre version and runtime compatibility.
