package com.autodash.sport;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.View;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.content.Context;

public class MainActivity extends Activity {

    Dashboard dashboard;
    LocationManager gps;
    float speed = 0;
    float rpm = 850;
    int fuel = 65;
    boolean started = false;
    boolean automatic = true;
    int manualRpm = 1500;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        dashboard = new Dashboard(this);
        setContentView(dashboard);

        gps = (LocationManager)
            getSystemService(Context.LOCATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                100);
        } else {
            startGPS();
        }

        dashboard.post(update);
    }

    void startGPS() {
        try {
            gps.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                0.5f,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location loc) {
                        if (loc.hasSpeed()) {
                            speed = loc.getSpeed() * 3.6f;
                        }
                        dashboard.invalidate();
                    }

                    @Override public void onProviderEnabled(String p) {}
                    @Override public void onProviderDisabled(String p) {}
                    @Override public void onStatusChanged(
                        String p, int s, Bundle e) {}
                });
        } catch (SecurityException e) {}
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(
            requestCode, permissions, results);

        if (requestCode == 100 &&
            results.length > 0 &&
            results[0] == PackageManager.PERMISSION_GRANTED) {
            startGPS();
        }
    }

    Runnable update = new Runnable() {
        @Override
        public void run() {
            if (started) {
                if (automatic) {
                    int gear = speed < 20 ? 1 :
                               speed < 45 ? 2 :
                               speed < 75 ? 3 :
                               speed < 110 ? 4 : 5;

                    float target = 850 +
                        (speed / (gear * 1.8f)) * 100;

                    rpm += (target - rpm) * 0.12f;
                } else {
                    rpm += (manualRpm - rpm) * 0.15f;
                }
                rpm = Math.max(850, Math.min(8000, rpm));
            } else {
                rpm = 0;
            }

            dashboard.invalidate();
            dashboard.postDelayed(this, 40);
        }
    };

    class Dashboard extends View {

        Paint p = new Paint(3);
        Canvas c;
        float w, h;

        Dashboard(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void text(String s, float x, float y,
                  float size, int color) {
            p.setColor(color);
            p.setTextSize(size);
            p.setTypeface(android.graphics.Typeface.create(
                "sans-serif", android.graphics.Typeface.BOLD));
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(s, x, y, p);
        }

        void circle(float x, float y, float r,
                    int color, float stroke) {
            p.setColor(color);
            p.setStyle(stroke > 0 ?
                Paint.Style.STROKE : Paint.Style.FILL);
            p.setStrokeWidth(stroke);
            c.drawCircle(x, y, r, p);
            p.setStyle(Paint.Style.FILL);
        }

        void gauge(float x, float y, float r,
                   float value, float max, String label,
                   String unit, int color) {

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(15);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(Color.rgb(45, 52, 66));

            RectF oval = new RectF(
                x-r, y-r, x+r, y+r);

            c.drawArc(oval, 135, 270, false, p);

            p.setColor(color);
            c.drawArc(oval, 135,
                270 * Math.min(value/max, 1), false, p);

            p.setStrokeWidth(2);
            p.setColor(Color.LTGRAY);

            for (int i = 0; i <= 8; i++) {
                double a = Math.toRadians(135 + i * 33.75);
                float x1 = x + (float)Math.cos(a)*(r-13);
                float y1 = y + (float)Math.sin(a)*(r-13);
                float x2 = x + (float)Math.cos(a)*(r-25);
                float y2 = y + (float)Math.sin(a)*(r-25);
                c.drawLine(x1,y1,x2,y2,p);
            }

            double angle = Math.toRadians(
                135 + 270 * Math.min(value/max,1));

            p.setColor(Color.WHITE);
            p.setStrokeWidth(5);
            c.drawLine(x,y,
                x+(float)Math.cos(angle)*(r-32),
                y+(float)Math.sin(angle)*(r-32),p);

            circle(x,y,8,Color.RED,0);

            p.setStyle(Paint.Style.FILL);
            text(label,x,y+r+28,20,Color.WHITE);
            text(unit,x,y+r+51,15,Color.LTGRAY);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            c = canvas;
            w = getWidth();
            h = getHeight();

            c.drawColor(Color.rgb(7,10,18));

            text("AUTO DASH SPORT",w/2,35,24,
                Color.rgb(0,200,255));

            float r = Math.min(h*0.35f,w*0.19f);
            float cy = h*0.47f;

            gauge(w*0.27f,cy,r,speed,220,
                "VELOCIDAD","KM/H",
                Color.rgb(0,190,255));

            gauge(w*0.73f,cy,r,rpm,8000,
                "TACÓMETRO","RPM",
                Color.rgb(255,55,65));

            text(String.format("%.0f",speed),
                w*0.27f,cy+10,42,Color.WHITE);

            text(String.format("%.0f",rpm),
                w*0.73f,cy+10,42,Color.WHITE);

            text("COMBUSTIBLE",w/2,cy-25,15,
                Color.LTGRAY);

            text(fuel+"%",w/2,cy+15,35,
                fuel<=15?Color.RED:Color.GREEN);

            p.setColor(Color.DKGRAY);
            c.drawRoundRect(
                w/2-48,cy+35,w/2+48,cy+48,
                8,8,p);

            p.setColor(fuel<=15?Color.RED:Color.GREEN);
            c.drawRoundRect(
                w/2-48,cy+35,
                w/2-48+96*fuel/100f,cy+48,
                8,8,p);

            text("TEMP. 90°C",w/2,cy+85,17,
                Color.rgb(255,170,40));

            text("BAT 12.4 V",w/2,cy+110,17,
                Color.rgb(0,190,255));

            text("GPS: "+(speed>=0?"ACTIVO":"ESPERANDO"),
                w/2,cy+137,13,Color.GREEN);

            float by = h-28;

            text(started?"ENGINE ON":"ENGINE START",
                w*0.16f,by,17,
                started?Color.GREEN:Color.RED);

            text(automatic?"MODO AUTO":"MODO MANUAL",
                w*0.50f,by,17,Color.CYAN);

            text("GAS -   "+fuel+"%   GAS +",
                w*0.83f,by,16,Color.WHITE);

            if (!started) {
                text("PULSA ENGINE START PARA ENCENDER",
                    w/2,h*0.88f,16,Color.LTGRAY);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                float x = e.getX();
                float y = e.getY();

                if (y > h-65 && x < w*0.32f) {
                    started = !started;
                    if (!started) rpm = 0;
                } else if (y > h-65 &&
                           x < w*0.68f) {
                    automatic = !automatic;
                } else if (y > h-65 && x > w*0.68f) {
                    if (x < w*0.84f) {
                        fuel = Math.max(0,fuel-5);
                    } else {
                        fuel = Math.min(100,fuel+5);
                    }
                } else if (!automatic && started) {
                    if (x > w/2) {
                        manualRpm = Math.min(8000,manualRpm+300);
                    } else {
                        manualRpm = Math.max(850,manualRpm-300);
                    }
                }

                invalidate();
                return true;
            }
            return true;
        }
    }
                  }
