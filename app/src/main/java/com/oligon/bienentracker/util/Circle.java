package com.oligon.bienentracker.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.oligon.bienentracker.R;

public class Circle extends View {

    private int mColor = Color.RED;
    private float mDimension = 0;
    private String mLabel = "";

    private TextPaint mTextPaint;
    private Paint mCirclePaint;
    private float mTextHeight;

    public Circle(Context context) {
        super(context);
        init(null, 0);
    }

    public Circle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public Circle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.Circle, defStyle, 0);

        mColor = a.getColor(R.styleable.Circle_circleColor, mColor);
        mLabel = a.getString(R.styleable.Circle_label);
        mDimension = a.getDimension(R.styleable.Circle_textSize, 12);

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(mColor);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mDimension);
        mTextPaint.setColor(getCorrespondingColor(mColor));

        mTextHeight = mTextPaint.descent() + mTextPaint.ascent();
    }

    private int getCorrespondingColor(int color) {
        if (color == 0xFF3F51B5 || color == 0xFFF44336)
            return Color.WHITE;
        else
            return Color.BLACK;
    }

    private void invalidateCirclePaint() {
        mCirclePaint.setColor(mColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;


        canvas.drawCircle(paddingLeft + contentWidth / 2,
                paddingTop + contentHeight / 2,
                contentHeight / 2, mCirclePaint);

        if (mLabel.length() > 0)
            canvas.drawText(mLabel,
                    paddingLeft + contentWidth / 2,
                    paddingTop + (contentHeight - mTextHeight) / 2,
                    mTextPaint);
    }

    public void setLabel(String label) {
        mLabel = label;
        invalidateTextPaintAndMeasurements();
    }

    private void setColor(int color) {
        mColor = ContextCompat.getColor(getContext(), color);
        invalidateCirclePaint();
    }


    public void setYear(int year) {
        year = year % 10;
        if (year == 0 || year == 5)
            setColor(R.color.beeBlue);
        else if (year == 1 || year == 6)
            setColor(R.color.beeWhite);
        else if (year == 2 || year == 7)
            setColor(R.color.beeYellow);
        else if (year == 3 || year == 8)
            setColor(R.color.beeRed);
        else
            setColor(R.color.beeGreen);
    }
}
