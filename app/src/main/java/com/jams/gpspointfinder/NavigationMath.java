package com.jams.gpspointfinder;
/** Dependency-free navigation geometry, also runnable in desktop JVM tests. */
public final class NavigationMath {
 private NavigationMath() {}
 public static double meters(double a,double b,double c,double d){double r=6371000,p1=Math.toRadians(a),p2=Math.toRadians(c),dp=p2-p1,dl=Math.toRadians(d-b);double h=Math.pow(Math.sin(dp/2),2)+Math.cos(p1)*Math.cos(p2)*Math.pow(Math.sin(dl/2),2);return 2*r*Math.asin(Math.min(1,Math.sqrt(h)));}
 public static double segmentDistance(double lat,double lon,double aLat,double aLon,double bLat,double bLon){double y=111195,x=y*Math.cos(Math.toRadians(lat));double ax=(aLon-lon)*x,ay=(aLat-lat)*y,bx=(bLon-lon)*x,by=(bLat-lat)*y,dx=bx-ax,dy=by-ay;double t=dx*dx+dy*dy<1?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/(dx*dx+dy*dy)));return Math.hypot(ax+t*dx,ay+t*dy);}
 public static int nearestForwardSegment(double lat,double lon,double[][] route,int current){if(route.length<2)return 0;int start=Math.max(0,Math.min(current,route.length-2));int best=start;double d=Double.MAX_VALUE;for(int i=start;i<route.length-1;i++){double v=segmentDistance(lat,lon,route[i][0],route[i][1],route[i+1][0],route[i+1][1]);if(v<d){d=v;best=i;}}return best;}
 public static double remaining(double lat,double lon,double[][] route,int segment){if(route.length<2)return 0;int s=Math.max(0,Math.min(segment,route.length-2));double result=meters(lat,lon,route[s+1][0],route[s+1][1]);for(int i=s+1;i<route.length-1;i++)result+=meters(route[i][0],route[i][1],route[i+1][0],route[i+1][1]);return result;}
 public static boolean arrived(double distance,double accuracy){return distance<=30&&accuracy<=40;}
 public static boolean shouldReroute(double deviation,double accuracy,int samples,long elapsedMs){return deviation>65&&accuracy<=35&&samples>=3&&elapsedMs>=20000;}
}
