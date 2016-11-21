package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float size = 75;
    private int[] origin = new int[2];
    private ArrayList<Point> points = new ArrayList<>();
    private ArrayList<Integer> colors = new ArrayList<>();
    private ArrayList<Float> sizes = new ArrayList<>();
    private VelocityTracker vTracker;
    private boolean useVTracker = false;
    private int color = Color.WHITE;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.TRANSPARENT);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.STROKE);
        _paint.setStrokeWidth(0);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
        imageView.getLocationOnScreen(origin);
    }

    /**
     * Sets the brush type.
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
        switch (_brushType){
            case VTracker:
                useVTracker = true;
                break;

            case Circle:
                useVTracker = false;
                size = 50;
                break;

            case Square:
                useVTracker = false;
                size = 50;
                break;
        }
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        _offScreenBitmap = Bitmap.createBitmap(_offScreenBitmap.getWidth(), _offScreenBitmap.getHeight(), _offScreenBitmap.getConfig());
        _paint = new Paint();
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
            Bitmap newBitmap = Bitmap.createBitmap(_offScreenBitmap);
            _offScreenCanvas.setBitmap(newBitmap);

            // draws all saved points
            if(points.size() > 0) {
                switch (_brushType) {
                    case Square:
                        while (points.size() > 0) {
                            _paint.setColor(colors.get(0));
                            Point point = points.get(0);
                            _offScreenCanvas.drawRect(point.x - (size / 2), point.y - (size / 2), point.x + (size / 2), point.y + (size / 2), _paint);
                            points.remove(0);
                            colors.remove(0);
                        }
                        break;

                    case Circle:
                        while (points.size() > 0) {
                            _paint.setColor(colors.get(0));
                            Point point = points.get(0);
                            _offScreenCanvas.drawCircle(point.x, point.y, size / 2, _paint);
                            points.remove(0);
                            colors.remove(0);
                        }

                    case VTracker:
                        while (points.size() > 0) {
                            _paint.setColor(colors.get(0));
                            Point point = points.get(0);
                            _offScreenCanvas.drawCircle(point.x, point.y, sizes.get(0) / 2, _paint);
                            points.remove(0);
                            colors.remove(0);
                            sizes.remove(0);
                        }
                }
            }
            _offScreenBitmap = newBitmap;
            canvas = _offScreenCanvas;
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);


    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        float x;
        float y;
        Bitmap imageViewBitmap = _imageView.getDrawingCache();
        switch(motionEvent.getActionMasked()){
            //Sets up the velocity tracker.
            case MotionEvent.ACTION_DOWN:
                if(useVTracker) {
                    if (vTracker == null) {
                        vTracker = VelocityTracker.obtain();
                    } else {
                        vTracker.clear();
                    }
                    vTracker.addMovement(motionEvent);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int numPointers = motionEvent.getPointerCount();
                //Iterates through all pointers and stores color and position in parallel ArrayLists
                for (int p = 0; p < numPointers; p++) {
                    x = motionEvent.getX(p);
                    y = motionEvent.getY(p);
                    _paint = new Paint();
                    if (getBitmapPositionInsideImageView(_imageView).contains((int) x, (int) y)) {
                        color = imageViewBitmap.getPixel((int) x, (int) y);
                    }

                    //Stores size from the first pointer's velocity for points for all other pointers.
                    if (useVTracker) {
                        vTracker.addMovement(motionEvent);
                        vTracker.computeCurrentVelocity(10);
                        Double v = Math.sqrt(Math.pow(vTracker.getXVelocity(), 2) + Math.pow(vTracker.getYVelocity(), 2));
                        if (v == null || v < 1) {
                            v = 1.0;
                        }
                        sizes.add((float) Math.min(v * 1.5, 300));
                    }

                    points.add(new Point((int) x, (int) y));
                    color = Color.argb(_alpha, Color.red(color), Color.green(color), Color.blue(color));
                    colors.add(color);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    //Returns the part of the Bitmap within the border for saving
    public Bitmap getBitmap(){
        Rect r = getBitmapPositionInsideImageView(_imageView);
        Bitmap bitmap = Bitmap.createBitmap(_offScreenBitmap,r.left,r.top,r.width(),r.height());
        return bitmap;
    }


    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

