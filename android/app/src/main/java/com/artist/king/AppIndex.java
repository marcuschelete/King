package com.artist.king;
import java.text.Normalizer;
import java.util.Locale;
final class AppIndex {
    static String group(String name){String normalized=Normalizer.normalize(name==null?"":name.trim(),Normalizer.Form.NFD).replaceAll("\\p{M}+","").toUpperCase(Locale.ROOT);if(normalized.isEmpty())return "#";char first=normalized.charAt(0);return first>='A'&&first<='Z'?String.valueOf(first):"#";}
    static int order(String group){return "#".equals(group)?26:group.charAt(0)-'A';}
}
