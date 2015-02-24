package com.aus.sgsp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class Util {

    private static final String sgFmtStr = "yyyy-MM-dd'T'HH:mm:ss";
    private static final SimpleDateFormat sgFormat = new SimpleDateFormat(sgFmtStr, Locale.US);

    public static Date parseTimeStr(String timeStr) throws ParseException {
        return sgFormat.parse(timeStr);
    }

}
