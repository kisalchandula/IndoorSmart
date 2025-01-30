package com.kisal.indoorsmart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MapView extends View {
    private Bitmap floorMapBitmap; // Floor map image
    private final Paint paint;
    private final Paint pathPaint = new Paint(); // Paint for the path
    private float pointX = -1; // Position of the draggable point
    private float pointY = -1;
    private boolean dragging = false; // Tracks if the user is dragging the point
    boolean isStartPointSelectionEnabled = false;

    private final List<float[]> pathPoints = new ArrayList<>(); // List to store path coordinates

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();

        // Initialize user path drawing
        pathPaint.setColor(0xFF0000FF); // Blue color for the path
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(5); // Thickness of the path
        pathPaint.setAntiAlias(true);
    }

    public void setFloorMapBitmap(Bitmap bitmap) {
        this.floorMapBitmap = bitmap;
        invalidate(); // Redraw the canvas
    }

    public void enableStartPointSelection() {
        isStartPointSelectionEnabled = true;
        Toast.makeText(getContext(), "Tap on the map to set the start point.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Check if the floor map is null
        if (floorMapBitmap == null) {
            paint.setColor(0xFF000000); // Black color
            paint.setTextSize(50); // Set text size
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setStyle(Paint.Style.FILL);

            canvas.drawText("Upload map here", getWidth() / 2, getHeight() / 2, paint);
            return;
        }

        // Draw the floor map scaled to fit the canvas
        @SuppressLint("DrawAllocation") Matrix matrix = new Matrix();
        float scaleX = (float) getWidth() / floorMapBitmap.getWidth();
        float scaleY = (float) getHeight() / floorMapBitmap.getHeight();
        float scale = Math.min(scaleX, scaleY);

        float offsetX = (getWidth() - (floorMapBitmap.getWidth() * scale)) / 2;
        float offsetY = (getHeight() - (floorMapBitmap.getHeight() * scale)) / 2;

        matrix.setScale(scale, scale);
        matrix.postTranslate(offsetX, offsetY);

        canvas.drawBitmap(floorMapBitmap, matrix, paint);

        // Draw the traveled path
        if (!pathPoints.isEmpty()) {
            @SuppressLint("DrawAllocation") Path path = new Path();
            float[] start = pathPoints.get(0);
            path.moveTo(start[0], start[1]); // Start from the first point

            for (int i = 1; i < pathPoints.size(); i++) {
                float[] point = pathPoints.get(i);
                path.lineTo(point[0], point[1]); // Draw lines to the next points
            }
            canvas.drawPath(path, pathPaint);
        }

        // Draw the position indicator
        if (pointX >= 0 && pointY >= 0) {
            paint.setColor(0xFFFF0000); // Red color
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pointX, pointY, 10, paint); // Draw a point at (x, y)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (pointX < 0 && pointY < 0) {
                    pointX = x;
                    pointY = y;
                    pathPoints.add(new float[]{x, y}); // Start the path
                    invalidate();
                } else if (Math.sqrt(Math.pow(x - pointX, 2) + Math.pow(y - pointY, 2)) <= 20) {
                    dragging = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    pointX = x;
                    pointY = y;
                    pathPoints.add(new float[]{x, y}); // Add points to the path
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                dragging = false;
                break;
        }
        return true;
    }

    /**
     * Updates the position of the point by adding the given offsets to the current position.
     * Also adds the new position to the traveled path.
     *
     * @param deltaX The X offset to add to the current position.
     * @param deltaY The Y offset to add to the current position.
     */
    public void updatePosition(float deltaX, float deltaY) {
        pointX += deltaX;
        pointY += deltaY;
        pathPoints.add(new float[]{pointX, pointY}); // Add to the path
        invalidate();
    }

}
