package com.artist.king;
import java.time.*;
/** All-day Calendar Provider timestamps represent UTC dates, not local instants. */
final class CalendarAgenda {
    static LocalDate date(long timestamp,boolean allDay,ZoneId zone){return Instant.ofEpochMilli(timestamp).atZone(allDay?ZoneOffset.UTC:zone).toLocalDate();}
    static boolean includes(long begin,long end,boolean allDay,LocalDate today,int days,ZoneId zone){
        if(allDay){LocalDate first=date(begin,true,zone),last=date(end,true,zone);if(!last.isAfter(first))last=first.plusDays(1);return first.isBefore(today.plusDays(days))&&last.isAfter(today);}
        long start=today.atStartOfDay(zone).toInstant().toEpochMilli(),limit=today.plusDays(days).atStartOfDay(zone).toInstant().toEpochMilli();return begin<limit&&(end>start||begin>=start);
    }
    static long order(long begin,boolean allDay,LocalDate today,ZoneId zone){long value=allDay?date(begin,true,zone).atStartOfDay(zone).toInstant().toEpochMilli():begin;return Math.max(value,today.atStartOfDay(zone).toInstant().toEpochMilli());}
}
