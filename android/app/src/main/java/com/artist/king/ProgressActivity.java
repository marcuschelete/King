package com.artist.king;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Offline RIVAL dashboard. Only packaged assets can run with the narrow storage bridge. */
public class ProgressActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingExport;
    private static final int OPEN=40, SAVE=41;
    private static final String ORIGIN="https://king.local/";
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff000000);getWindow().setNavigationBarColor(0xff000000);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff000000);
        Button back=new Button(this);back.setText("‹  KING Focus                     PROGRESS / 1.2");back.setTextColor(0xffffffff);back.setBackgroundColor(0xff111111);back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(-1,48));
        web=new WebView(this);web.setBackgroundColor(0xff000000);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportMultipleWindows(false);
        web.addJavascriptInterface(new StorageBridge(),"KingProgress");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
                Uri uri=r.getUrl();String path=uri.getPath();
                if(!"https".equals(uri.getScheme())||!"king.local".equals(uri.getHost())||path==null||path.contains(".."))return denied();
                try{String asset=path.equals("/")?"index.html":path.substring(1);String mime=asset.endsWith(".js")?"text/javascript":asset.endsWith(".css")?"text/css":asset.endsWith(".html")?"text/html":asset.endsWith(".svg")?"image/svg+xml":"application/octet-stream";return new WebResourceResponse(mime,"UTF-8",getAssets().open("progress/"+asset));}catch(IOException e){return denied();}
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onJsConfirm(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(ProgressActivity.this).setMessage(message).setPositiveButton("Continue",(d,w)->result.confirm()).setNegativeButton("Cancel",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
            @Override public boolean onJsPrompt(WebView v,String url,String message,String value,JsPromptResult result){EditText input=new EditText(ProgressActivity.this);input.setText(value);new AlertDialog.Builder(ProgressActivity.this).setMessage(message).setView(input).setPositiveButton("Save",(d,w)->result.confirm(input.getText().toString())).setNegativeButton("Cancel",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params){if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;try{startActivityForResult(params.createIntent(),OPEN);}catch(ActivityNotFoundException e){fileCallback.onReceiveValue(null);fileCallback=null;toast("No file picker available.");}return true;}
        });
        web.loadUrl(ORIGIN);
    }
    private WebResourceResponse denied(){return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private final class StorageBridge {
        @JavascriptInterface public String getItem(){return getSharedPreferences("king",MODE_PRIVATE).getString("rivalData",null);}
        @JavascriptInterface public void setItem(String value){if(value!=null&&value.length()<=2*1024*1024)getSharedPreferences("king",MODE_PRIVATE).edit().putString("rivalData",value).commit();}
        @JavascriptInterface public void exportFile(String name,String mime,String content){
            if(content==null||content.length()>2*1024*1024)return;
            runOnUiThread(()->{if(pendingExport!=null){toast("Finish the current export first.");return;}pendingExport=content;Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/csv".equals(mime)?"text/csv":"application/json").putExtra(Intent.EXTRA_TITLE,"text/csv".equals(mime)?"king-progress.csv":"king-progress-backup.json");try{startActivityForResult(intent,SAVE);}catch(ActivityNotFoundException e){pendingExport=null;toast("No file picker available.");}});
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==OPEN&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}
        if(request==SAVE){String content=pendingExport;pendingExport=null;if(result==RESULT_OK&&data!=null&&data.getData()!=null&&content!=null){try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){if(stream==null)throw new IOException();stream.write(content.getBytes(StandardCharsets.UTF_8));toast("Progress exported.");}catch(IOException e){toast("Could not export progress.");}}}
    }
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);web.removeJavascriptInterface("KingProgress");web.destroy();super.onDestroy();}
}
