package fr.sary.radioprogrammee;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.Normalizer;
import java.util.*;

/** Find alternative streams for the SAME station, never a similarly named station. */
public final class StreamResolver {
 public interface Callback { void complete(String[] urls); }
 private static final String[] HOSTS={"de1.api.radio-browser.info","nl1.api.radio-browser.info","at1.api.radio-browser.info"};
 private static String clean(String s){
  String n=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","");
  return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
 }
 private static JSONArray get(String address) throws Exception {
  HttpURLConnection con=null;
  try {
   con=(HttpURLConnection)new URL(address).openConnection();
   con.setConnectTimeout(4500);con.setReadTimeout(5000);
   con.setRequestProperty("User-Agent","RadioProgrammee/1.5 (Android)");
   con.setRequestProperty("Accept","application/json");
   if(con.getResponseCode()!=200)throw new IOException("HTTP "+con.getResponseCode());
   ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
   try(InputStream in=con.getInputStream()){
    while((n=in.read(buffer))!=-1){b.write(buffer,0,n);if(b.size()>250000)throw new IOException("Response too large");}
   }
   return new JSONArray(b.toString("UTF-8"));
  } finally {if(con!=null)con.disconnect();}
 }
 public static void find(Context context,String name,String uuid,String country,String original,Callback callback){
  Context app=context.getApplicationContext();
  new Thread(()->{
   LinkedHashSet<String> urls=new LinkedHashSet<>();
   String wanted=clean(name);
   String code=country==null?"":country.trim();
   // Search by immutable station ID when available; fall back to exact station name.
   for(int mode=0;mode<2&&urls.isEmpty();mode++)for(String host:HOSTS){
    try {
     String endpoint;
     if(mode==0&&uuid!=null&&!uuid.isEmpty())endpoint="/json/stations/byuuid/"+URLEncoder.encode(uuid,"UTF-8");
     else endpoint="/json/stations/search?name="+URLEncoder.encode(name,"UTF-8")+"&limit=100&hidebroken=false";
     JSONArray arr=get("https://"+host+endpoint);
     for(int i=0;i<arr.length()&&urls.size()<10;i++){
      JSONObject o=arr.optJSONObject(i);if(o==null)continue;
      String candidateUuid=o.optString("stationuuid","");
      boolean sameId=uuid!=null&&!uuid.isEmpty()&&uuid.equalsIgnoreCase(candidateUuid);
      boolean sameName=!wanted.isEmpty()&&wanted.equals(clean(o.optString("name","")));
      boolean sameCountry=code.isEmpty()||code.equalsIgnoreCase(o.optString("countrycode",""));
      if(!sameId&&(!sameName||!sameCountry))continue;
      for(String key:new String[]{"url_resolved","url"}){
       String u=o.optString(key,"").trim();
       if(StationCatalog.valid(u)&&!u.equals(original))urls.add(u);
      }
     }
     if(!urls.isEmpty())break;
    }catch(Exception ignored){}
   }
   callback.complete(urls.toArray(new String[0]));
  },"radio-stream-repair").start();
 }
 private StreamResolver(){}
}
