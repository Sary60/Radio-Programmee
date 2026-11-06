package fr.sary.radioprogrammee;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;
public class RadioService extends Service {
 MediaPlayer player; Handler handler=new Handler(Looper.getMainLooper());boolean playing=false;int retries=0;
 public void onCreate(){super.onCreate();NotificationChannel channel=new NotificationChannel("radio","Lecture radio",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);}
 Notification notification(){return new Notification.Builder(this,"radio").setContentTitle("Radio Programmée").setContentText("Lecture de la radio Internet").setSmallIcon(android.R.drawable.ic_media_play).build();}
 public int onStartCommand(Intent i,int flags,int id){if(i!=null&&"STOP".equals(i.getAction())){playing=false;handler.removeCallbacksAndMessages(null);release();stopForeground(true);stopSelf();return START_NOT_STICKY;}playing=true;startForeground(1,notification());retries=0;connect();return START_STICKY;}
 void release(){if(player!=null){try{player.reset();player.release();}catch(Exception ignored){}player=null;}}
 void connect(){release();if(!playing)return;try{player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());String url=Schedule.prefs(this).getString("url","https://scdn.nrjaudio.fm/adwz1/fr/30201/mp3_128.mp3");player.setDataSource(url);player.setOnPreparedListener(p->{if(playing){p.start();retries=0;}});player.setOnErrorListener((p,a,b)->{retry();return true;});player.prepareAsync();}catch(Exception e){retry();}}
 void retry(){release();if(playing){long wait=Math.min(60000,5000L*(++retries));handler.postDelayed(this::connect,wait);}}
 public void onDestroy(){playing=false;handler.removeCallbacksAndMessages(null);release();super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
}
