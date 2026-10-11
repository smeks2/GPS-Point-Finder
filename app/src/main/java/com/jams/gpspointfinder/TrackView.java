package com.jams.gpspointfinder;
import android.content.*;import android.graphics.*;import android.view.*;import org.json.*;
public class TrackView extends View {
 final Paint p=new Paint(3);final MainActivity a;TrackView(MainActivity a){super(a);this.a=a;setBackgroundColor(Color.rgb(218,235,221));}
 void dot(Canvas c,float x,float y,int color,float r){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();p.setColor(Color.rgb(40,78,57));p.setTextSize(28);c.drawText("JEJAK OFFLINE • SKEMATIK (BUKAN PETA TOPOGRAFI)",12,35,p);
 try{JSONArray arr=new JSONArray(a.getSharedPreferences("track",Context.MODE_PRIVATE).getString("latest","[]"));double centerLat=Double.isNaN(a.curLat)?(Double.isNaN(a.targetLat)?0:a.targetLat):a.curLat;double centerLon=Double.isNaN(a.curLon)?(Double.isNaN(a.targetLon)?0:a.targetLon):a.curLon;
 double scale=0.00002;for(int j=0;j<arr.length();j++){JSONObject q=arr.getJSONObject(j);scale=Math.max(scale,Math.max(Math.abs(q.getDouble("lat")-centerLat),Math.abs((q.getDouble("lon")-centerLon)*Math.cos(Math.toRadians(centerLat))))*1.4);}if(!Double.isNaN(a.targetLat))scale=Math.max(scale,Math.max(Math.abs(a.targetLat-centerLat),Math.abs((a.targetLon-centerLon)*Math.cos(Math.toRadians(centerLat))))*1.4);
 float factor=(float)(Math.min(w,h)*.43/scale);Path path=new Path();for(int j=0;j<arr.length();j++){JSONObject q=arr.getJSONObject(j);float x=w/2+(float)((q.getDouble("lon")-centerLon)*Math.cos(Math.toRadians(centerLat)))*factor;float y=h/2-(float)(q.getDouble("lat")-centerLat)*factor;if(j==0)path.moveTo(x,y);else path.lineTo(x,y);}p.setColor(Color.rgb(8,116,155));p.setStrokeWidth(6);p.setStyle(Paint.Style.STROKE);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
 if(!Double.isNaN(a.targetLat)){float x=w/2+(float)((a.targetLon-centerLon)*Math.cos(Math.toRadians(centerLat)))*factor;float y=h/2-(float)(a.targetLat-centerLat)*factor;dot(c,x,y,Color.RED,13);p.setColor(Color.RED);p.setTextSize(24);c.drawText("PIN",x+16,y,p);}
 if(!Double.isNaN(a.curLat))dot(c,w/2,h/2,Color.BLUE,12);
 p.setColor(Color.DKGRAY);p.setTextSize(22);c.drawText("Biru: GPS  •  Merah: Pin  •  Garis: Jejak",12,h-20,p);
 }catch(Exception ignored){}
 }
}
