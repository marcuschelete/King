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
    private final UpdateChecker updates=new UpdateChecker();
    private static final int WALLPAPER=33;
    private LinearLayout root, body;
    private ScrollView mainScroll;
    private String touchPage;
    private boolean touchAtTop;
    private TextView clock, timerLabel;
    private int ink, paper, muted, line;
    private String page = "Home", visibleDay = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<App> apps = new ArrayList<>();
    private float downX, downY;
    private boolean swiped;
    private long touchStart;
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
        if(!prefs.getBoolean("designV2",false))prefs.edit().putBoolean("fullscreen",true).putBoolean("designV2",true).apply();
        render();
    }
    @Override protected void onResume() {super.onResume(); loadApps(); render(); handler.removeCallbacks(tick); handler.post(tick);updates.check(this,false);}
    @Override protected void onPause() {super.onPause();handler.removeCallbacks(tick);}
    @Override protected void onNewIntent(Intent i) {super.onNewIntent(i);setIntent(i);page="Home";render();updates.check(this,false);}
    @Override public void onBackPressed() {go(Arrays.asList("Ritual","Tasks","Notes").contains(page)?"Board":"Home");}
    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN&&indexRail!=null){int[] location=new int[2];indexRail.getLocationOnScreen(location);indexDragging=e.getRawX()>=location[0]&&e.getRawX()<location[0]+indexRail.getWidth()&&e.getRawY()>=location[1]&&e.getRawY()<location[1]+indexRail.getHeight();}
        if(indexDragging){boolean handled=super.dispatchTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)indexDragging=false;return handled;}

        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
            downX=e.getX();downY=e.getY();swiped=false;touchPage=page;
            touchAtTop=mainScroll==null||!mainScroll.canScrollVertically(-1);touchStart=SystemClock.elapsedRealtime();
        }
        if(swiped)return true;
        if(e.getPointerCount()==1 && (e.getActionMasked()==MotionEvent.ACTION_MOVE||e.getActionMasked()==MotionEvent.ACTION_UP)){
            float dx=e.getX()-downX,dy=e.getY()-downY;
            String target=GestureRoutes.destination(touchPage,dx/getResources().getDisplayMetrics().density,dy/getResources().getDisplayMetrics().density,touchAtTop,SystemClock.elapsedRealtime()-touchStart);
            if(target!=null){
                MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();
                swiped=true; prefs.edit().putBoolean("gesturesSeen",true).apply();go(target);return true;
            }
        }
        return super.dispatchTouchEvent(e);
    }
    private void render() {
        visibleDay=day();boolean light=prefs.getBoolean("light",false);setTheme(light?R.style.LightTheme:R.style.AppTheme);
        paper=light?0xffeeeae3:0xff0d171f;ink=light?0xff15212a:0xfff4f1e9;muted=light?0xff46545e:0xffb7c2cb;line=light?0x30506070:0x40d8e4ed;
        getWindow().setStatusBarColor(paper);getWindow().setNavigationBarColor(paper);
        getWindow().getDecorView().setSystemUiVisibility(light?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);
        if(prefs.getBoolean("fullscreen",true))getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        clock=null;timerLabel=null;indexRail=null;indexDragging=false;
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(Atmosphere.background(this,!page.equals("Home")));root.setPadding(dp(22),dp(10),dp(22),dp(8));setContentView(root);
        LinearLayout top=row();
        if(page.equals("Home")){
            Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level=battery==null?-1:battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=battery==null?100:battery.getIntExtra(BatteryManager.EXTRA_SCALE,100);
            TextView status=text(level<0?"":Math.round(level*100f/Math.max(1,scale))+"%",13,muted);status.setGravity(Gravity.CENTER);top.addView(status,new LinearLayout.LayoutParams(0,dp(48),1));
            TextView settings=button("⚙",()->go("Settings"));settings.setContentDescription("Settings");settings.setTextSize(23);settings.setGravity(Gravity.CENTER);top.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));
        }else{
            TextView back=button("‹",()->go(Arrays.asList("Ritual","Tasks","Notes").contains(page)?"Board":"Home"));back.setContentDescription("Back");back.setTextSize(30);top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
            TextView label=text(page.equals("Board")?"your day":page.equals("Ritual")?"routine":page.toLowerCase(Locale.ROOT),14,muted);top.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            if(page.equals("Board")){TextView add=button("+",()->capture("todo: "));add.setContentDescription("Add task");add.setTextSize(28);top.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));}
        }
        root.addView(top);mainScroll=new ScrollView(this);mainScroll.setFillViewport(true);mainScroll.setVerticalScrollBarEnabled(false);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(8),0,dp(24));mainScroll.addView(body);root.addView(mainScroll,new LinearLayout.LayoutParams(-1,0,1));
        switch(page){case "Apps":appsPage();break;case "Board":board();break;case "Ritual":ritual();break;case "Focus":focus();break;case "Agenda":agenda();break;case "Settings":settings();break;case "Tasks":title("Tasks","");entries("tasks");body.addView(button("+ Add task",()->capture("todo: ")));break;case "Notes":title("Notes","");entries("notes");body.addView(button("+ Add note",()->capture("note: ")));break;default:home();}
        if(page.equals("Home"))homeShortcuts();
        // Native accessibility actions are an alternative to gestures, without a persistent tab bar.
        root.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            public void onInitializeAccessibilityNodeInfo(View v,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(v,info);int id=10001;for(String name:new String[]{"Home","Apps","Board","Agenda","Settings"})info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(id++,"Open "+name));}
            public boolean performAccessibilityAction(View v,int action,Bundle args){if(action>=10001&&action<=10005){go(new String[]{"Home","Apps","Board","Agenda","Settings"}[action-10001]);return true;}return super.performAccessibilityAction(v,action,args);}
        });
    }
    private void home() {
        float height=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density;
        space(height<650?18:52);
        clock=text(new SimpleDateFormat(prefs.getBoolean("clock24",true)?"HH:mm":"h:mm a",Locale.getDefault()).format(new Date()),prefs.getBoolean("clock24",true)?66:48,ink);
        clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));clock.setGravity(Gravity.CENTER);body.addView(clock,new LinearLayout.LayoutParams(-1,-2));
        clock.setOnLongClickListener(v->{go("Settings");return true;});clock.setContentDescription("Clock. Long press for settings");
        space(12);TextView date=text(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()),16,muted);date.setGravity(Gravity.CENTER);body.addView(date,new LinearLayout.LayoutParams(-1,-2));
        space(height<650?24:32);int count=0;
        for(String id:list("pins")){App a=findApp(id);if(a!=null&&!set("hidden").contains(id)){appRow(body,a,true);if(++count==5)break;}}
        space(16);homeFolders();
        if(remaining()>0){space(12);timerLabel=text(formatDuration(remaining()),15,muted);timerLabel.setGravity(Gravity.CENTER);timerLabel.setOnClickListener(v->go("Focus"));body.addView(timerLabel);}
        if(!prefs.getBoolean("gesturesSeen",false)){space(24);TextView hint=text("↑ apps     ← your day     → agenda",11,muted);hint.setGravity(Gravity.CENTER);body.addView(hint);}
    }

    private void homeShortcuts(){
        LinearLayout shortcuts=row();shortcuts.setPadding(dp(4),dp(4),dp(4),dp(10));
        shortcuts.addView(shortcut("leftShortcut",true),new LinearLayout.LayoutParams(dp(56),dp(56)));
        shortcuts.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));
        shortcuts.addView(shortcut("rightShortcut",false),new LinearLayout.LayoutParams(dp(56),dp(56)));root.addView(shortcuts);
    }
    private TextView shortcut(String key,boolean phone){
        TextView view=button("",()->{
            App selected=findApp(prefs.getString(key,""));if(selected!=null){openApp(selected);return;}
            if(prefs.contains(key)){toast("Shortcut app is no longer available. Choose another.");assignShortcut(key);return;}
            if(phone)safeStart(new Intent(Intent.ACTION_DIAL));else assignShortcut(key);
        });
        view.setGravity(Gravity.CENTER);view.setPadding(dp(16),0,dp(16),0);
        android.graphics.drawable.Drawable icon=getDrawable(phone?R.drawable.ic_phone:R.drawable.ic_calculator);icon.setTint(ink);icon.setBounds(0,0,dp(24),dp(24));view.setCompoundDrawables(icon,null,null,null);
        GradientDrawable bg=new GradientDrawable();bg.setColor(prefs.getBoolean("light",false)?0xaaffffff:0x80304150);bg.setCornerRadius(dp(28));bg.setStroke(dp(1),line);view.setBackground(bg);
        App selected=findApp(prefs.getString(key,""));view.setContentDescription((selected==null?(phone?"Phone":"Calculator"):label(selected))+". Hold to assign an app.");
        view.setOnLongClickListener(v->{assignShortcut(key);return true;});return view;
    }
    private void assignShortcut(String key){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(20),0,dp(20),0);
        EditText search=input("Find an app","");search.setSingleLine(true);panel.addView(search);
        ScrollView scroll=new ScrollView(this);LinearLayout choices=new LinearLayout(this);choices.setOrientation(LinearLayout.VERTICAL);scroll.addView(choices);panel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(300)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Assign shortcut").setView(panel).setNegativeButton("Cancel",null).create();
        Runnable filter=()->{choices.removeAllViews();for(App app:apps)if(label(app).toLowerCase(Locale.ROOT).contains(search.getText().toString().toLowerCase(Locale.ROOT)))choices.addView(button(label(app),()->{prefs.edit().putString(key,app.id).apply();dialog.dismiss();render();}));};
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){filter.run();}public void afterTextChanged(Editable e){}});filter.run();dialog.show();
    }
    private void agenda(){
        title("A day with direction.",new SimpleDateFormat("EEEE · d MMMM",Locale.getDefault()).format(new Date()));
        LinearLayout container=body;body=card(container);section("YOUR CALENDAR AGENDA");calendarCard();
        body=card(container);section("YOUR NEXT ACTIONS");entries("tasks");body.addView(button("+ Add a task",()->capture("todo: ")));
        body=card(container);section("MAKE SPACE");body.addView(button("Start a focus session  →",()->go("Focus")));
        body=container;
    }
    private void wallpaperOptions(){
        new AlertDialog.Builder(this).setTitle("Choose your atmosphere").setItems(new String[]{"Night Ridge · illustrated mountains","Choose a photo","Solid · minimal"},(d,n)->{
            if(n==1){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),WALLPAPER);}catch(ActivityNotFoundException e){toast("No photo picker available.");}}
            else{prefs.edit().putString("wallpaperMode",n==0?"ridge":"solid").apply();render();}
        }).show();
    }
    private void saveWallpaper(Uri uri){
        try{
            android.graphics.Bitmap bitmap=android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,source)->{
                int w=info.getSize().getWidth(),h=info.getSize().getHeight();float scale=Math.min(1f,2048f/Math.max(w,h));decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            if(bitmap==null)throw new IOException();
            File temporary=new File(getFilesDir(),"wallpaper.pending");try(OutputStream out=new FileOutputStream(temporary)){if(!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,out))throw new IOException();}bitmap.recycle();
            if(!temporary.renameTo(new File(getFilesDir(),"wallpaper.jpg")))throw new IOException();
            Atmosphere.invalidate();prefs.edit().putString("wallpaperMode","photo").apply();render();toast("Wallpaper updated.");
        }catch(Exception e){toast("Could not use that photo. Try a JPEG or PNG image.");}
    }

    private String expandedFolder="";
    private android.view.View indexRail;
    private boolean indexDragging;
    private void homeFolders(){
        JSONArray folders=array("folders");
        for(int i=0;i<folders.length();i++){
            JSONObject folder=folders.optJSONObject(i);if(folder==null)continue;String id=folder.optString("id");
            LinearLayout container=card(body);TextView heading=button(folder.optString("name","Folder")+(id.equals(expandedFolder)?"  −":"  +"),()->{expandedFolder=id.equals(expandedFolder)?"":id;render();});heading.setTextSize(18);heading.setOnLongClickListener(v->{folderOptions(id);return true;});container.addView(heading);
            if(!id.equals(expandedFolder))continue;
            JSONArray members=folder.optJSONArray("apps");List<App> visible=new ArrayList<>();if(members!=null)for(int j=0;j<members.length();j++){App a=findApp(members.optString(j));if(a!=null&&!set("hidden").contains(a.id))visible.add(a);}
            if(visible.isEmpty())container.addView(text("Hold the folder title to add apps.",13,muted));
            int columns=4;for(int start=0;start<visible.size();start+=columns){LinearLayout icons=row();for(int col=0;col<columns;col++){int n=start+col;if(n>=visible.size()){icons.addView(new Space(this),new LinearLayout.LayoutParams(0,dp(64),1));continue;}App app=visible.get(n);ImageButton icon=new ImageButton(this);try{icon.setImageDrawable(getPackageManager().getActivityIcon(ComponentName.unflattenFromString(app.id)));}catch(PackageManager.NameNotFoundException e){icon.setImageDrawable(getPackageManager().getDefaultActivityIcon());}android.graphics.ColorMatrix matrix=new android.graphics.ColorMatrix();matrix.setSaturation(0);icon.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);icon.setPadding(dp(13),dp(13),dp(13),dp(13));android.util.TypedValue value=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,value,true);icon.setBackgroundResource(value.resourceId);icon.setContentDescription(label(app));icon.setTooltipText(label(app));icon.setOnClickListener(v->launch(app));icon.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle(label(app)).setItems(new String[]{"Open app","Remove from this folder","App settings"},(d,which)->{if(which==0)launch(app);else if(which==2)organise(app);else{JSONArray all=array("folders");for(int z=0;z<all.length();z++){JSONObject f=all.optJSONObject(z);if(f!=null&&id.equals(f.optString("id"))){JSONArray old=f.optJSONArray("apps"),updated=new JSONArray();if(old!=null)for(int k=0;k<old.length();k++)if(!app.id.equals(old.optString(k)))updated.put(old.optString(k));try{f.put("apps",updated);}catch(JSONException ignored){}}}saveArray("folders",all);render();}}).show();return true;});icons.addView(icon,new LinearLayout.LayoutParams(0,dp(64),1));}container.addView(icons);}
        }
        TextView add=button("+ New folder",()->newFolder());add.setGravity(Gravity.CENTER);add.setTextColor(muted);body.addView(add);
    }
    private void newFolder(){edit("Folder name","",name->{if(name.isEmpty())return;JSONArray all=array("folders");JSONObject folder=new JSONObject();String id=UUID.randomUUID().toString();try{folder.put("id",id).put("name",name).put("apps",new JSONArray());}catch(JSONException ignored){}all.put(folder);saveArray("folders",all);expandedFolder=id;render();folderApps(id);});}
    private void folderOptions(String id){
        JSONArray all=array("folders");for(int i=0;i<all.length();i++){JSONObject folder=all.optJSONObject(i);if(folder==null||!id.equals(folder.optString("id")))continue;final int position=i;
            new AlertDialog.Builder(this).setTitle(folder.optString("name")).setItems(new String[]{"Choose apps","Rename folder","Move up","Move down","Delete folder"},(d,which)->{
                if(which==0){folderApps(id);return;}
                if(which==1){edit("Folder name",folder.optString("name"),name->{if(name.isEmpty())return;try{folder.put("name",name);}catch(JSONException ignored){}saveArray("folders",all);render();});return;}
                if(which==4){confirm("Delete this folder? Your apps stay installed.",()->{all.remove(position);saveArray("folders",all);render();});return;}
                int other=which==2?position-1:position+1;if(other<0||other>=all.length())return;try{Object swap=all.get(other);all.put(other,folder);all.put(position,swap);}catch(JSONException ignored){}saveArray("folders",all);render();
            }).show();break;
        }
    }
    private void folderApps(String id){
        JSONArray all=array("folders");JSONObject found=null;for(int i=0;i<all.length();i++){JSONObject f=all.optJSONObject(i);if(f!=null&&id.equals(f.optString("id")))found=f;}if(found==null)return;final JSONObject folder=found;
        Set<String> selected=new LinkedHashSet<>();JSONArray stored=folder.optJSONArray("apps");if(stored!=null)for(int i=0;i<stored.length();i++)selected.add(stored.optString(i));
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),0,dp(18),0);EditText search=input("Find apps","");search.setSingleLine(true);panel.addView(search);ScrollView scroll=new ScrollView(this);LinearLayout options=new LinearLayout(this);options.setOrientation(LinearLayout.VERTICAL);scroll.addView(options);panel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(320)));
        Runnable filter=()->{options.removeAllViews();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(App app:apps)if(label(app).toLowerCase(Locale.ROOT).contains(q)){CheckBox cb=new CheckBox(this);cb.setText(label(app));cb.setTextColor(ink);cb.setMinHeight(dp(48));cb.setChecked(selected.contains(app.id));cb.setOnCheckedChangeListener((v,on)->{if(on)selected.add(app.id);else selected.remove(app.id);});options.addView(cb);}};search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){filter.run();}public void afterTextChanged(Editable e){}});filter.run();
        new AlertDialog.Builder(this).setTitle("Apps in "+folder.optString("name")).setView(panel).setPositiveButton("Save",(d,w)->{try{folder.put("apps",new JSONArray(selected));}catch(JSONException ignored){}saveArray("folders",all);render();}).setNegativeButton("Cancel",null).show();
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
    private void appsPage(){
        int index=root.indexOfChild(mainScroll);root.removeView(mainScroll);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);root.addView(body,index,new LinearLayout.LayoutParams(-1,0,1));
        EditText search=input("Find an app","");search.setSingleLine(true);body.addView(search);
        LinearLayout shell=new LinearLayout(this);body.addView(shell,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout rail=new LinearLayout(this);rail.setOrientation(LinearLayout.VERTICAL);indexRail=rail;shell.addView(rail,new LinearLayout.LayoutParams(dp(36),-1));
        ScrollView listScroll=new ScrollView(this);listScroll.setVerticalScrollBarEnabled(false);mainScroll=listScroll;shell.addView(listScroll,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);listScroll.addView(results);Map<String,TextView> headings=new LinkedHashMap<>();Map<String,TextView> letters=new LinkedHashMap<>();String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
        java.util.function.Consumer<String> jump=letter->{TextView target=headings.get(letter);if(target==null){for(String k:headings.keySet())if(AppIndex.order(k)>=AppIndex.order(letter)){target=headings.get(k);break;}}if(target!=null){listScroll.smoothScrollTo(0,target.getTop());for(Map.Entry<String,TextView> entry:letters.entrySet())entry.getValue().setTextColor(entry.getKey().equals(letter)?ink:muted);}};
        for(int i=0;i<alphabet.length();i++){String letter=alphabet.substring(i,i+1);TextView t=text(letter,11,muted);t.setGravity(Gravity.CENTER);t.setContentDescription("Jump to "+letter);t.setFocusable(true);t.setOnClickListener(v->jump.accept(letter));rail.addView(t,new LinearLayout.LayoutParams(-1,0,1));letters.put(letter,t);}
        View.OnTouchListener drag=(v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_MOVE){int[] location=new int[2];rail.getLocationOnScreen(location);float y=e.getRawY()-location[1];int n=Math.max(0,Math.min(26,(int)(y/Math.max(1,rail.getHeight())*27)));jump.accept(alphabet.substring(n,n+1));return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP){rail.performClick();return true;}return true;};rail.setOnTouchListener(drag);for(TextView t:letters.values())t.setOnTouchListener(drag);
        Runnable filter=()->{results.removeAllViews();headings.clear();Set<String> hidden=set("hidden");String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);List<App> matches=new ArrayList<>();for(App a:apps)if(!hidden.contains(a.id)&&label(a).toLowerCase(Locale.ROOT).contains(q))matches.add(a);matches.sort((a,b)->{int group=Integer.compare(AppIndex.order(AppIndex.group(label(a))),AppIndex.order(AppIndex.group(label(b))));return group!=0?group:label(a).compareToIgnoreCase(label(b));});for(App a:matches){String group=AppIndex.group(label(a));if(!headings.containsKey(group)){TextView heading=text(group,12,muted);heading.setPadding(dp(4),dp(14),0,dp(4));headings.put(group,heading);results.addView(heading);}appRow(results,a,false);}for(Map.Entry<String,TextView> entry:letters.entrySet())entry.getValue().setAlpha(headings.containsKey(entry.getKey())?1f:.3f);if(matches.isEmpty())results.addView(text("No matching apps.",16,muted));listScroll.scrollTo(0,0);};
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){filter.run();}public void afterTextChanged(Editable e){}});filter.run();
    }
    private void appRow(LinearLayout parent,App a,boolean big) {
        String prefix="";
        TextView t=button(prefix+label(a)+(set("distract").contains(a.id)?"  ·":""),()->launch(a));t.setTextSize((big?21:19)+prefs.getInt("size",0));
        if(!big&&prefs.getBoolean("letters",false)){LetterIcon icon=new LetterIcon(label(a).substring(0,1).toUpperCase(Locale.ROOT));icon.setBounds(0,0,dp(34),dp(34));t.setCompoundDrawables(icon,null,null,null);t.setCompoundDrawablePadding(dp(14));}
        t.setTypeface(Typeface.create(big?"sans-serif":prefs.getString("weight","sans-serif-light"),Typeface.NORMAL));t.setGravity(big?Gravity.CENTER:(prefs.getBoolean("right",false)?Gravity.END:Gravity.START)|Gravity.CENTER_VERTICAL);if(big){t.setMinHeight(dp(56));t.setPadding(dp(4),dp(12),dp(4),dp(12));}
        t.setOnLongClickListener(v->{organise(a);return true;});parent.addView(t,new LinearLayout.LayoutParams(-1,-2));
    }
    private void organise(App a) {
        boolean pinned=list("pins").contains(a.id),dis=set("distract").contains(a.id);
        String[] options={pinned?"Unpin from home":"Pin to home","Move to top of home","Rename",dis?"Remove distraction pause":"Mark as distracting","Hide app","App info","Add to folder"};
        new AlertDialog.Builder(this).setTitle(label(a)).setItems(options,(d,n)->{
            if(n==0){List<String> p=list("pins");if(pinned)p.remove(a.id);else p.add(a.id);saveList("pins",p);}
            if(n==1){List<String> p=list("pins");p.remove(a.id);p.add(0,a.id);saveList("pins",p);}
            if(n==2){edit("App name",label(a),s->{prefs.edit().putString("name_"+a.id,s.isEmpty()?a.label:s).apply();loadApps();render();});return;}
            if(n==3)toggleSet("distract",a.id);
            if(n==4)toggleSet("hidden",a.id);
            if(n==6){JSONArray folders=array("folders");if(folders.length()==0){newFolder();return;}String[] names=new String[folders.length()];for(int j=0;j<names.length;j++)names[j]=folders.optJSONObject(j).optString("name");new AlertDialog.Builder(this).setTitle("Add to folder").setItems(names,(dialog,j)->{JSONObject f=folders.optJSONObject(j);JSONArray ids=f.optJSONArray("apps");if(ids==null)ids=new JSONArray();boolean exists=false;for(int k=0;k<ids.length();k++)if(a.id.equals(ids.optString(k)))exists=true;if(!exists)ids.put(a.id);try{f.put("apps",ids);}catch(JSONException ignored){}saveArray("folders",folders);render();}).show();return;}
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
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(17),dp(18),dp(17));
        boolean light=prefs.getBoolean("light",false);
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,light?new int[]{0xd9ffffff,0xa6f5f3ee}:new int[]{0xa630414f,0x99131f2b});
        background.setCornerRadius(dp(24));background.setStroke(dp(1),light?0x30506070:0x2bc9d9e5);c.setBackground(background);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);parent.addView(c,lp);return c;
    }
    private void board(){
        LinearLayout board=body;
        LinearLayout progress=card(board);progress.addView(text("PROOF OF PROGRESS",12,muted));TextView progressLink=button("Raise your standard.  ↗",()->startActivity(new Intent(this,ProgressActivity.class)));progressLink.setTextSize(25);progress.addView(progressLink);progress.addView(text("BODY / MIND / CRAFT / MONEY / RECOVERY / SPIRIT",10,muted));try{JSONObject data=new JSONObject(prefs.getString("rivalData","{} ")).optJSONObject("state");int records=data==null?0:data.optJSONArray("entries").length();JSONObject history=data==null?null:data.optJSONObject("completed");JSONArray completed=history==null?null:history.optJSONArray(day());TextView numbers=text(records+" records  /  "+(completed==null?0:completed.length())+" promises today",16,ink);numbers.setTypeface(Typeface.MONOSPACE);progress.addView(numbers);}catch(Exception ignored){}
        LinearLayout pair=row();pair.setGravity(Gravity.TOP);board.addView(pair);
        LinearLayout first=new LinearLayout(this);first.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1);half.rightMargin=dp(6);pair.addView(first,half);
        LinearLayout second=new LinearLayout(this);second.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams other=new LinearLayout.LayoutParams(0,-2,1);other.leftMargin=dp(6);pair.addView(second,other);
        body=card(first);body.setMinimumHeight(dp(172));body.setGravity(Gravity.CENTER_VERTICAL);
        clock=text(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),30,ink);body.addView(clock);
        body.addView(text(new SimpleDateFormat("EEEE\nd MMM",Locale.getDefault()).format(new Date()),14,muted));
        body=card(second);body.setMinimumHeight(dp(172));body.addView(button("tasks  +",()->go("Tasks")));JSONArray tasks=array("tasks");int shown=0;
        for(int i=0;i<tasks.length()&&shown<2;i++){JSONObject t=tasks.optJSONObject(i);if(t!=null&&!t.optBoolean("done")){TextView preview=text("○  "+t.optString("text"),15,ink);preview.setMaxLines(2);preview.setEllipsize(TextUtils.TruncateAt.END);body.addView(preview);shown++;}}
        if(shown==0)body.addView(text("A clear slate.",14,muted));
        body=card(board);habitCard();
        body=card(board);financeCard();
        body=card(board);section("FOCUS");body.addView(text(prefs.getInt("sessions_"+day(),0)+" sessions today",20,ink));body.addView(button(remaining()>0?"Continue session  →":"Begin deep work  →",()->go("Focus")));
        body=card(board);section("SCREEN TIME");usageCard();
        body=card(board);section("UPCOMING DATES");calendarCard();
        body=board;
    }
    private int habitMonthOffset=0;
    private String habitFilter="";
    private void openProgress(String view){startActivity(new Intent(this,ProgressActivity.class).putExtra("view",view));}
    private JSONObject progressState(){try{return new JSONObject(prefs.getString("rivalData","{}")).optJSONObject("state");}catch(JSONException e){return null;}}
    private boolean habitDue(JSONObject habit,String date){
        if(habit==null||habit.optString("created","9999").compareTo(date)>0||(!habit.optString("archived").isEmpty()&&habit.optString("archived").compareTo(date)<=0))return false;
        int weekday=java.time.LocalDate.parse(date).getDayOfWeek().getValue()%7;JSONArray days=habit.optJSONArray("days");if(days!=null)for(int i=0;i<days.length();i++)if(days.optInt(i,-1)==weekday)return true;return false;
    }
    private boolean habitDone(JSONObject state,String date,String id){JSONObject completed=state.optJSONObject("completed");JSONArray ids=completed==null?null:completed.optJSONArray(date);if(ids!=null)for(int i=0;i<ids.length();i++)if(id.equals(ids.optString(i)))return true;return false;}
    private void habitCard(){
        section("HABIT CALENDAR");JSONObject state=progressState();JSONArray habits=state==null?null:state.optJSONArray("habits");
        if(habits==null){body.addView(text("Your Proof of Progress commitments appear here after you open them once.",14,muted));body.addView(button("Open habits  →",()->openProgress("today")));return;}
        String label="All scheduled habits";for(int i=0;i<habits.length();i++){JSONObject h=habits.optJSONObject(i);if(h!=null&&habitFilter.equals(h.optString("id")))label=h.optString("name");}
        body.addView(button(label+"  ▾",()->{String[] names=new String[habits.length()+1],ids=new String[habits.length()+1];names[0]="All scheduled habits";ids[0]="";for(int i=0;i<habits.length();i++){JSONObject h=habits.optJSONObject(i);names[i+1]=h==null?"Unknown":h.optString("name");ids[i+1]=h==null?"":h.optString("id");}new AlertDialog.Builder(this).setTitle("Track a habit").setItems(names,(d,n)->{habitFilter=ids[n];render();}).show();}));
        java.time.LocalDate today=java.time.LocalDate.now(),month=today.withDayOfMonth(1).plusMonths(habitMonthOffset);
        LinearLayout controls=row();TextView previous=button("‹",()->{habitMonthOffset--;render();});previous.setContentDescription("Previous month");controls.addView(previous,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView title=text(month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy",Locale.getDefault())),17,ink);title.setGravity(Gravity.CENTER);controls.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView next=button("›",()->{if(habitMonthOffset<0){habitMonthOffset++;render();}});next.setEnabled(habitMonthOffset<0);next.setAlpha(habitMonthOffset<0?1:.3f);next.setContentDescription("Next month");controls.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));body.addView(controls);
        LinearLayout weekdays=row();for(String name:new String[]{"M","T","W","T","F","S","S"}){TextView t=text(name,10,muted);t.setGravity(Gravity.CENTER);weekdays.addView(t,new LinearLayout.LayoutParams(0,dp(24),1));}body.addView(weekdays);
        int offset=month.getDayOfWeek().getValue()-1,cells=((offset+month.lengthOfMonth()+6)/7)*7;
        for(int start=0;start<cells;start+=7){LinearLayout week=row();for(int col=0;col<7;col++){int number=start+col-offset+1;if(number<1||number>month.lengthOfMonth()){week.addView(new Space(this),new LinearLayout.LayoutParams(0,dp(42),1));continue;}String date=month.withDayOfMonth(number).toString();int total=0,done=0;for(int i=0;i<habits.length();i++){JSONObject h=habits.optJSONObject(i);if(h!=null&&(habitFilter.isEmpty()||habitFilter.equals(h.optString("id")))&&habitDue(h,date)){total++;if(habitDone(state,date,h.optString("id")))done++;}}
            boolean future=date.compareTo(today.toString())>0;TextView dot=button(number+"\n"+(done==0?"○":"●"),()->habitDay(date));dot.setPadding(0,0,0,0);dot.setMinHeight(dp(40));dot.setGravity(Gravity.CENTER);dot.setTextSize(12);dot.setTextColor(future?muted:done>0?ink:muted);dot.setAlpha(future?.25f:total==0?.4f:done==0?.65f:1f);dot.setContentDescription(date+": "+done+" of "+total+" scheduled habits complete");dot.setEnabled(!future);GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(18));bg.setColor(done>0?(done==total?0x508899aa:0x208899aa):Color.TRANSPARENT);if(date.equals(today.toString()))bg.setStroke(dp(1),muted);dot.setBackground(bg);week.addView(dot,new LinearLayout.LayoutParams(0,dp(42),1));}
            body.addView(week);
        }
        body.addView(text("Tap a date to check off habits. ○ none · ● some/all; brighter means more completed.",11,muted));body.addView(button("Manage habits  →",()->openProgress("today")));
    }
    private void habitDay(String date){
        JSONObject state=progressState();if(state==null)return;JSONArray habits=state.optJSONArray("habits");if(habits==null)return;List<String> names=new ArrayList<>(),ids=new ArrayList<>();for(int i=0;i<habits.length();i++){JSONObject h=habits.optJSONObject(i);if(h!=null&&(habitFilter.isEmpty()||habitFilter.equals(h.optString("id")))&&habitDue(h,date)){names.add(h.optString("name"));ids.add(h.optString("id"));}}
        if(ids.isEmpty()){toast("No habits scheduled for this date.");return;}boolean[] selected=new boolean[ids.size()];for(int i=0;i<selected.length;i++)selected[i]=habitDone(state,date,ids.get(i));
        new AlertDialog.Builder(this).setTitle(date).setMultiChoiceItems(names.toArray(new String[0]),selected,(d,n,on)->selected[n]=on).setPositiveButton("Save",(d,w)->{
            try{JSONObject wrapper=new JSONObject(prefs.getString("rivalData","{}")),latest=wrapper.getJSONObject("state"),completed=latest.optJSONObject("completed");if(completed==null)completed=new JSONObject();JSONArray prior=completed.optJSONArray(date);Set<String> values=new LinkedHashSet<>();if(prior!=null)for(int i=0;i<prior.length();i++)values.add(prior.optString(i));for(int i=0;i<ids.size();i++){if(selected[i])values.add(ids.get(i));else values.remove(ids.get(i));}completed.put(date,new JSONArray(values));latest.put("completed",completed);prefs.edit().putString("rivalData",wrapper.toString()).commit();render();}catch(JSONException e){toast("Could not update habits.");}
        }).setNegativeButton("Cancel",null).show();
    }
    private void financeCard(){
        section("MONEY AT A GLANCE");body.addView(text("Accounts, wallets & cash",22,ink));space(8);
        LinearLayout actions=row();for(String kind:new String[]{"income","expense","accounts"}){TextView action=button(kind.equals("income")?"+ Income":kind.equals("expense")?"− Expense":"Accounts",()->openProgress(kind));action.setTextSize(14);action.setGravity(Gravity.CENTER);actions.addView(action,new LinearLayout.LayoutParams(0,dp(52),1));}body.addView(actions);
        body.addView(text("Log once. Track where it went, including fees.",12,muted));
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
        title("Daily rituals","Resets at midnight");
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
        title("One thing.","Deep work, deliberate rest");
        body.addView(text("Distracting apps pause inside this launcher during a session. Long-press apps to mark them. Notifications and other launch routes remain available.",16,muted));space(24);
        if(remaining()>0){timerLabel=text(formatDuration(remaining()),64,ink);body.addView(timerLabel);body.addView(text(prefs.getString("focusTask",""),22,ink));body.addView(button("End session",()->confirm("End this focus session early?",()->{prefs.edit().putLong("focusEnd",0).apply();render();})));}
        else {EditText task=input("What will you finish?",prefs.getString("focusTask",""));body.addView(task);for(int minutes:new int[]{25,50,90})body.addView(button("Start "+minutes+" minutes  →",()->{String s=task.getText().toString().trim();if(s.isEmpty()){toast("Choose one task first.");return;}prefs.edit().putString("focusTask",s).putLong("focusEnd",System.currentTimeMillis()+minutes*60000L).putLong("focusElapsedEnd",SystemClock.elapsedRealtime()+minutes*60000L).putInt("focusBoot",Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,0)).apply();hideKeyboard();render();}));}
        space(20);body.addView(text(prefs.getInt("sessions_"+day(),0)+" completed sessions today",15,muted));body.addView(button("Open Do Not Disturb settings",()->safeStart(new Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS))));body.addView(button("Choose distracting apps",()->go("Apps")));body.addView(text("The timer continues when you leave or lock the phone. Completion is recorded when KING next resumes; it does not sound a background alarm.",13,muted));
    }
    private long remaining(){if(prefs.getLong("focusEnd",0)==0)return 0;int boot=Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,0);long ms=boot==prefs.getInt("focusBoot",-1)?prefs.getLong("focusElapsedEnd",0)-SystemClock.elapsedRealtime():prefs.getLong("focusEnd",0)-System.currentTimeMillis();return Math.max(0,ms);}
    private String formatDuration(long ms){long s=(ms+999)/1000;return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private static class AgendaEvent {
        long id,begin,end;boolean allDay;String title,calendar;
        AgendaEvent(Cursor c){id=c.getLong(0);title=c.isNull(1)?"Untitled event":c.getString(1);begin=c.getLong(2);end=c.getLong(3);allDay=c.getInt(4)==1;calendar=c.isNull(6)?"Calendar":c.getString(6);}
    }
    private int agendaDays(){String value=prefs.getString("agendaDays","3");return "1".equals(value)?1:"7".equals(value)?7:"14".equals(value)?14:3;}
    private void calendarCard(){
        if(checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){body.addView(button("Show my calendar agenda",()->requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},10)));return;}
        int days=agendaDays();
        body.addView(button(days==1?"Today  ▾":"Today + "+(days-1)+" days  ▾",()->new AlertDialog.Builder(this).setTitle("Agenda range").setItems(new String[]{"Today","Today and the next two days","Next seven days","Next fourteen days"},(d,n)->{prefs.edit().putString("agendaDays",new String[]{"1","3","7","14"}[n]).apply();render();}).show()));
        LinearLayout events=new LinearLayout(this);events.setOrientation(LinearLayout.VERTICAL);body.addView(events);events.addView(text("Reading calendars on this phone…",14,muted));
        body.addView(button("Calendar sources & sync help",()->calendarSources()));body.addView(button("Refresh agenda",()->render()));
        body.addView(button("Open calendar",()->safeStart(new Intent(Intent.ACTION_VIEW,Uri.parse("content://com.android.calendar/time/"+System.currentTimeMillis())))));
        final LinearLayout renderedRoot=root;final Set<String> excluded=set("excludedCalendars");
        new Thread(()->{
            java.time.ZoneId zone=java.time.ZoneId.systemDefault();java.time.LocalDate today=java.time.LocalDate.now(zone);
            long start=today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),end=today.plusDays(days+1).atStartOfDay(zone).toInstant().toEpochMilli();
            Uri.Builder uri=CalendarContract.Instances.CONTENT_URI.buildUpon();ContentUris.appendId(uri,start);ContentUris.appendId(uri,end);
            List<AgendaEvent> results=new ArrayList<>();String error=null;boolean limited=false;
            String[] columns={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END,CalendarContract.Instances.ALL_DAY,CalendarContract.Instances.CALENDAR_ID,CalendarContract.Instances.CALENDAR_DISPLAY_NAME,CalendarContract.Instances.STATUS};
            // All local calendar sources, without ownership, organizer or visibility restrictions.
            try(Cursor c=getContentResolver().query(uri.build(),columns,null,null,CalendarContract.Instances.BEGIN+" ASC")){
                if(c==null)throw new IllegalStateException();
                while(c.moveToNext()){
                    if(excluded.contains(c.getString(5))||c.getInt(7)==CalendarContract.Events.STATUS_CANCELED)continue;
                    AgendaEvent event=new AgendaEvent(c);if(CalendarAgenda.includes(event.begin,event.end,event.allDay,today,days,zone))results.add(event);
                    if(results.size()>=500){limited=true;break;}
                }
            }catch(RuntimeException e){error="Could not read Android calendar data. Check calendar permission and use Calendar sources & sync help.";}
            results.sort((x,y)->{int order=Long.compare(CalendarAgenda.order(x.begin,x.allDay,today,zone),CalendarAgenda.order(y.begin,y.allDay,today,zone));return order!=0?order:Boolean.compare(y.allDay,x.allDay);});
            final String failure=error;final boolean truncated=limited;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed()||root!=renderedRoot)return;events.removeAllViews();
                if(failure!=null){events.addView(text(failure,14,muted));return;}
                if(results.isEmpty())events.addView(text("No events found in this range from your selected device calendars. Missing school dates? Check Calendar sources & sync help below.",14,muted));
                String previous="";int count=0;
                for(AgendaEvent event:results){
                    if(count++>=30)break;
                    java.time.LocalDate first=CalendarAgenda.date(event.begin,event.allDay,zone),group=first.isBefore(today)?today:first;
                    String heading=group.equals(today)?"TODAY":group.equals(today.plusDays(1))?"TOMORROW":group.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM",Locale.getDefault())).toUpperCase(Locale.getDefault());
                    if(!heading.equals(previous)){TextView label=text(heading,11,muted);label.setPadding(0,dp(18),0,dp(6));events.addView(label);previous=heading;}
                    String when;
                    if(event.allDay){when="All day";java.time.LocalDate last=CalendarAgenda.date(event.end,true,zone).minusDays(1);if(last.isAfter(first))when+=" · through "+last.format(java.time.format.DateTimeFormatter.ofPattern("d MMM",Locale.getDefault()));}
                    else{java.text.DateFormat time=new SimpleDateFormat(prefs.getBoolean("clock24",true)?"HH:mm":"h:mm a",Locale.getDefault());when=(first.isBefore(today)?"Started "+first.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM",Locale.getDefault()))+" ":"")+time.format(new Date(event.begin));if(event.end>event.begin)when+=" – "+(CalendarAgenda.date(event.end,false,zone).equals(first)?"":new SimpleDateFormat("EEE d MMM ",Locale.getDefault()).format(new Date(event.end)))+time.format(new Date(event.end));}
                    TextView link=button(event.title+"\n"+when+" · "+event.calendar,()->safeStart(new Intent(Intent.ACTION_VIEW,ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,event.id)).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,event.begin).putExtra(CalendarContract.EXTRA_EVENT_END_TIME,event.end)));link.setTextSize(16);events.addView(link);
                }
                if(results.size()>30||truncated)events.addView(text("Showing the first 30 events. Narrow the range or choose fewer calendars to see later items.",13,muted));
            });
        },"king-calendar-read").start();
    }
    private void calendarSources(){
        List<String> ids=new ArrayList<>(),labels=new ArrayList<>();Set<String> excluded=set("excludedCalendars");
        String[] projection={CalendarContract.Calendars._ID,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,CalendarContract.Calendars.ACCOUNT_NAME,CalendarContract.Calendars.SYNC_EVENTS,CalendarContract.Calendars.VISIBLE};
        try(Cursor c=getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,projection,null,null,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME+" ASC")){
            if(c==null)throw new IllegalStateException();
            while(c.moveToNext()){ids.add(c.getString(0));labels.add(c.getString(1)+"\n"+c.getString(2)+" · "+(c.getInt(3)==1?"Device sync on":"Device sync off")+(c.getInt(4)==0?" · hidden in calendar app":""));}
        }catch(RuntimeException e){toast("Calendar sources could not be read. Check KING's calendar permission.");return;}
        if(ids.isEmpty()){calendarSyncHelp();return;}
        boolean[] checked=new boolean[ids.size()];for(int i=0;i<checked.length;i++)checked[i]=!excluded.contains(ids.get(i));
        new AlertDialog.Builder(this).setTitle("Calendars shown in KING").setMultiChoiceItems(labels.toArray(new String[0]),checked,(dialog,which,on)->{if(on)excluded.remove(ids.get(which));else excluded.add(ids.get(which));})
            .setPositiveButton("Save",(d,w)->{saveList("excludedCalendars",excluded);render();}).setNeutralButton("Missing school calendar?",(d,w)->calendarSyncHelp()).setNegativeButton("Cancel",null).show();
    }
    private void calendarSyncHelp(){
        new AlertDialog.Builder(this).setTitle("Bring school dates into KING")
            .setMessage("KING reads calendars stored by Android, including shared and subscribed calendars. It cannot fetch events that Google Calendar has not exposed to Android.\n\n1. Open Google Calendar → Settings → your school calendar (try Show more). Enable Sync if offered and ensure the calendar is selected.\n2. In Android Settings → Accounts → your Google account → Account sync, enable Calendar.\n3. In Google Calendar, use Menu → Refresh. Then return to KING and tap Refresh agenda.\n\nCalendar sources lists what KING can see and the device sync flag. Selecting a source here only changes KING's display; it does not turn on Google sync. If the school calendar is absent, or still has missing events after syncing, its provider may require another sync method.")
            .setPositiveButton("Open calendar",(d,w)->safeStart(new Intent(Intent.ACTION_VIEW,Uri.parse("content://com.android.calendar/time/"+System.currentTimeMillis()))))
            .setNeutralButton("Account sync settings",(d,w)->safeStart(new Intent(Settings.ACTION_SYNC_SETTINGS))).setNegativeButton("Close",null).show();
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
        title("Your space.","Appearance, shortcuts and updates");
        body.addView(button("Notes archive",()->go("Notes")));
        section("UPDATES");body.addView(button("Check for updates",()->updates.check(this,true)));toggle("Check GitHub automatically","autoUpdates",true);
        body.addView(text("Checks at most once every six hours when you return to KING. Downloads open in your browser. Android confirms installation.",13,muted));
        section("ATMOSPHERE");body.addView(button("Wallpaper & appearance",()->wallpaperOptions()));
        body.addView(button("Create home folder",()->newFolder()));
        body.addView(button("Assign left shortcut",()->assignShortcut("leftShortcut")));body.addView(button("Assign right shortcut",()->assignShortcut("rightShortcut")));
        body.addView(button("Archived daily rituals",()->go("Ritual")));
        body.addView(button("Navigation & gestures",()->new AlertDialog.Builder(this).setTitle("Move quietly").setMessage("From Home: swipe up for apps, left for your day, right for agenda.\n\nFrom your day or agenda: swipe horizontally back toward Home. Swipe right from routines, tasks, notes or settings to go back. In Apps, swipe down at the top to return Home.\n\nTap the clock and hold for settings. Android Back and the small back arrow also work.").setPositiveButton("Got it",null).show()));
        body.addView(button("Open apps",()->go("Apps")));body.addView(button("Open dashboard",()->go("Board")));body.addView(button("Open focus",()->go("Focus")));
        body.addView(button("Phone",()->safeStart(new Intent(Intent.ACTION_DIAL))));
        body.addView(button("Set KING as default Home app",()->{if(Build.VERSION.SDK_INT>=29){RoleManager r=getSystemService(RoleManager.class);if(r!=null&&r.isRoleAvailable(RoleManager.ROLE_HOME)&&!r.isRoleHeld(RoleManager.ROLE_HOME)){startActivityForResult(r.createRequestRoleIntent(RoleManager.ROLE_HOME),9);return;}}safeStart(new Intent(Settings.ACTION_HOME_SETTINGS));}));
        toggle("Milk white theme","light",false);toggle("Hide status bar","fullscreen",true);toggle("Soft haptic feedback","haptics",true);toggle("Drawer letter icons","letters",false);toggle("Right aligned drawer","right",false);toggle("24-hour clock","clock24",true);
        body.addView(button("Drawer font weight",()->new AlertDialog.Builder(this).setTitle("Typography").setItems(new String[]{"Light","Regular","Bold"},(d,n)->{prefs.edit().putString("weight",new String[]{"sans-serif-light","sans-serif","sans-serif-black"}[n]).apply();render();}).show()));
        body.addView(button("Letter icon stroke",()->new AlertDialog.Builder(this).setTitle("Icon stroke").setItems(new String[]{"Fine","Standard","Strong"},(d,n)->{prefs.edit().putInt("stroke",n+1).apply();render();}).show()));
        body.addView(button("App text size",()->new AlertDialog.Builder(this).setTitle("Text size").setItems(new String[]{"Compact","Standard","Large"},(d,n)->{prefs.edit().putInt("size",new int[]{-3,0,4}[n]).apply();render();}).show()));
        body.addView(button("Hidden apps ("+set("hidden").size()+")",()->{List<App> h=new ArrayList<>();for(App a:apps)if(set("hidden").contains(a.id))h.add(a);String[] names=new String[h.size()];for(int i=0;i<h.size();i++)names[i]="Show "+label(h.get(i));new AlertDialog.Builder(this).setTitle("Restore hidden apps").setItems(names,(d,n)->{toggleSet("hidden",h.get(n).id);render();}).setNegativeButton("Close",null).show();}));
        section("YOUR DATA");body.addView(button("Export backup",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"king-focus-backup.json");try{startActivityForResult(i,EXPORT);}catch(ActivityNotFoundException e){toast("No file picker available.");}}));
        body.addView(button("Restore backup",()->confirm("Replace KING's current settings, tasks, notes and routine history with a backup?",()->{try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),IMPORT);}catch(ActivityNotFoundException e){toast("No file picker available.");}})));
        body.addView(button("Android app permissions",()->safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))));
        body.addView(button("Switch back to another launcher",()->safeStart(new Intent(Settings.ACTION_HOME_SETTINGS))));space(12);
        body.addView(text("KING Focus 1.4.1 • Independent, original launcher.\nNo ads. No analytics. Network access is used only to check GitHub updates.\nBackups are unencrypted: keep them private. Voice input is handled by your chosen speech provider. Daily rituals are a checklist, not alarm notifications.",13,muted));
    }
    @Override public void onRequestPermissionsResult(int req,String[] p,int[] r){super.onRequestPermissionsResult(req,p,r);render();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        if(request==WALLPAPER&&data.getData()!=null){saveWallpaper(data.getData());return;}
        if(request==VOICE){ArrayList<String> s=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(s!=null&&!s.isEmpty())capture(s.get(0));}
        if(request==EXPORT&&data.getData()!=null){try{JSONObject backup=new JSONObject();backup.put("format","king-focus-v1");JSONObject values=new JSONObject();for(Map.Entry<String,?> e:prefs.getAll().entrySet())values.put(e.getKey(),e.getValue());backup.put("values",values);try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();out.write(backup.toString(2).getBytes(StandardCharsets.UTF_8));}toast("Backup saved.");}catch(Exception e){toast("Could not export backup.");}}
        if(request==IMPORT&&data.getData()!=null){try(InputStream in=getContentResolver().openInputStream(data.getData())){if(in==null)throw new IOException();ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>2*1024*1024)throw new IOException();bytes.write(buffer,0,n);}JSONObject b=new JSONObject(bytes.toString("UTF-8"));if(!"king-focus-v1".equals(b.optString("format")))throw new IOException();JSONObject values=b.getJSONObject("values");validateBackup(values);SharedPreferences.Editor ed=prefs.edit().clear();Iterator<String> keys=values.keys();while(keys.hasNext()){String k=keys.next();Object v=values.get(k);if(v instanceof Boolean)ed.putBoolean(k,(Boolean)v);else if(v instanceof Number){if(k.equals("size")||k.equals("stroke")||k.equals("focusBoot")||k.startsWith("sessions_"))ed.putInt(k,((Number)v).intValue());else ed.putLong(k,((Number)v).longValue());}else if(v instanceof String)ed.putString(k,(String)v);}ed.putLong("focusEnd",0).commit();loadApps();render();toast("Backup restored. Active timer cleared.");}catch(Exception e){toast("Invalid backup. Your existing data was kept.");}}
    }
    private void validateBackup(JSONObject v)throws Exception {Iterator<String> it=v.keys();while(it.hasNext()){String k=it.next();Object o=v.get(k);boolean bool=Arrays.asList("light","fullscreen","haptics","letters","right","clock24","designV2","gesturesSeen","autoUpdates").contains(k)||k.startsWith("done_");boolean number=Arrays.asList("size","stroke","focusBoot","focusEnd","focusElapsedEnd","updateChecked","updateSnoozed").contains(k)||k.startsWith("sessions_");if(bool&&!(o instanceof Boolean)||number&&!(o instanceof Number)||!bool&&!number&&!(o instanceof String))throw new IOException();}
        if(v.has("folders")){JSONArray folders=new JSONArray(v.getString("folders"));Set<String> ids=new HashSet<>();for(int i=0;i<folders.length();i++){JSONObject f=folders.getJSONObject(i);String id=f.getString("id");if(id.isEmpty()||!ids.add(id)||f.getString("name").trim().isEmpty())throw new IOException();JSONArray members=f.getJSONArray("apps");for(int j=0;j<members.length();j++)if(!(members.get(j) instanceof String))throw new IOException();}}
        for(String key:new String[]{"tasks","notes","routines","pins","hidden","distract"})if(v.has(key)){JSONArray a=new JSONArray(v.getString(key));for(int i=0;i<a.length();i++){if(Arrays.asList("pins","hidden","distract").contains(key)){if(!(a.get(i) instanceof String))throw new IOException();}else{JSONObject o=a.getJSONObject(i);if(!o.has("id")||!o.has("text"))throw new IOException();if(key.equals("routines")&&!o.optString("time").matches("([01]\\d|2[0-3]):[0-5]\\d"))throw new IOException();}}}
    }
    private class LetterIcon extends android.graphics.drawable.Drawable {
        private final String letter; private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        LetterIcon(String s){letter=s;}
        public void draw(android.graphics.Canvas canvas){android.graphics.Rect b=getBounds();paint.setColor(ink);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(prefs.getInt("stroke",1)));canvas.drawRect(b.left+dp(2),b.top+dp(2),b.right-dp(2),b.bottom-dp(2),paint);paint.setStyle(android.graphics.Paint.Style.FILL);paint.setTextSize(dp(18));paint.setTypeface(Typeface.MONOSPACE);paint.setTextAlign(android.graphics.Paint.Align.CENTER);canvas.drawText(letter,b.exactCenterX(),b.exactCenterY()-(paint.ascent()+paint.descent())/2,paint);}
        public void setAlpha(int a){paint.setAlpha(a);} public void setColorFilter(android.graphics.ColorFilter f){paint.setColorFilter(f);} public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
    private void toggle(String label,String key,boolean fallback){Switch s=new Switch(this);s.setText(label);s.setTextSize(16);s.setTextColor(ink);s.setPadding(0,dp(10),0,dp(10));s.setMinHeight(dp(52));s.setChecked(prefs.getBoolean(key,fallback));s.setOnCheckedChangeListener((b,c)->{prefs.edit().putBoolean(key,c).apply();render();});body.addView(s);}
    private void go(String p){hideKeyboard();page=p;render();body.setAlpha(0.35f);body.animate().alpha(1f).setDuration(150).start();}
    private void hideKeyboard(){View f=getCurrentFocus();if(f!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(f.getWindowToken(),0);}
    private void safeStart(Intent i){try{startActivity(i);}catch(RuntimeException e){toast("This action isn't available on your device.");}}
    private void title(String h,String sub){body.addView(text(h,28,ink));space(8);body.addView(text(sub,11,muted));space(22);}
    private void section(String s){space(8);body.addView(text(s,11,muted));space(8);}
    private void space(int h){Space s=new Space(this);body.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);if(page.equals("Home")&&!prefs.getBoolean("light",false))t.setShadowLayer(dp(6),0,dp(1),0xb0000000);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setLineSpacing(dp(3),1);return t;}
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
