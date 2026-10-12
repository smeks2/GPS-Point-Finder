package com.jams.gpspointfinder;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import org.maplibre.android.MapLibre;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;

/** Distant navigation map. Tilt is perspective, not elevation terrain. */
public final class MapLibreNavigationView extends FrameLayout {
 private final MapView mapView;
 private MapLibreMap map;
 private Marker targetMarker;
 private Marker userMarker;
 private double lat=Double.NaN,lon=Double.NaN,targetLat=Double.NaN,targetLon=Double.NaN;
 private double targetBearing=0;
 private final TextView message;
 private static final String RASTER_STYLE="{\"version\":8,\"name\":\"JAIMS OSM\",\"sources\":{\"osm\":{\"type\":\"raster\",\"tiles\":[\"https://tile.openstreetmap.org/{z}/{x}/{y}.png\"],\"tileSize\":256,\"attribution\":\"© OpenStreetMap contributors\"}},\"layers\":[{\"id\":\"osm\",\"type\":\"raster\",\"source\":\"osm\"}]}";
 public MapLibreNavigationView(Context c,Bundle savedState){
  super(c); MapLibre.getInstance(c);mapView=new MapView(c);addView(mapView,new FrameLayout.LayoutParams(-1,-1));
  message=new TextView(c);message.setText("MAPLIBRE • PANDANGAN CONDONG (bukan terrain 3D)\nPeta memerlukan internet untuk tile yang belum tersedia.");message.setTextColor(Color.WHITE);message.setBackgroundColor(0xBB18382C);message.setPadding(12,8,12,8);
  FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);addView(message,lp);
  mapView.onCreate(savedState);
  mapView.getMapAsync(m->{map=m;map.setStyle(new Style.Builder().fromJson(RASTER_STYLE),style->refresh());});
 }
 public void setPositions(double a,double b,double ta,double tb,double bearing){lat=a;lon=b;targetLat=ta;targetLon=tb;targetBearing=Double.isFinite(bearing)?bearing:0;refresh();}
 private void refresh(){if(map==null)return;
  if(Double.isFinite(lat)&&Double.isFinite(lon)){
   LatLng here=new LatLng(lat,lon);
   map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(here).zoom(17).tilt(55).bearing(targetBearing).build()),400);
   if(userMarker==null)userMarker=map.addMarker(new MarkerOptions().position(here).title("GPS saya"));else userMarker.setPosition(here);
  }
  if(Double.isFinite(targetLat)&&Double.isFinite(targetLon)){
   LatLng pin=new LatLng(targetLat,targetLon);
   if(targetMarker==null)targetMarker=map.addMarker(new MarkerOptions().position(pin).title("Pin sasaran"));else targetMarker.setPosition(pin);
  }
 }
 public void onStart(){mapView.onStart();} public void onResume(){mapView.onResume();}
 public void onPause(){mapView.onPause();}public void onStop(){mapView.onStop();}
 public void onLowMemory(){mapView.onLowMemory();}public void onDestroy(){mapView.onDestroy();}
}
