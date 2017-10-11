package org.tintensoft.opticalformreader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.floor;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.putText;
import static org.opencv.imgproc.Imgproc.pyrDown;
import static org.opencv.imgproc.Imgproc.resize;

public class ImageProcessing extends AppCompatActivity  {

    private static final String TAG = "ImageProcessing";

    public Mat mRgba;
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    onImageView();
                    break;

                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    ImageView resim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageprocessing);
        resim = (ImageView) findViewById(R.id.imageView2);
    }

  int contourNo, correct = 0, wrong = 0;

    public void onImageView() {

        Mat img = getImage();

        Mat background = getImageBackground();
        resize(img, img, background.size());

        Mat gray = new Mat();
        Mat gray1 = new Mat();
        Mat blured = new Mat();
        Mat edged = new Mat();
        Mat thresh = new Mat();
        Mat perspective = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<MatOfPoint> cnts = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();
        Rect rect = null;
        cvtColor(img, gray, COLOR_BGR2GRAY);

        Imgproc.GaussianBlur(gray, blured, new Size(3, 3), 2);
        Imgproc.Canny(blured, edged, 75, 175);
        Imgproc.threshold(blured, thresh, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i = 0; i < contours.size(); i++) {
            if (contourArea(contours.get(i)) > 4000000 && contourArea(contours.get(i)) < 8000000) {
                contourNo = i;
                drawContours(img, cnts, i, new Scalar(255, 0, 0), 10);
            }
        }

        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourNo).toArray());
        double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

        MatOfPoint points = new MatOfPoint(approxCurve.toArray());

        int[] pointIndex = PointListControl(points.toList());
        img = getPerspective(pointIndex, points.toList(), img);
        img = getPerspectiveImage(img);

        cvtColor(img, gray, COLOR_BGR2GRAY);
        background = getPerspectiveBackgroundImage(rect, background);
        cvtColor(background, gray1, COLOR_BGR2GRAY);

        Core.absdiff(gray1, gray, perspective);

        Imgproc.GaussianBlur(perspective, blured, new Size(3, 3), 2);
        Imgproc.Canny(blured, edged, 75, 175);
        Imgproc.threshold(blured, thresh, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Imgproc.medianBlur(thresh, thresh, 45);

        Imgproc.findContours(thresh, cnts, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i = cnts.size() - 1; i > -1; i--) {

            if (contourArea(cnts.get(i)) > 4000 && contourArea(cnts.get(i)) < 30000) {

                rect = Imgproc.boundingRect(cnts.get(i));
                int q = getAnswerKey(rect, img.cols());
                boolean b = checkAnswer(q);
                if (b) {
                    drawContours(img, cnts, i, new Scalar(0, 255, 0), 10);
                    correct++;
                } else {
                    drawContours(img, cnts, i, new Scalar(255, 0, 0), 10);
                    wrong++;
                }
            }
        }

        String text = "Correct:" + correct + " Wrong:" + wrong + "  Empty:" + (100 - (correct + wrong));

        putText(img, text, new Point(100, 3500), Core.FONT_HERSHEY_SIMPLEX, 4.0, new Scalar(0, 0, 0), 10);
        pyrDown(img, img);
        displayImage(img);
    }


    int[] QUESTION_KEY = new int[100];
    int[] ANSWER_KEY = {0, 2, 3, 4, 5, 1, 4, 4, 1, 4, 2, 1, 5, 5, 1, 3, 4, 4, 2, 2, 3, 1, 4, 1, 3, 2, 5, 1, 5, 2, 1, 4, 5, 2, 4, 3, 2, 3, 1, 4, 5,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    double baslangic = 110, tolerans = 35;
    int katsayi = 0, questionOrder = 0, questionNo = 0, referans = 593, ortalamaParametresi = 25, soruSayisi = 25;

    private int getAnswerKey(Rect rect, int rows) {

        if (rect.tl().x > 25 && rect.tl().x < rows / 4) {
            katsayi = 0;
        } else if (rect.tl().x > rows / 4 && rect.tl().x < rows / 4 * 2) {
            katsayi = 1;
        } else if (rect.tl().x > rows / 4 * 2 && rect.tl().x < rows / 4 * 3) {
            katsayi = 2;
        } else if (rect.tl().x > rows / 4 * 3 && rect.tl().x < rows) {
            katsayi = 3;
        }

        questionOrder = (int) floor((rect.tl().y) / 162) + 1;
        questionNo = questionOrder + katsayi * soruSayisi;
        double column = katsayi * referans;
        double orta = (rect.tl().x + rect.br().x) / 2;

        if ((baslangic + column) - tolerans < orta && orta < ((baslangic + column) + tolerans)) {
            Log.d("Cevap", (questionNo) + " Cevap a");
            QUESTION_KEY[questionNo] = 1;
        } else if (((baslangic * 1.77 + column) - tolerans) < orta && orta < ((baslangic * 1.77 + column) + tolerans)) {
            Log.d("Cevap", (questionNo) + " Cevap b");
            QUESTION_KEY[questionNo] = 2;
        } else if (((baslangic * 2.54 + column) - tolerans) < orta && orta < ((baslangic * 2.54 + column) + tolerans)) {
            Log.d("Cevap", (questionNo) + " Cevap c");
            QUESTION_KEY[questionNo] = 3;
        } else if (((baslangic * 3.27 + column) - tolerans) < orta && orta < ((baslangic * 3.27 + column) + tolerans)) {
            Log.d("Cevap", questionNo + " Cevap d");
            QUESTION_KEY[questionNo] = 4;
        } else if (((baslangic * 4.09 + column) - tolerans) < orta && orta < ((baslangic * 4.09 + column) + tolerans)) {
            Log.d("Cevap", questionNo + " Cevap e");
            QUESTION_KEY[questionNo] = 5;
        }
        return questionNo;
    }

    private boolean checkAnswer(int q) {
        boolean correct = false;

        if (QUESTION_KEY[q] == ANSWER_KEY[q]) {
            correct = true;
        } else {
            correct = false;
        }
        return correct;
    }

    private Mat getPerspectiveImage(Mat img) {
        List<Point> corners = new ArrayList<>();
        List<Point> target = new ArrayList<>();


        corners.add(new Point(775, 2460));
        corners.add(new Point(765, 4002));
        corners.add(new Point(2033, 2454));
        corners.add(new Point(2040, 3998));

        target.add(new Point(0, 0));
        target.add(new Point(0, img.rows()));
        target.add(new Point(img.cols(), 0));
        target.add(new Point(img.cols(), img.rows()));

        Mat cornersMat = Converters.vector_Point2f_to_Mat(corners);
        Mat targetMat = Converters.vector_Point2f_to_Mat(target);
        Mat trans = Imgproc.getPerspectiveTransform(cornersMat, targetMat);
        Imgproc.warpPerspective(img, img, trans, new Size(img.cols(), img.rows()));

        return img;
    }

    private Mat getPerspective(int pointIndex[], List<Point> point, Mat img) {
        List<Point> corners = new ArrayList<>();
        List<Point> target = new ArrayList<>();

        corners.add(new Point(point.get(pointIndex[0]).x, point.get(pointIndex[0]).y));
        corners.add(new Point(point.get(pointIndex[2]).x, point.get(pointIndex[2]).y));
        corners.add(new Point(point.get(pointIndex[1]).x, point.get(pointIndex[1]).y));
        corners.add(new Point(point.get(pointIndex[3]).x, point.get(pointIndex[3]).y));

        target.add(new Point(0, 0));
        target.add(new Point(0, img.rows()));
        target.add(new Point(img.cols(), 0));
        target.add(new Point(img.cols(), img.rows()));

        Mat cornersMat = Converters.vector_Point2f_to_Mat(corners);
        Mat targetMat = Converters.vector_Point2f_to_Mat(target);
        Mat trans = Imgproc.getPerspectiveTransform(cornersMat, targetMat);
        Imgproc.warpPerspective(img, img, trans, new Size(img.cols(), img.rows()));

        return img;
    }

    private int[] PointListControl(List<Point> point) {

        int pointIndex[] = new int[4];
        Double uzaklik[] = new Double[4];
        Double uzaklikCopy[] = new Double[4];

        for (int i = 0; i < point.size(); i++) {
            uzaklik[i] = sqrt(pow(point.get(i).x, 2) + pow(point.get(i).y, 2));
            uzaklikCopy[i] = uzaklik[i];

        }

        Collections.sort(Arrays.asList(uzaklik), new Comparator<Double>() {
            public int compare(Double o1, Double o2) {
                return Double.compare(o1, o2);
            }
        });

        for (int i = 0; i < point.size(); i++) {
            for (int j = 0; j < point.size(); j++) {
                if (uzaklik[i] == uzaklikCopy[j]) {
                    pointIndex[i] = j;
                }
            }
        }

        return pointIndex;
    }


    private Mat getPerspectiveBackgroundImage(Rect rect, Mat img) {
        List<Point> corners = new ArrayList<>();
        List<Point> target = new ArrayList<>();

        corners.add(new Point(829, 2161));
        corners.add(new Point(814, 3170));
        corners.add(new Point(1913, 2176));
        corners.add(new Point(1898, 3181));

        target.add(new Point(0, 0));
        target.add(new Point(0, img.rows()));
        target.add(new Point(img.cols(), 0));
        target.add(new Point(img.cols(), img.rows()));

        Mat cornersMat = Converters.vector_Point2f_to_Mat(corners);
        Mat targetMat = Converters.vector_Point2f_to_Mat(target);
        Mat trans = Imgproc.getPerspectiveTransform(cornersMat, targetMat);
        Imgproc.warpPerspective(img, img, trans, new Size(img.cols(), img.rows()));

        return img;
    }

    private Mat getImage() {
        String imgURL = MainActivity.imgURL;
        Mat img;
        Log.d("Path", imgURL);

        img = Imgcodecs.imread(imgURL, -1);

        return img;
    }

    private Mat getImageBackground() {

        Mat img = null;
        try {
            img = Utils.loadResource(this, R.drawable.i62, CV_8UC4);
            Log.d("BoyutBack", img.rows() + " " + img.cols());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }


    private void displayImage(Mat img) {

        Bitmap bm = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bm);
        resim.setImageBitmap(bm);

    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "Succes");
            correct = 0;
            wrong = 0;
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        } else {
            Log.d(TAG, "NOT Succes");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        }
    }



}
