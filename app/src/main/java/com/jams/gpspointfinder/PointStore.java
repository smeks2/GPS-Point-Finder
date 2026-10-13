package com.jams.gpspointfinder;
import android.content.*;import org.json.*;import java.util.*;
public class PointStore {
 private final android.content.SharedPreferences prefs;
 PointStore(Context c){prefs=c.getSharedPreferences("points_v3",Context.MODE_PRIVATE);}
 JSONArray points(){try{return new JSONArray(prefs.getString("points","[]"));}catch(Exception e){return new JSONArray();}}
 void add(String name,double lat,double lon,float accuracy){try{JSONArray a=points();JSONObject o=new JSONObject();o.put("name",name);o.put("lat",lat);o.put("lon",lon);o.put("accuracy",accuracy);o.put("time",System.currentTimeMillis());a.put(o);prefs.edit().putString("points",a.toString()).apply();}catch(Exception ignored){}}
}
