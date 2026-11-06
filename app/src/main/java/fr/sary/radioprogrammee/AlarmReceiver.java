package fr.sary.radioprogrammee;
import android.content.*;
public class AlarmReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){Schedule.refresh(c);}}
