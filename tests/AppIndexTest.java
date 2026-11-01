package com.artist.king;
public class AppIndexTest {
    public static void main(String[] args){String[][] cases={{"WhatsApp","W"},{"  absa","A"},{"École","E"},{"123 Games","#"},{"银行","#"},{"","#"},{"Zoom","Z"}};for(String[] pair:cases)if(!pair[1].equals(AppIndex.group(pair[0])))throw new AssertionError(pair[0]);if(AppIndex.order("#")<=AppIndex.order("Z"))throw new AssertionError();System.out.println("8 alphabet index checks passed");}
}
