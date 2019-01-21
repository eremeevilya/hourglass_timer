package eremeew_ilya.hourglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;


public class HourglassView extends View
{
    public static final int STATE_STOP = 1;
    public static final int STATE_ROTATE = 2;
    public static final int STATE_WORK = 3;

    private Handler handler;

    private Paint paint_b; // Для балона
    private Paint paint_plane; // Для основания
    private Paint paint_sand; // Для песка
    private Paint paint_grain; // Для песчинок

    private Path path;

    private float progress;

    private float angle = 0.0f; // Угол поворота

    private int state = STATE_STOP;

    private float speed_grain = 0; // Скорость падения песчинок
    private float grain_start_height, grain_end_height; //  Начальная и конечная высоты песчинок

    private ArrayList<Grain> grains;

    private  MyThread thread = null;

    public HourglassView(Context context)
    {
        super(context);

        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                Bundle bundle = msg.getData();
                angle = bundle.getFloat("angle", 0);
                int _state = bundle.getInt("state", 0);
                if(_state != 0)
                    state = _state;

                boolean _invalidate = bundle.getBoolean("invalidate", false);
                if(_invalidate)
                    invalidate();

                invalidate();
            }
        };

        paint_b = new Paint(); // Для балона
        paint_b.setColor(Color.BLUE);
        paint_b.setStrokeWidth(4);

        paint_plane = new Paint();
        paint_plane.setColor(Color.BLUE);

        paint_sand = new Paint();
        paint_sand.setColor(Color.YELLOW);

        paint_grain = new Paint();
        paint_grain.setColor(Color.YELLOW);
        paint_grain.setStrokeWidth(2.0f);

        path = new Path();

        grains = new ArrayList<>();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        //Log.i("qwerty", "state: " + state);
        super.onDraw(canvas);

        // Рисуем фон
        canvas.drawColor(Color.GRAY);

        int w = getWidth();
        int h = getHeight();

        float slot = w / 88.0f; // Щель в балоне

        speed_grain = h / 88.0f;
        grain_start_height = h / 2.0f;
        grain_end_height = h * 7.0f / 8.0f;

        final float h_sand = h / 4.0f; // Высота песка
        float k;

        // Рисуем песок в состоянии STATE_WORK
        if(state == STATE_WORK)
        {
            float h_down = h_sand - h_sand * progress; // Высота нижней кучи
            float h_rect_down = h_down - h / 8.0f;

            float h_up = h_sand * progress; // Высота верхней кучи
            float h_rect_up = h_up - h / 8.0f;

            // --------------- Рисуем падающие песчинки --------------------------------------------
            for(int i = 0; i < grains.size(); i++)
            {
                grains.get(i).draw(canvas);
                grains.get(i).moveDown();
            }

            if(grains.size() < 45)
            {
                grains.add(new Grain(w / 2.0f, h / 2.0f, slot));
            }
            // -------------------------------------------------------------------------------------

            if(progress > 0.5f)
            {
                // ----------- Нижняя куча ---------------------------------------------------------
                k = (1 - progress) * 2.0f / 8.0f;
                drawTriangleDown(canvas, path, w / 2.0f, h * 7.0f / 8.0f, k);
                //----------------------------------------------------------------------------------

                // ----------- Верхняя куча --------------------------------------------------------
                path.reset();
                path.moveTo(w * 3.0f / 8.0f, h * 3.0f / 8.0f);
                path.lineTo(w * 3.0f / 8.0f, h * 3.0f / 8.0f - h_rect_up);
                path.lineTo(w * 5.0f / 8.0f, h * 3.0f / 8.0f - h_rect_up);
                path.lineTo(w * 5.0f / 8.0f, h * 3.0f / 8.0f);
                canvas.drawPath(path, paint_sand);

                drawTriangleUp(canvas, path, w / 2.0f, h / 2.0f, 1.0f / 8.0f);
                //----------------------------------------------------------------------------------
            }
            else
            {
                // ----------- Нижняя куча ---------------------------------------------------------
                drawTriangleDown(canvas, path, w / 2.0f, h * 7.0f / 8.0f - h_rect_down, 1.0f / 8.0f);

                path.reset();
                path.moveTo(w * 3.0f / 8.0f, h * 7.0f / 8.0f);
                path.lineTo(w * 3.0f / 8.0f, h * 7.0f / 8.0f - h_rect_down);
                path.lineTo(w * 5.0f / 8.0f, h * 7.0f / 8.0f - h_rect_down);
                path.lineTo(w * 5.0f / 8.0f, h * 7.0f / 8.0f);
                canvas.drawPath(path, paint_sand);
                //----------------------------------------------------------------------------------

                // ----------- Верхняя куча --------------------------------------------------------
                k = progress * 2.0f / 8.0f;
                drawTriangleUp(canvas, path, w / 2.0f, h / 2.0f, k);
                // ---------------------------------------------------------------------------------
            }
        }

        // Поворот
        if(state == STATE_ROTATE)
        {
            canvas.translate(w / 2.0f, h / 2.0f);
            canvas.rotate(angle);
            canvas.translate(-w / 2.0f, -h / 2.0f);
        }

        // Рисуем песок в состоянии STATE_STOP и STATE_ROTATE
        if(state == STATE_STOP || state == STATE_ROTATE)
        {
            path.reset();
            path.moveTo(w * 3.0f / 8.0f, h * 7.0f / 8.0f);
            path.lineTo(w * 3.0f / 8.0f, h * 6.0f / 8.0f);
            path.lineTo(w / 2.0f, h * 5.0f / 8.0f);
            path.lineTo(w * 5.0f / 8.0f, h * 6.0f / 8.0f);
            path.lineTo(w * 5.0f / 8.0f, h * 7.0f / 8.0f);
            canvas.drawPath(path, paint_sand);
        }

        // Рисуем верхнее основание
        path.reset();
        path.moveTo(w / 4.0f, 0.0f);
        path.lineTo(w * 6.0f / 8.0f, 0.0f);
        path.lineTo(w * 5.0f / 8.0f, h / 8.0f);
        path.lineTo(w * 3.0f / 8.0f, h / 8.0f);
        canvas.drawPath(path, paint_plane);

        // Рисуем нижнее основание
        path.reset();
        path.moveTo(w / 4.0f, h);
        path.lineTo(w * 3.0f / 8.0f, h * 7.0f / 8.0f);
        path.lineTo(w * 5.0f / 8.0f, h * 7.0f / 8.0f);
        path.lineTo(w * 6.0f / 8.0f, h);
        canvas.drawPath(path, paint_plane);

        // Рисуем балон, левая часть
        canvas.drawLine(w * 3.0f / 8.0f, h / 8.0f, w * 3.0f / 8.0f, h * 3.0f / 8.0f, paint_b);
        canvas.drawLine(w * 3.0f / 8.0f, h * 3.0f / 8.0f, w / 2.0f - slot, h / 2.0f, paint_b);
        canvas.drawLine(w / 2.0f - slot, h / 2.0f, w * 3.0f / 8.0f, h * 5.0f / 8.0f, paint_b);
        canvas.drawLine(w * 3.0f / 8.0f, h * 5.0f / 8.0f, w * 3.0f / 8.0f, h * 7.0f / 8.0f, paint_b);

        // Балон, правая часть
        canvas.drawLine(w * 5.0f / 8.0f, h / 8.0f, w * 5.0f / 8.0f, h * 3.0f / 8.0f, paint_b);
        canvas.drawLine(w * 5.0f / 8.0f, h * 3.0f / 8.0f, w / 2.0f + slot, h / 2.0f, paint_b);
        canvas.drawLine(w / 2.0f + slot, h / 2.0f, w * 5.0f / 8.0f, h * 5.0f / 8.0f, paint_b);
        canvas.drawLine(w * 5.0f / 8.0f, h * 5.0f / 8.0f, w * 5.0f / 8.0f, h * 7.0f / 8.0f, paint_b);
    }

    private void drawTriangleDown(Canvas canvas, Path path, float translate_x, float translate_y, float k)
    {
        float w = getWidth();
        float h = getHeight();

        path.reset();
        path.moveTo(-k * w, 0.0f);
        path.lineTo(0.0f, -k * h);
        path.lineTo(k * w, 0.0f);

        canvas.translate(translate_x, translate_y);
        canvas.drawPath(path, paint_sand);
        canvas.translate(-translate_x, -translate_y);
    }

    private void drawTriangleUp(Canvas canvas, Path path, float translate_x, float translate_y, float k)
    {
        float w = getWidth();
        float h = getHeight();

        path.reset();
        path.moveTo(0.0f, 0.0f);
        path.lineTo(-k * w, -k * h);
        path.lineTo(k * w, -k * h);

        canvas.translate(translate_x, translate_y);
        canvas.drawPath(path, paint_sand);
        canvas.translate(-translate_x, -translate_y);
    }

    public void start()
    {
        Log.i("qwerty", "start()");
        state = STATE_ROTATE;

        grains.clear();

        thread = new MyThread();
        thread.start();
    }

    public void stop()
    {
        Log.i("qwerty", "stop()");
        state = STATE_STOP;
        angle = 0;
        grains.clear();
        invalidate();

        if(thread != null)
            thread.is_run = false;
    }

    public void setProgress(int val, int max)
    {
        progress = (float)val / (float)max;

        if(state != STATE_ROTATE)
            state = STATE_WORK;

        invalidate();
    }


    private class MyThread extends Thread
    {
        public boolean is_run = false;

        Bundle bundle;

        public MyThread()
        {
            bundle = new Bundle();
        }

        @Override
        public void run()
        {
            Log.i("qwerty", "start run");
            super.run();

            Message msg;

            final int n_rot = 20;
            for(int i = 0; i <= n_rot; i++) // Цыкл поворота
            {
                try {
                    sleep(500 / n_rot);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                bundle.putFloat("angle", 180 * i / n_rot);
                msg = handler.obtainMessage();
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
            bundle.putInt("state", STATE_WORK);
            msg = handler.obtainMessage();
            msg.setData(bundle);
            handler.sendMessage(msg);

            is_run = true;
            while(is_run)
            {
                try {
                    sleep(45);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                bundle.putBoolean("invalidate", true);
                msg = handler.obtainMessage();
                handler.sendMessage(msg);
            }
            Log.i("qwerty", "stop run");
        }
    }


    // Класс песчинка
    private class Grain
    {
        private float x_pos, y_pos;

        public Grain(float x_pos, float y_pos)
        {
            this.x_pos = x_pos;
            this.y_pos = y_pos;
        }

        public Grain(float x_pos, float y_pos, float d_x_pos_)
        {
            Random rand = new Random();

            this.x_pos = x_pos + (rand.nextFloat() * d_x_pos_ * 2.0f - d_x_pos_);
            this.y_pos = y_pos;
        }

        public void moveDown()
        {
            if(y_pos >= grain_end_height)
            {
                y_pos = grain_start_height;
                return;
            }

            y_pos += speed_grain;
        }

        public void draw(Canvas canvas)
        {
            canvas.drawPoint(x_pos, y_pos, paint_grain);
        }
    }
}
