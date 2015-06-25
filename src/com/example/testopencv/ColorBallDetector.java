package com.example.testopencv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class ColorBallDetector {
    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    //private static double mMinContourArea2 = 0.01;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(35,200,200,0);
   // private Scalar mColorRadius = new Scalar(35,150,150,0);
    private Mat mSpectrum = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    private List<MatOfPoint> mBallContours = new ArrayList<MatOfPoint>();

    // Cache
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();
    
    boolean mReady = false;

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }

    public void setHsvColor(Scalar hsvColor) {
        double minH = (hsvColor.val[0]-mColorRadius.val[0]-5 >= 0) ? hsvColor.val[0]-mColorRadius.val[0]-5 : 0;
        double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;
        
        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH-minH; j++) {
            byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
        mReady = true;
    }

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage) {
    	mContours = new ArrayList<MatOfPoint>();
    	mBallContours = new ArrayList<MatOfPoint>();
    	
        Imgproc.cvtColor(rgbaImage, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.size()==0){
        	return;
        }

        // Find max contour area
        double maxArea = 0;
        int indTable = 0;
        for (int i=0; i<contours.size(); i++){
            double area = Imgproc.contourArea(contours.get(i));
            if (area > maxArea){
                maxArea = area;
                indTable = i;
            }
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                mContours.add(contour);
            }
        }
        
     
        int first = 0, second = 0, third = 0;
        if (indTable == 0){
        	first = second = third = 1;
        }
        
        for (int i=0; i<contours.size(); i++){
        	if (mHierarchy.get(0,i)[3] != -1 && mHierarchy.get(0, i)[3] == indTable && Imgproc.contourArea(contours.get(i)) < 0.05*maxArea){
                if (Imgproc.contourArea(contours.get(i)) > Imgproc.contourArea(contours.get(first))) {
                	third = second;
                	second = first;
                	first = i;
                }
                else if (Imgproc.contourArea(contours.get(i)) > Imgproc.contourArea(contours.get(second))) {
                	third = second;
                	second = i;
                }
                else if (Imgproc.contourArea(contours.get(i)) > Imgproc.contourArea(contours.get(third))) {
                	third = i;
                }
        	}
        }
        
        mBallContours.add(contours.get(first));
        mBallContours.add(contours.get(second));
        mBallContours.add(contours.get(third));
        Log.i("sizeContour", mBallContours.size()+"");
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
    
    public List<MatOfPoint> getMaxContour(Mat rgba){
    	//indice du contour choisi dans la matrice contours
    	int c = 0;
    	//Pour determiner le contour qui a l'aire maximale, quand on a trouvé plusieurs contours
    	double min = 99999999;
        List<MatOfPoint> contours = getContours();
        MatOfPoint2f quadrangle = new MatOfPoint2f();
        Point centerScreen = new Point(MainActivity.width/2, MainActivity.height/2);
        
        if (contours.size()!=1){
        	for (int i=0; i<contours.size(); i++){
                Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), quadrangle, Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true)*0.1, true);
                Rect roi = Imgproc.boundingRect(new MatOfPoint(quadrangle.toArray()));
                Point center = new Point((roi.tl().x+roi.br().x)/2,(roi.tl().y+roi.br().y)/2);
                if ((center.x-centerScreen.x)*(center.x-centerScreen.x)+(center.y-centerScreen.y)*(center.y-centerScreen.y)<min){
        			c = i;
        			min = (center.x-centerScreen.x)*(center.x-centerScreen.x)+(center.y-centerScreen.y)*(center.y-centerScreen.y);
        		}
        	}
        }
        
        ArrayList<MatOfPoint> result = new ArrayList<MatOfPoint>();
        result.add(contours.get(c));
        return result;
    }
    
    public List<MatOfPoint> getBallContours() {
        return mBallContours;
    }
    
    public static int abc = 0;
    
    public boolean baisserLimite(){
    	Log.i("ibiki", abc + "");
    	abc++;
    	for (int i=1; i<3; i++){
    		if (mLowerBound.val[i] < mUpperBound.val[i]-1){
    			if (mLowerBound.val[i] < mUpperBound.val[i]-1){
    				mLowerBound.val[i]+=5;
    				mUpperBound.val[i]-=5;
    			}
    		}
    	}
    	
    	if (mLowerBound.val[1] >= mUpperBound.val[1]-1){
    		return false;
    	}
    	mBallContours = new ArrayList<MatOfPoint>();
    	return true;
    }
    
    boolean isReady(){
    	return mReady;
    }
}
