package com.jams.gpspointfinder;
import android.content.Context;import org.json.*;import java.util.*;
public final class TrailStore {
 private static final String PREF="my_trails";
 private static JSONArray read(Context c,String key){try{return new JSONArray(c.getSharedPreferences(PREF,0).getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
 public static JSONArray history(Context c){return read(c,"history");}
 public static void addWaypoint(Context c,String name,int type,double lat,double lon,float acc){try{JSONArray a=read(c,"current_waypoints");JSONObject p=new JSONObject();p.put("name",name);p.put("type",new String[]{"START","CHECKPOINT","BOUNDARY","END"}[Math.max(0,Math.min(3,type))]);p.put("lat",lat);p.put("lon",lon);p.put("accuracy",acc);p.put("time",System.currentTimeMillis());a.put(p);c.getSharedPreferences(PREF,0).edit().putString("current_waypoints",a.toString()).apply();}catch(Exception ignored){}}
 public static int saveCurrent(Context c,String title){try{JSONArray track=new JSONArray(c.getSharedPreferences("track",0).getString("latest","[]"));JSONArray waypoints=read(c,"current_waypoints");if(track.length()==0&&waypoints.length()==0)return 0;JSONObject session=new JSONObject();session.put("name",title);session.put("created",System.currentTimeMillis());session.put("track",track);session.put("waypoints",waypoints);JSONArray old=history(c),updated=new JSONArray();updated.put(session);for(int i=0;i<old.length()&&i<49;i++)updated.put(old.get(i));c.getSharedPreferences(PREF,0).edit().putString("history",updated.toString()).putString("current_waypoints","[]").apply();return track.length();}catch(Exception e){return 0;}}
}
