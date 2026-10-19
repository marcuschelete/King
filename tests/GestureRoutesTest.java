package com.artist.king;
public class GestureRoutesTest {
    static void expect(String wanted,String page,float x,float y,boolean top,long elapsed){String got=GestureRoutes.destination(page,x,y,top,elapsed);if(wanted==null?got!=null:!wanted.equals(got))throw new AssertionError(page+" ("+x+","+y+"): "+got+" != "+wanted);}
    public static void main(String[] args){
        expect("Apps","Home",0,-110,true,200);expect("Board","Home",-110,5,true,200);expect("Focus","Home",110,0,true,200);
        expect("Home","Board",110,0,false,200);expect("Home","Focus",-110,0,true,200);expect("Board","Notes",110,0,true,200);
        expect(null,"Board",0,-110,true,200);expect(null,"Ritual",0,110,true,200);expect(null,"Apps",0,110,false,200);
        expect("Home","Apps",0,110,true,200);expect(null,"Home",40,0,true,200);expect(null,"Home",110,100,true,200);expect(null,"Home",110,0,true,900);
        System.out.println("13 gesture routing checks passed");
    }
}
