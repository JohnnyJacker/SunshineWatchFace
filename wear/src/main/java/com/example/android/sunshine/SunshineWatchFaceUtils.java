package com.example.android.sunshine;

import java.util.Calendar;


public final class SunshineWatchFaceUtils {

    public static String getDate(Calendar calendar) {
        String stringDate;
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case 1:
                stringDate = "SUN, ";
                break;
            case 2:
                stringDate = "MON, ";
                break;
            case 3:
                stringDate = "TUE, ";
                break;
            case 4:
                stringDate = "WED, ";
                break;
            case 5:
                stringDate = "THU, ";
                break;
            case 6:
                stringDate = "FRI, ";
                break;
            case 7:
                stringDate = "SAT, ";
                break;
            default:
                stringDate = "";
        }
        switch (calendar.get(Calendar.MONTH)) {
            case 0:
                stringDate = stringDate + "JAN ";
                break;
            case 1:
                stringDate = stringDate + "FEB ";
                break;
            case 2:
                stringDate = stringDate + "MAR ";
                break;
            case 3:
                stringDate = stringDate + "APR ";
                break;
            case 4:
                stringDate = stringDate + "MAY ";
                break;
            case 5:
                stringDate = stringDate + "JUN ";
                break;
            case 6:
                stringDate = stringDate + "JUL ";
                break;
            case 7:
                stringDate = stringDate + "AUG ";
                break;
            case 8:
                stringDate = stringDate + "SEP ";
                break;
            case 9:
                stringDate = stringDate + "OCT ";
                break;
            case 10:
                stringDate = stringDate + "NOV ";
                break;
            case 11:
                stringDate = stringDate + "DEC ";
                break;
            default:
                break;
        }
        stringDate = stringDate + calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.YEAR);
        return stringDate;
    }

    public static int getIcon(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }


    private SunshineWatchFaceUtils() {
    }
}
