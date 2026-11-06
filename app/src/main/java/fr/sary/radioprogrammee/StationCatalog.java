package fr.sary.radioprogrammee;
import android.content.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

public final class StationCatalog {
 static final String REMOTE="https://raw.githubusercontent.com/Sary60/Radio-Programmee/main/stations.json";
 static final String[][] DEFAULTS={
  {"https://streaming.nrjaudio.fm/ouuku85n3nje?origine=fluxradios","https://scdn.nrjaudio.fm/adwz1/fr/30201/mp3_128.mp3"},
  {"https://streamer-02.rtl.fr/rtl-1-44-128","https://streamer-01.rtl.fr/rtl-1-44-128"},
  {"https://stream.rfm.fr/rfm.mp3"},
  {"https://streaming.nrjaudio.fm/oug7girb92oc?origine=fluxradios","https://scdn.nrjaudio.fm/adwz1/fr/30601/mp3_128.mp3"},
  {"https://scdn.nrjaudio.fm/adwz1/fr/30001/mp3_128.mp3"},
  {"https://icecast.radiofrance.fr/franceinter-midfi.mp3"}
 };
 static String[] urls(Context c,int station){
  if(station<0||station>=DEFAULTS.length)return new String[0];
  LinkedHashSet<String> all=new LinkedHashSet<>();
  try {String cache=Schedule.prefs(c).getString("catalog_json","");
   JSONArray stations=new JSONObject(cache).getJSONArray("stations");
   JSONArray arr=stations.getJSONObject(station).getJSONArray("urls");
   for(int i=0;i<arr.length();i++){String u=arr.optString(i,"");if(valid(u))all.add(u);}
  }catch(Exception ignored){}
  for(String u:DEFAULTS[station])all.add(u);
  return all.toArray(new String[0]);
 }
 static boolean valid(String u){return u.startsWith("https://")||u.startsWith("http://");}
 static void updateAsync(Context context){
  Context c=context.getApplicationContext();
  new Thread(()->{
   HttpURLConnection con=null;
   try{
    con=(HttpURLConnection)new URL(REMOTE).openConnection();con.setConnectTimeout(5000);con.setReadTimeout(5000);
    con.setRequestProperty("User-Agent","RadioProgrammee/1.2");
    if(con.getResponseCode()!=200)return;
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[2048];int n;
    try(InputStream in=con.getInputStream()){while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>32768)return;}}
    String raw=out.toString("UTF-8");JSONArray stations=new JSONObject(raw).getJSONArray("stations");
    if(stations.length()!=6)return;
    for(int i=0;i<6;i++){
     JSONArray arr=stations.getJSONObject(i).getJSONArray("urls");if(arr.length()<1||arr.length()>8)return;
     for(int j=0;j<arr.length();j++)if(!valid(arr.getString(j)))return;
    }
    Schedule.prefs(c).edit().putString("catalog_json",raw).apply();
   }catch(Exception ignored){}finally{if(con!=null)con.disconnect();}
  },"radio-catalog").start();
 }
 private StationCatalog(){}
}
