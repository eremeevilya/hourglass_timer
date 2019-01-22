package eremeew_ilya.hourglass;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;


public class TimerService extends Service
{
    public static final int NOTIFICATION_ID = 45;
    public static final int NOTIFICATION_TIMEOUT_ID = 46;

    public static final String ACTION_MY_TIMER_SERVICE = "eremeew_ilya.hourglass.TimerService";
    public static final String KEY_OUT = "EXTRA_OUT";

    private static boolean is_start = false;
    private static int timer_time = 0;

    public static boolean isStart(){return is_start;}

    public static int getTimerTime(){return timer_time;}

    private Context context;

    private TimerThread timer;

    private long time_stop = 0;
    private long current_time = System.currentTimeMillis();

    private Resources resources;

    private int sound_Id;
    private SoundPool soundPool = null;
    private AssetManager asset_manager;

    private NotificationManager notification_manager;

    public TimerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("qwerty", "service  onStartCommand");
        context = getApplicationContext();

        if(intent != null)
        {
            resources = getResources();

            asset_manager = getAssets();

            notification_manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            loadSound();

            time_stop = intent.getLongExtra(MainActivity.KEY_TIME_STOP, 0);

            timer = new TimerThread();
            timer.start();
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy()
    {
        Log.i("qwerty", "service  onDestroy");
        super.onDestroy();

        is_start = false;
        timer.loop = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    void loadSound()
    {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        AssetFileDescriptor afd;

        sound_Id = -1;
        try {
            afd = asset_manager.openFd("tone_900Hz_03s.mp3");
            sound_Id = soundPool.load(afd, 1);
        }
        catch (IOException e)
        {
            Log.e("qwerty", "error load MP3.   " + e.getMessage());
            sound_Id = -1;
            e.printStackTrace();
        }
    }


    // class
    private class TimerThread extends Thread
    {
        private boolean loop = true;

        public TimerThread()
        {
            //current_time = System.currentTimeMillis();
        }

        @Override
        public void run()
        {
            super.run();

            is_start = true;

            // Удаляем старое уведомление, если таковое существует
            notification_manager.cancel(NOTIFICATION_TIMEOUT_ID);

            Intent notification_intent = new Intent(context, MainActivity.class);
            PendingIntent content_intent = PendingIntent.getActivity(context, 0, notification_intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            // Создаем уведомление
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setContentIntent(content_intent)
                    .setSmallIcon(R.drawable.ic_hourglass_full_black_24dp)
                    .setContentTitle(resources.getString(R.string.app_name))
                    .setShowWhen(true)
                    .setUsesChronometer(false)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                    .setOngoing(true);

            // Повышаем приоритет сервиса и показываем уведомление
            startForeground(NOTIFICATION_ID, builder.build());

            Intent send_intent = new Intent();
            send_intent.setAction(ACTION_MY_TIMER_SERVICE);
            send_intent.addCategory(Intent.CATEGORY_DEFAULT);

            current_time = System.currentTimeMillis();
            timer_time = (int) (time_stop - current_time) / 1000;
            while (timer_time >= 0)
            {
                if(!loop)
                {
                    // Возвращаем сервису обычный приоритет и удаляем уведомление
                    stopForeground(true);

                    return;
                }

                Log.i("qwerty", "timer: " + timer_time);
                send_intent.putExtra(KEY_OUT, timer_time);
                sendBroadcast(send_intent);

                // Обновляем уведомление
                builder.setContentText("TIME: " + FormatTime.formatTime((int)(time_stop - current_time) / 1000));
                startForeground(NOTIFICATION_ID, builder.build());

                if(timer_time < 10 && sound_Id > 0)
                {
                    // Воспроизводим звук тона
                    soundPool.play(sound_Id, 1, 1, 1, 0, 1);
                }

                sleep_1000();

                current_time = System.currentTimeMillis();
                timer_time = (int) (time_stop - current_time) / 1000;
            }
            is_start = false;

            // Возвращаем сервису обычный приоритет и удаляем уведомление
            stopForeground(true);

            // Создаем новое уведомление, об окончании времени
            builder.setContentText(resources.getString(R.string.time_out));
            // Добавляем звук и вибрацию
            Uri ring_uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            long[] vibrate = new long[]{100, 3000, 100, 1000, 100};
            builder.setSound(ring_uri)
                    .setVibrate(vibrate)
                    .setOngoing(false)
                    .setWhen(System.currentTimeMillis())
                    .setUsesChronometer(true)
                    .setAutoCancel(true);
            notification_manager.notify(NOTIFICATION_TIMEOUT_ID, builder.build());
        }

        // Задержка 1 секунда
        private void sleep_1000()
        {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
