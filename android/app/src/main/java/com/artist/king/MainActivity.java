package com.artist.king;

import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.speech.RecognizerIntent;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** Offline, native home replacement. All special permissions are opt-in. */
public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root, body;
    private TextView clock, timerLabel;
    private int ink, paper, muted, line;
    private String page = "Home", visibleDay = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<App> apps = new ArrayList<>();
    private float downX, downY;
    private boolean swiped;
    private static final int VOICE=5, EXPORT=6, IMPORT=7;
    private static class App {
        String id, pkg, label;
        App(String i,String p,String l) {id=i;pkg=p;label=l;}
    }
    private final Runnable tick = new Runnable() { public void run() {
        if (!visibleDay.equals(day())) render();
        if (clock != null) clock.setText(new SimpleDateFormat(prefs.getBoolean("clock24",true)?"HH:mm":"h:mm a", Locale.getDefault()).format(new Date()));
        long end=prefs.getLong("focusEnd",0);
        if(end>0 && remaining()==0) {
            prefs.edit().putLong("focusEnd",0).putInt("sessions_"+day(),prefs.getInt("sessions_"+day(),0)+1).apply();
            toast("Focus complete. Take a deliberate break.");
            if(page.equals("Focus")||page.equals("Home")) render();
        }
        if(timerLabel!=null) timerLabel.setText(formatDuration(remaining()));
        handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); prefs=getSharedPreferences("king",MODE_PRIVATE);
        if(!prefs.contains("routines")) {
            JSONArray a=new JSONArray();
            a.put(item("Prayer & intention", "07:00")).put(item("Deep study", "08:00")).put(item("Train & move", "17:00")).put(item("Evening review", "21:00"));
            prefs.edit().putString("routines",a.toString()).apply();
        }
        render();
    }
    @Override protected void onResume() {super.onResume(); loadApps(); render(); handler.removeCallbacks(tick); handler.post(tick);}
    @Override protected void onPause() {super.onPause();handler.removeCallbacks(tick);}
    @Override protected void onNewIntent(Intent i) {super.onNewIntent(i);setIntent(i);page="Home";render();}
    @Override public void onBackPressed() {page="Home";render();}
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent e) {
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();swiped=false;}
        if(e.getAction()==MotionEvent.ACTION_UP && !swiped && Math.abs(e.getX()-downX)>dp(110) && Math.abs(e.getY()-downY)<dp(60)) {
            String[] pages={"Home","Apps","Board","Ritual","Focus","Settings"};
            int n=Arrays.asList(pages).indexOf(page), next=n+(e.getX()<downX?1:-1);
            if(next>=0&&next<pages.length){MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();go(pages[next]);swiped=true;return true;}
        }
        return super.dispatchTouchEvent(e);
    }
    private void render() {
        visibleDay=day(); boolean light=prefs.getBoolean("light",false);setTheme(light?R.style.LightTheme:R.style.AppTheme);paper=light?Color.WHITE:Color.BLACK;ink=light?Color.BLACK:Color.WHITE;muted=light?0xff626262:0xffa5a5a5;line=light?0xffdddddd:0xff282828;
        getWindow().setStatusBarColor(paper); getWindow().setNavigationBarColor(paper);
        getWindow().getDecorView().setSystemUiVisibility(light?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);
        if(prefs.getBoolean("fullscreen",false))getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        clock=null;timerLabel=null;
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(paper);root.setPadding(dp(22),dp(12),dp(22),dp(8));setContentView(root);
        LinearLayout brand=row();TextView logo=text("K I N G  /  F O C U S",12,muted);brand.addView(logo,new LinearLayout.LayoutParams(0,dp(40),1));TextView gear=button("•••",()->go("Settings"));gear.setContentDescription("Settings");brand.addView(gear);root.addView(brand);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(12),0,dp(20));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        switch(page){case "Apps":appsPage();break;case "Board":board();break;case "Ritual":ritual();break;case "Focus":focus();break;case "Settings":settings();break;default:home();}
        View divider=new View(this);divider.setBackgroundColor(line);root.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        LinearLayout nav=row();for(String s:new String[]{"Home","Apps","Board","Ritual","Focus"}){TextView b=button(s,()->go(s));b.setTextSize(11);b.setPadding(0,dp(14),0,dp(14));b.setGravity(Gravity.CENTER);b.setTextColor(page.equals(s)?ink:muted);nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}root.addView(nav);
    }
    private void home() {
        clock=text("",64,ink);clock.setTypeface(Typeface.create("sans-serif-thin",Typeface.NORMAL));body.addView(clock);
        body.addView(text(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault()),12,muted));space(24);
        TextView intention=text(prefs.getString("intention","Give this day a purpose."),22,ink);intention.setOnClickListener(v->edit("Today's intention",prefs.getString("intention",""),s->{prefs.edit().putString("intention",s).apply();render();}));body.addView(intention);space(24);
        if(remaining()>0){body.addView(button("FOCUS ACTIVE  →",()->go("Focus")));timerLabel=text(formatDuration(remaining()),24,ink);body.addView(timerLabel);space(12);}
        List<String> pins=list("pins");section("ESSENTIALS");int count=0;
        for(String id:pins){App a=findApp(id);if(a!=null&&!set("hidden").contains(id)){appRow(body,a,true);count++;}}
        if(count==0){body.addView(text("A quiet home starts with fewer choices.",16,muted));body.addView(button("Choose your essential apps  →",()->go("Apps")));}
        space(18);section("NEXT IN YOUR DAY");body.addView(text(nextRoutine(),18,ink));
        body.addView(button("Quick capture  +",()->capture(null)));
        body.addView(button("Phone",()->safeStart(new Intent(Intent.ACTION_DIAL))));
    }
    private void loadApps() {
        apps.clear(); Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo r:getPackageManager().queryIntentActivities(i,0)){
            if(r.activityInfo.packageName.equals(getPackageName()))continue;
            ComponentName c=new ComponentName(r.activityInfo.packageName,r.activityInfo.name);
            apps.add(new App(c.flattenToString(),r.activityInfo.packageName,r.loadLabel(getPackageManager()).toString()));
        }
        apps.sort((a,b)->label(a).compareToIgnoreCase(label(b)));
    }
    private void appsPage() {
        title("Less noise.","YOUR APPS / Long-press to organise");
        EditText search=input("Find an app","");search.setSingleLine(true);body.addView(search);
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);
        Runnable filter=()->{results.removeAllViews();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(App a:apps)if(!set("hidden").contains(a.id)&&label(a).toLowerCase(Locale.ROOT).contains(q))appRow(results,a,false);if(results.getChildCount()==0)results.addView(text("No matching apps.",16,muted));};
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){filter.run();}public void afterTextChanged(Editable e){}});filter.run();
    }
    private void appRow(LinearLayout parent,App a,boolean big) {
        String prefix="";
        TextView t=button(prefix+label(a)+(set("distract").contains(a.id)?"  ·":""),()->launch(a));t.setTextSize((big?29:20)+prefs.getInt("size",0));
        if(prefs.getBoolean("letters",false)){LetterIcon icon=new LetterIcon(label(a).substring(0,1).toUpperCase(Locale.ROOT));icon.setBounds(0,0,dp(34),dp(34));t.setCompoundDrawables(icon,null,null,null);t.setCompoundDrawablePadding(dp(14));}
        t.setTypeface(Typeface.create(prefs.getString("weight","sans-serif-light"),Typeface.NORMAL));t.setGravity(prefs.getBoolean("right",false)?Gravity.RIGHT|Gravity.CENTER_VERTICAL:Gravity.LEFT|Gravity.CENTER_VERTICAL);
        t.setOnLongClickListener(v->{organise(a);return true;});parent.addView(t,new LinearLayout.LayoutParams(-1,-2));
    }
    private void organise(App a) {
        boolean pinned=list("pins").contains(a.id),dis=set("distract").contains(a.id);
        String[] options={pinned?"Unpin from home":"Pin to home","Move to top of home","Rename",dis?"Remove distraction pause":"Mark as distracting","Hide app","App info"};
        new AlertDialog.Builder(this).setTitle(label(a)).setItems(options,(d,n)->{
            if(n==0){List<String> p=list("pins");if(pinned)p.remove(a.id);else p.add(a.id);saveList("pins",p);}
            if(n==1){List<String> p=list("pins");p.remove(a.id);p.add(0,a.id);saveList("pins",p);}
            if(n==2){edit("App name",label(a),s->{prefs.edit().putString("name_"+a.id,s.isEmpty()?a.label:s).apply();loadApps();render();});return;}
            if(n==3)toggleSet("distract",a.id);
            if(n==4)toggleSet("hidden",a.id);
            if(n==5)safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)));
            render();
        }).show();
    }
    private void launch(App a) {
        if(!set("distract").contains(a.id)){openApp(a);return;}
        if(remaining()>0){new AlertDialog.Builder(this).setTitle("Protect this session.").setMessage("This app is paused inside KING until your focus session ends. Phone and essential apps remain available.").setPositiveButton("Back to focus",(d,w)->go("Focus")).setNegativeButton("Stay here",null).show();return;}
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(8),dp(24),0);EditText why=input("What are you opening it for?","");panel.addView(why);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Choose, don't drift.").setMessage("Take five seconds before opening "+label(a)+".").setView(panel).setNegativeButton("Leave it",null).setPositiveButton("Wait 5s",null).create();
        dialog.setOnShowListener(v->{android.widget.Button b=dialog.getButton(-1);b.setEnabled(false);long ready=SystemClock.elapsedRealtime()+5000;
            Runnable r=new Runnable(){public void run(){if(!dialog.isShowing())return;long left=ready-SystemClock.elapsedRealtime();b.setText(left>0?"Wait "+((left+999)/1000)+"s":"Open intentionally");b.setEnabled(left<=0&&!why.getText().toString().trim().isEmpty());handler.postDelayed(this,250);}};handler.post(r);
            dialog.setOnDismissListener(x->handler.removeCallbacks(r));b.setOnClickListener(x->{dialog.dismiss();openApp(a);});});dialog.show();
    }
    private void openApp(App a){Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName.unflattenFromString(a.id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);safeStart(i);}
    private void board() {
        title("Clear your mind.","CAPTURE / PLAN / REFLECT");body.addView(button("+  Quick capture",()->capture(null)));
        body.addView(button("Voice capture",()->{new AlertDialog.Builder(this).setTitle("Voice input").setMessage("KING stores the result locally. Your phone's speech provider may use the internet; offline recognition is requested but is not guaranteed. Type instead for fully offline capture.").setPositiveButton("Continue",(d,w)->{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);try{startActivityForResult(i,VOICE);}catch(ActivityNotFoundException ex){toast("No speech provider. Use your keyboard microphone or type.");}}).setNegativeButton("Cancel",null).show();}));
        section("NEXT EVENT");calendarCard();space(16);section("TASKS");entries("tasks");body.addView(button("+  Add task",()->capture("todo: ")));space(16);section("NOTES");entries("notes");space(16);section("PHONE AWARENESS");usageCard();
    }
    private void capture(String initial) {
        edit("Quick capture • todo: or note:",initial==null?"":initial,s->{if(s.isEmpty())return;String key=s.toLowerCase(Locale.ROOT).startsWith("todo:")?"tasks":"notes";s=s.replaceFirst("(?i)^(todo|note):\\s*","");if(s.trim().isEmpty())return;JSONArray a=array(key);a.put(item(s,""));saveArray(key,a);render();});
    }
    private void entries(String key) {
        JSONArray arr=array(key);if(arr.length()==0)body.addView(text(key.equals("tasks")?"Nothing pending. Capture one clear next action.":"A place for thoughts worth keeping.",15,muted));
        for(int n=0;n<arr.length();n++){final int index=n;JSONObject o=arr.optJSONObject(n);if(o==null)continue;
            LinearLayout r=row();boolean task=key.equals("tasks");
            if(task){CheckBox cb=new CheckBox(this);cb.setButtonTintList(android.content.res.ColorStateList.valueOf(muted));cb.setChecked(o.optBoolean("done"));cb.setContentDescription("Complete "+o.optString("text"));cb.setOnCheckedChangeListener((b,c)->{try{o.put("done",c);}catch(JSONException ignored){}saveArray(key,arr);});r.addView(cb);}
            TextView t=button(o.optString("text"),()->edit("Edit "+(task?"task":"note"),o.optString("text"),s->{try{o.put("text",s);}catch(JSONException ignored){}saveArray(key,arr);render();}));t.setTextSize(17);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));TextView del=button("×",()->confirm("Delete this entry?",()->{arr.remove(index);saveArray(key,arr);render();}));del.setContentDescription("Delete entry");r.addView(del);body.addView(r);
        }
    }
    private void ritual() {
        title("Keep your word.","DAILY ROUTINE / RESETS AT MIDNIGHT");body.addView(text("Small acts, faithfully repeated.",17,muted));space(16);
        JSONArray a=array("routines");int complete=0;
        for(int n=0;n<a.length();n++){
            JSONObject o=a.optJSONObject(n);if(o==null)continue;String id=o.optString("id");boolean done=prefs.getBoolean("done_"+day()+"_"+id,false);if(done)complete++;
            LinearLayout r=row();CheckBox cb=new CheckBox(this);cb.setChecked(done);cb.setButtonTintList(android.content.res.ColorStateList.valueOf(muted));cb.setContentDescription("Complete "+o.optString("text"));cb.setOnCheckedChangeListener((b,c)->{prefs.edit().putBoolean("done_"+day()+"_"+id,c).apply();render();});r.addView(cb);
            final int index=n;TextView t=button(o.optString("time")+"   "+o.optString("text"),()->routineEditor(o,index));t.setTextSize(18);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));body.addView(r);
            body.addView(text("     "+streak(id)+" day streak",12,muted));
        }
        space(16);body.addView(text(complete+" / "+a.length()+" completed today",18,ink));body.addView(button("+  Add a daily ritual",()->routineEditor(null,-1)));
        space(18);section("EVENING REVIEW");body.addView(text(prefs.getString("review_"+day(),"What mattered? What distracted you? What changes tomorrow?"),16,muted));body.addView(button("Write today's review",()->edit("Evening review",prefs.getString("review_"+day(),""),s->{prefs.edit().putString("review_"+day(),s).apply();render();})));
    }
    private void routineEditor(JSONObject existing,int index) {
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),0,dp(24),0);EditText name=input("Ritual",existing==null?"":existing.optString("text"));EditText time=input("Time • HH:mm",existing==null?"08:00":existing.optString("time"));panel.addView(name);panel.addView(time);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Daily ritual").setView(panel).setPositiveButton("Save",null).setNegativeButton("Cancel",null).setNeutralButton(existing==null?"":"Delete",(v,w)->{if(existing!=null)confirm("Delete this ritual?",()->{JSONArray a=array("routines");a.remove(index);saveArray("routines",a);render();});}).create();
        d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{String s=name.getText().toString().trim(),t=time.getText().toString().trim();if(s.isEmpty()||!t.matches("([01]\\d|2[0-3]):[0-5]\\d")){toast("Enter a name and a time such as 08:30.");return;}JSONObject o=existing==null?item(s,t):existing;try{o.put("text",s);o.put("time",t);JSONArray a=array("routines");if(index<0)a.put(o);else a.put(index,o);saveArray("routines",a);}catch(JSONException ignored){}d.dismiss();render();}));d.show();
    }
    private String nextRoutine(){JSONArray a=array("routines");String now=new SimpleDateFormat("HH:mm",Locale.ROOT).format(new Date()),first="",next="";for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||prefs.getBoolean("done_"+day()+"_"+o.optString("id"),false))continue;String t=o.optString("time")+"  "+o.optString("text");if(first.isEmpty()||t.compareTo(first)<0)first=t;if(o.optString("time").compareTo(now)>=0&&(next.isEmpty()||t.compareTo(next)<0))next=t;}return !next.isEmpty()?next:!first.isEmpty()?"Still to do: "+first:"Today's rituals are complete.";}
    private int streak(String id){Calendar c=Calendar.getInstance();int n=0;if(!prefs.getBoolean("done_"+date(c)+"_"+id,false))c.add(Calendar.DATE,-1);for(int i=0;i<3660;i++){if(!prefs.getBoolean("done_"+date(c)+"_"+id,false))break;n++;c.add(Calendar.DATE,-1);}return n;}
    private void focus() {
        title("One thing.","DEEP WORK / DELIBERATE REST");
        body.addView(text("Distracting apps pause inside this launcher during a session. Long-press apps to mark them. Notifications and other launch routes remain available.",16,muted));space(24);
        if(remaining()>0){timerLabel=text(formatDuration(remaining()),64,ink);body.addView(timerLabel);body.addView(text(prefs.getString("focusTask",""),22,ink));body.addView(button("End session",()->confirm("End this focus session early?",()->{prefs.edit().putLong("focusEnd",0).apply();render();})));}
        else {EditText task=input("What will you finish?",prefs.getString("focusTask",""));body.addView(task);for(int minutes:new int[]{25,50,90})body.addView(button("Start "+minutes+" minutes  →",()->{String s=task.getText().toString().trim();if(s.isEmpty()){toast("Choose one task first.");return;}prefs.edit().putString("focusTask",s).putLong("focusEnd",System.currentTimeMillis()+minutes*60000L).putLong("focusElapsedEnd",SystemClock.elapsedRealtime()+minutes*60000L).putInt("focusBoot",Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,0)).apply();hideKeyboard();render();}));}
        space(20);body.addView(text(prefs.getInt("sessions_"+day(),0)+" completed sessions today",15,muted));body.addView(button("Open Do Not Disturb settings",()->safeStart(new Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS))));body.addView(button("Choose distracting apps",()->go("Apps")));body.addView(text("The timer continues when you leave or lock the phone. Completion is recorded when KING next resumes; it does not sound a background alarm.",13,muted));
    }
    private long remaining(){if(prefs.getLong("focusEnd",0)==0)return 0;int boot=Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,0);long ms=boot==prefs.getInt("focusBoot",-1)?prefs.getLong("focusElapsedEnd",0)-SystemClock.elapsedRealtime():prefs.getLong("focusEnd",0)-System.currentTimeMillis();return Math.max(0,ms);}
    private String formatDuration(long ms){long s=(ms+999)/1000;return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private void calendarCard(){
        if(checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){body.addView(button("Show my next calendar event",()->requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},10)));return;}
        long now=System.currentTimeMillis();Uri.Builder u=CalendarContract.Instances.CONTENT_URI.buildUpon();ContentUris.appendId(u,now);ContentUris.appendId(u,now+7*86400000L);
        try(Cursor c=getContentResolver().query(u.build(),new String[]{CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.ALL_DAY},CalendarContract.Instances.END+">?",new String[]{Long.toString(now)},CalendarContract.Instances.BEGIN+" ASC")){
            if(c!=null&&c.moveToFirst()){long start=c.getLong(1);String when=c.getInt(2)==1?"All day":new SimpleDateFormat("EEE, HH:mm",Locale.getDefault()).format(new Date(start));body.addView(text(c.getString(0)+"\n"+when,18,ink));}else body.addView(text("No events in the next seven days.",15,muted));
        }catch(SecurityException e){body.addView(text("Calendar access unavailable.",15,muted));}
        body.addView(button("Open calendar",()->safeStart(new Intent(Intent.ACTION_VIEW,Uri.parse("content://com.android.calendar/time/"+now)))));
    }
    private boolean usageAllowed(){AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;}
    private void usageCard(){
        if(!usageAllowed()){body.addView(text("Optional Usage Access reveals screen activity and unlocks. All calculations stay on this phone.",15,muted));body.addView(button("Enable Usage Access",()->safeStart(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));return;}
        Calendar mid=Calendar.getInstance();mid.set(Calendar.HOUR_OF_DAY,0);mid.set(Calendar.MINUTE,0);mid.set(Calendar.SECOND,0);mid.set(Calendar.MILLISECOND,0);long start=mid.getTimeInMillis(),now=System.currentTimeMillis(),screenStart=-1,total=0;int unlocks=0;
        try{UsageStatsManager manager=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);UsageEvents events=manager.queryEvents(start-86400000L,now);UsageEvents.Event e=new UsageEvents.Event();if(events==null){body.addView(text("Usage history unavailable.",15,muted));return;}
            while(events.hasNextEvent()){events.getNextEvent(e);long ts=e.getTimeStamp();if(e.getEventType()==UsageEvents.Event.SCREEN_INTERACTIVE){if(screenStart<0)screenStart=Math.max(ts,start);}else if(e.getEventType()==UsageEvents.Event.SCREEN_NON_INTERACTIVE){if(screenStart>=0&&ts>=start)total+=Math.max(0,ts-screenStart);screenStart=-1;}else if(e.getEventType()==UsageEvents.Event.KEYGUARD_HIDDEN&&ts>=start)unlocks++;}
            if(screenStart>=0)total+=now-screenStart;
            body.addView(text((total/60000)+" min screen active   ·   "+unlocks+" unlocks",21,ink));body.addView(text("Today, from Android event history. Estimates depend on device reporting; screen-active time includes the lock screen.",13,muted));
        }catch(RuntimeException e){body.addView(text("This device did not provide usage history.",15,muted));}
    }
    private void settings(){
        title("Make room.","APPEARANCE / ACCESS / YOUR DATA");
        body.addView(button("Set KING as default Home app",()->{if(Build.VERSION.SDK_INT>=29){RoleManager r=getSystemService(RoleManager.class);if(r!=null&&r.isRoleAvailable(RoleManager.ROLE_HOME)&&!r.isRoleHeld(RoleManager.ROLE_HOME)){startActivityForResult(r.createRequestRoleIntent(RoleManager.ROLE_HOME),9);return;}}safeStart(new Intent(Settings.ACTION_HOME_SETTINGS));}));
        toggle("Milk white theme","light",false);toggle("Hide status bar","fullscreen",false);toggle("Soft haptic feedback","haptics",true);toggle("Letter icons","letters",false);toggle("Right aligned app list","right",false);toggle("24-hour clock","clock24",true);
        body.addView(button("Font weight",()->new AlertDialog.Builder(this).setTitle("Typography").setItems(new String[]{"Light","Regular","Bold"},(d,n)->{prefs.edit().putString("weight",new String[]{"sans-serif-light","sans-serif","sans-serif-black"}[n]).apply();render();}).show()));
        body.addView(button("Letter icon stroke",()->new AlertDialog.Builder(this).setTitle("Icon stroke").setItems(new String[]{"Fine","Standard","Strong"},(d,n)->{prefs.edit().putInt("stroke",n+1).apply();render();}).show()));
        body.addView(button("App text size",()->new AlertDialog.Builder(this).setTitle("Text size").setItems(new String[]{"Compact","Standard","Large"},(d,n)->{prefs.edit().putInt("size",new int[]{-3,0,4}[n]).apply();render();}).show()));
        body.addView(button("Hidden apps ("+set("hidden").size()+")",()->{List<App> h=new ArrayList<>();for(App a:apps)if(set("hidden").contains(a.id))h.add(a);String[] names=new String[h.size()];for(int i=0;i<h.size();i++)names[i]="Show "+label(h.get(i));new AlertDialog.Builder(this).setTitle("Restore hidden apps").setItems(names,(d,n)->{toggleSet("hidden",h.get(n).id);render();}).setNegativeButton("Close",null).show();}));
        section("YOUR DATA");body.addView(button("Export backup",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"king-focus-backup.json");try{startActivityForResult(i,EXPORT);}catch(ActivityNotFoundException e){toast("No file picker available.");}}));
        body.addView(button("Restore backup",()->confirm("Replace KING's current settings, tasks, notes and routine history with a backup?",()->{try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),IMPORT);}catch(ActivityNotFoundException e){toast("No file picker available.");}})));
        body.addView(button("Android app permissions",()->safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))));
        body.addView(button("Switch back to another launcher",()->safeStart(new Intent(Settings.ACTION_HOME_SETTINGS))));space(12);
        body.addView(text("KING Focus 1.0 • Independent, original launcher.\nNo ads. No analytics. No network permission.\nBackups are unencrypted: keep them private. Voice input is handled by your chosen speech provider. Daily rituals are a checklist, not alarm notifications.",13,muted));
    }
    @Override public void onRequestPermissionsResult(int req,String[] p,int[] r){super.onRequestPermissionsResult(req,p,r);render();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        if(request==VOICE){ArrayList<String> s=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(s!=null&&!s.isEmpty())capture(s.get(0));}
        if(request==EXPORT&&data.getData()!=null){try{JSONObject backup=new JSONObject();backup.put("format","king-focus-v1");JSONObject values=new JSONObject();for(Map.Entry<String,?> e:prefs.getAll().entrySet())values.put(e.getKey(),e.getValue());backup.put("values",values);try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();out.write(backup.toString(2).getBytes(StandardCharsets.UTF_8));}toast("Backup saved.");}catch(Exception e){toast("Could not export backup.");}}
        if(request==IMPORT&&data.getData()!=null){try(InputStream in=getContentResolver().openInputStream(data.getData())){if(in==null)throw new IOException();ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>2*1024*1024)throw new IOException();bytes.write(buffer,0,n);}JSONObject b=new JSONObject(bytes.toString("UTF-8"));if(!"king-focus-v1".equals(b.optString("format")))throw new IOException();JSONObject values=b.getJSONObject("values");validateBackup(values);SharedPreferences.Editor ed=prefs.edit().clear();Iterator<String> keys=values.keys();while(keys.hasNext()){String k=keys.next();Object v=values.get(k);if(v instanceof Boolean)ed.putBoolean(k,(Boolean)v);else if(v instanceof Number){if(k.equals("size")||k.equals("stroke")||k.equals("focusBoot")||k.startsWith("sessions_"))ed.putInt(k,((Number)v).intValue());else ed.putLong(k,((Number)v).longValue());}else if(v instanceof String)ed.putString(k,(String)v);}ed.putLong("focusEnd",0).commit();loadApps();render();toast("Backup restored. Active timer cleared.");}catch(Exception e){toast("Invalid backup. Your existing data was kept.");}}
    }
    private void validateBackup(JSONObject v)throws Exception {Iterator<String> it=v.keys();while(it.hasNext()){String k=it.next();Object o=v.get(k);boolean bool=Arrays.asList("light","fullscreen","haptics","letters","right","clock24").contains(k)||k.startsWith("done_");boolean number=Arrays.asList("size","stroke","focusBoot","focusEnd","focusElapsedEnd").contains(k)||k.startsWith("sessions_");if(bool&&!(o instanceof Boolean)||number&&!(o instanceof Number)||!bool&&!number&&!(o instanceof String))throw new IOException();}
        for(String key:new String[]{"tasks","notes","routines","pins","hidden","distract"})if(v.has(key)){JSONArray a=new JSONArray(v.getString(key));for(int i=0;i<a.length();i++){if(Arrays.asList("pins","hidden","distract").contains(key)){if(!(a.get(i) instanceof String))throw new IOException();}else{JSONObject o=a.getJSONObject(i);if(!o.has("id")||!o.has("text"))throw new IOException();if(key.equals("routines")&&!o.optString("time").matches("([01]\\d|2[0-3]):[0-5]\\d"))throw new IOException();}}}
    }
    private class LetterIcon extends android.graphics.drawable.Drawable {
        private final String letter; private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        LetterIcon(String s){letter=s;}
        public void draw(android.graphics.Canvas canvas){android.graphics.Rect b=getBounds();paint.setColor(ink);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(prefs.getInt("stroke",1)));canvas.drawRect(b.left+dp(2),b.top+dp(2),b.right-dp(2),b.bottom-dp(2),paint);paint.setStyle(android.graphics.Paint.Style.FILL);paint.setTextSize(dp(18));paint.setTypeface(Typeface.MONOSPACE);paint.setTextAlign(android.graphics.Paint.Align.CENTER);canvas.drawText(letter,b.exactCenterX(),b.exactCenterY()-(paint.ascent()+paint.descent())/2,paint);}
        public void setAlpha(int a){paint.setAlpha(a);} public void setColorFilter(android.graphics.ColorFilter f){paint.setColorFilter(f);} public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
    private void toggle(String label,String key,boolean fallback){Switch s=new Switch(this);s.setText(label);s.setTextSize(16);s.setTextColor(ink);s.setPadding(0,dp(10),0,dp(10));s.setMinHeight(dp(52));s.setChecked(prefs.getBoolean(key,fallback));s.setOnCheckedChangeListener((b,c)->{prefs.edit().putBoolean(key,c).apply();render();});body.addView(s);}
    private void go(String p){hideKeyboard();page=p;render();}
    private void hideKeyboard(){View f=getCurrentFocus();if(f!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(f.getWindowToken(),0);}
    private void safeStart(Intent i){try{startActivity(i);}catch(RuntimeException e){toast("This action isn't available on your device.");}}
    private void title(String h,String sub){body.addView(text(h,36,ink));space(8);body.addView(text(sub,11,muted));space(22);}
    private void section(String s){space(8);body.addView(text(s,11,muted));space(8);}
    private void space(int h){Space s=new Space(this);body.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setLineSpacing(dp(3),1);return t;}
    private TextView button(String s,Runnable action){TextView t=text(s,16,ink);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(4),dp(12),dp(4),dp(12));t.setMinHeight(dp(48));t.setFocusable(true);t.setClickable(true);android.util.TypedValue value=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackground,value,true);t.setBackgroundResource(value.resourceId);t.setOnClickListener(v->{if(prefs.getBoolean("haptics",true))v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);action.run();});return t;}
    private EditText input(String hint,String value){EditText e=new EditText(this);e.setTextColor(ink);e.setHintTextColor(muted);e.setTextSize(18);e.setHint(hint);e.setText(value);e.setPadding(dp(8),dp(14),dp(8),dp(14));e.setMinHeight(dp(52));e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);return e;}
    interface Save{void run(String s);}
    private void edit(String title,String initial,Save save){EditText e=input(title,initial);LinearLayout panel=new LinearLayout(this);panel.setPadding(dp(20),0,dp(20),0);panel.addView(e,new LinearLayout.LayoutParams(-1,-2));AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(panel).setPositiveButton("Save",(v,w)->save.run(e.getText().toString().trim())).setNegativeButton("Cancel",null).create();d.setOnShowListener(v->{e.requestFocus();e.setSelection(e.length());});d.getWindow();d.show();}
    private void confirm(String s,Runnable r){new AlertDialog.Builder(this).setMessage(s).setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->r.run()).show();}
    private JSONObject item(String text,String time){JSONObject o=new JSONObject();try{o.put("id",UUID.randomUUID().toString());o.put("text",text);o.put("time",time);o.put("done",false);}catch(JSONException ignored){}return o;}
    private JSONArray array(String k){try{return new JSONArray(prefs.getString(k,"[]"));}catch(JSONException e){return new JSONArray();}}
    private void saveArray(String k,JSONArray a){prefs.edit().putString(k,a.toString()).apply();}
    private List<String> list(String k){JSONArray a=array(k);List<String> result=new ArrayList<>();for(int i=0;i<a.length();i++)result.add(a.optString(i));return result;}
    private Set<String> set(String k){return new HashSet<>(list(k));}
    private void saveList(String k,Collection<String> s){saveArray(k,new JSONArray(s));}
    private void toggleSet(String k,String s){Set<String> a=set(k);if(!a.remove(s))a.add(s);saveList(k,a);}
    private App findApp(String id){for(App a:apps)if(a.id.equals(id))return a;return null;}
    private String label(App a){String s=prefs.getString("name_"+a.id,a.label);return s.isEmpty()?a.label:s;}
    private String day(){return date(Calendar.getInstance());}
    private String date(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(c.getTime());}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
