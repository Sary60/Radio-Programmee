package fr.sary.radioprogrammee;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;import org.json.*;import java.util.*;
public class RadioService extends Service {
 public static final String EVENT="fr.sary.radioprogrammee.PLAYBACK_STATUS";
 MediaPlayer player;final Handler handler=new Handler(Looper.getMainLooper());
 boolean playing=false,searching=false,searched=false;int generation=0,attempt=0,station=7;
 String currentUrl="",originalUrl="",stationName="",stationUuid="",countryCode="";
 ArrayList<String> candidates=new ArrayList<>();Runnable timeout;
 public void onCreate(){super.onCreate();NotificationChannel channel=new NotificationChannel("radio","Lecture radio",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);}
 Notification notification(String state){return new Notification.Builder(this,"radio").setContentTitle("Radio Programmée").setContentText(state).setSmallIcon(android.R.drawable.ic_media_play).build();}
 void report(String state){Schedule.prefs(this).edit().putString("playback_state",state).putInt("playback_station",station).apply();sendBroadcast(new Intent(EVENT).setPackage(getPackageName()).putExtra("state",state).putExtra("station",station));}
 public int onStartCommand(Intent intent,int flags,int id){
  String action=intent==null?"PLAY":intent.getAction();
  if("STOP".equals(action)){playing=false;generation++;handler.removeCallbacksAndMessages(null);release();report("stopped");stopForeground(true);stopSelf();return START_NOT_STICKY;}
  android.content.SharedPreferences p=Schedule.prefs(this);
  String url=p.getString("url","");int wanted=p.getInt("station",7);
  if(!StationCatalog.valid(url)){playing=false;generation++;handler.removeCallbacksAndMessages(null);release();station=wanted;report("stopped");stopForeground(true);stopSelf();return START_NOT_STICKY;}
  boolean force="REPAIR".equals(action);
  if(!force&&playing&&wanted==station&&url.equals(originalUrl)&&player!=null)return START_STICKY;
  generation++;playing=true;searching=false;searched=false;station=wanted;
  handler.removeCallbacksAndMessages(null);release();attempt=0;originalUrl=url;currentUrl="";
  stationName=p.getString("favorite_name","");stationUuid=p.getString("favorite_uuid","");countryCode=p.getString("favorite_country","");
  candidates.clear();candidates.add(url);
  startForeground(1,notification("Connexion en cours"));
  if(force){attempt=1;repair();}else connect();
  return START_STICKY;
 }
 void release(){if(timeout!=null){handler.removeCallbacks(timeout);timeout=null;}if(player!=null){MediaPlayer p=player;player=null;try{p.reset();p.release();}catch(Exception ignored){}}}
 void connect(){
  release();if(!playing)return;
  if(attempt>=candidates.size()){
   if(!searched){repair();return;}
   report("failed");startForeground(1,notification("Flux indisponible"));return;
  }
  final String candidate=candidates.get(attempt++);currentUrl=candidate;
  report(attempt==1&&!searched?"connecting":"retrying");
  try{
   MediaPlayer mp=new MediaPlayer();player=mp;
   mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
   mp.setDataSource(candidate);
   mp.setOnPreparedListener(p->{if(playing&&player==p){try{p.start();if(timeout!=null){handler.removeCallbacks(timeout);timeout=null;}report("playing");startForeground(1,notification("Lecture en cours"));saveGood(candidate);}catch(Exception e){retry();}}});
   mp.setOnErrorListener((p,a,b)->{if(player==p)retry();return true;});
   mp.setOnCompletionListener(p->{if(player==p)retry();});
   mp.prepareAsync();timeout=()->{if(player==mp&&playing)retry();};handler.postDelayed(timeout,12000);
  }catch(Exception e){retry();}
 }
 void retry(){release();if(playing){report("retrying");handler.postDelayed(this::connect,600);}}
 void repair(){
  if(searching||!playing)return;
  searched=true;searching=true;release();report("repairing");
  final int token=generation;
  final String name=stationName,uuid=stationUuid,country=countryCode,old=originalUrl;
  if(name.isEmpty()) {searching=false;report("failed");return;}
  StreamResolver.find(this,name,uuid,country,old,urls->handler.post(()->{
   if(!playing||token!=generation)return;
   searching=false;
   for(String u:urls)if(!candidates.contains(u))candidates.add(u);
   if(attempt>=candidates.size()){report("failed");startForeground(1,notification("Aucun autre flux trouvé"));}
   else connect();
  }));
 }
 void saveGood(String url){
  android.content.SharedPreferences p=Schedule.prefs(this);
  p.edit().putString("url",url).putString("last_good_favorite",url).apply();
  try{
   JSONArray a=new JSONArray(p.getString("favorites","[]"));boolean changed=false;
   for(int i=0;i<a.length();i++){
    JSONObject o=a.optJSONObject(i);if(o==null)continue;
    boolean match=!stationUuid.isEmpty()&&stationUuid.equals(o.optString("uuid",""));
    if(!match)match=stationName.equals(o.optString("name",""))&&originalUrl.equals(o.optString("url",""));
    if(match){o.put("url",url);changed=true;}
   }
   if(changed)p.edit().putString("favorites",a.toString()).apply();
   originalUrl=url;
  }catch(Exception ignored){}
  sendBroadcast(new Intent(EVENT).setPackage(getPackageName()).putExtra("state","favorites_updated").putExtra("station",station));
 }
 public void onDestroy(){playing=false;generation++;handler.removeCallbacksAndMessages(null);release();super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
}
