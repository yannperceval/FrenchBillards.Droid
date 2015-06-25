package com.example.testopencv;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class Table {
	private MatOfPoint mPoints;
	private static int offSet = MainActivity.offSet;
	private static int width = MainActivity.width;
	private static int height = MainActivity.height;
	
	public Table(MatOfPoint m){
		mPoints = m;
		width = MainActivity.width;
		height = MainActivity.height;
	}
	
	public Point get(int i){
		if (i>=0){
			return new Point(mPoints.get(i,0));
		}
		return null;
	}
	
	public Point get(Point p){
		for (int i=0; i<mPoints.rows(); i++){
			if (mPoints.get(i, 0)[0] == p.x && mPoints.get(i, 0)[1] == p.y){
				return new Point(mPoints.get(i,0));
			}
		}
		return null;
	}
	
	public Point getVoisinDroite(Point p){
		for (int i=0; i<mPoints.rows(); i++){
			if (mPoints.get(i, 0)[0] == p.x && mPoints.get(i, 0)[1] == p.y && i>0){
				return new Point(mPoints.get(i-1,0));
			}
			else if (mPoints.get(i, 0)[0] == p.x && mPoints.get(i, 0)[1] == p.y && i==0){
				return new Point(mPoints.get(mPoints.rows()-1,0));
			}
		}
		return null;
	}
	
	public Point getVoisinGauche(Point p){
		for (int i=0; i<mPoints.rows(); i++){
			if (mPoints.get(i, 0)[0] == p.x && mPoints.get(i, 0)[1] == p.y && i<mPoints.rows()-1){
				return new Point(mPoints.get(i+1,0));
			}
			else if (mPoints.get(i,0)[0] == p.x && mPoints.get(i, 0)[1] == p.y && i==mPoints.rows()-1){
				return new Point(mPoints.get(0,0));
			}
		}
		return null;
	}

	public MatOfPoint getPoints() {
		return mPoints;
	}
	
	public Point getPoint(int i){
		return new Point(mPoints.get(i, 0)[0], mPoints.get(i, 0)[1]);
	}

	public void setPoints(MatOfPoint mPoints) {
		this.mPoints = mPoints;
	}
	
	public static double angleBetween(Point a, Point b){
		Point c = new Point(a.x+100,a.y);
		
		double distAC = Math.sqrt(  (a.x-c.x)*(a.x-c.x) + (a.y-c.y)*(a.y-c.y) );  
		double distAB = Math.sqrt(  (a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y) );
		
		double scal = Math.abs((c.x-a.x))*Math.abs((b.x-a.x)) + Math.abs((c.y-a.y))*Math.abs((b.y-a.y));
		
		if (a.x>b.x && a.y>b.y || a.x<b.x && a.y<b.y){
			return Math.acos(scal/(distAC*distAB))*180/Math.PI;
		}
		return -Math.acos(scal/(distAC*distAB))*180/Math.PI;
	}
	
	public int getFirstVertex(){
		for (int i=0; i<mPoints.rows()-1; i++){
			if (mPoints.get(i, 0)[0]>offSet && mPoints.get(i,0)[0]<width-offSet && mPoints.get(i, 0)[1] > offSet && mPoints.get(i,0)[1]<height-offSet
			 && mPoints.get(i+1, 0)[0]>offSet && mPoints.get(i+1,0)[0]<width-offSet && mPoints.get(i+1, 0)[1] > offSet && mPoints.get(i+1,0)[1]<height-offSet){
				return i;
			}
		}
		if (mPoints.get(mPoints.rows()-1, 0)[0]>offSet && mPoints.get(mPoints.rows()-1,0)[0]<width-offSet && mPoints.get(mPoints.rows()-1, 0)[1] > offSet && mPoints.get(mPoints.rows()-1,0)[1]<height-offSet
				 && mPoints.get(0, 0)[0]>offSet && mPoints.get(0,0)[0]<width-offSet && mPoints.get(0, 0)[1] > offSet && mPoints.get(0,0)[1]<height-offSet){
			return mPoints.rows()-1;
		}
		return -1;
	}
	
	public boolean out(int i){
		if (mPoints.get(i, 0)[0]<=offSet || mPoints.get(i,0)[0]>=width-offSet || mPoints.get(i, 0)[1] <= offSet || mPoints.get(i,0)[1]>=height-offSet){
			return true;
		}
		return false;
	}
}
