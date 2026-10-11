package com.jams.gpspointfinder;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;
import java.util.regex.*;

public class MainActivity extends Activity implements SensorEventListener {
    EditText coordinates; TextView info, arrow, status; LinearLayout page;
    SensorManager sm; Sensor rotation;
    double targetLat=Double.NaN,targetLon=Double.NaN,azimuth=0;
    float targetBearing=Float.NaN;
    boolean tracking=false;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
        float d=i.getFloatExtra("distance",-1),a=i.getFloatExtra("accuracy",-1);
        targetBearing=i.getFloatExtra("bearing",Float.NaN);
        if(d>=0){info.setText(String.format(Locale.US,"Jarak: %.1f m\nKetepatan GPS: ±%.1f m\nArah sasaran: %.0f°",d,a,targetBearing));
            status.setText(i.getStringExtra("message"));updateArrow();}
        else status.setText(i.getStringExtra("message"));
    }};
    @Override public void onCreate(Bundle b){super.onCreate(b);
        sm=(SensorManager)getSystemService(SENSOR_SERVICE);rotation=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        page=new LinearLayout(this);page.setOrientation(1);page.setPadding(24,24,24,24);page.setBackgroundColor(Color.rgb(15,35,30));
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
        title("📍 GPS Point Finder v2.1",27);
        text("Masukkan latitude, longitude. Bunyi terus berjalan apabila aplikasi diminimumkan atau skrin dikunci.");
        coordinates=new EditText(this);coordinates.setTextColor(Color.WHITE);coordinates.setHintTextColor(Color.LTGRAY);
        coordinates.setHint("6.6983512, 116.7967728");coordinates.setMinLines(2);page.addView(coordinates);
        button("TETAPKAN TITIK SASARAN",()->setTarget());
        button("GPS SAYA / MULA CARI + BUNYI",()->start());
        button("HENTI",()->stopTracking());
        button("BUKA ARAH DALAM GOOGLE MAPS",()->openMaps());
        // Keep the arrow in its own fixed-height region, so rotation cannot cover the distance text.
        android.widget.FrameLayout compass=new android.widget.FrameLayout(this);
        compass.setLayoutParams(new LinearLayout.LayoutParams(-1,260));page.addView(compass);
        arrow=new TextView(this);arrow.setText("➤");arrow.setTextColor(Color.WHITE);arrow.setTextSize(72);arrow.setGravity(Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams ap=new android.widget.FrameLayout.LayoutParams(200,200,Gravity.CENTER);compass.addView(arrow,ap);
        info=title("Jarak: — m\nKetepatan: — m\nArah sasaran: —°",23);
        status=title("Menunggu koordinat sasaran",16);
        text("GPS di bawah pokok boleh tersasar beberapa meter. Bukan alat ukur hak milik tanah.");
        android.content.SharedPreferences prefs=getSharedPreferences("gps",MODE_PRIVATE);
        String saved=prefs.getString("coords","");if(!saved.isEmpty()){coordinates.setText(saved);setTarget();}
        Intent in=getIntent();if(Intent.ACTION_SEND.equals(in.getAction())&&in.getStringExtra(Intent.EXTRA_TEXT)!=null){
            coordinates.setText(in.getStringExtra(Intent.EXTRA_TEXT));setTarget();}
    }
    TextView title(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(0,14,0,14);page.addView(t);return t;}
    void text(String s){title(s,15);}
    void button(String s,Runnable r){Button b=new Button(this);b.setText(s);page.addView(b);b.setOnClickListener(v->r.run());}
    void setTarget(){String s=coordinates.getText().toString().trim();
        Matcher m=Pattern.compile("(-?\\d{1,3}\\.\\d+)\\s*(?:,|%2C|\\s)\\s*(-?\\d{1,3}\\.\\d+)",Pattern.CASE_INSENSITIVE).matcher(s);
        if(!m.find()){status.setText("Format tidak dikenali. Masukkan: latitude, longitude");return;}
        double a=Double.parseDouble(m.group(1)),c=Double.parseDouble(m.group(2));
        if(Math.abs(a)>90&&Math.abs(c)<=90){double t=a;a=c;c=t;}
        if(Math.abs(a)>90||Math.abs(c)>180){status.setText("Koordinat di luar julat");return;}
        targetLat=a;targetLon=c;getSharedPreferences("gps",MODE_PRIVATE).edit().putString("coords",String.format(Locale.US,"%.8f, %.8f",a,c)).apply();
        status.setText(String.format(Locale.US,"Sasaran: %.7f, %.7f",a,c));
    }
    void start(){if(Double.isNaN(targetLat))setTarget();if(Double.isNaN(targetLat))return;
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1);return;}
        Intent i=new Intent(this,TrackingService.class).setAction(TrackingService.ACTION_START);
        i.putExtra(TrackingService.EXTRA_LAT,targetLat);i.putExtra(TrackingService.EXTRA_LON,targetLon);
        try{startForegroundService(i);tracking=true;status.setText("Pencarian aktif di latar belakang. Semak notifikasi GPS.");}
        catch(Exception e){status.setText("Tidak dapat mula GPS: "+e.getMessage());}
    }
    void stopTracking(){stopService(new Intent(this,TrackingService.class));tracking=false;status.setText("Pengesanan dihentikan");}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);
        if(r==1&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)start();else status.setText("Benarkan lokasi tepat untuk mencari sasaran");}
    void openMaps(){if(Double.isNaN(targetLat))setTarget();if(Double.isNaN(targetLat))return;
        Uri u=Uri.parse(String.format(Locale.US,"google.navigation:q=%.8f,%.8f",targetLat,targetLon));
        try{startActivity(new Intent(Intent.ACTION_VIEW,u).setPackage("com.google.android.apps.maps"));}
        catch(Exception e){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(String.format(Locale.US,"https://www.google.com/maps/dir/?api=1&destination=%.8f,%.8f",targetLat,targetLon))));}
    }
    void updateArrow(){if(Float.isNaN(targetBearing))return;
        float relative=(float)((targetBearing-azimuth+360)%360);
        arrow.setRotation(relative-90);
    }
    @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){
        float[] matrix=new float[9],angles=new float[3];SensorManager.getRotationMatrixFromVector(matrix,e.values);
        SensorManager.getOrientation(matrix,angles);azimuth=(Math.toDegrees(angles[0])+360)%360;updateArrow();}}
    @Override public void onAccuracyChanged(Sensor s,int a){}
    @Override protected void onResume(){super.onResume();if(rotation!=null)sm.registerListener(this,rotation,SensorManager.SENSOR_DELAY_UI);
        IntentFilter f=new IntentFilter(TrackingService.ACTION_UPDATE);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);}
    @Override protected void onPause(){super.onPause();sm.unregisterListener(this);unregisterReceiver(receiver);}
}
