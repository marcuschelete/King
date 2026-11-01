package com.artist.king;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.*;
import java.io.File;

/** Original local alpine artwork, with a softly blurred backdrop on secondary pages. */
final class Atmosphere {
    private static Bitmap photo;
    private static long photoStamp=-1;
    static void invalidate(){photo=null;photoStamp=-1;}
    static Drawable background(Context context,boolean soft){
        SharedPreferences prefs=context.getSharedPreferences("king",0);
        boolean light=prefs.getBoolean("light",false);
        if("solid".equals(prefs.getString("wallpaperMode","ridge")))return new ColorDrawable(light?0xffeeeae3:0xff0d1118);
        File file=new File(context.getFilesDir(),"wallpaper.jpg");
        if("photo".equals(prefs.getString("wallpaperMode","ridge"))&&file.isFile()){
            if(photo==null||photoStamp!=file.lastModified()){photo=BitmapFactory.decodeFile(file.getPath());photoStamp=file.lastModified();}
            if(photo!=null)return new Scene(photo,soft,light);
        }
        return new Scene(null,soft,light);
    }
    private static class Scene extends Drawable {
        final Bitmap source;final boolean soft,light;final Paint paint=new Paint(3);Bitmap rendered;int width,height;
        Scene(Bitmap source,boolean soft,boolean light){this.source=source;this.soft=soft;this.light=light;}
        @Override public void draw(Canvas canvas){
            Rect bounds=getBounds();if(bounds.width()==0||bounds.height()==0)return;
            if(rendered==null||width!=bounds.width()||height!=bounds.height()){
                width=bounds.width();height=bounds.height();int w=soft?70:420,h=Math.max(1,w*height/width);
                rendered=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(rendered);
                if(source!=null){float scale=Math.max(w/(float)source.getWidth(),h/(float)source.getHeight());Matrix m=new Matrix();m.setScale(scale,scale);m.postTranslate((w-source.getWidth()*scale)/2,(h-source.getHeight()*scale)/2);c.drawBitmap(source,m,paint);}
                else{
                    paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xff060d19,0xff334654,0xff12272e},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);
                    paint.setColor(0xffb4beb9);c.drawCircle(w*.77f,h*.22f,w*.048f,paint);
                    ridge(c,w,h,new float[]{0,.61f,.15f,.51f,.29f,.57f,.52f,.35f,.75f,.56f,.87f,.47f,1,.56f},0xff536570);
                    ridge(c,w,h,new float[]{0,.72f,.2f,.55f,.34f,.64f,.52f,.46f,.7f,.65f,.84f,.59f,1,.7f},0xff293e4a);
                    ridge(c,w,h,new float[]{0,.79f,.22f,.68f,.43f,.77f,.68f,.59f,.86f,.71f,1,.67f},0xff142933);
                    ridge(c,w,h,new float[]{0,.88f,.32f,.78f,.54f,.85f,.81f,.73f,1,.81f},0xff091921);
                }
                if(soft)blur(rendered,w,h);
                paint.setColor(light?0xcceeeae3:soft?0x78050b12:0x88050b12);c.drawRect(0,0,w,h,paint);
            }
            paint.setColor(Color.WHITE);canvas.drawBitmap(rendered,null,bounds,paint);
        }
        void blur(Bitmap bitmap,int w,int h){
            int[] pixels=new int[w*h],out=new int[w*h];bitmap.getPixels(pixels,0,w,0,0,w,h);
            for(int pass=0;pass<3;pass++){
                for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                    int r=0,g=0,b=0,n=0;
                    for(int yy=Math.max(0,y-2);yy<=Math.min(h-1,y+2);yy++)for(int xx=Math.max(0,x-2);xx<=Math.min(w-1,x+2);xx++){int color=pixels[yy*w+xx];r+=(color>>16)&255;g+=(color>>8)&255;b+=color&255;n++;}
                    out[y*w+x]=0xff000000|((r/n)<<16)|((g/n)<<8)|(b/n);
                }
                int[] swap=pixels;pixels=out;out=swap;
            }
            bitmap.setPixels(pixels,0,w,0,0,w,h);
        }
        void ridge(Canvas c,int w,int h,float[] points,int color){Path path=new Path();path.moveTo(0,h);for(int i=0;i<points.length;i+=2)path.lineTo(points[i]*w,points[i+1]*h);path.lineTo(w,h);path.close();paint.setColor(color);c.drawPath(path,paint);}
        public void setAlpha(int alpha){}public void setColorFilter(ColorFilter filter){}public int getOpacity(){return PixelFormat.OPAQUE;}
    }
}
