package com.jams.gpspointfinder;
import android.Manifest;import android.speech.tts.TextToSpeech;import android.text.*;import java.net.*;import java.io.*;import java.util.concurrent.*;import android.app.*;import android.content.*;import android.content.pm.*;import android.graphics.*;import android.hardware.*;import android.location.*;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;import java.util.regex.*;import org.osmdroid.config.Configuration;import org.osmdroid.tileprovider.tilesource.TileSourceFactory;import org.osmdroid.views.MapView;import org.osmdroid.views.overlay.Marker;import org.osmdroid.views.overlay.Polyline;import org.osmdroid.util.GeoPoint;import org.osmdroid.views.overlay.Overlay;import org.maplibre.android.MapLibre;import org.maplibre.android.maps.MapLibreMap;import org.maplibre.android.camera.CameraPosition;import org.maplibre.android.camera.CameraUpdateFactory;import org.maplibre.android.geometry.LatLng;import org.maplibre.android.annotations.MarkerOptions;import org.maplibre.android.annotations.IconFactory;
public class MainActivity extends Activity implements SensorEventListener,LocationListener {
 EditText destinationInput;LinearLayout suggestionBox;TextView routeInfo;Handler searchHandler=new Handler(Looper.getMainLooper());Runnable pendingSearch;int searchVersion=0;Polyline driveRoute;boolean navigationActive=false,routeBusy=false;long lastRerouteAt=0;int offRouteSamples=0,nextStepIndex=0;double routeDistanceMeters=0,routeDurationSeconds=0;int routeSegment=0;ArrayList<GeoPoint> navigationPoints=new ArrayList<>();ArrayList<NavStep> navigationSteps=new ArrayList<>();TextToSpeech tts;boolean voiceReady=false;Button startNavigationButton,stopNavigationButton;static class NavStep {double lat,lon,distance;String instruction;NavStep(double a,double b,double d,String i){lat=a;lon=b;distance=d;instruction=i;}}double destinationLat=Double.NaN,destinationLon=Double.NaN;String destinationName="";
 FrameLayout mapRoot;ScrollView fieldScroll;LinearLayout floatingSearch;Button fieldToggle;LinearLayout page;TextView metrics,status,modeStatus,markProgressLabel;ProgressBar markProgress;Spinner markType;Button findButton,recordButton,beepButton;boolean findOn=false,recordOn=false,beepOn=false;EditText coords,name;Spinner saved;MapView realMap;org.maplibre.android.maps.MapView map3d;MapLibreMap mapLibre;org.maplibre.android.annotations.Marker blue3d,red3d;TextView smartMapStatus;String smartMode="AUTO";boolean near2d=false;Marker targetMarker,userMarker;Polyline routeLine;boolean mapCentered=false,followGps=true;Overlay navigationOverlay;SensorManager sm;Sensor rotation;LocationManager lm;PointStore store;
 double targetLat=Double.NaN,targetLon=Double.NaN,heading=Double.NaN,curLat=Double.NaN,curLon=Double.NaN;float bearing=Float.NaN,course=Float.NaN,accuracy=Float.NaN,distance=-1,total=0;double altitude=Double.NaN;
 boolean measuring=false;boolean liveGpsSubscribed=false;long lastGpsFix=0;long started=0;ArrayList<Location> samples=new ArrayList<>();Handler handler=new Handler(Looper.getMainLooper());
 final BroadcastReceiver updates=new BroadcastReceiver(){public void onReceive(Context c,Intent i){curLat=i.getDoubleExtra("lat",Double.NaN);curLon=i.getDoubleExtra("lon",Double.NaN);bearing=i.getFloatExtra("bearing",Float.NaN);course=i.getFloatExtra("course",Float.NaN);accuracy=i.getFloatExtra("accuracy",Float.NaN);distance=i.getFloatExtra("distance",-1);altitude=i.getDoubleExtra("altitude",Double.NaN);total=i.getFloatExtra("total",0);lastGpsFix=SystemClock.elapsedRealtime();render();updateMap();refreshModes();updateNavigation();}};
 @Override public void onCreate(Bundle b){super.onCreate(b);store=new PointStore(this);sm=(SensorManager)getSystemService(SENSOR_SERVICE);rotation=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);lm=(LocationManager)getSystemService(LOCATION_SERVICE);
 MapLibre.getInstance(this);Configuration.getInstance().load(this,getSharedPreferences("osmdroid",MODE_PRIVATE));Configuration.getInstance().setUserAgentValue(getPackageName());
 mapRoot=new FrameLayout(this);mapRoot.setBackgroundColor(Color.rgb(225,235,228));setContentView(mapRoot);
 fieldScroll=new ScrollView(this);fieldScroll.setFillViewport(false);fieldScroll.setBackgroundColor(Color.WHITE);page=new LinearLayout(this);page.setPadding(dp(14),dp(12),dp(14),dp(16));page.setOrientation(1);page.setBackgroundColor(Color.WHITE);fieldScroll.addView(page);
 floatingSearch=new LinearLayout(this);floatingSearch.setOrientation(1);floatingSearch.setPadding(dp(10),dp(7),dp(10),dp(7));floatingSearch.setBackground(makePanelBackground());
 label("JAMS NAVIGATOR v4.3 — NAVIGATION BETA",22);
 label("DRIVE MODE — Taip destinasi untuk melihat cadangan tempat",16);
 destinationInput=new EditText(this);destinationInput.setSingleLine(true);destinationInput.setHint("🔍 Cari destinasi: bandar, jalan, kedai, sekolah...");page.addView(destinationInput);
 suggestionBox=new LinearLayout(this);suggestionBox.setOrientation(1);page.addView(suggestionBox);
 routeInfo=label("Cari tempat, pilih cadangan, kemudian tekan LALUAN JALAN RAYA.",14);
 button("CARI DESTINASI",()->searchDestinations(destinationInput.getText().toString(),++searchVersion));
 button("KIRA LALUAN",()->requestDrivingRoute());
 startNavigationButton=modeButton("▶ START NAVIGATION",()->startNavigation());
 stopNavigationButton=modeButton("■ STOP NAVIGATION",()->stopNavigation());
 destinationInput.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence t,int start,int count,int after){}public void onTextChanged(CharSequence t,int start,int before,int count){int id=++searchVersion;if(pendingSearch!=null)searchHandler.removeCallbacks(pendingSearch);String q=t.toString().trim();if(q.length()<3){suggestionBox.removeAllViews();return;}pendingSearch=()->searchDestinations(q,id);searchHandler.postDelayed(pendingSearch,550);}public void afterTextChanged(Editable e){}});
 label("FIELD MODE — MARK • FIND • TRACK",14);
 name=new EditText(this);name.setHint("Nama batu / titik (contoh: Batu 01)");page.addView(name);
 markType=new Spinner(this);markType.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"START — Lokasi mula","CHECKPOINT — Persinggahan","BOUNDARY — Batu sempadan","END — Lokasi akhir"}));page.addView(markType);
 markProgressLabel=label("MARK POINT: Sedia • Jangan bergerak semasa cerapan",15);
 markProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);markProgress.setMax(60);markProgress.setProgress(0);page.addView(markProgress,new LinearLayout.LayoutParams(-1,24));
 button("MARK POINT — UKUR 60 SAAT",()->startMark());
 coords=new EditText(this);coords.setHint("Latitude, Longitude — contoh: 6.698351, 116.796772");page.addView(coords);
 button("TETAPKAN PIN DARIPADA KOORDINAT",()->parseTarget());
 saved=new Spinner(this);page.addView(saved);reload();button("CARI TITIK DIPILIH",()->selectPoint());
 modeStatus=label("STATUS: STANDBY",17);
 findButton=modeButton("FIND POINT",()->{if(!findOn&&Double.isNaN(targetLat)){status.setText("Tetapkan pin sasaran dahulu sebelum FIND.");return;}findOn=!findOn;if(findOn)beepOn=true;applyMode();});
 recordButton=modeButton("REKOD JEJAK",()->{recordOn=!recordOn;applyMode();});
 beepButton=modeButton("SMART BEEP",()->{if(!beepOn&&(!findOn||Double.isNaN(targetLat))){status.setText("Smart Beep memerlukan FIND dan pin sasaran.");return;}beepOn=!beepOn;applyMode();});
 button("HENTI SEMUA",()->{stopService(new Intent(this,TrackingService.class));findOn=false;recordOn=false;beepOn=false;getSharedPreferences("gps_mode",MODE_PRIVATE).edit().putBoolean("running",false).putBoolean("find",false).putBoolean("record",false).putBoolean("beep",false).apply();refreshModes();});
 button("LAPOR TRAFIK — SIMPAN LOKAL (PILOT)",()->showTrafficReportDialog());
 button("PTT RADIO — BELUM DISAMBUNG",()->new AlertDialog.Builder(this).setTitle("PTT Community Beta").setMessage("PTT awam dan konvoi peribadi memerlukan server suara, pengesahan pengguna dan sambungan selamat. Tiada audio dihantar oleh versi prototaip ini.").setPositiveButton("OK",null).show());
 button("KONGSI JEJAK GPX",()->shareGpx());
 button("MY TRAILS — SEJARAH PERJALANAN",()->showTrails());
 button("SIMPAN SESI TRAIL SEKARANG",()->saveTrail());
 metrics=label("Heading --° | Course --° | Bearing --°",18);
 realMap=new MapView(this);realMap.setTileSource(TileSourceFactory.MAPNIK);realMap.setMultiTouchControls(true);realMap.getController().setZoom(16.0);navigationOverlay=new NavigationOverlay();realMap.getOverlays().add(navigationOverlay);mapRoot.addView(realMap,new FrameLayout.LayoutParams(-1,-1));
  map3d=new org.maplibre.android.maps.MapView(this);map3d.onCreate(b);mapRoot.addView(map3d,new FrameLayout.LayoutParams(-1,-1));map3d.setVisibility(View.GONE);
  map3d.getMapAsync(map->{mapLibre=map;map.setStyle("https://tiles.openfreemap.org/styles/liberty",style->{updateMap();});});
  smartMapStatus=label("SMART MAP: AUTO — 3D >30m, 2D ≤30m, zon carian ≤4m",14);
  button("SMART MAP: AUTO / 2D / 3D",()->{smartMode=smartMode.equals("AUTO")?"2D":smartMode.equals("2D")?"3D":"AUTO";updateMap();});
 button("IKUT SAYA — ANAK PANAH GPS",()->{followGps=true;mapCentered=false;updateMap();status.setText("IKUT SAYA aktif. Peta mengikuti kedudukan GPS.");});
 button("LIHAT PIN SASARAN",()->{if(!Double.isNaN(targetLat)){followGps=false;realMap.getController().animateTo(new GeoPoint(targetLat,targetLon));realMap.getController().setZoom(18.0);}});
 label("▲ BIRU = SAYA DI SINI    ● MERAH = SASARAN",14);label("© OpenStreetMap contributors (ODbL)",12);
 status=label("Peta jalan sebenar OpenStreetMap. Internet diperlukan untuk tile baharu; kawasan yang pernah dilihat mungkin tersedia dalam cache. Bukan satelit atau 3D.",14);
 label("Pin sasaran kekal. Bacaan GPS boleh tersasar beberapa meter; bukan pengesahan sempadan sah.",13);
 // Full-screen map: move the existing autocomplete UI above the map without duplicating its listeners.
 page.removeView(destinationInput);page.removeView(suggestionBox);page.removeView(routeInfo);
 floatingSearch.addView(destinationInput,new LinearLayout.LayoutParams(-1,dp(50)));
 floatingSearch.addView(suggestionBox);
 floatingSearch.addView(routeInfo);
 FrameLayout.LayoutParams searchParams=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);searchParams.setMargins(dp(12),dp(12),dp(12),0);mapRoot.addView(floatingSearch,searchParams);
 LinearLayout bottomBar=new LinearLayout(this);bottomBar.setOrientation(LinearLayout.HORIZONTAL);bottomBar.setPadding(dp(8),dp(7),dp(8),dp(7));bottomBar.setBackground(makePanelBackground());
 Button follow=new Button(this);follow.setText("◎ SAYA");follow.setAllCaps(false);bottomBar.addView(follow,new LinearLayout.LayoutParams(0,dp(50),1));follow.setOnClickListener(v->{followGps=true;mapCentered=false;updateMap();});
 Button mapMode=new Button(this);mapMode.setText("◈ 2D/3D");mapMode.setAllCaps(false);bottomBar.addView(mapMode,new LinearLayout.LayoutParams(0,dp(50),1));mapMode.setOnClickListener(v->{smartMode=smartMode.equals("AUTO")?"2D":smartMode.equals("2D")?"3D":"AUTO";updateMap();});
 fieldToggle=new Button(this);fieldToggle.setText("☰ MENU");fieldToggle.setAllCaps(false);bottomBar.addView(fieldToggle,new LinearLayout.LayoutParams(0,dp(50),1));fieldToggle.setOnClickListener(v->toggleFieldPanel());
 FrameLayout.LayoutParams barParams=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);barParams.setMargins(dp(10),0,dp(10),dp(12));mapRoot.addView(bottomBar,barParams);
 FrameLayout.LayoutParams fieldParams=new FrameLayout.LayoutParams(-1,-1,Gravity.TOP);fieldParams.setMargins(dp(8),dp(100),dp(8),dp(86));mapRoot.addView(fieldScroll,fieldParams);fieldScroll.setVisibility(View.GONE);
 // Bottom toolbar must stay clickable above the field panel.
 bottomBar.bringToFront();
 tts=new TextToSpeech(this,result->{voiceReady=result==TextToSpeech.SUCCESS;if(voiceReady)tts.setLanguage(new Locale("ms","MY"));});
 if(getIntent()!=null&&Intent.ACTION_SEND.equals(getIntent().getAction())){String t=getIntent().getStringExtra(Intent.EXTRA_TEXT);if(t!=null){coords.setText(t);parseTarget();}}
 }
 // Beta uses public demonstration services; replace with contracted services before public launch.
 void searchDestinations(String q,int id){
  if(q.trim().length()<3)return;
  routeInfo.setText("Mencari lokasi: "+q+" ...");
  new Thread(()->{try{
   String url="https://photon.komoot.io/api/?q="+URLEncoder.encode(q,"UTF-8")+"&limit=7&lat=6.7&lon=116.8";
   JSONObject result=new JSONObject(httpGet(url));JSONArray features=result.optJSONArray("features");
   ArrayList<String> names=new ArrayList<>();ArrayList<double[]> points=new ArrayList<>();
   if(features!=null)for(int i=0;i<features.length();i++){
    JSONObject f=features.optJSONObject(i);if(f==null)continue;JSONObject props=f.optJSONObject("properties");JSONObject geom=f.optJSONObject("geometry");if(props==null||geom==null)continue;
    JSONArray c=geom.optJSONArray("coordinates");if(c==null||c.length()<2)continue;
    String n=props.optString("name",props.optString("street","Lokasi"));String city=props.optString("city",props.optString("county",""));String state=props.optString("state","");String country=props.optString("country","");
    String display=n+(city.isEmpty()?"":", "+city)+(state.isEmpty()?"":", "+state)+(country.isEmpty()?"":", "+country);
    names.add(display);points.add(new double[]{c.getDouble(1),c.getDouble(0)});
   }
   runOnUiThread(()->{if(id!=searchVersion)return;suggestionBox.removeAllViews();routeInfo.setText(names.isEmpty()?"Tiada padanan. Cuba nama tempat yang lebih khusus.":"Pilih salah satu daripada "+names.size()+" cadangan:");for(int i=0;i<names.size();i++){final int k=i;Button b=new Button(this);b.setAllCaps(false);b.setText("📍 "+names.get(i));suggestionBox.addView(b);b.setOnClickListener(v->{destinationName=names.get(k);destinationLat=points.get(k)[0];destinationLon=points.get(k)[1];destinationInput.setText(destinationName);destinationInput.setSelection(destinationInput.length());searchVersion++;suggestionBox.removeAllViews();targetLat=destinationLat;targetLon=destinationLon;persistTarget();coords.setText(String.format(Locale.US,"%.7f, %.7f",targetLat,targetLon));routeInfo.setText("DESTINASI: "+destinationName+"\nTekan KIRA LALUAN kemudian START NAVIGATION.");updateMap();});}});
  }catch(Exception e){runOnUiThread(()->{if(id==searchVersion)routeInfo.setText("Carian gagal. Semak internet atau cuba lagi: "+e.getMessage());});}}).start();
 }
 String httpGet(String url) throws Exception {HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestProperty("User-Agent","JAMSNavigatorPilot/4.1 (Android beta)");c.setConnectTimeout(9000);c.setReadTimeout(9000);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return out.toString("UTF-8");}finally{c.disconnect();}}
 void requestDrivingRoute(){fetchRoute(false);}
 void fetchRoute(boolean reroute){
  if(routeBusy)return;
  if(Double.isNaN(destinationLat)){routeInfo.setText("Pilih destinasi dahulu.");return;}
  if(Double.isNaN(curLat)||Double.isNaN(curLon)){routeInfo.setText("GPS belum tersedia. Hidupkan lokasi dan tunggu fix.");return;}
  routeBusy=true;routeInfo.setText(reroute?"Mengira semula laluan...":"Mengira laluan jalan raya...");
  final double fromLat=curLat,fromLon=curLon,toLat=destinationLat,toLon=destinationLon;
  new Thread(()->{try{
   String url=String.format(Locale.US,"https://router.project-osrm.org/route/v1/driving/%.7f,%.7f;%.7f,%.7f?overview=full&geometries=geojson&steps=true",fromLon,fromLat,toLon,toLat);
   JSONObject obj=new JSONObject(httpGet(url));JSONArray routes=obj.optJSONArray("routes");if(routes==null||routes.length()==0)throw new Exception("Tiada laluan ditemui");
   JSONObject r=routes.getJSONObject(0);JSONArray coordinates=r.getJSONObject("geometry").getJSONArray("coordinates");ArrayList<GeoPoint> pts=new ArrayList<>();
   for(int i=0;i<coordinates.length();i++){JSONArray pair=coordinates.getJSONArray(i);pts.add(new GeoPoint(pair.getDouble(1),pair.getDouble(0)));}
   ArrayList<NavStep> steps=new ArrayList<>();JSONArray legs=r.optJSONArray("legs");if(legs!=null)for(int j=0;j<legs.length();j++){
    JSONArray arr=legs.getJSONObject(j).optJSONArray("steps");if(arr==null)continue;
    for(int i=0;i<arr.length();i++){JSONObject st=arr.getJSONObject(i),man=st.optJSONObject("maneuver");if(man==null)continue;JSONArray loc=man.optJSONArray("location");if(loc==null)continue;
     String type=man.optString("type","turn"),mod=man.optString("modifier","");String road=st.optString("name","");
     String instruction=instructionFor(type,mod,road);steps.add(new NavStep(loc.getDouble(1),loc.getDouble(0),st.optDouble("distance",0),instruction));
    }
   }
   double dist=r.optDouble("distance"),dur=r.optDouble("duration");
   runOnUiThread(()->{routeBusy=false;navigationPoints=pts;navigationSteps=steps;nextStepIndex=0;routeSegment=0;offRouteSamples=0;routeDistanceMeters=dist;routeDurationSeconds=dur;
    if(driveRoute!=null)realMap.getOverlays().remove(driveRoute);driveRoute=new Polyline();driveRoute.setPoints(pts);driveRoute.getOutlinePaint().setColor(Color.rgb(25,110,240));driveRoute.getOutlinePaint().setStrokeWidth(12f);realMap.getOverlays().add(driveRoute);
    followGps=true;smartMode="2D";realMap.setVisibility(View.VISIBLE);map3d.setVisibility(View.GONE);realMap.getController().setZoom(17.0);realMap.invalidate();
    if(reroute&&navigationActive){speak("Laluan baharu tersedia");updateNavigation();}
    else routeInfo.setText(String.format(Locale.US,"LALUAN KE %s\n%.1f km • ±%d min (tiada trafik langsung). Tekan START NAVIGATION.",destinationName,dist/1000,(int)Math.round(dur/60)));
   });
  }catch(Exception e){runOnUiThread(()->{routeBusy=false;routeInfo.setText("Gagal mendapatkan laluan: "+e.getMessage()+". Cuba lagi.");});}}).start();
 }
 static String instructionFor(String type,String mod,String road){
  String move;
  if("arrive".equals(type))return "Anda telah tiba di destinasi";
  if("depart".equals(type))move="Mulakan perjalanan";
  else if("roundabout".equals(type)||"rotary".equals(type))move="Masuk bulatan";
  else if("merge".equals(type))move="Masuk ke laluan";
  else if("fork".equals(type))move="Ambil simpang";
  else if(mod.contains("left"))move="Belok kiri";
  else if(mod.contains("right"))move="Belok kanan";
  else if("uturn".equals(mod))move="Buat pusingan U";
  else move="Teruskan perjalanan";
  return move+(road.isEmpty()?"":" ke "+road);
 }
 void speak(String msg){if(voiceReady&&tts!=null)tts.speak(msg,TextToSpeech.QUEUE_FLUSH,null,"jams_nav");}
 void startNavigation(){
  if(navigationPoints.size()<2||navigationSteps.isEmpty()){routeInfo.setText("Kira laluan dahulu sebelum START.");return;}
  if(Double.isNaN(curLat)){routeInfo.setText("Tunggu GPS sebelum START.");return;}
  navigationActive=true;nextStepIndex=0;routeSegment=0;offRouteSamples=0;followGps=true;smartMode="2D";speak("Navigasi bermula");updateNavigation();
 }
 void stopNavigation(){navigationActive=false;offRouteSamples=0;if(tts!=null)tts.stop();routeInfo.setText("Navigasi dihentikan. Laluan masih dipaparkan.");}
 double meters(double a,double b,double c,double d){float[] out=new float[1];Location.distanceBetween(a,b,c,d,out);return out[0];}
 double routeDeviationMeters(){double best=Double.MAX_VALUE;if(navigationPoints.size()<2)return best;
  for(int i=1;i<navigationPoints.size();i++){GeoPoint a=navigationPoints.get(i-1),b=navigationPoints.get(i);double latScale=111195,lonScale=111195*Math.cos(Math.toRadians(curLat));
   double ax=(a.getLongitude()-curLon)*lonScale,ay=(a.getLatitude()-curLat)*latScale,bx=(b.getLongitude()-curLon)*lonScale,by=(b.getLatitude()-curLat)*latScale;
   double dx=bx-ax,dy=by-ay,t=(dx*dx+dy*dy)<1?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/(dx*dx+dy*dy)));
   best=Math.min(best,Math.hypot(ax+t*dx,ay+t*dy));}
  return best;
 }
 void updateNavigation(){
  if(!navigationActive||Double.isNaN(curLat)||navigationSteps.isEmpty()||navigationPoints.size()<2)return;
  if(Float.isFinite(accuracy)&&accuracy>60){routeInfo.setText("GPS kurang tepat (±"+Math.round(accuracy)+"m). Menunggu bacaan stabil...");return;}
  double destDist=NavigationMath.meters(curLat,curLon,destinationLat,destinationLon);
  if(NavigationMath.arrived(destDist,Float.isFinite(accuracy)?accuracy:99)){navigationActive=false;routeInfo.setText("DESTINASI DICAPAI: "+destinationName);speak("Anda telah tiba di destinasi");return;}
  double[][] shape=new double[navigationPoints.size()][2];for(int i=0;i<shape.length;i++){shape[i][0]=navigationPoints.get(i).getLatitude();shape[i][1]=navigationPoints.get(i).getLongitude();}
  routeSegment=NavigationMath.nearestForwardSegment(curLat,curLon,shape,routeSegment);
  double deviation=NavigationMath.segmentDistance(curLat,curLon,shape[routeSegment][0],shape[routeSegment][1],shape[routeSegment+1][0],shape[routeSegment+1][1]);
  if(deviation>65&&Float.isFinite(accuracy)&&accuracy<=35)offRouteSamples++;else offRouteSamples=0;
  if(NavigationMath.shouldReroute(deviation,Float.isFinite(accuracy)?accuracy:99,offRouteSamples,SystemClock.elapsedRealtime()-lastRerouteAt)&&!routeBusy){lastRerouteAt=SystemClock.elapsedRealtime();offRouteSamples=0;fetchRoute(true);return;}
  // Advance past maneuvers that have already been passed; avoid repeating old instructions.
  while(nextStepIndex<navigationSteps.size()-1){NavStep current=navigationSteps.get(nextStepIndex);double d=NavigationMath.meters(curLat,curLon,current.lat,current.lon);
   if(d<35){nextStepIndex++;continue;}
   int closest=nextStepIndex;double best=d;for(int j=nextStepIndex+1;j<Math.min(navigationSteps.size(),nextStepIndex+5);j++){NavStep candidate=navigationSteps.get(j);double candidateDistance=NavigationMath.meters(curLat,curLon,candidate.lat,candidate.lon);if(candidateDistance+25<best){best=candidateDistance;closest=j;}}
   if(closest>nextStepIndex)nextStepIndex=closest;else break;
  }
  NavStep next=navigationSteps.get(Math.min(nextStepIndex,navigationSteps.size()-1));double d=NavigationMath.meters(curLat,curLon,next.lat,next.lon);
  double remain=NavigationMath.remaining(curLat,curLon,shape,routeSegment);double etaSec=routeDistanceMeters>0?routeDurationSeconds*(remain/routeDistanceMeters):0;
  routeInfo.setText(String.format(Locale.US,"NAVIGASI AKTIF • %s\n%s dalam %.0f m\nBaki %.1f km • ETA ±%d min (tanpa trafik langsung)\nGPS ±%.0f m",destinationName,next.instruction,d,remain/1000,(int)Math.ceil(etaSec/60),accuracy));
  if(followGps){realMap.getController().setCenter(new GeoPoint(curLat,curLon));if(realMap.getZoomLevelDouble()<16)realMap.getController().setZoom(17.0);}
 }
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 android.graphics.drawable.GradientDrawable makePanelBackground(){android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(18));bg.setStroke(dp(1),Color.rgb(215,224,220));return bg;}
 void toggleFieldPanel(){boolean open=fieldScroll.getVisibility()!=View.VISIBLE;fieldScroll.setVisibility(open?View.VISIBLE:View.GONE);fieldToggle.setText(open?"✕ TUTUP":"☰ MENU");if(open){fieldScroll.bringToFront();floatingSearch.bringToFront();fieldToggle.bringToFront();}else{floatingSearch.bringToFront();}}
 TextView label(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextColor(Color.rgb(19,43,39));v.setTextSize(size);v.setPadding(0,10,0,10);page.addView(v);return v;}
 void button(String t,Runnable r){Button b=new Button(this);b.setText(t);page.addView(b);b.setOnClickListener(v->r.run());}
 Button modeButton(String title,Runnable r){Button b=new Button(this);b.setAllCaps(false);page.addView(b);b.setOnClickListener(v->r.run());return b;}
 void refreshModes(){if(modeStatus==null)return;modeStatus.setText("STATUS: "+(findOn?"FIND ON  ":"")+(recordOn?"TRACK ON  ":"")+(beepOn&&findOn?"BEEP ON  ":"")+(!findOn&&!recordOn?"STANDBY":"")+(lastGpsFix==0?" | GPS BELUM ADA":" | GPS TERIMA"));styleMode(findButton,"FIND POINT",findOn);styleMode(recordButton,"REKOD JEJAK",recordOn);styleMode(beepButton,"SMART BEEP",beepOn);}
 void styleMode(Button b,String name,boolean active){if(b==null)return;b.setText((active?"● AKTIF — ":"○ OFF — ")+name);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(active?Color.rgb(28,136,79):Color.rgb(207,214,211)));b.setTextColor(active?Color.WHITE:Color.rgb(25,40,36));}
 void applyMode(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},8);return;}
 if(!findOn&&!recordOn){beepOn=false;stopService(new Intent(this,TrackingService.class));getSharedPreferences("gps_mode",MODE_PRIVATE).edit().putBoolean("running",false).putBoolean("find",false).putBoolean("record",false).putBoolean("beep",false).apply();refreshModes();return;}
 Intent i=new Intent(this,TrackingService.class).setAction(TrackingService.ACTION_START);i.putExtra(TrackingService.EXTRA_LAT,targetLat);i.putExtra(TrackingService.EXTRA_LON,targetLon);i.putExtra("record",recordOn);i.putExtra("find",findOn);i.putExtra("beep",beepOn);
 try{startForegroundService(i);status.setText("MENUNGGU GPS — semak Location dan notifikasi tracking. Mod Find/Track dihantar ke servis.");}catch(Exception e){findOn=false;recordOn=false;beepOn=false;status.setText("SERVIS GAGAL: "+e.getMessage());}refreshModes();}
 void persistTarget(){getSharedPreferences("gps_target",MODE_PRIVATE).edit().putLong("lat",Double.doubleToRawLongBits(targetLat)).putLong("lon",Double.doubleToRawLongBits(targetLon)).apply();}
 void reload(){JSONArray a=store.points();ArrayList<String> names=new ArrayList<>();for(int j=0;j<a.length();j++){JSONObject p=a.optJSONObject(j);names.add((j+1)+". "+p.optString("name")+" (±"+p.optDouble("accuracy",0)+"m)");}if(names.isEmpty())names.add("Tiada titik tersimpan");saved.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));}
 void selectPoint(){JSONObject p=store.points().optJSONObject(saved.getSelectedItemPosition());if(p==null){status.setText("Belum ada titik disimpan");return;}targetLat=p.optDouble("lat");targetLon=p.optDouble("lon");persistTarget();coords.setText(String.format(Locale.US,"%.8f, %.8f",targetLat,targetLon));status.setText("Pin kekal: "+p.optString("name"));render();updateMap();}
 void parseTarget(){Matcher m=Pattern.compile("(-?\\d{1,3}\\.\\d+)\\s*[,\\s]\\s*(-?\\d{1,3}\\.\\d+)").matcher(coords.getText());if(!m.find()){Toast.makeText(this,"Format: latitude, longitude",Toast.LENGTH_LONG).show();return;}double a=Double.parseDouble(m.group(1)),b=Double.parseDouble(m.group(2));if(Math.abs(a)>90||Math.abs(b)>180){Toast.makeText(this,"Koordinat tidak sah",Toast.LENGTH_LONG).show();return;}targetLat=a;targetLon=b;persistTarget();status.setText("Pin sasaran ditetapkan. Aktifkan FIND untuk panduan bunyi.");render();updateMap();}
 void startMark(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},7);return;}samples.clear();measuring=true;started=SystemClock.elapsedRealtime();markProgress.setProgress(0);markProgressLabel.setText("CERAPAN BERMULA — TELEFON MESTI PEGUN");try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);}catch(Exception e){status.setText(e.toString());return;}status.setText("Cerapan 60 saat bermula. Berdiri diam di titik.");handler.postDelayed(new Runnable(){public void run(){if(!measuring)return;long remain=60-(SystemClock.elapsedRealtime()-started)/1000;status.setText("MARK POINT: "+Math.max(0,remain)+" saat • "+samples.size()+" bacaan");markProgress.setProgress((int)Math.min(60,(SystemClock.elapsedRealtime()-started)/1000));markProgressLabel.setText("JANGAN BERGERAK • "+Math.max(0,remain)+" saat lagi • "+samples.size()+" bacaan diterima");if(remain>0)handler.postDelayed(this,1000);else finishMark();}},1000);}
 void finishMark(){measuring=false;markProgress.setProgress(60);lm.removeUpdates(this);if(samples.size()<10){markProgressLabel.setText("GAGAL — bacaan GPS tidak mencukupi. Ulang cerapan.");status.setText("Bacaan GPS tidak mencukupi. Cuba semula di kawasan terbuka.");return;}double la=0,lo=0,weight=0;float best=Float.MAX_VALUE;for(Location l:samples){double w=1.0/Math.max(4,l.getAccuracy()*l.getAccuracy());la+=l.getLatitude()*w;lo+=l.getLongitude()*w;weight+=w;best=Math.min(best,l.getAccuracy());}la/=weight;lo/=weight;String n=name.getText().toString().trim();if(n.isEmpty())n="Titik "+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());store.add(n,la,lo,best);TrailStore.addWaypoint(this,n,markType.getSelectedItemPosition(),la,lo,best);markProgressLabel.setText("SELESAI — waypoint direkod dalam My Trails");targetLat=la;targetLon=lo;persistTarget();coords.setText(String.format(Locale.US,"%.8f, %.8f",la,lo));reload();status.setText(String.format(Locale.US,"DISIMPAN: %s • %d bacaan • GPS terbaik ±%.1fm. Bukan ketepatan purata yang dijamin.",n,samples.size(),best));render();}
 @Override public void onLocationChanged(Location l){if(measuring&&l.hasAccuracy()&&l.getAccuracy()<=25&&l.getElapsedRealtimeNanos()>0)samples.add(l);if(!Double.isNaN(l.getLatitude())){curLat=l.getLatitude();curLon=l.getLongitude();accuracy=l.hasAccuracy()?l.getAccuracy():Float.NaN;altitude=l.hasAltitude()?l.getAltitude():Double.NaN;course=l.hasBearing()&&l.hasSpeed()&&l.getSpeed()>=1?l.getBearing():Float.NaN;lastGpsFix=SystemClock.elapsedRealtime();if(!findOn&&!recordOn){distance=-1;bearing=Float.NaN;if(!Double.isNaN(targetLat)){Location target=new Location("target");target.setLatitude(targetLat);target.setLongitude(targetLon);distance=l.distanceTo(target);bearing=l.bearingTo(target);}}render();updateMap();updateNavigation();}}
 void startTracking(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},8);return;}Intent i=new Intent(this,TrackingService.class).setAction(TrackingService.ACTION_START);i.putExtra(TrackingService.EXTRA_LAT,targetLat);i.putExtra(TrackingService.EXTRA_LON,targetLon);i.putExtra("record",true);try{startForegroundService(i);status.setText("GPS + jejak aktif. Notifikasi kekal ketika skrin dikunci.");}catch(Exception e){status.setText("Gagal: "+e.getMessage());}}
 void render(){if(metrics==null)return;String h=Double.isNaN(heading)?"--":String.format(Locale.US,"%.0f",heading);String c=Float.isNaN(course)?"--":String.format(Locale.US,"%.0f",course);String b=Float.isNaN(bearing)?"--":String.format(Locale.US,"%.0f",(bearing+360)%360);
 String turn="";if(!Double.isNaN(heading)&&!Float.isNaN(bearing)){double diff=((bearing-heading+540)%360)-180;turn=Math.abs(diff)>165?" | PUSING BALIK":String.format(Locale.US," | Pusing %s %.0f°",diff>=0?"kanan":"kiri",Math.abs(diff));}
 metrics.setText("HEADING "+h+"°  COURSE "+c+"°\nBEARING PIN "+b+"°"+turn+"\nJARAK "+(distance<0?"--":String.format(Locale.US,"%.1f m",distance))+"  GPS ±"+(Float.isNaN(accuracy)?"--":String.format(Locale.US,"%.1f m",accuracy))+"\nALTITUDE "+(Double.isNaN(altitude)?"--":String.format(Locale.US,"%.0f m (GPS)",altitude))+"  TRACK "+String.format(Locale.US,"%.0f m",total));}
 // The navigation overlay draws two visually distinct symbols without relying on default marker icons.
 class NavigationOverlay extends Overlay {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  @Override public void draw(Canvas canvas,MapView map,boolean shadow){
   if(shadow)return;
   android.graphics.Point pixel=new android.graphics.Point();
   if(!Double.isNaN(targetLat)&&!Double.isNaN(targetLon)){
    map.getProjection().toPixels(new GeoPoint(targetLat,targetLon),pixel);
    paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(210,44,54));
    canvas.drawCircle(pixel.x,pixel.y,14f,paint);
    paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4f);paint.setColor(Color.WHITE);
    canvas.drawCircle(pixel.x,pixel.y,14f,paint);paint.setStyle(Paint.Style.FILL);
   }
   if(Double.isNaN(curLat)||Double.isNaN(curLon))return;
   map.getProjection().toPixels(new GeoPoint(curLat,curLon),pixel);
   float x=pixel.x,y=pixel.y;
   // Accuracy radius is indicative; screen pixels are converted from map metres.
   if(!Float.isNaN(accuracy)&&accuracy>0&&accuracy<2000){
    float r=(float)Math.min(200,map.getProjection().metersToEquatorPixels(accuracy));
    paint.setStyle(Paint.Style.FILL);paint.setColor(Color.argb(36,20,116,235));canvas.drawCircle(x,y,r,paint);
    paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2f);paint.setColor(Color.argb(140,20,116,235));canvas.drawCircle(x,y,r,paint);
   }
   canvas.save();canvas.translate(x,y);
   // osmdroid map bearing rotates the map; compensate to retain true compass heading.
   float direction=(float)(!Float.isNaN(bearing)?bearing:(!Double.isNaN(heading)?heading:(!Float.isNaN(course)?course:0)));
   canvas.rotate(direction-map.getMapOrientation());
   Path arrow=new Path();arrow.moveTo(0,-32);arrow.lineTo(20,23);arrow.lineTo(0,13);arrow.lineTo(-20,23);arrow.close();
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeWidth(7f);paint.setColor(Color.WHITE);canvas.drawPath(arrow,paint);
   paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(20,116,235));canvas.drawPath(arrow,paint);
   canvas.restore();
  }
 }
 void updateMap(){
  if(realMap==null)return;
  if(!Double.isNaN(curLat)&&!Double.isNaN(curLon)&&followGps){
   GeoPoint here=new GeoPoint(curLat,curLon);
   if(!mapCentered){realMap.getController().setCenter(here);mapCentered=true;}
   else realMap.getController().animateTo(here);
  }
  try{
   String savedTrack=getSharedPreferences("track",MODE_PRIVATE).getString("latest","[]");
   JSONArray a=new JSONArray(savedTrack);ArrayList<GeoPoint> pts=new ArrayList<>();
   for(int j=0;j<a.length();j++){JSONObject o=a.optJSONObject(j);if(o!=null)pts.add(new GeoPoint(o.getDouble("lat"),o.getDouble("lon")));}
   if(routeLine==null){routeLine=new Polyline();routeLine.getOutlinePaint().setColor(Color.rgb(22,160,82));routeLine.getOutlinePaint().setStrokeWidth(8f);realMap.getOverlays().add(0,routeLine);}
   routeLine.setPoints(pts);
  }catch(Exception ignored){}
  updateSmartMap();
  realMap.invalidate();
 }
 void updateSmartMap(){
  if(realMap==null||map3d==null)return;
  if(smartMode.equals("AUTO")&&distance>=0){if(near2d&&distance>45)near2d=false;else if(!near2d&&distance<=30)near2d=true;}
  boolean use3d=smartMode.equals("3D")||(smartMode.equals("AUTO")&&!near2d);
  realMap.setVisibility(use3d?View.GONE:View.VISIBLE);map3d.setVisibility(use3d?View.VISIBLE:View.GONE);
  if(smartMapStatus!=null)smartMapStatus.setText("SMART MAP "+smartMode+" | "+(use3d?"3D CONDONG":"2D PRECISION")+(distance>=0&&distance<=4?" | DALAM ZON CARIAN (GPS ±"+String.format(Locale.US,"%.1f",accuracy)+"m)":""));
  if(mapLibre==null||mapLibre.getStyle()==null)return;
  if(blue3d!=null){mapLibre.removeMarker(blue3d);blue3d=null;}if(red3d!=null){mapLibre.removeMarker(red3d);red3d=null;}
  if(!Double.isNaN(targetLat)&&!Double.isNaN(targetLon))red3d=mapLibre.addMarker(new MarkerOptions().position(new LatLng(targetLat,targetLon)).title("PIN SASARAN"));
  if(!Double.isNaN(curLat)&&!Double.isNaN(curLon)){
   // Blue user arrow bitmap: always points to active target bearing, never to course.
   Bitmap bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);c.rotate(Float.isNaN(bearing)?0:bearing,40,40);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Path path=new Path();path.moveTo(40,4);path.lineTo(65,69);path.lineTo(40,55);path.lineTo(15,69);path.close();p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(20,116,235));c.drawPath(path,p);
   blue3d=mapLibre.addMarker(new MarkerOptions().position(new LatLng(curLat,curLon)).title("SAYA").icon(IconFactory.getInstance(this).fromBitmap(bitmap)));
   // Arrow rotation is baked into the bitmap; MapLibre annotation Marker has no setRotation(float).
   if(followGps&&use3d){CameraPosition cam=new CameraPosition.Builder().target(new LatLng(curLat,curLon)).zoom(17).tilt(55).bearing(Float.isFinite(course)?course:(Float.isFinite(bearing)?bearing:0)).build();mapLibre.animateCamera(CameraUpdateFactory.newCameraPosition(cam),500);}
  }
 }
 @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){float[] m=new float[9],a=new float[3];SensorManager.getRotationMatrixFromVector(m,e.values);SensorManager.getOrientation(m,a);heading=(Math.toDegrees(a[0])+360)%360;render();if(realMap!=null)realMap.invalidate();}}
 @Override protected void onStart(){super.onStart();if(map3d!=null)map3d.onStart();}
 @Override protected void onStop(){if(map3d!=null)map3d.onStop();super.onStop();}
 @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}if(map3d!=null)map3d.onDestroy();super.onDestroy();}
 @Override public void onLowMemory(){super.onLowMemory();if(map3d!=null)map3d.onLowMemory();}
 @Override public void onAccuracyChanged(Sensor s,int a){}
 @Override protected void onResume(){super.onResume();if(Double.isNaN(targetLat)){android.content.SharedPreferences t=getSharedPreferences("gps_target",MODE_PRIVATE);if(t.contains("lat")){targetLat=Double.longBitsToDouble(t.getLong("lat",0));targetLon=Double.longBitsToDouble(t.getLong("lon",0));coords.setText(String.format(Locale.US,"%.8f, %.8f",targetLat,targetLon));}}requestLiveGps();android.content.SharedPreferences modes=getSharedPreferences("gps_mode",MODE_PRIVATE);findOn=modes.getBoolean("find",false);recordOn=modes.getBoolean("record",false);beepOn=modes.getBoolean("beep",false);refreshModes();if(realMap!=null)realMap.onResume();if(map3d!=null)map3d.onResume();if(rotation!=null)sm.registerListener(this,rotation,SensorManager.SENSOR_DELAY_UI);IntentFilter f=new IntentFilter(TrackingService.ACTION_UPDATE);if(Build.VERSION.SDK_INT>=33)registerReceiver(updates,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(updates,f);}
 void requestLiveGps(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},9);return;}try{if(!liveGpsSubscribed){lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);liveGpsSubscribed=true;}Location last=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);if(last!=null)onLocationChanged(last);if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))status.setText("GPS telefon OFF — hidupkan Location.");}catch(Exception e){status.setText("GPS gagal: "+e.getMessage());}}
 @Override protected void onPause(){super.onPause();if(liveGpsSubscribed){lm.removeUpdates(this);liveGpsSubscribed=false;}if(realMap!=null)realMap.onPause();if(map3d!=null)map3d.onPause();sm.unregisterListener(this);unregisterReceiver(updates);if(measuring){measuring=false;markProgressLabel.setText("CERAPAN TERHENTI — ulang semula 60 saat");status.setText("Cerapan dihentikan kerana aplikasi ditinggalkan. Ulangi 60 saat.");}}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){if(r==7)startMark();if(r==8)applyMode();if(r==9)requestLiveGps();}}
 void showTrafficReportDialog(){
  final String[] types={"Kemalangan","Banjir","Jalan rosak","Kerja jalan","Kesesakan","Halangan jalan"};
  if(Double.isNaN(curLat)||Double.isNaN(curLon)){new AlertDialog.Builder(this).setMessage("Tunggu lokasi GPS sebelum membuat laporan. Laporan hanya disimpan pada telefon.").setPositiveButton("OK",null).show();return;}
  new AlertDialog.Builder(this).setTitle("Laporan trafik (lokal sahaja)").setItems(types,(d,which)->{
   try{TrafficReportStore.add(this,types[which],curLat,curLon,accuracy);status.setText("Laporan "+types[which]+" disimpan pada telefon. BELUM dihantar kepada komuniti.");}
   catch(Exception ex){status.setText("Gagal simpan laporan: "+ex.getMessage());}
  }).setNegativeButton("Batal",null).show();
 }
 void saveTrail(){String title="Trail "+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());int count=TrailStore.saveCurrent(this,title);status.setText("My Trails: "+title+" disimpan ("+count+" titik jejak).");}
 void showTrails(){final JSONArray history=TrailStore.history(this);final String[] titles=new String[history.length()];for(int i=0;i<history.length();i++){JSONObject t=history.optJSONObject(i);titles[i]=t==null?"Trail":t.optString("name")+" • "+t.optJSONArray("track").length()+" rekod GPS • "+t.optJSONArray("waypoints").length()+" waypoint";}if(titles.length==0){new AlertDialog.Builder(this).setMessage("Belum ada trail tersimpan. Tekan SIMPAN SESI TRAIL SEKARANG selepas merekod perjalanan.").setPositiveButton("OK",null).show();return;}new AlertDialog.Builder(this).setTitle("MY TRAILS").setItems(titles,(dialog,which)->showTrailDetail(history.optJSONObject(which))).setNegativeButton("Tutup",null).show();}
 void showTrailDetail(JSONObject trail){if(trail==null)return;JSONArray pts=trail.optJSONArray("track"),wps=trail.optJSONArray("waypoints");StringBuilder sb=new StringBuilder("Tarikh: "+trail.optString("name")+"\nJejak: "+(pts==null?0:pts.length())+" titik GPS\nWaypoint: "+(wps==null?0:wps.length())+"\n\n");if(wps!=null)for(int i=0;i<wps.length();i++){JSONObject w=wps.optJSONObject(i);if(w!=null)sb.append(w.optString("type")).append(" — ").append(w.optString("name")).append("\n").append(String.format(Locale.US,"%.7f, %.7f",w.optDouble("lat"),w.optDouble("lon"))).append("\n\n");}new AlertDialog.Builder(this).setTitle("BUTIRAN TRAIL").setMessage(sb.toString()).setPositiveButton("LIHAT DI PETA",(d,w)->displayTrail(trail)).setNegativeButton("Tutup",null).show();}
 void displayTrail(JSONObject trail){try{JSONArray pts=trail.optJSONArray("track");ArrayList<GeoPoint> line=new ArrayList<>();if(pts!=null)for(int i=0;i<pts.length();i++){JSONObject p=pts.optJSONObject(i);if(p!=null)line.add(new GeoPoint(p.getDouble("lat"),p.getDouble("lon")));}if(routeLine==null){routeLine=new Polyline();realMap.getOverlays().add(routeLine);}routeLine.setPoints(line);if(!line.isEmpty())realMap.getController().animateTo(line.get(0));JSONArray wps=trail.optJSONArray("waypoints");if(wps!=null)for(int i=0;i<wps.length();i++){JSONObject w=wps.optJSONObject(i);if(w==null)continue;Marker m=new Marker(realMap);m.setPosition(new GeoPoint(w.getDouble("lat"),w.getDouble("lon")));m.setTitle(w.optString("type")+": "+w.optString("name"));realMap.getOverlays().add(m);}realMap.invalidate();status.setText("Trail sejarah dipaparkan. Rekod asal kekal disimpan.");}catch(Exception e){status.setText("Gagal buka trail: "+e.getMessage());}}
 void shareGpx(){String json=getSharedPreferences("track",MODE_PRIVATE).getString("latest","[]");try{JSONArray a=new JSONArray(json);if(a.length()==0){Toast.makeText(this,"Hentikan rakaman dahulu; belum ada jejak tersimpan",Toast.LENGTH_LONG).show();return;}StringBuilder g=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"GPS Point Finder PRO\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>GPS Track</name><trkseg>");for(int j=0;j<a.length();j++){JSONObject o=a.getJSONObject(j);g.append(String.format(Locale.US,"<trkpt lat=\"%.8f\" lon=\"%.8f\">",o.getDouble("lat"),o.getDouble("lon")));if(!o.isNull("alt"))g.append("<ele>").append(o.getDouble("alt")).append("</ele>");g.append("</trkpt>");}g.append("</trkseg></trk></gpx>");Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/gpx+xml");i.putExtra(Intent.EXTRA_TEXT,g.toString());startActivity(Intent.createChooser(i,"Kongsi GPX sebagai teks"));}catch(Exception e){status.setText("GPX gagal: "+e.getMessage());}}
}
