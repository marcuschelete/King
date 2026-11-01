package com.artist.king;
import java.net.URI;
final class UpdatePolicy {
    static boolean allowed(String value){
        try{URI u=new URI(value);return "https".equals(u.getScheme())&&"github.com".equals(u.getHost())&&u.getUserInfo()==null&&u.getPort()==-1&&u.getQuery()==null&&u.getFragment()==null&&u.getPath().matches("/marcuschelete/King/raw/refs/heads/main/downloads/KING-Focus-[0-9]+\\.[0-9]+\\.[0-9]+\\.apk");}catch(Exception e){return false;}
    }
    static boolean due(long now,long last){return last<=0||now<last||now-last>=6*60*60*1000L;}
}
