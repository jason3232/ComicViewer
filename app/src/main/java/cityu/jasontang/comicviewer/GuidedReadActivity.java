package cityu.jasontang.comicviewer;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
//import org.opencv.core.Core;
import org.opencv.core.Core;
import org.opencv.core.CvType;
//import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
//import org.opencv.core.MatOfKeyPoint;
//import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
//import org.opencv.core.MatOfPoint2f;
//import org.opencv.core.Point;
import org.opencv.core.Point;
import org.opencv.core.Rect;
//import org.opencv.core.Range;
//import org.opencv.core.Scalar;
//import org.opencv.features2d.FeatureDetector;
//import org.opencv.features2d.MSER;
//import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
//import java.util.Iterator;
import java.util.List;
import java.lang.Math;
import java.util.Random;
//import java.util.Random;

//import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class GuidedReadActivity extends AppCompatActivity {
    private Uri imgUri = null;
    private List<Bitmap> panels;
    private int currentPanel = 0;
    private ImageView imgView;
    private int mode; // 0: foursplit; 1: findpanel
    private boolean otsu_threshold;
    private int threshold_value;
    private boolean split_panel;
    private boolean manual_split_panel;
    private int split_panel_area;
    private int split_panel_rows;
    private int split_panel_cols;
    private boolean keep_panel;
    private boolean four_sort;
    private boolean four_sort_merge;
//    private Bitmap imgBitmap = null;
//    private boolean loaded = false;
//    private boolean refined = false;
//    private List<Bitmap> refinedPanels;
//    private int currRefinedPanel;


    /* Initialize OpenCV */
    static {
        if (OpenCVLoader.initDebug()) {
            Log.i("OpenCV Init: ", "OpenCV initialize success");
        } else {
            Log.i("OpenCV Init: ", "OpenCV initialize failed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_read);

        setFullscreen();

        /* Action Bar setup */
        Toolbar thisToolBar = findViewById(R.id.toolbar);
        this.setSupportActionBar(thisToolBar);
        ActionBar ab = this.getSupportActionBar();
        if (ab != null) {
            setTitle("Guided Read");
            ab.setDisplayHomeAsUpEnabled(true);
        }

        /* Get intent package */
        Intent intent = getIntent();
        initConfig(intent);

        panels = new ArrayList<>();
    }

    /* Set Immersive Mode for full screen */
    private void setFullscreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        decorView.setBackgroundColor(0xFF212121);

        /* Return to immersive mode after 2 second */
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
                        }}, 2000);
                }
            }
        });
    }

    /* Get config parameters from intent */     //Should save SharedPreferences for this maybe?
    private void initConfig(Intent intent) {
        imgUri = intent.getParcelableExtra("clickedImg");
        mode = intent.getIntExtra("Mode", 0);
        if(mode == 1) {
            setTitle("Find Panel");
        }
        else {
            setTitle("Four Split");
        }
        otsu_threshold = intent.getBooleanExtra("Otsu", true);
        if(!otsu_threshold) {
            threshold_value = intent.getIntExtra("thresholdValue", 127);
            Log.d("Otsu","Using Binary Threshold");
            Log.d("Otsu","Value: "+threshold_value);
        }
        else {
            Log.d("Otsu","Using Otsu Threshold");
        }
        split_panel = intent.getBooleanExtra("Split", false);
        if(split_panel) {
            manual_split_panel = intent.getBooleanExtra("Manual", false);
            if(manual_split_panel) {
                split_panel_area = intent.getIntExtra("Area", 40);
                split_panel_rows = intent.getIntExtra("Rows", 2);
                split_panel_cols = intent.getIntExtra("Cols", 2);
            }
            keep_panel = intent.getBooleanExtra("keepPanel", false);
            if( split_panel_rows==1 && split_panel_cols==1 ) {
                //One row and one cols mean no split
                split_panel=false;
            }
        }
        four_sort = intent.getBooleanExtra("Four", false);
        if(four_sort) {
            four_sort_merge = intent.getBooleanExtra("FourMerge", true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setPage();
    }

    /* Process the selected Page */
    private void setPage() {
        Bitmap imgBitmap = null;
        /* Generate Bitmap from Uri */
        try {
            //Should resample the bitmap for optimization
            BitmapFactory.Options imgOption = new BitmapFactory.Options();
            imgOption.inDither = true;
            imgOption.inPreferredConfig = Bitmap.Config.ARGB_8888;
            imgBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imgUri),null, imgOption);
        }
        //Need better exception handling
        catch (IOException e) {
            e.printStackTrace();
        }

        /* Process Bitmap according to mode */
        if (imgBitmap != null) {
            switch (mode) {
                case 0:
                    fourSplit(imgBitmap);
                    break;
                case 1:
                    findPanel(imgBitmap);
                    break;
            }
        }
        else {
            //Note: use error handling for this maybe?
            Toast.makeText(GuidedReadActivity.this, "Error processing image.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void nextPage(View view) {
       if(currentPanel < panels.size() -1 ) {
            currentPanel += 1;
            imgView.setImageBitmap(panels.get(currentPanel));
        }
        else {
            Toast.makeText(GuidedReadActivity.this, "This is the last panel.", Toast.LENGTH_SHORT).show();
        }
    }

    public void previousPage(View view) {
        if(currentPanel > 0) {
            currentPanel -= 1;
            imgView.setImageBitmap(panels.get(currentPanel));
        }
        else {
            Toast.makeText(GuidedReadActivity.this, "This is the first panel.", Toast.LENGTH_SHORT).show();
        }
    }

    /* Split Bitmap to 4 equal parts and set on ImageView */
    private void fourSplit(Bitmap imgBitmap) {
        int rows = 2;
        int cols = 2;
        panels = splitImage(imgBitmap, rows, cols);

        //Set panel to imageView
        imgView = findViewById(R.id.imgView);
        imgView.setImageBitmap(panels.get(currentPanel));
    }

    /* Split Bitmap according to a set number of rows and cols */
    private List<Bitmap> splitImage(Bitmap imgBitmap, int rows, int cols) {
        List<Bitmap> bitmapList = new ArrayList<>();
        int width = imgBitmap.getWidth() / rows;
        int height = imgBitmap.getHeight() / cols;

        //From Top to Bottom
        for (int i = 0; i < cols; i++) {
            //From Right to Left
            for (int j = rows-1; j >= 0; j--) {
                Bitmap bm = Bitmap.createBitmap(imgBitmap, j * width, i * height, width, height);
                bitmapList.add(bm);
            }
        }

        return bitmapList;
    }

    /* Find Panel using findContour, split Bitmap and set ImageView */          //Should split into sub-classes?
    private void findPanel(Bitmap imgBitmap) {
        /* For double spread page images */
        if(imgBitmap.getWidth() > imgBitmap.getHeight()) {
            Bitmap leftBitmap = Bitmap.createBitmap(imgBitmap, 0, 0, imgBitmap.getWidth()/2 , imgBitmap.getHeight());
            Bitmap rightBitmap = Bitmap.createBitmap(imgBitmap, imgBitmap.getWidth()/2, 0, imgBitmap.getWidth()/2 , imgBitmap.getHeight());
            //Handle the right page first
            findPanel(rightBitmap);
            findPanel(leftBitmap);
            return;
        }

        /* Convert Page Bitmap to Mat */
        Mat imgMat = new Mat(imgBitmap.getWidth(), imgBitmap.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(imgBitmap, imgMat);

        /* Convert Mat to Greyscale */
        Mat gryMat = new Mat(imgBitmap.getWidth(), imgBitmap.getHeight(), CvType.CV_8UC1);
        Imgproc.cvtColor(imgMat, gryMat, Imgproc.COLOR_RGB2GRAY);

        boolean isBlackBG = checkBlackBackground(gryMat);
        if(isBlackBG) {
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
            if(!otsu_threshold) {
                Imgproc.threshold(gryMat, gryMat, threshold_value, 255,  Imgproc.THRESH_BINARY);
            }
            else {
                Imgproc.threshold(gryMat, gryMat, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);
            }
            Imgproc.morphologyEx(gryMat, gryMat, Imgproc.MORPH_CLOSE, kernel);
//            Imgproc.dilate(gryMat, gryMat, kernel);
        }
        else {
            /* Threshold the image */
            if (!otsu_threshold) {
                Imgproc.threshold(gryMat, gryMat, threshold_value, 255, Imgproc.THRESH_BINARY_INV);
            } else {
                Imgproc.threshold(gryMat, gryMat, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY_INV);
            }
        }

        /* Find Contours */
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(gryMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        /* Calculate Image Dimensions */
        int imgWidth = gryMat.width();
        int imgHeight = gryMat.height();
        double imgArea = (double) imgWidth * imgHeight;

        /* Calculate boundingRect set */
        List<Rect> boundRect = new ArrayList<>();
        double noiseThreshold = imgArea * 0.0001; //Threshold for noise bits
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area > noiseThreshold) {
                boundRect.add(Imgproc.boundingRect(new MatOfPoint(contours.get(i).toArray())));
            }
        }

//        /* Debug draw set of BoundingRect */
//        Mat drawing = Mat.zeros(imgMat.size(), CvType.CV_8UC3);
//        for (int i = 0; i < boundRect.size(); i++) {
//            Rect bRect = boundRect.get(i);
//            Imgproc.rectangle(drawing, bRect.tl(), bRect.br(), new Scalar(255, 255, 255), 1);
//        }

        /* Filter the boundingRects */
        //Calculate Threshold for removing small boundingRect
        double areaThreshold = imgArea * 0.005;
        //Calculate Threshold for merging intersecting boundingRects while keeping touching boundingRects separated
        double interThreshold = imgArea * 0.001;
        //Process the List N times (N = boundRect.size() maybe?) (Need more optimization)
        int n = 25;
        filterRect(boundRect, areaThreshold, interThreshold, n);

        /* Return if no acceptable panels are found */
        if(boundRect.size() < 1) {
            Toast.makeText(GuidedReadActivity.this, "No Panel detected, adjust your settings.", Toast.LENGTH_SHORT).show();
            imgMat.release();
            gryMat.release();
            hierarchy.release();
            finish();
        }

        /* Sort the boundingRect List */
        if(four_sort) {
            if(four_sort_merge)
                mergeFourKomaStrip(boundRect);
            double widthThreshold = imgWidth * 0.01;
            sortRects(boundRect, widthThreshold);
        }
        else {
            sortRects(boundRect);
        }

//        /* Debug Draw ROIs boundingRect Pre Split */
//        Random rng = new Random(12345);
//        Mat preSplit = Mat.zeros(imgMat.size(), CvType.CV_8UC3);
//        for (Rect drawRect : boundRect) {
//            Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
//            Imgproc.rectangle(preSplit, drawRect.tl(), drawRect.br(), color, -1);
//        }
//        for (Rect drawRect : boundRect) {
//            Imgproc.rectangle(preSplit, drawRect.tl(), drawRect.br(), new Scalar(255, 255, 255), 2);
//        }
//        for (int i=0; i<boundRect.size(); i++) {
//            Rect r = boundRect.get(i);
//            Point center = new Point ( (r.tl().x+(r.width*0.5)) , (r.tl().y+(r.height*0.5)));
//            Imgproc.putText(preSplit, ""+(i+1), center, Core.FONT_HERSHEY_COMPLEX, 3, new Scalar(255,255,255), 5);
//        }

        /* Split Panels again if option is set */
        if(split_panel) {
            /* Split with manual settings if set */
            if(manual_split_panel) {
                double areaPercent = split_panel_area * 0.01;
                double splitAreaThreshold = imgArea * areaPercent;
                splitPanel(boundRect, splitAreaThreshold, split_panel_cols, split_panel_rows);
            }
            /* Auto determine split settings */
            else {
                int orientation = getResources().getConfiguration().orientation;
                splitPanel(boundRect, imgHeight, imgWidth, orientation);
            }
        }

//        /* Debug Draw ROIs boundingRect Post Split */
//        Mat postSplit = Mat.zeros(imgMat.size(), CvType.CV_8UC3);
//        for (Rect drawRect : boundRect) {
//            Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
//            Imgproc.rectangle(postSplit, drawRect.tl(), drawRect.br(), color, -1);
//        }
//        for (Rect drawRect : boundRect) {
//            Imgproc.rectangle(postSplit, drawRect.tl(), drawRect.br(), new Scalar(255, 255, 255), 2);
//        }
//        for (int i=0; i<boundRect.size(); i++) {
//            Rect r = boundRect.get(i);
//            Point center = new Point ( (r.tl().x+(r.width*0.5)) , (r.tl().y+(r.height*0.5)));
//            Imgproc.putText(postSplit, ""+(i+1), center, Core.FONT_HERSHEY_COMPLEX, 3, new Scalar(255,255,255), 5);
//        }

//        /* Debug print bounding boxes */
//        Bitmap boundBitmap = Bitmap.createBitmap(drawing.cols(), drawing.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(drawing, boundBitmap);
//        Bitmap preSplitBitmap = Bitmap.createBitmap(preSplit.cols(), preSplit.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(preSplit, preSplitBitmap);
//        Bitmap postSplitBitmap = Bitmap.createBitmap(postSplit.cols(), postSplit.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(postSplit, postSplitBitmap);
//        panels.add(imgBitmap);
//        panels.add(preSplitBitmap);
//        panels.add(boundBitmap);
//        panels.add(postSplitBitmap);
//        panels.add(imgBitmap);

        /* Release debug Mat */
//        drawing.release();
//        preSplit.release();
//        postSplit.release();

        /* Generate and convert ROI to Bitmap */
        for (Rect rect : boundRect) {
            Mat m = new Mat(imgMat, rect);
            Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(m, bm);
            panels.add(bm);
            m.release();
        }

        /* Release Mat */
        imgMat.release();
        gryMat.release();
        hierarchy.release();

        /* Set panel to imageView */
        imgView = findViewById(R.id.imgView);
        imgView.setImageBitmap(panels.get(currentPanel));
    }

    /* Check if background is black */
    private boolean checkBlackBackground(Mat tgtMat) {
        /* Crop the two edge */
        Rect leftBorder = new Rect(0,0,5, tgtMat.cols());
        Mat leftBorderMat = new Mat(tgtMat, leftBorder);
        Imgproc.threshold(leftBorderMat, leftBorderMat, 20, 255,  Imgproc.THRESH_BINARY);

        int right = (int) tgtMat.size().width - 5;
        Rect rightBorder = new Rect(right,0,5, tgtMat.cols());
        Mat rightBorderMat = new Mat(tgtMat, rightBorder);
        Imgproc.threshold(rightBorderMat, rightBorderMat, 20, 255,  Imgproc.THRESH_BINARY);

        /* Count white pixel to see if the background is black */
        return Core.countNonZero(leftBorderMat) < tgtMat.cols() || Core.countNonZero(rightBorderMat) < tgtMat.cols();
    }

    /* Filter a Rect List according to thresholds for a number of iterations */
    private void filterRect(List<Rect> rectList, double areaThreshold, double interThreshold, int iteration) {
        //TODO: Optimize this
        //Maybe iteration not needed? Need more testing
        for (int n = 0; n < iteration; n++) {
            int size = rectList.size();
            for (int i = 0; i < size; i++) {
                Rect rectA = rectList.get(i);
                int intersectCnt = 0;
                //Remove if smaller than threshold again
                if ( rectA.area() < areaThreshold ) {
                    rectList.remove(rectA);
                    size--;
                    i--;
                    continue;
                }
                //Compare rectA against every other rect in the List, to be optimized?
                for (int j = i+1; j < size; j++) {
                    Rect rectB = rectList.get(j);
                    //Get intersect area Rect
                    Rect interRect = intersect(rectA, rectB);
                    //Remove rectA if it is completely inside rectB and vice versa
                    if (interRect.equals(rectA)) {
                        rectList.remove(rectA);
                        size--;
                        i--;
                        break;
                    }
                    else if (interRect.equals(rectB)) {
                        rectList.remove(rectB);
                        size--;
                        j--;
                    }
                    //Check if intersecting area bigger than threshold
                    else if (interRect.area() > interThreshold) {
                        /* Try to group fragmented elements and overlapping panels to preserve reading flow */
                        if( (interRect.width < rectB.width *0.5 && interRect.height < rectB.height * 0.5) )
                            intersectCnt+=1;
                        if( (rectA.area() < areaThreshold * 10 || rectB.area() < areaThreshold * 10) )
                            intersectCnt+=1;
                        if( (interRect.area() > rectA.area()-interRect.area() || interRect.area() > rectB.area()-interRect.area() ) )
                            intersectCnt+=2;
                        if ( intersectCnt > 1) {
                            Rect mergeRect = merge(rectA, rectB);
                            rectList.add(mergeRect);
                            rectList.remove(rectA);
                            rectList.remove(rectB);
                            size--;
                            i--;
                            break;
                        }
                    }
                }
            }
        }
    }

    /* Merge 4-Koma Panels to a Strip */
    private void mergeFourKomaStrip(List<Rect> rectList) {
        int size = rectList.size();
        for (int i = 0; i < size; i++) {
            Rect rectA = rectList.get(i);
            int ctr = 1;
            for (int j = i+1; j < size; j++) {
                if(ctr >= 4) {
                    break;
                }
                Rect rectB = rectList.get(j);
                if(Math.abs(rectA.tl().x - rectB.tl().x) < rectA.width * 0.01 && Math.abs(rectA.width - rectB.width) < rectA.width * 0.01) {
                    Rect m = merge(rectA, rectB);
                    rectList.add(i, m);
                    rectList.remove(rectA);
                    rectList.remove(rectB);
                    rectA = rectList.get(i);
                    j--;
                    size--;
                    ctr++;
                }
            }
        }
    }

    /* Sort a Rect list from Right to Left, Top to Bottom */
    private void sortRects(List<Rect> rectList, final double widthThreshold) {
        Collections.sort(rectList, new Comparator<Rect>() {
            @Override
            public int compare(Rect o1, Rect o2) {
                //If on the same col, sort by Y
                if(Math.abs(o1.tl().x - o2.tl().x) < widthThreshold) {
                    int result = Double.compare(Math.floor(o1.tl().y), Math.floor(o2.tl().y));
                    if (result != 0) {
                        return result;
                    }
                }
                //Else sort by X
                return Double.compare(Math.floor(o2.tl().x),Math.floor(o1.tl().x));
            }
        });
    }

    /* Sort a Rect list from Top to Bottom, Right to Left */
    private void sortRects(List<Rect> rectList) {
        Collections.sort(rectList, new Comparator<Rect>() {
            @Override
            public int compare(Rect o1, Rect o2) {
                //Reversed compare for X for Right to Left
                int resultX = Double.compare(Math.floor(o2.tl().x),Math.floor(o1.tl().x));
                int resultY = Double.compare(Math.floor(o1.tl().y), Math.floor(o2.tl().y));
                Log.d("Compare: Rect1","[tl.x "+o1.tl().x+" tl.y "+o1.tl().y+"] [br.x "+o1.br().x+" br.y "+o1.br().y+"]");
                Log.d("Compare: Rect2","[tl.x "+o2.tl().x+" tl.y "+o2.tl().y+"] [br.x "+o2.br().x+" br.y "+o2.br().y+"]");
               if( resultX != 0 ) {
                   //Check if panels on the same row
                   if ( o2.tl().y >= o1.tl().y && o2.br().y <= o1.br().y
                           || o2.tl().y <= o1.tl().y && ( o1.tl().y - o2.tl().y ) < o2.height
                           || o2.br().y >= o1.br().y && ( o2.br().y - o1.br().y ) < o2.height) {

                       //Check if panels overlapped on the same col. Probably some errors here, need more testings
                       if( !( o2.tl().x >= o1.tl().x && o2.tl().x <= o1.br().x )
                               && !( o2.br().x <= o1.br().x && o2.br().x >= o1.tl().x ) ) {
                           return resultX;
                       }
                   }
               }
               return resultY;
            }
        });
    }

    /* Split panels according to area, cols and rows*/
    private void splitPanel(List<Rect> rectList, double areaThreshold, int cols, int rows) {
        int size = rectList.size();
        for (int i = 0; i < size; i++) {
            Rect rect = rectList.get(i);
            List<Rect> rects = null;
            if(rect.area() > areaThreshold) {
                rects = splitRects(rect, cols, rows);
            }
            if (rects != null && rects.size() > 1) {
                int count = rects.size();
                rectList.addAll(i, rects);
                i += count;
                size += count;
                /* Remove the panel after splitting if not set to keep */
                if (!keep_panel) {
                    rectList.remove(rect);
                    i--;
                    size--;
                }
            }
        }
    }

    /* Split panel according to dimensions and device orientation */
    private void splitPanel(List<Rect> rectList, int height, int width, int orientation) {
        int size = rectList.size();
        for (int i = 0; i < size; i++) {
            Rect rect = rectList.get(i);
            List<Rect> rects = null;
            /* Split according to device orientation */     //Could be more refined maybe
            switch(orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    //Large Panels or full page
                    if (rect.height > height * 0.7 && rect.width > width * 0.7) {
                        rects = splitRects(rect, 3, 1);
                    }
                    //Wide Panels with medium height
                    else if(rect.height > height * 0.4 && rect.width > width * 0.7) {
                        rects = splitRects(rect, 2, 1);
                    }
                    //Tall Panels
                    else if(rect.height > height * 0.5){
                        rects = splitRects(rect, 3, 1);
                    }
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    //Large Panels or full page
                    if (rect.height > height * 0.7 && rect.width > width * 0.7) {
                        rects = splitRects(rect, 2, 2);
                    }
                    //Wide Panels
                    else if(rect.width > width * 0.7) {
                        rects = splitRects(rect, 1, 2);
                    }
                    break;
            }
            if (rects != null && rects.size() > 1) {
                int count = rects.size();
                rectList.addAll(i, rects);
                i += count;
                size += count;
                /* Remove the panel after splitting if not set to keep */
                if (!keep_panel) {
                    rectList.remove(rect);
                    i--;
                    size--;
                }
            }
        }
    }

    /* Split Rect to a set number of Cols and Rows */
    private List<Rect> splitRects(Rect rect, int cols, int rows) {
        List<Rect> rects = new ArrayList<>();
        int height = rect.height / cols;
        int width = rect.width / rows;

        for (int j = 0; j < cols; j++) {
            for (int i = (rows-1); i >= 0; i--) {
                rects.add( new Rect(rect.x+(i * width), rect.y+(j * height), width, height) );
            }
        }
        return rects;
    }

    /* Detect Rect intersection */
    private Rect intersect(Rect rectA, Rect rectB) {
        int left = max(rectA.x, rectB.x);
        int top = max(rectA.y, rectB.y);
        int right = min(rectA.x + rectA.width, rectB.x + rectB.width);
        int bottom = min(rectA.y + rectA.height, rectB.y + rectB.height);

        if (left <= right && top <= bottom) {
            return new Rect(left, top, right - left, bottom - top);
        } else {
            return new Rect();
        }
    }

    /* Merge Rect */
    private Rect merge(Rect rectA, Rect rectB) {
        int left = min(rectA.x, rectB.x);
        int top  = min(rectA.y, rectB.y);
        int right = max(rectA.x + rectA.width, rectB.x + rectB.width);
        int bottom = max(rectA.y + rectA.height, rectB.y + rectB.height);

        return new Rect(left, top, right - left, bottom - top);
    }
}
