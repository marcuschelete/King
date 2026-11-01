package com.artist.king;
import java.time.*;
public class CalendarAgendaTest {
    static int checks;
    static long utc(String day){return LocalDate.parse(day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();}
    static void check(boolean value){checks++;if(!value)throw new AssertionError("Calendar check "+checks);}
    public static void main(String[] args){
        LocalDate today=LocalDate.of(2026,10,25);
        for(String name:new String[]{"Africa/Gaborone","America/Los_Angeles","Pacific/Kiritimati"}){
            ZoneId zone=ZoneId.of(name);
            check(CalendarAgenda.includes(utc("2026-10-25"),utc("2026-10-26"),true,today,1,zone));
            check(CalendarAgenda.date(utc("2026-10-25"),true,zone).equals(today));
            check(!CalendarAgenda.includes(utc("2026-10-24"),utc("2026-10-25"),true,today,3,zone));
            check(CalendarAgenda.includes(utc("2026-10-24"),utc("2026-10-27"),true,today,1,zone));
            check(CalendarAgenda.includes(utc("2026-10-27"),utc("2026-10-28"),true,today,3,zone));
            check(!CalendarAgenda.includes(utc("2026-10-28"),utc("2026-10-29"),true,today,3,zone));
            long start=today.atStartOfDay(zone).toInstant().toEpochMilli();
            check(CalendarAgenda.order(utc("2026-10-25"),true,today,zone)==start);
            check(CalendarAgenda.includes(start+3600000,start+7200000,false,today,1,zone));
            check(CalendarAgenda.includes(start-3600000,start+3600000,false,today,1,zone));
            check(!CalendarAgenda.includes(start-7200000,start,false,today,1,zone));
            check(CalendarAgenda.includes(start,start,false,today,1,zone));
        }
        ZoneId london=ZoneId.of("Europe/London");long late=today.atTime(23,30).atZone(london).toInstant().toEpochMilli();check(CalendarAgenda.includes(late,late+60000,false,today,1,london));long tomorrow=today.plusDays(1).atStartOfDay(london).toInstant().toEpochMilli();check(!CalendarAgenda.includes(tomorrow,tomorrow+60000,false,today,1,london));
        System.out.println(checks+" calendar range/timezone checks passed");
    }
}
