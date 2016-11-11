package com.insyde.factorytest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;

public class Runin2dActivity extends Activity {
	static final String TAG = "2D";
    private int NUMBER_OF_SPRITES = 200;
    private SampleView mSampleView;

    private int TIME_TO_STRESS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		RunInLogUtil.writeLog(TAG, "test start");
        TIME_TO_STRESS = getIntent().getIntExtra("testTime", 20) * 60 * 1000;
        new ResultThread().start();

        mSampleView = new SampleView(this.getApplicationContext());
        setContentView(mSampleView);
    }

    class ResultThread extends Thread {
        public void run() {
            SystemClock.sleep(TIME_TO_STRESS);

			RunInLogUtil.writeLog(TAG, "test pass");
            setResult(RESULT_OK);
            finish();
        }
    }

    private class SampleView extends View {
        DrawObject[] mDrawable = new DrawObject[NUMBER_OF_SPRITES];

        public SampleView(Context context) {
            super(context);
            setFocusable(true);
            setFocusableInTouchMode(true);
            Drawable d = context.getResources().getDrawable(R.drawable.ic_launcher);
            for (int i = 0; i < NUMBER_OF_SPRITES; i++) {
                double vx, vy;
                vx = Math.random() * 10.0 + 2.0;
                vy = Math.random() * 10.0 + 2.0;
                if (Math.random() > 0.5)
                    vx = -vx;
                if (Math.random() > 0.5)
                    vy = -vy;
                mDrawable[i] = new DrawObject(Math.random() * 800, Math.random() * 500, vx, vy, d);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);
            for (int i = 0; i < NUMBER_OF_SPRITES; i++) {
                mDrawable[i].nextStep(canvas.getWidth(), canvas.getHeight());
                mDrawable[i].draw(canvas);
            }
            invalidate();
        }
    }

    public class DrawObject {
        private Drawable mDrawable;
        private double mX, mY, mVX, mVY;

        public DrawObject(double x, double y, double vx, double vy, Drawable drawable) {
            mX = x;
            mY = y;
            mVX = vx;
            mVY = vy;

            mDrawable = drawable;
            mDrawable.setBounds((int) mX, (int) mY, mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
        }

        public Drawable getDrawable() {
            return mDrawable;
        }

        public void setDrawable(Drawable mDrawable) {
            this.mDrawable = mDrawable;
        }

        public void nextStep(int width, int height) {
            if (mX + mDrawable.getIntrinsicWidth() + mVX > width) {
                mX = width - mDrawable.getIntrinsicWidth();
                mVX = -mVX;
            } else if (mX + mVX <= 0.0) {
                mX = 0;
                mVX = -mVX;
            } else {
                mX += mVX;
            }

            if (mY + mDrawable.getIntrinsicHeight() + mVY > height) {
                mY = height - mDrawable.getIntrinsicHeight();
                mVY = -mVY;
            } else if (mY + mVY <= 0.0) {
                mY = 0;
                mVY = -mVY;
            } else {
                mY += mVY;
            }

            mDrawable.setBounds((int) mX, (int) mY, (int) mX + mDrawable.getIntrinsicWidth(), (int) mY + mDrawable.getIntrinsicHeight());
        }

        public void draw(Canvas canvas) {
            mDrawable.draw(canvas);
        }
    }

}
