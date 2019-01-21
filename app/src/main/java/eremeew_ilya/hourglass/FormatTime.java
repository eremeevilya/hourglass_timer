package eremeew_ilya.hourglass;


public class FormatTime
{
    public static String formatTime(int time_int)
    {
        int h = (time_int / (60 * 60));
        int m = ((time_int % (60 * 60)) / 60);
        int s = (time_int % 60);

        String s_h, s_m, s_s;

        if(h < 10)
            s_h = "0" + h;
        else
            s_h = String.valueOf(h);

        if(m < 10)
            s_m = "0" + m;
        else
            s_m = String.valueOf(m);

        if(s < 10)
            s_s = "0" + s;
        else
            s_s = String.valueOf(s);

        return s_h + ":" + s_m + ":" + s_s;
    }
}
