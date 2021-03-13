package com.dongmyungahn.android.expensediary.utilities;

import android.text.format.DateFormat;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

public class ExpenseUtil {

    public static Calendar timestampToCalendar(double timestamp){
        if(timestamp > 0) {
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis((long) timestamp);
            return cal;
        } else {
            return null;
        }
    }

    public static String timestampToDateString(double timestamp){
        Calendar cal = timestampToCalendar(timestamp);
        if(cal != null) return DateFormat.format("MM/dd/yyyy hh:mm aaa", cal).toString();
        else            return null;
    }


    public static String expenseNumberFormat(double amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        return numberFormat.format(amount);
    }

    public static String expenseCurrencyFormatter(double amount) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return numberFormat.format(amount);
    }

    public static double getDoubleFromCurrency(String s) {
        Number number = null;
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        try
        {
            number = numberFormat.parse(s);
        }
        catch (ParseException e)
        {
            return 0;
        }
        return number.doubleValue();
    }
}
