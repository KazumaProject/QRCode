/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.journeyapps.barcodescanner;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class ViewfinderView extends View {
    protected static final String TAG = ViewfinderView.class.getSimpleName();

    protected static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    protected static final long ANIMATION_DELAY = 80L;
    protected static final int CURRENT_POINT_OPACITY = 0xA0;
    protected static final int MAX_RESULT_POINTS = 20;
    protected static final int POINT_SIZE = 6;
    protected boolean maskVisibility;
    protected boolean laserVisibility2;

    protected final Paint paint;
    protected Bitmap resultBitmap;
    protected int maskColor;
    protected final int resultColor;
    protected final int laserColor;

    protected final int targetColor;

    protected final int resultPointColor;
    protected boolean laserVisibility;
    protected int scannerAlpha;
    protected List<ResultPoint> possibleResultPoints;
    protected List<ResultPoint> lastPossibleResultPoints;
    protected CameraPreview cameraPreview;

    // Cache the framingRect and previewSize, so that we can still draw it after the preview
    // stopped.
    protected Rect framingRect;
    protected Size previewSize;

    public CameraPreview getCameraPreview(){
        return this.cameraPreview;
    }

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Resources resources = getResources();

        // Get set attributes on view
        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.zxing_finder);

        this.maskColor = attributes.getColor(R.styleable.zxing_finder_zxing_viewfinder_mask,
                resources.getColor(R.color.zxing_viewfinder_mask));
        this.resultColor = attributes.getColor(R.styleable.zxing_finder_zxing_result_view,
                resources.getColor(R.color.zxing_result_view));
        this.laserColor = attributes.getColor(R.styleable.zxing_finder_zxing_viewfinder_laser,
                resources.getColor(R.color.zxing_laser));
        this.targetColor = attributes.getColor(R.styleable.zxing_finder_zxing_viewfinder_laser,
                resources.getColor(R.color.zxing_off_white));
        this.resultPointColor = attributes.getColor(R.styleable.zxing_finder_zxing_possible_result_points,
                resources.getColor(R.color.zxing_possible_result_points));
        this.laserVisibility = attributes.getBoolean(R.styleable.zxing_finder_zxing_viewfinder_laser_visibility,
                true);
        this.maskVisibility = false;
        this.laserVisibility2 = false;

        attributes.recycle();

        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(MAX_RESULT_POINTS);
        lastPossibleResultPoints = new ArrayList<>(MAX_RESULT_POINTS);
    }

    public void setCameraPreview(CameraPreview view) {
        this.cameraPreview = view;

        view.addStateListener(new CameraPreview.StateListener() {
            @Override
            public void previewSized() {
                refreshSizes();
                invalidate();
            }

            @Override
            public void previewStarted() {

            }

            @Override
            public void previewStopped() {

            }

            @Override
            public void cameraError(Exception error) {

            }

            @Override
            public void cameraClosed() {

            }
        });
    }

    protected void refreshSizes() {
        if (cameraPreview == null) {
            return;
        }
        Rect framingRect = cameraPreview.getFramingRect();
        Size previewSize = cameraPreview.getPreviewSize();
        if (framingRect != null && previewSize != null) {
            this.framingRect = framingRect;
            this.previewSize = previewSize;
        }
    }

    private void drawSmallTarget(Canvas canvas, Paint paint, Rect rect){
        Path path = new Path();
        float margin = 40f;
        //Left Top
        path.rewind();
        path.moveTo((float) (rect.left + margin), (float) (rect.top));
        path.lineTo((float) (rect.left), (float) (rect.top));
        path.lineTo((float) (rect.left), (float) (rect.top + margin));
        canvas.drawPath(path,paint);
        path.close();
        // Left Bottom
        path.rewind();
        path.moveTo((float) (rect.left + margin), (float) (rect.bottom));
        path.lineTo((float) (rect.left), (float) (rect.bottom));
        path.lineTo((float) (rect.left), (float) (rect.bottom - margin));
        paint.setPathEffect(new CornerPathEffect(90f));
        canvas.drawPath(path,paint);
        path.close();
        //Right Top
        path.rewind();
        path.moveTo((float) (rect.right - margin), (float) (rect.top));
        path.lineTo((float) (rect.right), (float) (rect.top));
        path.lineTo((float) (rect.right), (float) (rect.top + margin));
        canvas.drawPath(path,paint);
        path.close();
        //Right Bottom
        path.rewind();
        path.moveTo((float) (rect.right - margin), (float) (rect.bottom));
        path.lineTo((float) (rect.right), (float) (rect.bottom));
        path.lineTo((float) (rect.right), (float) (rect.bottom - margin));
        canvas.drawPath(path,paint);
        path.close();
    }


    @Override
    public void onDraw(Canvas canvas) {
        refreshSizes();
        if (framingRect == null || previewSize == null) {
            return;
        }

        final Rect frame = framingRect;
        final Size previewSize = this.previewSize;


        if (resultPoints != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            PathEffect cornerPathEffect = new CornerPathEffect(90);
            paint.setColor(targetColor);
            paint.setAlpha(CURRENT_POINT_OPACITY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setPathEffect(cornerPathEffect);
            paint.setStrokeWidth(10f);
            if (resultPoints.size() >= 3){
                @SuppressLint("DrawAllocation") Rect rect = new Rect(
                        (int)(resultPoints.get(0).getX() - 50),
                        (int)(resultPoints.get(1).getY() - 30),
                        (int)(resultPoints.get(2).getX() + 80),
                        (int)(resultPoints.get(0).getY() + 100)
                );

                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setPathEffect(null);
                paint.setStrokeWidth(8f);
                canvas.drawRoundRect(
                        rect.left + 2,rect.top + 2,rect.right - 2,rect.bottom - 2,20f,20f,paint
                );

                @SuppressLint("DrawAllocation") Path path3 = new Path();
                paint.setPathEffect(null);
                paint.setStyle(Paint.Style.FILL);
                path3.addRoundRect(rect.left,rect.top,rect.right,rect.bottom,20f,20f,Path.Direction.CW);
                path3.setFillType(Path.FillType.INVERSE_WINDING);
                paint.setColor(maskColor);
                canvas.drawPath(path3,paint);

            } else {
                @SuppressLint("DrawAllocation") Rect rect = new Rect(
                        (int)(resultPoints.get(0).getX() - 50),
                        (int)(resultPoints.get(1).getY() - 30),
                        (int)(resultPoints.get(1).getX() + 80),
                        (int)(resultPoints.get(0).getY() + 120)
                );

                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setPathEffect(null);
                paint.setStrokeWidth(8f);
                canvas.drawRoundRect(
                        rect.left + 2,rect.top + 2,rect.right - 2,rect.bottom - 2,20f,20f,paint
                );
                @SuppressLint("DrawAllocation") Path path4 = new Path();
                paint.setPathEffect(null);
                paint.setStyle(Paint.Style.FILL);
                path4.addRoundRect(rect.left,rect.top,rect.right,rect.bottom,20f,20f,Path.Direction.CW);
                path4.setFillType(Path.FillType.INVERSE_WINDING);
                paint.setColor(maskColor);
                canvas.drawPath(path4,paint);
            }


        } else {

            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(6f);

            if (maskVisibility){
                if (!isShow){
                    @SuppressLint("DrawAllocation") Path path2 = new Path();
                    paint.setPathEffect(null);
                    path2.addRoundRect(frame.left,frame.top,frame.right,frame.bottom,20f,20f,Path.Direction.CW);
                    path2.setFillType(Path.FillType.INVERSE_WINDING);
                    paint.setColor(maskColor);
                    canvas.drawPath(path2,paint);
                }
            }else {
                if (rRectVisibility){
                    paint.setPathEffect(null);
                    paint.setColor(maskColor);
                    //canvas.drawRoundRect(frame.left,frame.top,frame.right,frame.bottom,20f,20f,paint);
                }

            }

            if (laserVisibility2) {
                paint.setColor(laserColor);
                paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
                scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
                final int middle = frame.height() / 2 + frame.top;
                canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
            }

            final float scaleX = this.getWidth() / (float) previewSize.width;
            final float scaleY = this.getHeight() / (float) previewSize.height;

            // draw the last possible result points
            if (!lastPossibleResultPoints.isEmpty()) {
                paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                paint.setColor(resultPointColor);
                float radius = POINT_SIZE / 2.0f;
                for (final ResultPoint point : lastPossibleResultPoints) {
                    canvas.drawCircle(
                             (int) (point.getX() * scaleX),
                             (int) (point.getY() * scaleY),
                            radius, paint
                    );
                }
                lastPossibleResultPoints.clear();
            }

            // draw current possible result points
            if (!possibleResultPoints.isEmpty()) {
                paint.setAlpha(CURRENT_POINT_OPACITY);
                paint.setColor(resultPointColor);
                for (final ResultPoint point : possibleResultPoints) {
                    canvas.drawCircle(
                            (int) (point.getX() * scaleX),
                            (int) (point.getY() * scaleY),
                            POINT_SIZE, paint
                    );
                }

                // swap and clear buffers
                final List<ResultPoint> temp = possibleResultPoints;
                possibleResultPoints = lastPossibleResultPoints;
                lastPossibleResultPoints = temp;
                possibleResultPoints.clear();

            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
    }

    public void drawViewfinder() {
        this.resultPoints = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param result An image of the result.
     */
    public void drawResultBitmap(Bitmap result) {
        resultBitmap = result;
        invalidate();
    }

    private List<ResultPoint> resultPoints;

    public void drawResultPointsRect(List<ResultPoint> points){
        if (points == null){
            resultPoints = null;
            invalidate();
        }else {
            if (resultPoints == null){
                resultPoints = points;
                invalidate();
            }

        }

    }

    /**
     * Only call from the UI thread.
     *
     * @param point a point to draw, relative to the preview frame
     */
    public void addPossibleResultPoint(ResultPoint point) {
        if (possibleResultPoints.size() < MAX_RESULT_POINTS)
            possibleResultPoints.add(point);
    }

    public void setMaskVisibility(boolean visibility) {
        this.maskVisibility = visibility;
    }

    public void setLaserVisibility(boolean visible) {
        this.laserVisibility2 = visible;
    }

    private boolean rRectVisibility = true;

    public void shouldRoundRectMaskVisible(boolean visibility){
        this.rRectVisibility = visibility;
    }

    private Boolean isShow = false;

    public void isResultShown(boolean isShow){
        this.isShow = isShow;
    }

}
