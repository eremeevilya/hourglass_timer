package eremeew_ilya.hourglass;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity
{
    public static final String PREFERENCES = "hourglass_app";
    public static final String KEY_PREFERENCES_START_TIME = "start_time";

    public static final String KEY_TIME_STOP = "time_stop";
    public static final String KEY_STATE = "extra_state";
    public static final int STATE_TIMEOUT = 1;

    private SharedPreferences preferences;

    private TextView tv_time;
    private TextView tv_start_time;
    private Button btn_start_stop;
    private LinearLayout ll_container;
    private HourglassView hourglass_view;

    private Resources resources;

    private long start_time_milliseconds = 20000;

    private MyBroadcastReceiver my_broadcast_receiver;

    private String[] items_time_strings;
    private long[] items_time_values;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("qwerty", "activity onCreate");

        preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        initItemsTime();

        resources = getResources();

        tv_start_time = (TextView)findViewById(R.id.tv_start_time);
        tv_time = (TextView)findViewById(R.id.tv_time);
        btn_start_stop = (Button)findViewById(R.id.btn_start_stop);
        ll_container = (LinearLayout)findViewById(R.id.ll_container);

        tv_start_time.setText(FormatTime.formatTime((int)(start_time_milliseconds / 1000)));

        if(TimerService.isStart())
        {
            btn_start_stop.setText(resources.getString(R.string.stop));
        }
        else
        {
            btn_start_stop.setText(resources.getString(R.string.start));
        }

        hourglass_view = new HourglassView(this, null);
        ll_container.addView(hourglass_view);

        /*
        // Получаем результат из TimerService, посредством Intent
        Intent intent = getIntent();
        if(intent != null)
        {
            // Если Activity запущена по причине окончания времени
            if(intent.getIntExtra(KEY_STATE, 0) == STATE_TIMEOUT)
            {
                Toast.makeText(this, resources.getString(R.string.time_out), Toast.LENGTH_LONG).show();
                btn_start_stop.setText(resources.getString(R.string.start));
            }
        }
        */
    }

    @Override
    public void onResume()
    {
        Log.i("qwerty", "activity  onResume");
        super.onResume();

        // Получаем настройки
        start_time_milliseconds = preferences.getLong(KEY_PREFERENCES_START_TIME, 30);
        tv_start_time.setText(FormatTime.formatTime((int)(start_time_milliseconds / 1000)));

        // Регистрируем BroadcastReceiver
        my_broadcast_receiver = new MyBroadcastReceiver();
        IntentFilter intent_filter = new IntentFilter(TimerService.ACTION_MY_TIMER_SERVICE);
        intent_filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(my_broadcast_receiver, intent_filter);
    }

    @Override
    protected void onPause()
    {
        Log.i("qwerty", "activity  onPause");
        super.onPause();

        // Снимаем регистрацию с BroadcastReceiver
        unregisterReceiver(my_broadcast_receiver);

        //finish();
    }

    public void onClickStartStop(View v)
    {
        if(TimerService.isStart())
        {
            btn_start_stop.setText(resources.getString(R.string.start));
            hourglass_view.stop();
            stop();
        }
        else
        {
            btn_start_stop.setText(resources.getString(R.string.stop));
            hourglass_view.start();
            start();
        }
    }

    public void setTime(View v)
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(resources.getString(R.string.set_time));
        dialog.setItems(items_time_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                start_time_milliseconds = items_time_values[which];
                tv_start_time.setText(FormatTime.formatTime((int)(start_time_milliseconds / 1000)));

                // Запоминаем в настройках
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(KEY_PREFERENCES_START_TIME, start_time_milliseconds);
                editor.apply();
            }
        });
        dialog.setNegativeButton(resources.getString(R.string.cancel), null);

        dialog.show();
    }

    private void start()
    {
        long time_stop = System.currentTimeMillis() + start_time_milliseconds;

        Intent intent = new Intent(this, TimerService.class);
        // В Intent передаем время остановки
        intent.putExtra(KEY_TIME_STOP, time_stop);
        startService(intent);
    }

    private void stop()
    {
        Intent intent = new Intent(this, TimerService.class);
        stopService(intent);
    }

    private void initItemsTime()
    {
        items_time_strings = new String[]{"10 сек", "20 сек", "30 сек" , "1 мин", "1 мин 30 сек",
                "2 мин", "5 мин", "10 мин", "15 мин", "30 мин", "1 час", "1 час 30 мин", "2 час",
                "2 час 30 мин", "3 час"};
        items_time_values = new long[]{
                1000 * 10,
                1000 * 20,
                1000 * 30,
                1000 * 60,
                1000 * 60 * 1 + 1000 * 30,
                1000 * 60 * 2,
                1000 * 60 * 5,
                1000 * 60 * 10,
                1000 * 60 * 15,
                1000 * 60 * 30,
                1000 * 60 * 60,
                1000 * 60 * 60 * 1 + 1000 * 60 * 30,
                1000 * 60 * 60 * 2,
                1000 * 60 * 60 * 2 + 1000 * 60 * 30,
                1000 * 60 * 60 * 3};
    }


    //
    public class MyBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int time_result = intent.getIntExtra(TimerService.KEY_OUT, 0);
            tv_time.setText(FormatTime.formatTime(time_result));

            if(time_result <= 0)
            {
                btn_start_stop.setText(resources.getString(R.string.start));
                hourglass_view.stop();
            }

            hourglass_view.setProgress(time_result, (int)(start_time_milliseconds / 1000));
        }
    }
}
