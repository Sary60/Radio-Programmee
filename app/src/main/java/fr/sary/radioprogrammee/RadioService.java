package fr.sary.radioprogrammee;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;
public class RadioService extends Service {
 MediaPlayer player;Handler handler=new Handler(Looper.getMainLooper());boolean playing=false;int retries=0;String currentUrl="";
 public void onCreate(){super.onCreate();NotificationChannel channel=new NotificationChannel("radio","Lecture radio",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);}
 Notification notification(){return new Notification.Builder(this,"radio").setContentTitle("Radio Programmée").setContentText("Lecture radio Internet").setSmallIcon(android.R.drawable.ic_media_play).build();}
 public int onStartCommand(Intent i,int flags,int id){if(i!=null&&"STOP".equals(i.getAction())){playing=false;handler.removeCallbacksAndMessages(null);release();stopForeground(true);stopSelf();return START_NOT_STICKY;}String url=Schedule.prefs(this).getString("url","https://icecast.radiofrance.fr/franceinter-midfi.mp3");if(playing&&url.equals(currentUrl)&&player!=null)return START_STICKY;playing=true;handler.removeCallbacksAndMessages(null);currentUrl=url;startForeground(1,notification());retries=0;connect();return START_STICKY;}
 void release(){if(player!=null){MediaPlayer p=player;player=null;try{p.reset();p.release();}catch(Exception ignored){}}}
 void connect(){release();if(!playing)return;try{MediaPlayer p=new MediaPlayer();player=p;p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());p.setDataSource(currentUrl);p.setOnPreparedListener(mp->{if(playing&&player==mp){mp.start();retries=0;}});p.setOnErrorListener((mp,a,b)->{if(player==mp)retry();return true;});p.prepareAsync();}catch(Exception e){retry();}}
 void retry(){release();if(playing){long wait=Math.min(60000,5000L*(++retries));handler.postDelayed(this::connect,wait);}}
 public void onDestroy(){playing=false;handler.removeCallbacksAndMessages(null);release();super.onDestroy();}public IBinder onBind(Intent i){return null;}
}
