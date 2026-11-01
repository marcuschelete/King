package com.artist.king;
public class UpdatePolicyTest {
    static void expect(boolean value,boolean expected){if(value!=expected)throw new AssertionError();}
    public static void main(String[] args){
        String good="https://github.com/marcuschelete/King/raw/refs/heads/main/downloads/KING-Focus-1.4.0.apk";
        expect(UpdatePolicy.allowed(good),true);
        for(String bad:new String[]{good.replace("https:","http:"),good.replace("github.com","github.com.evil.com"),good.replace("King/","other/"),good+"?redirect=evil",good.replace("1.4.0.apk","../evil.apk"),"javascript:alert(1)",good.replace("github.com","user@github.com")})expect(UpdatePolicy.allowed(bad),false);
        expect(UpdatePolicy.due(100,0),true);expect(UpdatePolicy.due(200,100),false);expect(UpdatePolicy.due(21600100,100),true);expect(UpdatePolicy.due(50,100),true);
        System.out.println("12 update policy checks passed");
    }
}
