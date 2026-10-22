package com.jams.gpspointfinder;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Pilot-only on-device reports. No network submission, no public sharing. */
public final class TrafficReportStore {
 private TrafficReportStore() {}
 public static synchronized void add(Context context,String type,double lat,double lon,float accuracy) throws Exception {
  if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat < -90||lat > 90||lon < -180||lon > 180) throw new IllegalArgumentException("Koordinat tidak sah");
  android.content.SharedPreferences prefs=context.getSharedPreferences("jams_traffic_pilot",Context.MODE_PRIVATE);
  JSONArray reports=new JSONArray(prefs.getString("reports","[]"));
  JSONObject report=new JSONObject();
  report.put("type",type);report.put("lat",lat);report.put("lon",lon);
  report.put("accuracy_m",Float.isFinite(accuracy)?accuracy:JSONObject.NULL);
  report.put("created_at_ms",System.currentTimeMillis());
  report.put("verified",false);report.put("sync_status","LOCAL_ONLY");
  reports.put(report);
  if(!prefs.edit().putString("reports",reports.toString()).commit()) throw new IllegalStateException("Gagal tulis storan");
 }
}
