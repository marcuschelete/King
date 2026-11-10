package com.artist.king;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Opt-in, movable audio controls. Never reads notifications or other apps. */
public class IslandService extends Service {
    private WindowManager wm; private WindowManager.LayoutParams params; private LinearLayout panel;
    private android.content.SharedPreferences prefs; private AudioManager audio;
    private boolean expanded,registered; private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable collapse=()->{if(expanded){expanded=false;show();}};
    private final BroadcastReceiver screen=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){expanded=false;remove();}
        else if(!getSystemService(KeyguardManager.class).isKeyguardLocked())show();
    }};
    public static void sync(Context c){
        boolean enabled=c.getSharedPreferences("king",MODE_PRIVATE).getBoolean("islandEnabled",false);
        Intent i=new Intent(c,IslandService.class);
        if(enabled&&Settings.canDrawOverlays(c)){try{c.startForegroundService(i);}catch(RuntimeException e){Toast.makeText(c,"Could not start island. Try enabling it again in Settings.",Toast.LENGTH_LONG).show();}}
        else c.stopService(i);
    }
    @Override public void onCreate(){super.onCreate();prefs=getSharedPreferences("king",MODE_PRIVATE);wm=getSystemService(WindowManager.class);audio=getSystemService(AudioManager.class);}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&"stop".equals(intent.getAction())){prefs.edit().putBoolean("islandEnabled",false).apply();stopSelf();return START_NOT_STICKY;}
        if(!prefs.getBoolean("islandEnabled",false)||!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("island","Floating island",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,40,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,41,new Intent(this,IslandService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"island").setSmallIcon(R.drawable.ic_king).setContentTitle("KING island is on").setContentText("Tap the pill for volume. Drag to move.").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Turn off",stop).build()).build();
        try{if(Build.VERSION.SDK_INT>=34)startForeground(42,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(42,n);}catch(RuntimeException e){stopSelf();return START_NOT_STICKY;}
        if(!registered){IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_SCREEN_ON);filter.addAction(Intent.ACTION_USER_PRESENT);if(Build.VERSION.SDK_INT>=33)registerReceiver(screen,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screen,filter);registered=true;}
        if(panel==null&&!getSystemService(KeyguardManager.class).isKeyguardLocked())show();
        return START_NOT_STICKY;
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView button(String label,Runnable action){TextView v=new TextView(this);v.setText(label);v.setTextColor(0xfff4f1e9);v.setTextSize(14);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(48));v.setPadding(dp(12),0,dp(12),0);v.setOnClickListener(w->{action.run();});return v;}
    private void remove(){handler.removeCallbacks(collapse);if(panel!=null){try{wm.removeView(panel);}catch(RuntimeException ignored){}panel=null;}}
    private void show(){
        remove();if(!Settings.canDrawOverlays(this)){stopSelf();return;}
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(4),dp(2),dp(4),dp(2));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xf21a222b);bg.setCornerRadius(dp(26));bg.setStroke(dp(1),0x557d8b99);panel.setBackground(bg);panel.setElevation(dp(8));
        params=new WindowManager.LayoutParams(expanded?Math.min(dp(280),getResources().getDisplayMetrics().widthPixels-dp(16)):dp(68),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.LEFT;params.x=prefs.getInt("islandX",Math.max(0,getResources().getDisplayMetrics().widthPixels-dp(84)));params.y=prefs.getInt("islandY",dp(72));
        TextView handle=button(expanded?"Volume  ·  tap to close":"•••",()->{expanded=!expanded;show();});handle.setContentDescription(expanded?"Collapse volume controls. Drag to move.":"Open volume controls. Drag to move.");panel.addView(handle);
        handle.setOnTouchListener(new View.OnTouchListener(){float x,y;int startX,startY;boolean moved;public boolean onTouch(View v,android.view.MotionEvent e){switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:x=e.getRawX();y=e.getRawY();startX=params.x;startY=params.y;moved=false;handler.removeCallbacks(collapse);return true;
            case MotionEvent.ACTION_MOVE:float dx=e.getRawX()-x,dy=e.getRawY()-y;if(Math.hypot(dx,dy)>dp(8))moved=true;if(moved){params.x=startX+(int)dx;params.y=startY+(int)dy;clamp();update();}return true;
            case MotionEvent.ACTION_UP:if(moved){prefs.edit().putInt("islandX",params.x).putInt("islandY",params.y).apply();schedule();}else v.performClick();return true;
            case MotionEvent.ACTION_CANCEL:schedule();return true;
        }return false;}});
        if(expanded){volume("Media",AudioManager.STREAM_MUSIC);volume("Ring",AudioManager.STREAM_RING);volume("Alarm",AudioManager.STREAM_ALARM);
            panel.addView(button("Sound settings",()->{expanded=false;show();try{startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException e){Toast.makeText(IslandService.this,"Open Android sound settings manually.",Toast.LENGTH_SHORT).show();}}));
        }
        panel.setOnTouchListener((v,e)->{if(expanded&&e.getAction()==MotionEvent.ACTION_OUTSIDE){expanded=false;show();return true;}return false;});
        clamp();try{wm.addView(panel,params);panel.post(()->{if(panel!=null){clamp();update();}});}catch(RuntimeException e){panel=null;stopSelf();return;}schedule();
    }
    private void schedule(){handler.removeCallbacks(collapse);if(expanded)handler.postDelayed(collapse,15000);}
    private void volume(String name,int stream){TextView label=button(name,()->{});label.setClickable(false);label.setMinHeight(dp(24));panel.addView(label);SeekBar slider=new SeekBar(this);slider.setContentDescription(name+" volume");slider.setMax(audio.getStreamMaxVolume(stream));slider.setProgress(audio.getStreamVolume(stream));slider.setEnabled(!audio.isVolumeFixed());panel.addView(slider,new LinearLayout.LayoutParams(-1,dp(48)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){handler.removeCallbacks(collapse);}public void onStopTrackingTouch(SeekBar b){schedule();}public void onProgressChanged(SeekBar b,int value,boolean user){if(!user)return;try{audio.setStreamVolume(stream,value,0);}catch(SecurityException e){Toast.makeText(IslandService.this,"Android restricts this change in Do Not Disturb. Use Sound settings.",Toast.LENGTH_SHORT).show();}b.setProgress(audio.getStreamVolume(stream));}});
    }
    private void clamp(){int height=panel!=null&&panel.getHeight()>0?panel.getHeight():dp(expanded?330:52);params.x=Math.max(0,Math.min(params.x,getResources().getDisplayMetrics().widthPixels-params.width));params.y=Math.max(0,Math.min(params.y,getResources().getDisplayMetrics().heightPixels-height-dp(56)));}
    private void update(){if(panel!=null)try{wm.updateViewLayout(panel,params);}catch(RuntimeException e){stopSelf();}}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);expanded=false;if(panel!=null)show();}
    @Override public void onDestroy(){remove();if(registered)unregisterReceiver(screen);stopForeground(true);super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
