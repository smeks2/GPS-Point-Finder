package com.jams.gpspointfinder;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.*;import android.location.*;import android.media.*;import android.os.*;import org.json.*;import java.util.*;
public class TrackingService extends Service implements LocationListener {
 public static final String ACTION_START="com.jams.gpspointfinder.START",ACTION_STOP="com.jams.gpspointfinder.STOP",ACTION_UPDATE="com.jams.gpspointfinder.UPDATE",ACTION_CONFIG="com.jams.gpspointfinder.CONFIG";
 public static final String EXTRA_LAT="lat",EXTRA_LON="lon";
 private LocationManager lm;private Handler h;private ToneGenerator tone;private Vibrator vib;
 private double lat=Double.NaN,lon=Double.NaN;private boolean running=false,recording=false,beepEnabled=false,findEnabled=false;
 private float filtered=Float.NaN;private long lastBeep=0,lastNotify=0,lastTrend=0;private float oldDist=Float.NaN;private Location previous;
 private JSONArray track=new JSONArray();private long lastSaved=0;private double total=0;
 private final Runnable ticker=new Runnable(){public void run(){if(!running)return;long now=SystemClock.elapsedRealtime();if(beepEnabled&&findEnabled&&!Float.isNaN(filtered)&&!Double.isNaN(lat)){
 long interval=filtered>100?4000:filtered>50?3000:filtered>20?2000:filtered>10?1000:filtered>5?650:filtered>2?450:350;
 if(now-lastBeep>=interval){tone.startTone(ToneGenerator.TONE_PROP_BEEP,90);if(filtered<=5&&previous!=null&&previous.hasAccuracy()&&previous.getAccuracy()<=5&&vib.hasVibrator())vib.vibrate(VibrationEffect.createOneShot(70,VibrationEffect.DEFAULT_AMPLITUDE));lastBeep=now;}}
 h.postDelayed(this,100);}};
 @Override public void onCreate(){super.onCreate();lm=(LocationManager)getSystemService(LOCATION_SERVICE);h=new Handler(Looper.getMainLooper());tone=new ToneGenerator(AudioManager.STREAM_MUSIC,75);vib=(Vibrator)getSystemService(VIBRATOR_SERVICE);
 ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("tracking_pro","GPS PRO Tracking",NotificationManager.IMPORTANCE_LOW));}
 private Notification note(String message){Intent i=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,TrackingService.class).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 return new Notification.Builder(this,"tracking_pro").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("GPS Point Finder PRO").setContentText(message).setContentIntent(pi).addAction(android.R.drawable.ic_media_pause,"Henti",stop).setOngoing(true).build();}
 private void saveMode(){getSharedPreferences("gps_mode",MODE_PRIVATE).edit().putBoolean("running",running).putBoolean("record",recording).putBoolean("find",findEnabled).putBoolean("beep",beepEnabled).apply();}
 @Override public int onStartCommand(Intent i,int flags,int id){
 if(i!=null&&ACTION_STOP.equals(i.getAction())){running=false;saveMode();stopSelf();return START_NOT_STICKY;}
 if(i==null)return START_NOT_STICKY;
 boolean config=ACTION_CONFIG.equals(i.getAction());
 if(!config&&!ACTION_START.equals(i.getAction()))return START_NOT_STICKY;
 if(config&&!running)return START_NOT_STICKY;
 if(i.hasExtra("record"))recording=i.getBooleanExtra("record",recording);
 if(i.hasExtra("find"))findEnabled=i.getBooleanExtra("find",findEnabled);
 if(i.hasExtra("beep"))beepEnabled=i.getBooleanExtra("beep",beepEnabled);
 if(i.hasExtra(EXTRA_LAT)){lat=i.getDoubleExtra(EXTRA_LAT,Double.NaN);lon=i.getDoubleExtra(EXTRA_LON,Double.NaN);}
 if(!running){
  running=true;filtered=Float.NaN;previous=null;total=0;track=new JSONArray();lastSaved=0;
  try{String previousTrack=getSharedPreferences("track",MODE_PRIVATE).getString("latest","[]");track=new JSONArray(previousTrack);}catch(Exception ignored){}
  try{startForeground(42,note("GPS sedang bermula..."),ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
   if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
   lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);h.removeCallbacks(ticker);h.post(ticker);
  }catch(Exception e){running=false;saveMode();stopSelf();return START_NOT_STICKY;}
 }
 if(!findEnabled){filtered=Float.NaN;oldDist=Float.NaN;}
 saveMode();
 ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(42,note("Rekod "+(recording?"ON":"OFF")+" | Find "+(findEnabled?"ON":"OFF")+" | Beep "+(beepEnabled?"ON":"OFF")));
 return START_STICKY;
 }
 @Override public void onLocationChanged(Location l){if(!running||!l.hasAccuracy()||l.getAccuracy()>60)return;if(previous!=null&&l.getElapsedRealtimeNanos()<=previous.getElapsedRealtimeNanos())return;
 long now=SystemClock.elapsedRealtime();if(previous!=null&&l.distanceTo(previous)>Math.max(50,l.getAccuracy()*4))return;
 if(recording&&(previous==null||l.distanceTo(previous)>Math.max(2,(l.getAccuracy()+previous.getAccuracy())*.35f))){
 try{JSONObject p=new JSONObject();p.put("lat",l.getLatitude());p.put("lon",l.getLongitude());p.put("alt",l.hasAltitude()?l.getAltitude():JSONObject.NULL);p.put("acc",l.getAccuracy());p.put("time",System.currentTimeMillis());track.put(p);if(previous!=null)total+=l.distanceTo(previous);}catch(Exception ignored){}
 }
 previous=l;
 float d=-1,b=Float.NaN;if(findEnabled&&!Double.isNaN(lat)){Location target=new Location("target");target.setLatitude(lat);target.setLongitude(lon);d=l.distanceTo(target);b=l.bearingTo(target);filtered=Float.isNaN(filtered)?d:filtered*.65f+d*.35f;}
 if(beepEnabled&&findEnabled&&!Float.isNaN(oldDist)&&!Float.isNaN(filtered)&&filtered-oldDist>Math.max(6,l.getAccuracy())&&now-lastTrend>10000){tone.startTone(ToneGenerator.TONE_PROP_NACK,220);lastTrend=now;}oldDist=filtered;
 if(now-lastSaved>15000){getSharedPreferences("track",MODE_PRIVATE).edit().putString("latest",track.toString()).apply();lastSaved=now;}
 Intent update=new Intent(ACTION_UPDATE).setPackage(getPackageName());update.putExtra("lat",l.getLatitude());update.putExtra("lon",l.getLongitude());update.putExtra("distance",d);update.putExtra("accuracy",l.getAccuracy());update.putExtra("bearing",b);update.putExtra("altitude",l.hasAltitude()?l.getAltitude():Double.NaN);update.putExtra("course",l.hasBearing()&&l.hasSpeed()&&l.getSpeed()>=1.0?l.getBearing():Float.NaN);update.putExtra("total",total);sendBroadcast(update);
 if(now-lastNotify>5000){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(42,note(d>=0?String.format(Locale.US,"Pin %.1f m | GPS ±%.1f m",d,l.getAccuracy()):"Rakaman jejak aktif"));lastNotify=now;}}
 @Override public void onDestroy(){running=false;saveMode();if(h!=null)h.removeCallbacks(ticker);if(lm!=null)lm.removeUpdates(this);getSharedPreferences("track",MODE_PRIVATE).edit().putString("latest",track.toString()).apply();if(tone!=null)tone.release();super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
