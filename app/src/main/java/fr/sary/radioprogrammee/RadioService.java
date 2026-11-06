package fr.sary.radioprogrammee;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;
public class RadioService extends Service {
 public static final String EVENT="fr.sary.radioprogrammee.PLAYBACK_STATUS";
 MediaPlayer player;Handler handler=new Handler(Looper.getMainLooper());boolean playing=false;
 int retries=0,attempt=0,station=0;String currentUrl="";String[] candidates=new String[0];
 Runnable timeout;
 public void onCreate(){super.onCreate();NotificationChannel channel=new NotificationChannel("radio","Lecture radio",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);}
 Notification notification(String state){return new Notification.Builder(this,"radio").setContentTitle("Radio Programmée").setContentText(state).setSmallIcon(android.R.drawable.ic_media_play).build();}
 void report(String state){Schedule.prefs(this).edit().putString("playback_state",state).putInt("playback_station",station).apply();sendBroadcast(new Intent(EVENT).setPackage(getPackageName()).putExtra("state",state).putExtra("station",station));}
 public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"STOP".equals(i.getAction())){playing=false;handler.removeCallbacksAndMessages(null);release();report("stopped");stopForeground(true);stopSelf();return START_NOT_STICKY;}
  int wanted=Schedule.prefs(this).getInt("station",0);
  String url=Schedule.prefs(this).getString("url",StationCatalog.DEFAULTS[0][0]);
  if(playing&&wanted==station&&url.equals(currentUrl)&&player!=null)return START_STICKY;
  playing=true;station=wanted;handler.removeCallbacksAndMessages(null);release();retries=0;attempt=0;
  if(station>=0&&station<6){candidates=StationCatalog.urls(this,station);String preferred=Schedule.prefs(this).getString("last_good_"+station,"");if(!preferred.isEmpty()&&station!=1&&station!=2){java.util.LinkedHashSet<String> all=new java.util.LinkedHashSet<>();all.add(preferred);for(String u:candidates)all.add(u);candidates=all.toArray(new String[0]);}}
  else candidates=new String[]{url};
  currentUrl=url;startForeground(1,notification("Connexion en cours"));connect();return START_STICKY;
 }
 void release(){if(timeout!=null){handler.removeCallbacks(timeout);timeout=null;}if(player!=null){MediaPlayer p=player;player=null;try{p.reset();p.release();}catch(Exception ignored){}}}
 void connect(){release();if(!playing)return;if(candidates.length==0){report("failed");return;}
  if(attempt>=candidates.length){retries++;if(retries>=3){report("failed");return;}attempt=0;report("retrying");handler.postDelayed(this::connect,Math.min(30000,5000L*retries));return;}
  final String candidate=candidates[attempt++];currentUrl=candidate;report(retries>0||attempt>1?"retrying":"connecting");
  try{MediaPlayer mp=new MediaPlayer();player=mp;mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
   mp.setDataSource(candidate);
   mp.setOnPreparedListener(p->{if(playing&&player==p){try{p.start();if(timeout!=null){handler.removeCallbacks(timeout);timeout=null;}report("playing");Schedule.prefs(this).edit().putString("last_good_"+station,candidate).apply();startForeground(1,notification("Lecture en cours"));retries=0;}catch(Exception e){retry();}}});
   mp.setOnErrorListener((p,a,b)->{if(player==p)retry();return true;});
   mp.setOnCompletionListener(p->{if(player==p)retry();});
   mp.prepareAsync();timeout=()->{if(player==mp&&playing)retry();};handler.postDelayed(timeout,15000);
  }catch(Exception e){retry();}
 }
 void retry(){release();if(playing){report("retrying");handler.postDelayed(this::connect,600);}}
 public void onDestroy(){playing=false;handler.removeCallbacksAndMessages(null);release();super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
}
