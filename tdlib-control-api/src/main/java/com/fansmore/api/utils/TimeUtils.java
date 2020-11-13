package com.fansmore.api.utils;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间工具类
 * 主要用于在Java代码中处理时间。
 * 包含格式化时间，字符串转时间，设置时间，获取时间，获取月份天数，对时间进行加法操作，获取特定时间(当天/月开始/结束)。
 *
 * @author yiChuYun_lq
 * @date 2019/2/21 18:26
 * @since 1.1.0
 */
public class TimeUtils {

    public static final String DATE_TIME_FORMAT_STR = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_STR = "yyyy-MM-dd";
    public static final String TIME_FORMAT_STR = "HH:mm:ss";

    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STR);
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(DATE_FORMAT_STR);
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(TIME_FORMAT_STR);

    /**
     * 设置年份
     *
     * @param date
     * @param year
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 9:56
     */
    public static void setYear(Date date, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.YEAR, year);
        date.setTime(calendar.getTimeInMillis());
    }

    /**
     * 设置月份（1~12）
     *
     * @param date
     * @param month
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 9:57
     */

    public static void setMonth(Date date, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MONTH, --month);
        date.setTime(calendar.getTimeInMillis());
    }

    /**
     * 设置天数
     *
     * @param date
     * @param day
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 9:57
     */
    public static void setDay(Date date, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, day);
        date.setTime(calendar.getTimeInMillis());
    }

    /**
     * 设置小时
     *
     * @param date
     * @param hour
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 9:58
     */
    public static void setHour(Date date, int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        date.setTime(calendar.getTimeInMillis());
    }

    /**
     * 设置分钟
     *
     * @param date
     * @param minute
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 9:58
     */
    public static void setMinute(Date date, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MINUTE, minute);
        date.setTime(calendar.getTimeInMillis());
    }

    /**
     * 设置秒钟
     *
     * @param date
     * @param second
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 9:58
     */
    public static void setSecond(Date date, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.SECOND, second);
        date.setTime(calendar.getTimeInMillis());
    }

    /**
     * 设置毫秒
     *
     * @param date
     * @param millisecond
     * @return void
     * @author yiChuYun_lq
     * @date 2019/4/8 9:50
     */
    public static void setMillisecond(Date date, int millisecond) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MILLISECOND, millisecond);
        date.setTime(calendar.getTimeInMillis());
    }

    /**
     * 设置年月日
     *
     * @param date
     * @param year
     * @param moth
     * @param day
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 10:04
     */
    public static void setDate(Date date, int year, int moth, int day) {
        TimeUtils.setYear(date, year);
        TimeUtils.setMonth(date, moth);
        TimeUtils.setDay(date, day);
    }

    /**
     * 设置时分秒
     *
     * @param date
     * @param hour
     * @param minute
     * @param second
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 10:04
     */
    public static void setTime(Date date, int hour, int minute, int second) {
        TimeUtils.setHour(date, hour);
        TimeUtils.setMinute(date, minute);
        TimeUtils.setSecond(date, second);
    }

    /**
     * 设置时分秒毫秒
     *
     * @param date
     * @param hour
     * @param minute
     * @param second
     * @param millisecond
     * @return void
     * @author yichuyun_lq
     * @date 2019/4/8 9:53
     */
    public static void setTime(Date date, int hour, int minute, int second, int millisecond) {
        TimeUtils.setHour(date, hour);
        TimeUtils.setMinute(date, minute);
        TimeUtils.setSecond(date, second);
        TimeUtils.setMillisecond(date, millisecond);
    }

    /**
     * 设置年月日时分秒
     *
     * @param date
     * @param year
     * @param moth
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 10:04
     */
    public static void setDateTime(Date date, int year, int moth, int day, int hour, int minute, int second) {
        TimeUtils.setYear(date, year);
        TimeUtils.setMonth(date, moth);
        TimeUtils.setDay(date, day);
        TimeUtils.setHour(date, hour);
        TimeUtils.setMinute(date, minute);
        TimeUtils.setSecond(date, second);
    }

    /**
     * 设置年月日时分秒毫秒
     *
     * @param date
     * @param year
     * @param moth
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @param millisecond
     * @return void
     * @author yichuyun_lq
     * @date 2019/4/8 9:55
     */
    public static void setDateTime(Date date, int year, int moth, int day, int hour, int minute, int second, int millisecond) {
        TimeUtils.setYear(date, year);
        TimeUtils.setMonth(date, moth);
        TimeUtils.setDay(date, day);
        TimeUtils.setHour(date, hour);
        TimeUtils.setMinute(date, minute);
        TimeUtils.setSecond(date, second);
        TimeUtils.setMillisecond(date, millisecond);
    }

    /**
     * 获取年份
     *
     * @param date
     * @return int
     * @author yiChuYun_lq
     * @date 2019/2/22 10:07
     */
    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    /**
     * 获取月份（1~12）
     *
     * @param date
     * @return int
     * @author yiChuYun_lq
     * @date 2019/2/22 10:08
     */
    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取天数
     *
     * @param date
     * @return int
     * @author yiChuYun_lq
     * @date 2019/2/22 10:18
     */
    public static int getDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DATE);
    }

    /**
     * 获取小时
     *
     * @param date
     * @return void
     * @author yiChuYun_lq
     * @date 2019/2/22 10:18
     */
    public static int getHour(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取分钟
     *
     * @param date
     * @return int
     * @author yiChuYun_lq
     * @date 2019/2/22 10:19
     */
    public static int getMinute(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);
    }

    /**
     * 获取秒钟
     *
     * @param date
     * @return int
     * @author yiChuYun_lq
     * @date 2019/2/22 10:20
     */
    public static int getSecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.SECOND);
    }

    /**
     * 获取毫秒
     *
     * @param date
     * @return int
     * @author yichuyun_lq
     * @date 2019/4/8 9:59
     */
    public static int getMillisecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MILLISECOND);
    }


    /**
     * 格式化年月日(yyyy-MM-dd)
     *
     * @param date
     * @return java.lang.String
     * @author yiChuYun_lq
     * @date 2019/2/22 10:21
     */
    public static String dateFormat(Date date) {
        return date2LocalDateTime(date).format(DATE_FORMAT);
    }

    /**
     * 格式化时分秒(HH:mm:ss)
     *
     * @param date
     * @return java.lang.String
     * @author yiChuYun_lq
     * @date 2019/2/22 10:22
     */
    public static String timeFormat(Date date) {
        return date2LocalDateTime(date).format(TIME_FORMAT);
    }

    /**
     * 格式化年月日时分秒(yyyy-MM-dd HH:mm:ss)
     *
     * @param date
     * @return java.lang.String
     * @author yiChuYun_lq
     * @date 2019/2/22 10:23
     */
    public static String dateTimeFormat(Date date) {
        return date2LocalDateTime(date).format(DATE_TIME_FORMAT);
    }

    /**
     * 格式化时间(自定义pattern)
     *
     * @param date
     * @param pattern
     * @return java.lang.String
     * @author yiChuYun_lq
     * @date 2019/2/22 11:45
     */
    public static String dateTimeFormat(Date date, String pattern) {
        return new SimpleDateFormat(pattern).format(date);
    }

    /**
     * 字符串时间转化为Date类型(yyyy-MM-dd)
     *
     * @param date
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:57
     */
    public static Date string2date(String date) {
        LocalDate parse = LocalDate.parse(date, DATE_FORMAT);
        return localDate2Date(parse);
    }

    /**
     * 字符串时间转化为Date类型(HH:mm:ss)
     *
     * @param date
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:57
     */
    public static Date string2time(String date) {
        LocalTime parse = LocalTime.parse(date, TIME_FORMAT);
        return localTime2Date(parse);
    }

    /**
     * 字符串时间转化为Date类型(yyyy-MM-dd HH:mm:ss)
     *
     * @param date
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:56
     */
    public static Date string2dateTime(String date) {
        LocalDateTime parse = LocalDateTime.parse(date, DATE_TIME_FORMAT);
        return localDateTime2Date(parse);
    }

    /**
     * 字符串时间转化为Date类型(自定义pattern)
     *
     * @param date
     * @param pattern
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:56
     */
    public static Date string2dateTime(String date, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        try {
            return format.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 对年份进行加法运算
     *
     * @param date
     * @param year
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:15
     */
    public static Date addYear(Date date, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, year);
        return calendar.getTime();
    }

    /**
     * 对月份进行加法运算
     *
     * @param date
     * @param month
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:16
     */
    public static Date addMonth(Date date, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, month);
        return calendar.getTime();
    }

    /**
     * 对天数进行加法运算
     *
     * @param date
     * @param day
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:17
     */
    public static Date addDay(Date date, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, day);
        return calendar.getTime();
    }

    /**
     * 对小时进行加法运算
     *
     * @param date
     * @param hour
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:19
     */
    public static Date addHour(Date date, int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hour);
        return calendar.getTime();
    }

    /**
     * 对分钟进行加法运算
     *
     * @param date
     * @param minute
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:20
     */
    public static Date addMinute(Date date, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    /**
     * 对秒钟进行加法运算
     *
     * @param date
     * @param second
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 11:20
     */
    public static Date addSecond(Date date, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, second);
        return calendar.getTime();
    }

    /**
     * 对毫秒数进行加法运算
     *
     * @param date
     * @param millisecond
     * @return java.util.Date
     * @author yichuyun_lq
     * @date 2019/4/8 10:01
     */
    public static Date addMillisecond(Date date, int millisecond) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MILLISECOND, millisecond);
        return calendar.getTime();
    }

    /**
     * 计算时间差
     * var1至var2所相差的时间毫秒数
     *
     * @param var1
     * @param var2
     * @return long
     * @author yiChuYun_lq
     * @date 2019/2/22 10:37
     */
    public static long timeDifference(Date var1, Date var2) {
        return var2.getTime() - var1.getTime();
    }

    /**
     * 获取指定月份有多少天
     *
     * @param date
     * @return int
     * @author yiChuYun_lq
     * @date 2019/2/22 11:05
     */
    public static int getDesignationMonthLastDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, 1);
        calendar.roll(Calendar.DATE, -1);
        return calendar.get(Calendar.DATE);
    }

    /**
     * 获取当前月有多少天
     *
     * @return int
     * @author yiChuYun_lq
     * @date 2019/2/22 11:06
     */
    public static int getCurrentMonthLastDay() {
        return TimeUtils.getDesignationMonthLastDay(Calendar.getInstance().getTime());
    }

    /**
     * 获取一天开始的时间
     *
     * @param date
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 10:42
     */
    public static Date startOfTheDay(Date date) {
        Date var = new Date(date.getTime());
        TimeUtils.setTime(var, 0, 0, 0, 0);
        return var;
    }

    /**
     * 获取一天结束的时间（毫秒数为0）
     *
     * @param date
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 10:46
     */
    public static Date endOfTheDay(Date date) {
        Date var = new Date(date.getTime());
        TimeUtils.setTime(var, 23, 59, 59, 0);
        return var;
    }

    /**
     * 当月开始时间
     *
     * @param date
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 10:56
     */
    public static Date startOfTheMonth(Date date) {
        Date var = new Date(date.getTime());
        TimeUtils.setTime(var, 0, 0, 0, 0);
        TimeUtils.setDay(var, 1);
        return var;
    }

    /**
     * 当月结束时间（毫秒数为0）
     *
     * @param date
     * @return java.util.Date
     * @author yiChuYun_lq
     * @date 2019/2/22 10:56
     */
    public static Date endOfTheMonth(Date date) {
        Date var = new Date(date.getTime());
        TimeUtils.setTime(var, 23, 59, 59, 0);
        TimeUtils.setDay(var, TimeUtils.getDesignationMonthLastDay(var));
        return var;
    }

    public static LocalDateTime date2LocalDateTime(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDate date2LocalDate(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalTime date2LocalTime(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.systemDefault()).toLocalTime();
    }

    public static Date localDateTime2Date(LocalDateTime localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = localDateTime.atZone(zoneId);
        return Date.from(zdt.toInstant());
    }

    public static Date localDate2Date(LocalDate localDate) {
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    public static Date localTime2Date(LocalTime localTime) {
        LocalDate localDate = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = LocalDateTime.of(localDate, localTime).atZone(zone).toInstant();
        return Date.from(instant);
    }
}
