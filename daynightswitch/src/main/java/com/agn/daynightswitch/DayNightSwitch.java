package com.agn.daynightswitch;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by VimalCvs on 02/11/2020.
 */

public class DayNightSwitch extends View implements Animator.AnimatorListener {

    private boolean is_animating = false;

    private GradientDrawable light_back_drawable;
    private BitmapDrawable dark_back_bitmap;
    private BitmapDrawable sun_bitmap;
    private BitmapDrawable moon_bitmap;
    private BitmapDrawable clouds_bitmap;

    private float value;
    private boolean is_night;
    private int duration;


    private DayNightSwitchListener listener;

    private DayNightSwitchAnimListener anim_listener;


    public DayNightSwitch(Context context) {
        this(context, null, 0);
    }

    public DayNightSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DayNightSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);

        value = 0;
        is_night = false;
        duration = 500;

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
        light_back_drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.parseColor("#"+Integer.toHexString(context.getResources().getColor(R.color.colorAccent))),
                        Color.parseColor("#"+Integer.toHexString(context.getResources().getColor(R.color.switch_light)))
        });

        light_back_drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        dark_back_bitmap = (BitmapDrawable) getContext().getResources().getDrawable(R.drawable.dark_background);
        sun_bitmap = (BitmapDrawable) getContext().getResources().getDrawable(R.drawable.img_sun);
        moon_bitmap = (BitmapDrawable) getContext().getResources().getDrawable(R.drawable.img_moon);
        clouds_bitmap = (BitmapDrawable) getContext().getResources().getDrawable(R.drawable.img_clouds);
    }

    public void toggle(){
        if (!is_animating) {
            is_animating = true;
            is_night = !is_night;
            if (listener != null)
                listener.onSwitch(is_night);
            startAnimation();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int space = getWidth() - getHeight();

        dark_back_bitmap.setBounds(0, 0, getWidth(), getHeight());
        dark_back_bitmap.setAlpha((int) (value * 255));
        dark_back_bitmap.draw(canvas);

        light_back_drawable.setCornerRadius(getHeight() >> 1);
        light_back_drawable.setBounds(0, 0, getWidth(), getHeight());
        light_back_drawable.setAlpha(255 - ((int) (value * 255)));
        light_back_drawable.draw(canvas);


        moon_bitmap.setBounds((space) - (int) (value * space)
                , 0
                , getWidth() - (int) (value * space)
                , getHeight());
        moon_bitmap.setAlpha((int) (value * 255));
        moon_bitmap.getBitmap();

        sun_bitmap.setBounds((space) - (int) (value * space)
                , 0
                , getWidth() - (int) (value * space)
                , getHeight());
        sun_bitmap.setAlpha(255 - ((int) (value * 255)));

        moon_bitmap.draw(canvas);
        sun_bitmap.draw(canvas);

        moon_bitmap.setAlpha((int) (value * 255));

        int clouds_bitmap_alpha = value <= 0.5 ? (255 - ((int) (((value - 0.5) * 2) * 255))) : 0;
        clouds_bitmap.setAlpha(clouds_bitmap_alpha);

        int clouds_bitmap_left = (int) ((getHeight() / 2) - (value * (getHeight() / 2)));
        clouds_bitmap.setBounds(clouds_bitmap_left
                , 0
                , clouds_bitmap_left + getHeight()
                , getHeight());
        clouds_bitmap.draw(canvas);

    }

    private void startAnimation() {
        final ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        if (value == 1)
            va.setFloatValues(1, 0);

        va.setDuration(duration);
        va.addListener(this);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                value = (float) animation.getAnimatedValue();
                if (anim_listener != null)
                    anim_listener.onAnimValueChanged(value);
                invalidate();
            }
        });
        va.start();
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if (anim_listener != null)
            anim_listener.onAnimStart();
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        is_animating = false;
        if (anim_listener != null)
            anim_listener.onAnimEnd();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    public void setIsNight(boolean is_night) {
        this.is_night = is_night;
        value = is_night ? 1 : 0;
        invalidate();
        if (listener != null)
            listener.onSwitch(is_night);

    }

    public boolean isNight() {
        return is_night;
    }

    public void setListener(DayNightSwitchListener listener) {
        this.listener = listener;
    }


    public void setAnimListener(DayNightSwitchAnimListener animListener) {
        this.anim_listener = animListener;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
