package com.jams.gpspointfinder;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.*;
import android.location.*;
import android.media.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.util.Locale;
import java.util.regex.*;

public class MainActivity extends Activity implements LocationListener, SensorEventListener {
  EditText coordinates;
  TextView info, arrow, status;
  LocationManager lm;
  SensorManager sm;
  Sensor rotation;
  Location current;
  double targetLat=Double.NaN,targetLon=Double.NaN, azimuth=0;
  boolean tracking=false, sound=true;
  final Handler handler=new Handler(Looper.getMainLooper());
  ToneGenerator tone;
  Vibrator vibrator;
  long lastBeep=0;
  LinearLayout page;
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    lm=(LocationManager)getSystemService(LOCATION_SERVICE);
    sm=(SensorManager)getSystemService(SENSOR_SERVICE);
    rotation=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
    tone=new ToneGenerator(AudioManager.STREAM_MUSIC,85);
    page=new LinearLayout(this); page.setOrientation(1); page.setPadding(24,24,24,24); page.setBackgroundColor(Color.rgb(15,35,30));
    ScrollView scroll=new ScrollView(this); scroll.addView(page); setContentView(scroll);
    title("📍 GPS Point Finder v2.0",27);
    text("Masukkan Latitude, Longitude (contoh 6.6983512, 116.7967728). Boleh juga kongsi pautan Google Maps ke aplikasi ini.");
    coordinates=new EditText(this); coordinates.setSingleLine(false); coordinates.setMinLines(2);
    coordinates.setTextColor(Color.WHITE); coordinates.setHintTextColor(Color.LTGRAY);
    coordinates.setHint("6.6983512, 116.7967728"); page.addView(coordinates);
    button("Tetapkan titik sasaran",()->setTarget());
    button("GPS Saya / Mula Cari + Bunyi",()->start());
    button("Henti",()->stop());
    button("Buka arah dalam Google Maps",()->openMaps());
    arrow=title("↑",75);
    info=title("Jarak: — m\nKetepatan: — m\nBearing: —°",23);
    status=title("Menunggu koordinat sasaran",16);
    text("Bunyi dan getaran dipercepat apabila semakin hampir. GPS di bawah pokok boleh tersasar beberapa meter. Bukan alat ukur hak milik tanah.");
    Intent in=getIntent(); if(Intent.ACTION_SEND.equals(in.getAction()) && in.getStringExtra(Intent.EXTRA_TEXT)!=null) {
      coordinates.setText(in.getStringExtra(Intent.EXTRA_TEXT)); setTarget();
    }
    handler.post(ticker);
  }
  TextView title(String s,int size){TextView t=new TextView(this); t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(0,14,0,14);page.addView(t);return t;}
  void text(String s){title(s,15);}
  void button(String s,Runnable r){Button b=new Button(this);b.setText(s);page.addView(b);b.setOnClickListener(v->r.run());}
  void setTarget(){
    String s=coordinates.getText().toString().trim();
    Matcher m=Pattern.compile("(-?\\d{1,3}\\.\\d+)\\s*[, ]\\s*(-?\\d{1,3}\\.\\d+)").matcher(s);
    if(!m.find()) {m=Pattern.compile("(-?\\d{1,3}\\.\\d+)%2C\\s*(-?\\d{1,3}\\.\\d+)",Pattern.CASE_INSENSITIVE).matcher(s);}
    if(!m.find(0)){status.setText("Format tidak dikenali. Masukkan: latitude, longitude");return;}
    double a=Double.parseDouble(m.group(1)),c=Double.parseDouble(m.group(2));
    // Google Maps commonly uses latitude first; Sabah longitude-first input also accepted.
    if(Math.abs(a)>90 && Math.abs(c)<=90){double temp=a;a=c;c=temp;}
    if(Math.abs(a)>90||Math.abs(c)>180){status.setText("Koordinat di luar julat");return;}
    targetLat=a;targetLon=c;status.setText(String.format(Locale.US,"Sasaran: %.7f, %.7f",a,c));update();
  }
  void start(){
    if(Double.isNaN(targetLat))setTarget();
    if(Double.isNaN(targetLat))return;
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1);return;
    }
    try {
      tracking=true;
      lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);
      if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1500,0,this);
      status.setText("Mencari GPS... Sila tunggu di kawasan terbuka");
    }catch(Exception e){status.setText("GPS gagal: "+e.getMessage());}
  }
  @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
    super.onRequestPermissionsResult(r,p,g);
    if(r==1 && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED)start();
    else status.setText("Kebenaran lokasi diperlukan");
  }
  void stop(){tracking=false;lm.removeUpdates(this);status.setText("Pengesanan dihentikan");}
  void openMaps(){
    if(Double.isNaN(targetLat)){setTarget();if(Double.isNaN(targetLat))return;}
    Uri u=Uri.parse(String.format(Locale.US,"google.navigation:q=%.8f,%.8f",targetLat,targetLon));
    try{startActivity(new Intent(Intent.ACTION_VIEW,u).setPackage("com.google.android.apps.maps"));}
    catch(Exception e){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(String.format(Locale.US,"https://www.google.com/maps/dir/?api=1&destination=%.8f,%.8f",targetLat,targetLon))));}
  }
  void update(){
    if(current==null||Double.isNaN(targetLat))return;
    Location target=new Location("target");target.setLatitude(targetLat);target.setLongitude(targetLon);
    float d=current.distanceTo(target),bearing=current.bearingTo(target);
    double relative=((bearing-azimuth)%360+360)%360;
    arrow.setText(String.format(Locale.US,"➤  %.0f°",relative));
    arrow.setRotation((float)relative-90);
    info.setText(String.format(Locale.US,"Jarak: %.1f m\nKetepatan GPS: ±%.1f m\nArah sasaran: %.0f°",d,current.getAccuracy(),(bearing+360)%360));
    if(tracking)status.setText(d<=Math.max(2,current.getAccuracy())?"Dalam zon sasaran (anggaran GPS)":"Bergerak menuju titik");
  }
  final Runnable ticker=new Runnable(){public void run(){
    if(tracking && current!=null && !Double.isNaN(targetLat)){
      Location t=new Location("target");t.setLatitude(targetLat);t.setLongitude(targetLon);
      float d=current.distanceTo(t);
      int interval=d>50?2200:d>30?1500:d>10?850:d>5?450:d>2?220:110;
      long now=SystemClock.elapsedRealtime();
      if(now-lastBeep>=interval){
        tone.startTone(ToneGenerator.TONE_PROP_BEEP,d<=2?400:100);
        if(d<=5 && vibrator!=null && vibrator.hasVibrator())vibrator.vibrate(VibrationEffect.createOneShot(d<=2?250:70,VibrationEffect.DEFAULT_AMPLITUDE));
        lastBeep=now;
      }
    }
    handler.postDelayed(this,100);
  }};
  @Override public void onLocationChanged(Location l){
    if(current==null||l.getAccuracy()<=current.getAccuracy()*1.8f||l.getTime()-current.getTime()>5000){current=l;update();}
  }
  @Override public void onSensorChanged(SensorEvent e){
    if(e.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){
      float[] matrix=new float[9],angles=new float[3];
      SensorManager.getRotationMatrixFromVector(matrix,e.values);
      SensorManager.getOrientation(matrix,angles);
      azimuth=(Math.toDegrees(angles[0])+360)%360;update();
    }
  }
  @Override public void onAccuracyChanged(Sensor s,int a){}
  @Override protected void onResume(){super.onResume();if(rotation!=null)sm.registerListener(this,rotation,SensorManager.SENSOR_DELAY_UI);}
  @Override protected void onPause(){super.onPause();sm.unregisterListener(this);}
  @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacks(ticker);stop();tone.release();}
}
