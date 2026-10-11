package com.jams.gpspointfinder;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.*;
import android.location.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;
import java.util.regex.*;

public class MainActivity extends Activity implements LocationListener, SensorEventListener {
    EditText coordinates;
    TextView info, arrow, status, diagnostics;
    LocationManager lm;
    SensorManager sm;
    Sensor rotation;
    Location current;
    double targetLat = Double.NaN, targetLon = Double.NaN, azimuth = 0;
    boolean tracking = false;
    boolean hasHeading = false;
    final Handler handler = new Handler(Looper.getMainLooper());
    ToneGenerator tone;
    Vibrator vibrator;
    long lastBeep = 0;
    LinearLayout page;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 24, 24, 24);
        page.setBackgroundColor(Color.rgb(15, 35, 30));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        setContentView(scroll);
        title("📍 GPS Point Finder v2.1", 27);
        text("Masukkan Latitude, Longitude. Contoh: 6.6983512, 116.7967728. Boleh juga kongsi koordinat Google Maps.");
        coordinates = new EditText(this);
        coordinates.setMinLines(2);
        coordinates.setTextColor(Color.WHITE);
        coordinates.setHintTextColor(Color.LTGRAY);
        coordinates.setHint("6.6983512, 116.7967728");
        page.addView(coordinates);
        button("TETAPKAN TITIK SASARAN", this::setTarget);
        button("GPS SAYA / MULA CARI + BUNYI", this::start);
        button("HENTI", this::stop);
        button("BUKA ARAH DALAM GOOGLE MAPS", this::openMaps);
        arrow = title("↑", 75);
        info = title("Jarak: — m\nKetepatan: — m\nBearing: —°", 23);
        status = title("Menunggu koordinat sasaran", 16);
        diagnostics = title("GPS: Belum dimulakan", 15);
        text("Bunyi dan getaran semakin cepat apabila hampir. Ketepatan GPS boleh merosot di bawah pokok atau bangunan. Bukan alat ukur hak milik tanah.");
        Intent in = getIntent();
        if (Intent.ACTION_SEND.equals(in.getAction()) && in.getStringExtra(Intent.EXTRA_TEXT) != null) {
            coordinates.setText(in.getStringExtra(Intent.EXTRA_TEXT));
            setTarget();
        }
        handler.post(ticker);
    }

    TextView title(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE);
        t.setPadding(0, 14, 0, 14); page.addView(t); return t;
    }
    void text(String s) { title(s, 15); }
    void button(String s, Runnable r) {
        Button b = new Button(this); b.setText(s); page.addView(b);
        b.setOnClickListener(v -> r.run());
    }

    void setTarget() {
        String s = coordinates.getText().toString().trim();
        Matcher m = Pattern.compile("(-?\\d{1,3}(?:\\.\\d+)?)\\s*(?:,|%2C|\\s)\\s*(-?\\d{1,3}(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE).matcher(s);
        if (!m.find()) {
            status.setText("Format tidak dikenali. Masukkan: latitude, longitude (atau longitude, latitude)");
            return;
        }
        double a = Double.parseDouble(m.group(1)), c = Double.parseDouble(m.group(2));
        if (Math.abs(a) > 90 && Math.abs(c) <= 90) { double tmp = a; a = c; c = tmp; }
        if (Math.abs(a) > 90 || Math.abs(c) > 180) {
            status.setText("Koordinat di luar julat"); return;
        }
        targetLat = a; targetLon = c;
        status.setText(String.format(Locale.US, "Sasaran: %.7f, %.7f. Tekan MULA CARI.", a, c));
        update();
    }

    void start() {
        if (Double.isNaN(targetLat)) setTarget();
        if (Double.isNaN(targetLat)) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }
        try {
            boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!gps && !net) {
                tracking = false;
                status.setText("Lokasi telefon dimatikan. Hidupkan Location dalam Settings.");
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return;
            }
            lm.removeUpdates(this);
            current = null;
            tracking = true;
            if (gps) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            if (net) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500, 0, this);
            status.setText("Mencari lokasi semasa... Cuba kawasan terbuka.");
            updateDiagnostics();
        } catch (Exception e) {
            tracking = false;
            status.setText("GPS gagal: " + e.getMessage());
        }
    }

    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (r == 1 && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) start();
        else status.setText("Kebenaran lokasi tepat diperlukan");
    }

    void stop() {
        tracking = false;
        if (lm != null) lm.removeUpdates(this);
        if (status != null) status.setText("Pengesanan dihentikan");
        if (tone != null) tone.stopTone();
    }

    void openMaps() {
        if (Double.isNaN(targetLat)) { setTarget(); if (Double.isNaN(targetLat)) return; }
        Uri u = Uri.parse(String.format(Locale.US, "google.navigation:q=%.8f,%.8f", targetLat, targetLon));
        try { startActivity(new Intent(Intent.ACTION_VIEW, u).setPackage("com.google.android.apps.maps")); }
        catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US,
                "https://www.google.com/maps/dir/?api=1&destination=%.8f,%.8f", targetLat, targetLon)))); }
            catch (Exception ex) { status.setText("Tidak dapat membuka aplikasi peta"); }
        }
    }

    void update() {
        if (current == null || Double.isNaN(targetLat)) return;
        Location target = new Location("target");
        target.setLatitude(targetLat); target.setLongitude(targetLon);
        float d = current.distanceTo(target);
        float bearing = (current.bearingTo(target) + 360) % 360;
        if (hasHeading) {
            double relative = (bearing - azimuth + 360) % 360;
            arrow.setText("↑");
            arrow.setRotation((float) relative);
        } else {
            arrow.setText("Arah kompas tidak tersedia");
            arrow.setTextSize(20);
            arrow.setRotation(0);
        }
        info.setText(String.format(Locale.US,
            "Jarak: %.1f m\nKetepatan GPS: ±%.1f m\nBearing sasaran: %.0f°", d, current.getAccuracy(), bearing));
        if (tracking) status.setText(d <= Math.max(2, current.getAccuracy())
            ? "Dalam zon sasaran (anggaran GPS; bukan titik tepat)" : "Lokasi diterima — bergerak menuju sasaran");
        updateDiagnostics();
    }

    void updateDiagnostics() {
        if (diagnostics == null) return;
        boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (current == null) {
            diagnostics.setText("GPS provider: " + (gps ? "ON" : "OFF") +
                " | Network: " + (net ? "ON" : "OFF") +
                "\nLokasi semasa: belum diterima" +
                (rotation == null ? "\nSensor kompas: tidak tersedia" : ""));
            return;
        }
        long age = Math.max(0, (SystemClock.elapsedRealtimeNanos() - current.getElapsedRealtimeNanos()) / 1000000000L);
        diagnostics.setText(String.format(Locale.US,
            "Lokasi saya: %.7f, %.7f\nSumber: %s | Usia bacaan: %d saat\nGPS: %s | Network: %s | Kompas: %s",
            current.getLatitude(), current.getLongitude(), current.getProvider(), age,
            gps ? "ON" : "OFF", net ? "ON" : "OFF", hasHeading ? "ON" : "Tiada"));
    }

    final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (tracking) {
                updateDiagnostics();
                if (current != null && !Double.isNaN(targetLat)) {
                    long age = Math.max(0, (SystemClock.elapsedRealtimeNanos() - current.getElapsedRealtimeNanos()) / 1000000000L);
                    if (age > 10) {
                        status.setText("Bacaan GPS sudah lama (" + age + " saat). Menunggu lokasi baharu...");
                    } else {
                        Location t = new Location("target");
                        t.setLatitude(targetLat); t.setLongitude(targetLon);
                        float d = current.distanceTo(t);
                        int interval = d > 50 ? 2200 : d > 30 ? 1500 : d > 10 ? 850 : d > 5 ? 450 : d > 2 ? 220 : 110;
                        long now = SystemClock.elapsedRealtime();
                        if (now - lastBeep >= interval) {
                            tone.startTone(ToneGenerator.TONE_PROP_BEEP, d <= 2 ? 400 : 100);
                            if (d <= 5 && vibrator != null && vibrator.hasVibrator())
                                vibrator.vibrate(VibrationEffect.createOneShot(d <= 2 ? 250 : 70, VibrationEffect.DEFAULT_AMPLITUDE));
                            lastBeep = now;
                        }
                    }
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override public void onLocationChanged(Location l) {
        if (l == null || !l.hasAccuracy()) return;
        if (current == null || l.getElapsedRealtimeNanos() > current.getElapsedRealtimeNanos()) {
            current = l;
            update();
        }
    }
    @Override public void onProviderDisabled(String provider) {
        if (tracking) status.setText("Provider dimatikan: " + provider);
        updateDiagnostics();
    }
    @Override public void onProviderEnabled(String provider) { updateDiagnostics(); }
    @Override public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] matrix = new float[9], angles = new float[3];
            SensorManager.getRotationMatrixFromVector(matrix, e.values);
            SensorManager.getOrientation(matrix, angles);
            azimuth = (Math.toDegrees(angles[0]) + 360) % 360;
            hasHeading = true;
            update();
        }
    }
    @Override public void onAccuracyChanged(Sensor s, int a) {}
    @Override protected void onResume() {
        super.onResume();
        if (rotation != null) sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
        if (tracking && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) start();
    }
    @Override protected void onPause() {
        super.onPause(); sm.unregisterListener(this);
        if (tracking) lm.removeUpdates(this);
        if (tone != null) tone.stopTone();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker); stop(); tone.release(); super.onDestroy();
    }
}
