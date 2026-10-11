package com.jams.gpspointfinder;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.*;
import android.media.*;
import android.os.*;
import java.util.*;

public class TrackingService extends Service implements LocationListener {
    public static final String ACTION_START="com.jams.gpspointfinder.START";
    public static final String ACTION_STOP="com.jams.gpspointfinder.STOP";
    public static final String ACTION_UPDATE="com.jams.gpspointfinder.UPDATE";
    public static final String EXTRA_LAT="lat", EXTRA_LON="lon";
    private static final String CHANNEL="gps_tracking";
    private LocationManager lm;
    private ToneGenerator tone;
    private Vibrator vibrator;
    private Handler handler;
    private Location current;
    private double targetLat, targetLon;
    private long lastBeep, lastTrendAlert;
    private float smoothDistance=Float.NaN, baselineDistance=Float.NaN;
    private long baselineTime;
    private boolean running=false;
    private final Runnable beeper=new Runnable(){ @Override public void run(){
        if(!running)return;
        if(current!=null && !Float.isNaN(smoothDistance)){
            float d=smoothDistance;
            long interval=d>100?4000:d>50?3000:d>20?2000:d>10?1000:d>5?500:d>2?250:180;
            long now=SystemClock.elapsedRealtime();
            if(now-lastBeep>=interval){
                // Accuracy limits near-target claims: don't sound an arrival signal on a poor fix.
                boolean close=d<=5 && current.hasAccuracy() && current.getAccuracy()<=5;
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, d<=2?110:85);
                if(close && vibrator!=null && vibrator.hasVibrator())
                    vibrator.vibrate(VibrationEffect.createOneShot(d<=2?170:65,VibrationEffect.DEFAULT_AMPLITUDE));
                lastBeep=now;
            }
        }
        handler.postDelayed(this,100);
    }};
    @Override public void onCreate(){super.onCreate();lm=(LocationManager)getSystemService(LOCATION_SERVICE);
        vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);tone=new ToneGenerator(AudioManager.STREAM_MUSIC,85);
        handler=new Handler(Looper.getMainLooper());
        NotificationChannel c=new NotificationChannel(CHANNEL,"Pencarian GPS",NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
    }
    private Notification notification(String message){
        Intent i=new Intent(this,MainActivity.class);i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,TrackingService.class).setAction(ACTION_STOP);
        PendingIntent sp=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("GPS Point Finder — mencari sasaran").setContentText(message)
            .setContentIntent(pi).addAction(android.R.drawable.ic_media_pause,"Henti",sp)
            .setOngoing(true).build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null && ACTION_STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(intent==null || !ACTION_START.equals(intent.getAction()))return START_NOT_STICKY;
        targetLat=intent.getDoubleExtra(EXTRA_LAT,Double.NaN);targetLon=intent.getDoubleExtra(EXTRA_LON,Double.NaN);
        if(Double.isNaN(targetLat)||Double.isNaN(targetLon)){stopSelf();return START_NOT_STICKY;}
        try{
            startForeground(42,notification("Menunggu lokasi GPS"),ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
            lm.removeUpdates(this);
            if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);
            if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1500,0,this);
            running=true;current=null;smoothDistance=Float.NaN;baselineDistance=Float.NaN;lastBeep=0;lastTrendAlert=0;
            handler.removeCallbacks(beeper);handler.post(beeper);
        }catch(Exception e){broadcast(-1,-1,0,"GPS gagal: "+e.getMessage());stopSelf();}
        return START_NOT_STICKY;
    }
    @Override public void onLocationChanged(Location l){
        if(!running)return;
        if(current!=null && l.getElapsedRealtimeNanos()<=current.getElapsedRealtimeNanos())return;
        if(!l.hasAccuracy() || l.getAccuracy()>75)return;
        current=l;
        Location target=new Location("target");target.setLatitude(targetLat);target.setLongitude(targetLon);
        float raw=l.distanceTo(target);
        smoothDistance=Float.isNaN(smoothDistance)?raw:0.65f*smoothDistance+0.35f*raw;
        long now=SystemClock.elapsedRealtime();
        if(Float.isNaN(baselineDistance)){baselineDistance=smoothDistance;baselineTime=now;}
        String trend="";
        if(now-baselineTime>=7000){
            float delta=smoothDistance-baselineDistance;
            float threshold=Math.max(5f,Math.min(15f,l.getAccuracy()));
            if(delta>threshold){
                trend=" | Semakin jauh";
                if(now-lastTrendAlert>10000){tone.startTone(ToneGenerator.TONE_PROP_NACK,250);lastTrendAlert=now;}
            }else if(delta< -threshold)trend=" | Semakin dekat";
            baselineDistance=smoothDistance;baselineTime=now;
        }
        String msg=String.format(Locale.US,"Jarak %.1f m | ±%.1f m%s",raw,l.getAccuracy(),trend);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(42,notification(msg));
        broadcast(raw,l.getAccuracy(),l.bearingTo(target),msg);
    }
    private void broadcast(float d,float accuracy,float bearing,String message){
        Intent i=new Intent(ACTION_UPDATE).setPackage(getPackageName());
        i.putExtra("distance",d);i.putExtra("accuracy",accuracy);i.putExtra("bearing",bearing);
        i.putExtra("message",message);sendBroadcast(i);
    }
    @Override public void onDestroy(){running=false;if(handler!=null)handler.removeCallbacks(beeper);
        if(lm!=null)lm.removeUpdates(this);if(tone!=null)tone.release();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
    @Override public void onProviderEnabled(String provider){}
    @Override public void onProviderDisabled(String provider){}
}
