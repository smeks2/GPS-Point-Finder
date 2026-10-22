# JAMS Navigator v4.0 Community Beta — Pelan Pelaksanaan

## Keadaan sebenar pakej ini
Source v3.5.1 asal dikekalkan, dengan penjenamaan pilot, storan laporan trafik **lokal sahaja**, dan skrin penerangan PTT yang **belum aktif**. **Tiada PTT langsung, routing, live traffic, atau backend yang telah dilaksanakan.** APK belum diuji/compile dalam persekitaran ini. Perubahan kod ini ialah langkah asas percubaan, bukan keluaran awam.

## Skop sasaran (Malaysia)
- DRIVE: carian destinasi, pengiraan laluan jalan, turn-by-turn, ETA, rerouting dan audio BM.
- FIELD: kekalkan navigasi terus-ke-pin, trail, GPX, mark point; jangan kelirukan bearing-ke-pin dengan bearing-jalan.
- PTT awam: saluran Malaysia/negeri/daerah; moderator, mute, report abuse, push-to-talk half duplex, token akses.
- PTT konvoi: saluran private dengan invite code, pemilik, kick/mute, expiry.
- Trafik komuniti: jenis insiden, geolokasi, masa, bukti pilihan, confidence, undi pengesahan, tempoh luput, moderasi.
- Hybrid: GPS dan peta offline berlesen; komunikasi dan sync laporan memerlukan internet.

## Cadangan komponen
- MapLibre untuk UI peta; pilih satu renderer utama secara beransur, bukannya menjalankan dua MapView serentak sepanjang masa.
- Routing: GraphHopper/Valhalla/OSRM; mula dengan server ujian. Geocoding Nominatim perlu patuh polisi penggunaan atau gunakan servis sendiri/berlesen.
- Live traffic: API komersial (HERE/TomTom) hanya selepas semak lesen, liputan Malaysia, kadar dan kos; jangan dakwa data Waze tersedia.
- PTT: WebRTC/Opus + signaling server HTTPS/WSS + TURN + identity/auth + room permission + moderation. Android microphone foreground service permission/type, background restrictions, audio focus, headset support.
- Backend laporan: PostgreSQL/PostGIS, HTTPS API, rate limiting, geospatial indexing, expiry/TTL, abuse workflow, audit, data retention.

## Kontrak API yang dicadangkan (belum wujud)
POST /v1/reports {type,lat,lon,accuracy_m,created_at,client_nonce}
GET /v1/reports?bbox=... -> {id,type,lat,lon,created_at,expires_at,verification_status}
POST /v1/reports/{id}/confirm ; POST /v1/reports/{id}/flag
POST /v1/ptt/token {room_id} -> short-lived scoped token
GET /v1/rooms?region=MY-12 ; POST /v1/rooms/private

## Fasa dan kriteria penerimaan
1. **P0 — Stabilkan source**: GitHub Actions build lulus, anak panah biru ke pin, Smart Map 30/45 m, GPS/track tidak regress. Semak source 3D MapLibre terhadap API sebenar.
2. **P1 — Pilot tempatan (pakej ini)**: laporan disimpan lokal; PTT disabled dan jelas dilabel; tiada lokasi dihantar tanpa tindakan pengguna.
3. **P2 — DRIVE**: route & reroute sebenar, ETA, arahan belok BM; uji jalan Sabah dan Semenanjung, offline failover.
4. **P3 — Community**: backend, log masuk pseudonim, laporan dengan pengesahan dan expiry, moderation, anti-spam, paparan live.
5. **P4 — PTT**: audio WebRTC dengan private/public rooms, permission dan keselamatan memandu; uji latency, reconnect, LTE loss, Bluetooth, skrin terkunci.
6. **P5 — Beta awam**: polisi privasi, terma, moderation SOP, kos API/server, load test, data deletion, crash analytics opt-in, ujian lapangan seluruh Malaysia.

## Keselamatan dan privasi
Jangan paparkan lokasi langsung pengguna PTT tanpa opt-in. Jangan simpan audio secara lalai. Papar laporan sebagai tidak disahkan sehingga moderasi. Tiada butang PTT interaktif semasa memandu kecuali kawalan hands-free yang mematuhi peraturan tempatan. Trafik dan jalan ditutup mesti disahkan; jangan guna laporan sebagai jaminan keselamatan.

## Cara build
Unzip dan commit ke repository GitHub; jalankan workflow Build Android APK. Pakej ini belum diuji compile di sini. Jika gagal, ambil mesej error Java/Gradle pertama dalam log. Simpan APK asal sebagai rollback.
