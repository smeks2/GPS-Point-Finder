import com.jams.gpspointfinder.NavigationMath;
public class NavigationMathTest {
 static int pass=0;static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);pass++;}
 public static void main(String[] args){double[][] road={{6.7,116.8},{6.701,116.8},{6.702,116.8},{6.703,116.8}};
 check(Math.abs(NavigationMath.meters(6.7,116.8,6.701,116.8)-111.19)<1,"GPS distance 111m");
 check(NavigationMath.nearestForwardSegment(6.7022,116.8,road,0)==2,"progress skips old segments");
 check(NavigationMath.nearestForwardSegment(6.7001,116.8,road,2)==2,"progress never moves backwards");
 check(NavigationMath.segmentDistance(6.7015,116.8,6.701,116.8,6.702,116.8)<1,"on-route deviation");
 check(NavigationMath.segmentDistance(6.7015,116.802,6.701,116.8,6.702,116.8)>200,"off-route deviation");
 check(NavigationMath.remaining(6.702,116.8,road,2)>100&&NavigationMath.remaining(6.702,116.8,road,2)<115,"remaining distance");
 check(NavigationMath.arrived(20,10)&&!NavigationMath.arrived(20,80),"arrival GPS accuracy guard");
 check(NavigationMath.shouldReroute(80,10,3,20000)&&!NavigationMath.shouldReroute(80,50,3,20000)&&!NavigationMath.shouldReroute(80,10,2,20000),"reroute debounce and accuracy");
 System.out.println("TOTAL "+pass+"/"+pass);
 }
}
