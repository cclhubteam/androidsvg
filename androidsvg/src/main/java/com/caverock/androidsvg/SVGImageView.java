package com.caverock.androidsvg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

import static android.content.Context.ACCESSIBILITY_SERVICE;

public class SVGImageView extends SVGBaseImageView implements View.OnTouchListener {

    private SVG svg;
    private GestureDetector gestureDetector;
    private SVGTouchListener svgTouchlistener;
    private SVGRenderedCallback svgRenderedCallback;

    private AccessibilityManager mAccessibilityManager;
    private String contentDescription;

    private static Point getPointForEvent(MotionEvent event) {
        int rawX = (int) event.getX();
        int rawY = (int) event.getY();

        return new Point(rawX, rawY);
    }

    public SVGImageView(Context context) {
        super(context);
    }

    public SVGImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SVGImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setContentDescription(String contentDescription) {
        this.contentDescription = contentDescription;
    }

    @Override
    public void setSVG(final SVG svg) {
        if (svg == null) {
            throw new IllegalArgumentException("SVG can not be null");
        }

        this.svg = svg;

        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService(ACCESSIBILITY_SERVICE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                        (int) Math.ceil(svg.getDocumentHeight()),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                svg.renderToCanvas(canvas);
                post(new Runnable() {
                    @Override
                    public void run() {
                        setImageBitmap(bitmap);
                        svgRenderedCallback.onSVGRendered();
                    }
                });
            }
        }).start();

    }

    public void setSVGTouchListener(SVGTouchListener listener) {
        svgTouchlistener = listener;
        gestureDetector = new GestureDetector(getContext(), new SVGGestureListener());
        setOnTouchListener(this);
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (mAccessibilityManager != null && mAccessibilityManager.isTouchExplorationEnabled()) {
            if (!TextUtils.isEmpty(contentDescription))
                announceForAccessibility(contentDescription);
            return onTouchEvent(event);
        } else {
            return super.onHoverEvent(event);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (svg == null || svgTouchlistener == null) {
            return true;
        }

        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }

        Point point = getPointForEvent(event);
        List<SVG.SvgObject> list = svg.getSVGObjectsByCoordinate(point);


        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_CANCEL:
                svgTouchlistener.onSVGObjectTouchUp(null, null);
            case MotionEvent.ACTION_UP:
                svgTouchlistener.onSVGObjectTouchUp(list, point);
                break;
            case MotionEvent.ACTION_MOVE:
                svgTouchlistener.onSVGObjectMove(point);
        }
        return true;
    }

    public void setSVGRenderedCallback(SVGRenderedCallback svgRenderedCallback) {
        this.svgRenderedCallback = svgRenderedCallback;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (svg != null) {
            svg.initShapesWithOffset(getWidth());
        }

    }

    private class SVGGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            if (svg != null && svgTouchlistener != null) {
                svgTouchlistener.onSVGObjectLongPress(getPointForEvent(e));
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (svg != null && svgTouchlistener != null) {
                Point point = getPointForEvent(e);
                List<SVG.SvgObject> list = svg.getSVGObjectsByCoordinate(point);
                svgTouchlistener.onSVGObjectTouch(list, point);
                return true;
            }
            return false;
        }
    }

    public interface SVGTouchListener {
        void onSVGObjectTouchUp(List<SVG.SvgObject> list, Point point);

        void onSVGObjectLongPress(Point point);

        void onSVGObjectTouch(List<SVG.SvgObject> objectList, Point touchPoint);

        void onSVGObjectMove(Point point);
    }

    public interface SVGRenderedCallback {
        void onSVGRendered();
    }
}
