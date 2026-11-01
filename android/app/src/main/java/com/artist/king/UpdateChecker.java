package com.artist.king;
import android.app.*;
import android.content.*;
import android.net.Uri;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class UpdateChecker {
    private boolean running;
    void check(Activity activity,boolean manual){
        SharedPreferences prefs=activity.getSharedPreferences("king",0);
        if(running||!manual&&(!prefs.getBoolean("autoUpdates",true)||!UpdatePolicy.due(System.currentTimeMillis(),prefs.getLong("updateChecked",0))))return;
        running=true;prefs.edit().putLong("updateChecked",System.currentTimeMillis()).apply();
        new Thread(()->{
            JSONObject release=null;String error=null;HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL("https://raw.githubusercontent.com/marcuschelete/King/main/downloads/update.json").openConnection();
                connection.setConnectTimeout(8000);connection.setReadTimeout(8000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);connection.setRequestProperty("Accept","application/json");
                if(connection.getResponseCode()!=200)throw new IOException();
                try(InputStream input=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[2048];int n;while((n=input.read(buffer))!=-1){if(bytes.size()+n>32768)throw new IOException();bytes.write(buffer,0,n);}
                    release=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
                }
                if(!"com.artist.king".equals(release.getString("applicationId"))||!UpdatePolicy.allowed(release.getString("apkUrl"))||release.getInt("versionCode")<=0)throw new IOException();
            }catch(Exception e){error="Could not check for updates. Check your connection and try again.";}finally{if(connection!=null)connection.disconnect();}
            final JSONObject result=release;final String failure=error;
            activity.runOnUiThread(()->{
                running=false;if(activity.isFinishing()||activity.isDestroyed()||!manual&&!activity.hasWindowFocus())return;
                if(failure!=null){if(manual)message(activity,failure);return;}
                try{
                    long installed=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).getLongVersionCode();
                    if(result.getLong("versionCode")<=installed){if(manual)message(activity,"KING is up to date.");return;}
                    if(!manual&&!UpdatePolicy.due(System.currentTimeMillis(),prefs.getLong("updateSnoozed",0)))return;
                    new AlertDialog.Builder(activity).setTitle("KING "+result.getString("versionName")+" is ready")
                        .setMessage(result.optString("notes","A new version is available.")+"\n\nDownload the APK, then open it to confirm installation. Your existing data stays in place.")
                        .setPositiveButton("Download update",(d,w)->{try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(result.optString("apkUrl"))));}catch(ActivityNotFoundException e){message(activity,"No browser is available to download the update.");}})
                        .setNegativeButton("Later",(d,w)->prefs.edit().putLong("updateSnoozed",System.currentTimeMillis()).apply()).show();
                }catch(Exception e){if(manual)message(activity,"The update information could not be read.");}
            });
        },"king-update-check").start();
    }
    private void message(Activity activity,String value){android.widget.Toast.makeText(activity,value,android.widget.Toast.LENGTH_LONG).show();}
}
