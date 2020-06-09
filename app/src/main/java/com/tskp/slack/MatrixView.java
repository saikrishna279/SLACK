package com.tskp.slack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class MatrixView extends View {

    private int[] states;

    private int[] colorGradientMap;
    private boolean[] enableGradientAnimatorMap;

    private Handler handler = new Handler();

    private int tempIndex = 0;

    private Runnable spawnAnimatorRunnable = new Runnable() {
        @Override
        public void run() {
            int temp = tempIndex;
            tempIndex = 0;
            spawnAnimator(temp);
        }
    };

    private int totalNum;

    private RectF drawRect;
    private Paint drawPaint;

    private double rectSide;

    private int rows, cols;
    private int width, height;

    private int GAP = 3;

    private double startGap = 0;

    public MatrixView(Context context) {
        super(context);
        setup();
    }

    public MatrixView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public MatrixView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public MatrixView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        drawRect = new RectF();
        drawPaint = new Paint();
        this.states = new int[totalNum];

        colorGradientMap = new int[totalNum];
        enableGradientAnimatorMap = new boolean[totalNum];
    }

    public void init(int[] states, int totalNum) {
        this.totalNum = totalNum;

        if(totalNum == 0){
            invalidate();
            return;
        }

        setup();

        /*
         * Calculate Number of Rows and Columns
         */
        double ratio = ((double) Math.min(width, height)) / ((double) Math.max(width, height)); // Minimum / Maximum side
        double totalArea = width * height;
        double areaPerRect = totalArea / totalNum;
        double sidePerRect = Math.sqrt(areaPerRect) * ratio;

        Log.d("achoo", "ratio: " + ratio + " totalArea: " + totalArea + " areaPerRect: " + areaPerRect + " sidePerRect: " + sidePerRect);

        cols = (int) ((width * 1.0) / sidePerRect);
        rows = (int) ((height * 1.0) / sidePerRect);

        rectSide = (width - (cols * GAP)) / (cols * 1.0);

        startGap = ((width - (cols * rectSide)) / 2);

        Log.d("achoo", "rows: " + rows + " cols: " + cols + " side: " + rectSide + " startGap: " + startGap);

        updateSocketStates(states);
    }

    public void updateSocketStates(int[] states) {
        if(states.length == 0){
            this.totalNum = 0;
            this.states = new int[0];
        }else
            System.arraycopy(states, 0, this.states, 0, Math.min(states.length, this.states.length)); // Copy array data

        invalidate();
    }

    int rowindex, colindex, i;

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        /**
         *
         * 0 - Grey  - null socket
         * 1 - Red - Creating socket
         * 2 - Green - Socket open and running
         * 10 - White flash - Socket pinged
         */

        for (i = 0; i < totalNum; i++) {

            rowindex = (i / cols);
            colindex = (i % cols);


            drawRect.top = ((float) (rowindex * rectSide)) + GAP + (float)startGap;
            drawRect.left = ((float) (colindex * rectSide)) + GAP + (float)startGap;
            drawRect.bottom = (float) (drawRect.top + rectSide - GAP);
            drawRect.right = (float) (drawRect.left + rectSide - GAP);


            switch (states[i]) {
                case 0:
                    drawPaint.setColor(0x11ffffff); // Grey
                    break;
                case 1:
                    drawPaint.setColor(0xffff0000); // Red
                    break;
                case 2:
                    if (!enableGradientAnimatorMap[i]) { // If not animating
                        drawPaint.setColor(0xff00ffbf); // Green
                    } else { // If animating values
                        if (colorGradientMap[i] != 0) {
                            drawPaint.setColor(colorGradientMap[i]); // Animate color change
                        } else
                            drawPaint.setColor(0xff00ffbf);
                    }
                    break;
                case 10:
                    if (!enableGradientAnimatorMap[i]) {
                        tempIndex = i;
                        handler.post(spawnAnimatorRunnable);
                        enableGradientAnimatorMap[i] = true; // Indicate to gradient animator thread to start animating colors
                        states[i] = 2;
                    }
                    break;
            }
            canvas.drawRect(drawRect, drawPaint);

        }
    }

    private void spawnAnimator(final int i) {

        ValueAnimator animator = ValueAnimator.ofArgb(0xffffffff, 0xff00ffbf).setDuration(250);


        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                colorGradientMap[i] = (int) valueAnimator.getAnimatedValue(); // Update gradient value in gradient map
                invalidate(); // Try to invalidate view every time color is animated.
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                enableGradientAnimatorMap[i] = false;
            }
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        Log.d("achoo", "h: " + height + " w: " + width);

        setMeasuredDimension(width, height);
    }

}
