package com.example.testopencv;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ClickableViewAccessibility")
public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
	public static long time = System.currentTimeMillis();
	public static int width = 0;
	public static int height = 0;
	public static int offSet = 0;
	
	private int mFirst = -1;
	
    private Mat                  mRgba;
    private Scalar               mBlobColorHsv;
    private ColorBallDetector    mDetector;
    private Point mTouchPoint = new Point();

    private Table mTable;
    private Point[] mTableVertices;
    private ArrayList<MatOfPoint> mContours;
    private Mat mCircles;
    private Mat mMatricePerspective;
    private Mat mPerspect;
    private ImageButton[] mButtons = new ImageButton[4];
    
    private boolean mSwitchText = true;
    private boolean mError = false;
    private boolean mSwitchEdges = false;
    
    private static final double WHITE = 1;
    private static final double RED = 11;
    private static final double YELLOW = 111;
    private static final double NOCOLOR = 0;
    
    
    private static int RESULT_LOAD_IMG = 1;
    private boolean currentUseImportPicture = false;
    public enum State {
    	ON_INIT, 			ON_START, 			ON_TRACKING_ELEMENT, ON_VALIDATE, 			ON_END, 
    	ON_IMPORT_PICTURE, 	ON_PICTURE_START, 	ON_PICTURE_ANALYSE,  ON_PICTURE_VALIDATE, 	ON_PICTURE_END
    }
    
    State mState;

    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView			 mOpenCvImportImageView;
    private Uri 				 importImageUri;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        

        System.out.println("[TEST] MainActivity : onCreate");

        setContentView(R.layout.activity_main);
		
        mainView();
    }
    
    public void mainView(){
        
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        mOpenCvImportImageView = (ImageView) findViewById(R.id.importImage);
        mOpenCvImportImageView.setOnTouchListener(MainActivity.this);
        
        mRgba = new Mat();
        
        mButtons[0] = (ImageButton)findViewById(R.id.imageButton1);
        //mButtons[0].setBackgroundColor(Color.TRANSPARENT);
        mButtons[0].setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	if (mDetector.isReady() && mBlobColorHsv != null){
			        mButtons[0].setVisibility(View.GONE);
			    	mButtons[1].setVisibility(0);
			    	mButtons[2].setVisibility(0);
			    	mButtons[3].setVisibility(0);
		    		nextState();
		    	}
		    }
        });
        mButtons[1] = (ImageButton)findViewById(R.id.imageButton2);
        //mButtons[1].setBackgroundColor(Color.TRANSPARENT);
        mButtons[1].setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("[TEST] Show solution... compute...");
                perspectiveTransform();
                System.out.println("[TEST] After perspectiveTransform");
				ArrayList<Pair<String,Pair<Double,Double>>> listeBoules = getPosition();
                System.out.println("[TEST] After getPosition");
				
		    	double blancheX = listeBoules.get(2).second.first;
		    	double blancheY = listeBoules.get(2).second.second;
		    	double rougeX   = listeBoules.get(0).second.first;//listeBoules.get(1).second.first;
		    	double rougeY   = listeBoules.get(0).second.second;//listeBoules.get(1).second.second;
		    	double jauneX   = listeBoules.get(1).second.first;//listeBoules.get(0).second.first;
		    	double jauneY   = listeBoules.get(1).second.second;//listeBoules.get(0).second.second;
                
		    	// Passer de la MainActivity à la SolutionCanvas
                Intent i = new Intent(MainActivity.this, SolutionCanvas.class);
                i.putExtra("blancheX", blancheX);
                i.putExtra("blancheY", blancheY);
                i.putExtra("rougeX", rougeX);
                i.putExtra("rougeY", rougeY);
                i.putExtra("jauneX", jauneX);
                i.putExtra("jauneY", jauneY);
                startActivity(i);
                finish();
		    }
        });
        mButtons[2] = (ImageButton)findViewById(R.id.imageButton3);
        //mButtons[2].setBackgroundColor(Color.TRANSPARENT);
        mButtons[2].setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	if (!mSwitchEdges && mTable.getFirstVertex()<mTableVertices.length-1){
		    		drawContours(mTable.getFirstVertex()+1);
		    		mSwitchEdges = true;
		    	}else if (!mSwitchEdges && mTable.getFirstVertex()==mTableVertices.length-1){
		    		drawContours(0);
		    		mSwitchEdges = true;
		    	}else {
		    		drawContours(mTable.getFirstVertex());
		    		mSwitchEdges = false;
		    	}
		    	if(currentUseImportPicture){
			        // on affiche la nouvelle image
			        Bitmap image = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
			        Utils.matToBitmap(mRgba, image);
			        mOpenCvImportImageView.setImageBitmap(null); // réinitialise l'ImageView
			        mOpenCvImportImageView.setImageBitmap(image);
		    	}
		    }
        });
        mButtons[3] = (ImageButton)findViewById(R.id.imageButton4);
        //mButtons[3].setBackgroundColor(Color.TRANSPARENT);
        mButtons[3].setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	if(currentUseImportPicture && mState == State.ON_PICTURE_VALIDATE){
                    importImage(importImageUri);
		    	}else{
			        mButtons[0].setVisibility(0);
			    	mButtons[1].setVisibility(View.GONE);
			    	mButtons[2].setVisibility(View.GONE);
			    	mButtons[3].setVisibility(View.GONE);
			    	mState = State.ON_START;
			    	if(currentUseImportPicture){
			    		mOpenCvImportImageView.setVisibility(View.GONE);
			    		mOpenCvCameraView.setVisibility(0);
			    		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, MainActivity.this, mLoaderCallback);
				    	currentUseImportPicture = false;
			    	}
		    	}
		    }
        });
        
        mDetector = new ColorBallDetector();
        mBlobColorHsv = new Scalar(255);
        mCircles = new Mat(1,3,CvType.CV_32FC4);
        mState = State.ON_START;
        mMatricePerspective = new Mat();
        mContours = new ArrayList<MatOfPoint>();;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        System.out.println("[TEST] MainActivity : onPause - mState : " + mState.toString());
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        System.out.println("[TEST] MainActivity : onResume - mState : " + mState.toString());
        if(mState == State.ON_IMPORT_PICTURE){
        	mState = State.ON_PICTURE_START;
        }else{
        	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        }        	
    }
    
    /*public void onSaveInstanceState(Bundle savedInstanceState) {
    	super.onSaveInstanceState(savedInstanceState);
    	savedInstanceState.putString("mState", mState.toString());
        System.out.println("[TEST] MainActivity : onSaveInstanceState - mState : " + mState.toString());
	}
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	mState = State.valueOf(savedInstanceState.getString("mState"));
        System.out.println("[TEST] MainActivity : onSaveInstanceState - mState : " + mState.toString());
    }*/

    public void onDestroy() {
        super.onDestroy();
        System.out.println("[TEST] MainActivity : onDestroy");
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK) {        	
        	if(mState == State.ON_PICTURE_ANALYSE ||  mState == State.ON_PICTURE_VALIDATE){
        		importImage(importImageUri);
        		return true; // pas de propagation de l'évenement
        		
        	}else if(mState == State.ON_PICTURE_START || mState != State.ON_PICTURE_START){
        		mButtons[0].setVisibility(0);
		    	mButtons[1].setVisibility(View.GONE);
		    	mButtons[2].setVisibility(View.GONE);
		    	mButtons[3].setVisibility(View.GONE);
		    	mState = State.ON_START;
		    	if(currentUseImportPicture){
		    		mOpenCvImportImageView.setVisibility(View.GONE);
		    		mOpenCvCameraView.setVisibility(0);
		    		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, MainActivity.this, mLoaderCallback);
			    	currentUseImportPicture = false;		    		
		    	}
		    	return true; // pas de propagation de l'évenement
		    	
        	}else{
        		// Passer du Menu à la MainActivity
		        Intent i = new Intent(MainActivity.this, Menu.class);
		        startActivity(i);
		        finish();
		        return true; // pas de propagation de l'évenement
        	}
        }
		return false; // propagation de l'évenement
    }

    public void onCameraViewStarted(int width, int height) {
    	System.out.println("[TEST] MainActivity : onCameraViewStarted");
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        int xOffset = (mOpenCvCameraView.getWidth() - mRgba.cols());
        int yOffset = (mOpenCvCameraView.getHeight() - mRgba.rows());
        MainActivity.width = mOpenCvCameraView.getWidth() - xOffset;
        MainActivity.height = mOpenCvCameraView.getHeight() - yOffset;
        MainActivity.offSet = width/300;
    }

    public void onCameraViewStopped() {
    	System.out.println("[TEST] MainActivity : onCameraViewStopped");
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
    	System.out.println("[TEST] MainActivity : onTouch Screen - mState=" + mState.toString());    		
    	if (mState == State.ON_START){
			 runOnUiThread(new Runnable() {
			        public void run() {
		    			TextView t = (TextView) findViewById(R.id.textColor);
		    	        t.setText("Prennez une photo");
			        }
			 });
			 computeAreaColor(event);
    	}else if(mState == State.ON_PICTURE_START){
    		mState = State.ON_PICTURE_ANALYSE;
			TextView t = (TextView) findViewById(R.id.textColor);
			t.setText("Patientez pendant le calcul...");
	        t.setBackgroundColor(0xff888888);
	        // récupère la couleur du tapis
	        computeAreaColor(event);
	        // on démarre le calcul de reconnaissance...
	        runOnUiThread(new Runnable() {
				public void run() {		    		
			        // on réimporte dans la Mat mRgba l'image d'origine
			        ImportImageUriToMat(importImageUri);
			        TextView t = (TextView) findViewById(R.id.textColor);
			        if (!track()){
				        mState = State.ON_PICTURE_VALIDATE;
				        t.setText("Trackage reussi");
				        t.setBackgroundColor(0xff00ff00);
			    		mButtons[1].setVisibility(0); // on affiche le bouton valider
				        mButtons[2].setVisibility(0); // on affiche le bouton inverser
				        // on affiche la nouvelle image
				        Bitmap image = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
				        Utils.matToBitmap(mRgba, image);
				        mOpenCvImportImageView.setImageBitmap(null); // réinitialise l'ImageView
				        mOpenCvImportImageView.setImageBitmap(image);
					}else{
				        mState = State.ON_PICTURE_START;
						t.setText("Erreur lors du trackage. Réessayez.");
				        t.setBackgroundColor(0xffff0000);
					}
				}
			});
    	}
        return false;
    }
    
    @SuppressWarnings("finally")
	public Mat onCameraFrame(CvCameraViewFrame inputFrame){
    	//System.out.println("[TEST][MAINACTIVITY][ONCAMERAFRAME] currentUseImportPicture : " + currentUseImportPicture);
    	if (mState != State.ON_END && mState != State.ON_VALIDATE){
    		mRgba = inputFrame.rgba();
    	}
    	try {
	    	if (mState == State.ON_START){
	    		if (mSwitchText){
	      			 runOnUiThread(new Runnable() {
	 			        public void run() {
	 		    			TextView t = (TextView) findViewById(R.id.textColor);
	 		    	        t.setText("Touchez une zone vide sur la table");
			    	        t.setBackgroundColor(0xff0000FF);
	 			        }
	      			 });
	    	        mSwitchText = false;
	    		}
	    		if (mError){
	      			 runOnUiThread(new Runnable() {
		 			        public void run() {
		 				    	mButtons[1].setVisibility(View.GONE);
		 				    	mButtons[2].setVisibility(View.GONE);
		 				    	if(!currentUseImportPicture)
		 				    		mButtons[3].setVisibility(View.GONE);
		 				        mButtons[0].setVisibility(0);
		 		    			TextView t = (TextView) findViewById(R.id.textColor);
		 		    	        t.setText("Erreur lors du calcul, recommencez");
				    	        t.setBackgroundColor(0xffFF0000);
		 			        }
		      			 });
		    	        mError = false;
	    		}
	    		if (mDetector.isReady()){
	    			mDetector.process(mRgba);
	    			Imgproc.drawContours(mRgba, mDetector.getMaxContour(mRgba), -1, new Scalar(0,255,0));
	    			findBallsContour();
	    			findColorBall(mRgba);
	      			runOnUiThread(new Runnable() {
		 			    public void run() {
		 			    	mButtons[0].setImageResource(R.drawable.apn);
		 			    }
		      		});
	    		}else {
	      			runOnUiThread(new Runnable() {
	 			        public void run() {
	 			        	mButtons[0].setImageResource(R.drawable.apn2);
	 			        }
	      		});
	    		}
	    	}
	    	
	    	else if (mState == State.ON_TRACKING_ELEMENT){
		   			 runOnUiThread(new Runnable() {
					        public void run() {
				    			TextView t = (TextView) findViewById(R.id.textColor);
				    	        t.setText("Patientez pendant le calcul");
				    	        t.setBackgroundColor(0xff888888);
					        }
					 });
				if (!track()){
					runOnUiThread(new Runnable() {
						public void run() {
				    		TextView t = (TextView) findViewById(R.id.textColor);
					        t.setText("Trackage reussi");
					        t.setBackgroundColor(0xff00ff00);
						}
					});
					nextState();
				}
	    	}
    	}
    	catch (Exception e){
    		mState = State.ON_START;
    		mError = true;
    	}
    	finally {
    		return mRgba;
    	}
    }
    
    
    private boolean track(){
    	boolean trouve = false;
    	while (!trouve){
    		mContours = new ArrayList<MatOfPoint>();
	    	//indice du contour choisi dans la matrice contours
	    	int c = 0;
	    	//Pour determiner le contour qui a l'aire maximale, quand on a trouvé plusieurs contours
	    	double max = 0.;
	        mDetector.process(mRgba);
	        List<MatOfPoint> contours = mDetector.getContours();
	        
	        if (contours.size()!=1){
	        	for (int i=0; i<contours.size(); i++){
	        		if ((contours.get(i).rows() == 4 || contours.get(i).rows() == 5 || contours.get(i).rows() == 6) && Imgproc.isContourConvex(contours.get(i)) && Imgproc.contourArea(contours.get(i))>max){
	        			c = i;
	        			max = Imgproc.contourArea(contours.get(i));
	        		}
	        	}
	        }
	        if(contours.size() == 0)
	        	return true;
	        
	        MatOfPoint2f contour = new MatOfPoint2f(contours.get(c).toArray());
	        MatOfPoint2f quadrangle = new MatOfPoint2f();
	 
		    Imgproc.approxPolyDP(contour, quadrangle, Imgproc.arcLength(contour, true)*0.01, true);
		    
		    if ( (quadrangle.rows() == 4 || quadrangle.rows() == 5 || quadrangle.rows() == 6) && Imgproc.contourArea(quadrangle) > 1000 && Imgproc.isContourConvex(new MatOfPoint(quadrangle.toArray()))){
		        mTable = new Table(new MatOfPoint(quadrangle.toArray()));
		        
		        mTableVertices = new Point[quadrangle.rows()];
		        for (int i=0; i<quadrangle.rows(); i++){
		        	mTableVertices[i] = new Point();
		        }
		        
		        int xOffset, yOffset;
		        if(currentUseImportPicture){
		        	xOffset = (mOpenCvImportImageView.getWidth() - mRgba.cols());
			        yOffset = (mOpenCvImportImageView.getHeight() - mRgba.rows());
		        }else{
		        	xOffset = (mOpenCvCameraView.getWidth() - mRgba.cols());
			        yOffset = (mOpenCvCameraView.getHeight() - mRgba.rows());
		        }        
		        
		        mTableVertices[0] = mTable.get(0);
		        mTableVertices[1] = mTable.get(1);
		        mTableVertices[2] = mTable.get(2);
		        mTableVertices[3] = mTable.get(3);
		        if (quadrangle.rows() > 4) { mTableVertices[4] = mTable.get(4); }
		        if (quadrangle.rows() > 5) { mTableVertices[5] = mTable.get(5); }
		        
		        Core.circle(mRgba, mTable.get(0), 6, new Scalar(255,0,0), -1, 8, 0 );
		        Core.circle(mRgba, mTable.get(1), 6, new Scalar(0,255,0), -1, 8, 0 );
		        Core.circle(mRgba, mTable.get(2), 6, new Scalar(0,0,255), -1, 8, 0 );
		        Core.circle(mRgba, mTable.get(3), 6, new Scalar(255,255,255), -1, 8, 0 );
		        if (quadrangle.rows() > 4) { Core.circle(mRgba, mTable.get(4), 6, new Scalar(255,0,255), -1, 8, 0 ); }
		        if (quadrangle.rows() > 5) { Core.circle(mRgba, mTable.get(5), 6, new Scalar(255,255,0), -1, 8, 0 ); }
		        
		        mContours.add(new MatOfPoint(quadrangle.toArray()));
		        
		        if(currentUseImportPicture){
		        	Log.i("image", mOpenCvImportImageView.getWidth() + " " + mOpenCvImportImageView.getHeight());
			        Log.i("image", (mOpenCvImportImageView.getWidth()-xOffset) + " " + (mOpenCvImportImageView.getHeight()-yOffset));
		        }else{
		        	Log.i("image", mOpenCvCameraView.getWidth() + " " + mOpenCvCameraView.getHeight());
			        Log.i("image", (mOpenCvCameraView.getWidth()-xOffset) + " " + (mOpenCvCameraView.getHeight()-yOffset));
		        }
		        
		        mSwitchText = true;
		        if(!currentUseImportPicture)
		        	nextState();
		        
				/**BALL CONTOURS**/
		        findBallsContour();
		        
		        /**BALL COLORS**/
		        findColorBall(mRgba);
		        
				drawContours(mTable.getFirstVertex());
		        takePicture(mRgba, "YO");
		        
			    contour.release();
			    quadrangle.release();
		    	return false;
		    }
	        
		    if (!mDetector.baisserLimite()){
		    	trouve = true;
		    }
    	}
    	return true;
    }
    
    private boolean perspectiveTransform(){
    	 /**PERSPECTIVE TRANSFORM**/
        /*Matrices des 4 points de la table finale
         * Dans le cas où on a les 4 coins on projette les 4 coins sur les 4 coins finales
         * Dans l'autre cas on projette les 4 points blancs sur les 4 points blancs correspondants sur la table finale
         */
        int xOffset, yOffset;
        if(currentUseImportPicture){
        	xOffset = (mOpenCvImportImageView.getWidth() - mRgba.cols());
	        yOffset = (mOpenCvImportImageView.getHeight() - mRgba.rows());
        }else{
        	xOffset = (mOpenCvCameraView.getWidth() - mRgba.cols());
	        yOffset = (mOpenCvCameraView.getHeight() - mRgba.rows());
        } 
        
        Mat end = new Mat(1,4, CvType.CV_32FC2);
    	Mat table = new Mat(1,4,CvType.CV_32FC2);
        ArrayList<MatOfPoint> m1 = new ArrayList<MatOfPoint>();
        ArrayList<MatOfPoint> m2 = new ArrayList<MatOfPoint>();
        ArrayList<MatOfPoint> m3 = new ArrayList<MatOfPoint>();
    	ArrayList<Point> liste = new ArrayList<Point>();
    	liste = getListGoodPoint();
    	
        /**Cas où on connait les 4 coins**/
        if (liste.size() == 4){
        	table = setVerticesOfTable(liste);
	    	
	        //Coordonnees et longueur de la projection de la table
	        double xMin = (mOpenCvCameraView.getWidth()-xOffset)/8;
	        double xMax = mOpenCvCameraView.getWidth()-xOffset-xMin;
	        double yMin = ((mOpenCvCameraView.getHeight()-yOffset)-(3./8.)*(mOpenCvCameraView.getWidth()-xOffset))/2;
	        double yMax = mOpenCvCameraView.getHeight()-yOffset-yMin;
	        end.put(0,0, xMax,yMin, xMin,yMin, xMin,yMax, xMax,yMax);
	        mMatricePerspective = Imgproc.getPerspectiveTransform(table, end);

	        mPerspect = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_32FC4, new Scalar(0));
	        Imgproc.warpPerspective(mRgba, mPerspect, mMatricePerspective, mRgba.size());
        }
        else {
	        /**Autre cas**/
	        liste = computeIntersections(liste);
	        if (liste.size()==4){
	        	table = setVerticesOfTable(liste);
		    	
		        //Coordonnees et longueur de la projection de la table
		        double xMin = (mOpenCvCameraView.getWidth()-xOffset)/8;
		        double xMax = mOpenCvCameraView.getWidth()-xOffset-xMin;
		        double yMin = ((mOpenCvCameraView.getHeight()-yOffset)-(3./8.)*(mOpenCvCameraView.getWidth()-xOffset))/2;
		        double yMax = mOpenCvCameraView.getHeight()-yOffset-yMin;
		        end.put(0,0, xMax,yMin, xMin,yMin, xMin,yMax, xMax,yMax);
		        mMatricePerspective = Imgproc.getPerspectiveTransform(table, end);

		        mPerspect = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_32FC4, new Scalar(0));
		        Imgproc.warpPerspective(mRgba, mPerspect, mMatricePerspective, mRgba.size());
	        }
	        else {
		        liste = getRealTableVertices();
		        if (liste.size()>=2){
			        double DIST = Math.sqrt((liste.get(0).x-liste.get(1).x)*(liste.get(0).x-liste.get(1).x)+(liste.get(0).y-liste.get(1).y)*(liste.get(0).y-liste.get(1).y));
			        double k = DIST/30; //35
			        Mat mask = new Mat(mRgba.size(), CvType.CV_8U, new Scalar(0));
			        Mat mask2 = new Mat(mRgba.size(), CvType.CV_8U, new Scalar(0));
			        Mat mask3 = new Mat(mRgba.size(), CvType.CV_8U, new Scalar(0));
			        Mat imgTmp = mRgba.clone();
			        Mat imgTmp2 = mRgba.clone();
			        Rect zone1 = new Rect(), zone2 = new Rect();
			        int radius = mRgba.rows()/150;
			        
			        //Coordonnees et longueur de la projection de la table
			        double xMin = (mOpenCvCameraView.getWidth()-xOffset)/8;
			        double xMax = mOpenCvCameraView.getWidth()-xOffset-xMin;
			        double xWidth = (xMax-xMin);
			        xMin += (1./8.)*xWidth;
			        xMax = xMin + (1./8.)*xWidth;
			        double yMin = ((mOpenCvCameraView.getHeight()-yOffset)-(3./8.)*(mOpenCvCameraView.getWidth()-xOffset))/2;
			        double yMax = mOpenCvCameraView.getHeight()-yOffset-yMin;
			        
			        /**A gauche**/
			        Point p = new Point();
			        Point a = liste.get(1);
			        Point b = mTable.getVoisinGauche(a);
			        double dist = Math.sqrt((b.x-a.x)*(b.x-a.x)+(b.y-a.y)*(b.y-a.y));
			        p.x = (b.x+a.x)/2;
			        p.y = (b.y+a.y)/2;
			        int cols = mRgba.cols();
			        int rows = mRgba.rows();
			        int x = (int)p.x;
			        int y = (int)p.y;
			        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
			        Rect touchedRect = new Rect();
			        touchedRect.x = (x>2) ? x-2 : 0;
			        touchedRect.y = (y>2) ? y-2 : 0;
			        touchedRect.width = (x+2 < cols) ? x + 2 - touchedRect.x : cols - touchedRect.x;
			        touchedRect.height = (y+2 < rows) ? y + 2 - touchedRect.y : rows - touchedRect.y;
			        Mat touchedRegionRgba = mRgba.submat(touchedRect);
			        Mat touchedRegionHsv = new Mat();
			        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
			        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
			        int pointCount = touchedRect.width*touchedRect.height;
			        for (int i = 0; i < mBlobColorHsv.val.length; i++)
			            mBlobColorHsv.val[i] /= pointCount;
			        
			        if (a.x<=b.x){
			        	p.x -=Math.cos((Table.angleBetween(a, b)-90)*Math.PI/180)*k;
			        	p.y -=Math.sin((Table.angleBetween(a, b)-90)*Math.PI/180)*k;
			        }
			        else {
			        	p.x +=Math.cos((Table.angleBetween(a, b)-90)*Math.PI/180)*k;
			        	p.y +=Math.sin((Table.angleBetween(a, b)-90)*Math.PI/180)*k;
			        }
			        RotatedRect roi = new RotatedRect(p, new Size(dist,k*2), Table.angleBetween(a, b));
			        m1 = new ArrayList<MatOfPoint>();
			        Point[] points = new Point[4];
			        roi.points(points);
			        m1.add(new MatOfPoint(points));
			        roi = new RotatedRect(p, new Size(0.95*dist,0.85*k*2), Table.angleBetween(a, b));//0.95,0.85=min
			        zone1 = roi.boundingRect();
			        roi.points(points);
			        m2.add(new MatOfPoint(points));
			        Imgproc.cvtColor(imgTmp, imgTmp, Imgproc.COLOR_RGB2GRAY);
			        
			        /**A droite **/
			        p = new Point();
			        Point a2 = liste.get(0);
			        Point b2 = mTable.getVoisinDroite(a2);
			        dist = Math.sqrt((b2.x-a2.x)*(b2.x-a2.x)+(b2.y-a2.y)*(b2.y-a2.y));
			        p.x = (b2.x+a2.x)/2;
			        p.y = (b2.y+a2.y)/2;
			        if (a2.x>=b2.x){
			        	p.x -=Math.cos((Table.angleBetween(a2, b2)-90)*Math.PI/180)*k;
			        	p.y -=Math.sin((Table.angleBetween(a2, b2)-90)*Math.PI/180)*k;
			        }
			        else {
			        	p.x +=Math.cos((Table.angleBetween(a2, b2)-90)*Math.PI/180)*k;
			        	p.y +=Math.sin((Table.angleBetween(a2, b2)-90)*Math.PI/180)*k;
			        }
			        roi = new RotatedRect(p, new Size(dist,k*2), Table.angleBetween(a2, b2));
			        points = new Point[4];
			        roi.points(points);
			        m1.add(new MatOfPoint(points));
			        roi = new RotatedRect(p, new Size(0.95*dist,0.85*k*2), Table.angleBetween(a2, b2));
			        zone2 = roi.boundingRect();
			        roi.points(points);
			        m3.add(new MatOfPoint(points));
			        
			        Imgproc.cvtColor(imgTmp2, imgTmp2, Imgproc.COLOR_RGB2HSV);
			        
			        Core.fillPoly(mask, m1, new Scalar(255));
			        Core.fillPoly(mask2, m2, new Scalar(255));
			        Core.fillPoly(mask3, m3, new Scalar(255));
			        
			        Mat gray = new Mat(mRgba.size(), CvType.CV_8UC1, new Scalar(0));
			        Mat tmp = mRgba.clone();
			        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
			        tmp.copyTo(gray,mask);
			        Mat blanc = new Mat(mRgba.size(), CvType.CV_8U, new Scalar(255));
			        Mat model = new Mat(mRgba.size(), CvType.CV_8U, new Scalar(0));
			        Mat blanc2 = new Mat(mRgba.size(), CvType.CV_8U, new Scalar(255));
			        Mat model2 = new Mat(mRgba.size(), CvType.CV_8U, new Scalar(0));
			        blanc.copyTo(model,mask2);
			        blanc2.copyTo(model2,mask3);
			        
			        ArrayList<ArrayList<Point>> dots = harrisTracker(gray,mask2,model,mask3,model2,zone1,zone2);
			        
			        Imgproc.drawContours(mRgba, m1, -1, new Scalar(0,255,0));
			        Imgproc.drawContours(mRgba, m2, -1, new Scalar(0,0,255));
			        Imgproc.drawContours(mRgba, m3, -1, new Scalar(0,0,255));
			        takePicture(mRgba, "YO");

			        double penteGauche = Math.abs((b.y-a.y)/(b.x-a.x));
			        double penteDroite = Math.abs((b2.y-a2.y)/(b2.x-a2.x));
			        List<MatOfPoint> dotLeftContours = getCLosestDots(1, liste, dots.get(0), penteGauche);
			        List<MatOfPoint> dotRightContours = getCLosestDots(0, liste, dots.get(1), penteDroite);
			        
			        Core.circle(mRgba, new Point(dotRightContours.get(0).get(0,0)[0], dotRightContours.get(0).get(0,0)[1]), radius, new Scalar(255,0,0), 3, 8, 0 );
			        Core.circle(mRgba, new Point(dotRightContours.get(1).get(0,0)[0], dotRightContours.get(1).get(0,0)[1]), radius, new Scalar(0,255,0), 3, 8, 0 );
			        Core.circle(mRgba, new Point(dotLeftContours.get(1).get(0,0)[0], dotLeftContours.get(1).get(0,0)[1]), radius, new Scalar(0,0,255), 3, 8, 0 );
			        Core.circle(mRgba, new Point(dotLeftContours.get(0).get(0,0)[0], dotLeftContours.get(0).get(0,0)[1]), radius, new Scalar(0,255,255), 3, 8, 0 );
			        
			    	table.put(0, 0, dotRightContours.get(0).get(0,0));
			    	table.put(0, 1, dotRightContours.get(1).get(0,0));
			    	table.put(0, 2, dotLeftContours.get(1).get(0,0));
			    	table.put(0, 3, dotLeftContours.get(0).get(0,0));
			        
			        end.put(0,0, xMin,yMin, xMax,yMin, xMax,yMax, xMin,yMax);
			        
			        mMatricePerspective = Imgproc.getPerspectiveTransform(table, end);
			        mPerspect = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_32FC4, new Scalar(0));
			        Imgproc.warpPerspective(mRgba, mPerspect, mMatricePerspective, mRgba.size());
		        }
	        }
        }
        for(int i = 0; i < mCircles.cols(); i++){
        	Core.circle(mPerspect, getTranformPoint(new Point(mCircles.get(0, i)[0], mCircles.get(0, i)[1])), 3, new Scalar(0,0,255), -1, 8, 0);
        }
        takePicture(mPerspect, "perspect.png");
        return true;
    }
    
    private void findColorBall(Mat m){
    	Mat hsv = mRgba.clone();
        Imgproc.cvtColor(mRgba, hsv, Imgproc.COLOR_RGB2HSV_FULL);
        for(int i = 0; i < mCircles.cols(); i++){
	           String color = colorInCircle(mCircles.get(0, i), mRgba, hsv);

	           //WHITE
	        	if (color.equals("white")){
	        		mCircles.put(0, i, mCircles.get(0, i)[0], mCircles.get(0, i)[1], mCircles.get(0, i)[2], WHITE);
	        	}
	        	//RED
	        	else if (color.equals("red")){
	        		mCircles.put(0, i, mCircles.get(0, i)[0], mCircles.get(0, i)[1], mCircles.get(0, i)[2], RED);
	        	}
	        	//YELLOW
	        	else if (color.equals("yellow")){
	        		mCircles.put(0, i, mCircles.get(0, i)[0], mCircles.get(0, i)[1], mCircles.get(0, i)[2], YELLOW);
	        	}
	        	else {
	        		mCircles.put(0, i, mCircles.get(0, i)[0], mCircles.get(0, i)[1], mCircles.get(0, i)[2], NOCOLOR);
	        	}
	     }
        
        determineColorBall(mCircles, mRgba, hsv);
        
        for(int i = 0; i < mCircles.cols(); i++){
	           Point center = new Point(mCircles.get(0,i)[0], mCircles.get(0,i)[1]);
	           int radius = (int)mCircles.get(0,i)[2];
	           
	           //draw center
	           Core.circle(m, center, 3, new Scalar(0,255,0), -1, 8, 0 );
	           
	           //WHITE
	        	if (mCircles.get(0, i)[3] == WHITE){
	        		Core.circle(m, center, radius, new Scalar(255,255,255), 2, 8, 0 );
	        	}
	        	//RED
	        	else if (mCircles.get(0, i)[3] == RED){
	        		Core.circle(m, center, radius, new Scalar(255,0,0), 2, 8, 0 );
	        	}
	        	//YELLOW
	        	else if (mCircles.get(0, i)[3] == YELLOW){
	        		Core.circle(m, center, radius, new Scalar(255,255,0), 2, 8, 0 );
	        	}
	     }
        hsv.release();
    }
    
    private void nextState(){
    	if (mState == State.ON_INIT){
    		mState = State.ON_START;
    	}
    	else if (mState == State.ON_START){
    		mState = State.ON_TRACKING_ELEMENT;
    	}
    	else if (mState == State.ON_TRACKING_ELEMENT){
    		mState = State.ON_VALIDATE;
    	}
    	else if (mState == State.ON_VALIDATE){
    		mState = State.ON_END;
    	}
    }
    
    private void takePicture(Mat mat, String name){
    	File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    	String filename = name + ".png";
    	File file = new File(path, filename);
    	filename = file.toString();
    	Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR);
    	Highgui.imwrite(filename, mat);
    	Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB);
    }
    
    //Suivant un cercle, on retourne la couleur de la boule qu'il entoure 
    @SuppressWarnings("finally")
	private String colorInCircle(double[] circle, Mat rgba, Mat hsv){
	    try{
		    Mat roi = hsv.submat(new Range((int)(circle[1]-circle[2]*0.5), (int)(circle[1]+circle[2]*0.5+1)), new Range((int)(circle[0]-circle[2]*0.5), (int)(circle[0]+circle[2]*0.5+1)));
		    Mat mask = new Mat(roi.rows(), roi.cols(), CvType.CV_8U);
		  
		    Scalar sumHsv = Core.mean(roi, mask);
		    Scalar sumRgba = converScalarHsv2Rgba(sumHsv);
		    
		    Log.i("colorHSV", sumHsv.val[0] + " " + sumHsv.val[1] + " " + sumHsv.val[2]);
		    Log.i("colorRGB", sumRgba.val[0] + " " + sumRgba.val[1] + " " + sumRgba.val[2]);
		    
		    //WHITE
		    if (sumHsv.val[1]<50){
		    	return "white";
		    }
		    //RED
		    if ((sumHsv.val[0] > 170 || sumHsv.val[0] < 16)){
		       	return "red";
		    }
		    //YELLOW
		    if (sumHsv.val[0] > 15 && sumHsv.val[0] < 60){
		        return "yellow";
		    }
	    }
	    finally{
	    	return "noColor";
	    }
    }
    
    private void findBallsContour(){
        mCircles = new Mat(1,3,CvType.CV_32FC4);
        List<MatOfPoint> ballContours = mDetector.getBallContours();
        ArrayList<MatOfPoint2f> contours_poly = new ArrayList<MatOfPoint2f>();
        ArrayList<Point> center = new ArrayList<Point>(ballContours.size());
        for (int i=0; i<ballContours.size(); i++){
        	contours_poly.add(new MatOfPoint2f());
        	center.add(new Point());
        }
        float[] radius = new float[]{0f};
        for (int i=0; i<ballContours.size(); i++){
        	Imgproc.approxPolyDP( new MatOfPoint2f(ballContours.get(i).toArray()), contours_poly.get(i), 3, true );
        	Imgproc.minEnclosingCircle( contours_poly.get(i), center.get(i), radius );
        	Core.circle(mRgba, center.get(i), 3, new Scalar(0,255,0), -1, 8, 0 );
        	Core.circle( mRgba, center.get(i), (int)radius[0], new Scalar(255,0,255), 2, 8, 0 );
        	mCircles.put(0, i, center.get(i).x, center.get(i).y, radius[0], NOCOLOR);
        }
    }
    
    private Scalar circleHsvColor(double[] circle, Mat hsv){
    	System.out.println("[TEST] circle=(" + circle[0] + ", " + circle[1] + ", " + circle[2] + ")");
    	Mat roi = hsv.submat(
    			new Range((int)(circle[1]-circle[2]*0.5), 
    			(int)(circle[1]+circle[2]*0.5+1)), 
    			new Range((int)(circle[0]-circle[2]*0.5), 
    			(int)(circle[0]+circle[2]*0.5+1)));
	    Mat mask = new Mat(roi.rows(), roi.cols(), CvType.CV_8U);
	    Scalar sumHsv = Core.mean(roi, mask);
	    return sumHsv;
    }
    
    private boolean determineColorBall(Mat circle, Mat rgba, Mat hsv){
    	double[] colors = new double[3];
    	double som = 0;
    	for (int i=0; i<3; i++){
    		colors[i] = circle.get(0, i)[3];
    		som += colors[i];
    	}

    	if (som == WHITE+RED+YELLOW){
    		return true;
    	}
    	if (som == 2*WHITE || som == 2*WHITE+RED || som == 2*YELLOW || som == 2*YELLOW+RED || som == 1*RED){
    		findYellowWhite(circle, hsv, som);
    	}
    	//Deux couleurs trouvees et une NOCOLOR
    	if (som%2 == 0 && som != NOCOLOR){
    		findLastColor(circle, hsv, som);
    	}
    	//else if (som == NOCOLOR || som == 3*WHITE || som == 3*YELLOW || som == 3*RED || som == 2*YELLOW+1*WHITE || som == 2*WHITE+1*YELLOW){
    	else {
    		if( circle.get(0, 0).length <= 3 ||
    			circle.get(0, 1).length <= 3 ||
    			circle.get(0, 2).length <= 3) // correction erreur
    			return false;
    		findAll(circle, hsv, som);
    	}
    	return true;
    }
    
    private void findYellowWhite(Mat circle, Mat hsv, double som){
    	int first = -1, second =-1;
    	if (som==2*WHITE || som == 2*WHITE + RED){
    		for (int i=0; i<3; i++){
    			if (circle.get(0, i)[3] == WHITE && first == -1){
    				first = i;
    			}
    			else if (circle.get(0, i)[3] == WHITE){
    				second = i;
    			}
    		}
        	Scalar firstHsv = circleHsvColor(circle.get(0, first), hsv);
        	Scalar secondHsv = circleHsvColor(circle.get(0, second), hsv);
        	if (firstHsv.val[1]<secondHsv.val[1]){
        		circle.put(0, second,circle.get(0, second)[0], circle.get(0, second)[1], circle.get(0, second)[2], YELLOW);
        	}
        	else {
        		circle.put(0, first,circle.get(0, first)[0], circle.get(0, first)[1], circle.get(0, first)[2], YELLOW);
        	}
    	}
    	
    	else if (som==2*YELLOW || som==2*YELLOW+RED){
    		for (int i=0; i<3; i++){
    			if (circle.get(0, i)[3] == YELLOW && first == -1){
    				first = i;
    			}
    			else if (circle.get(0, i)[3] == YELLOW){
    				second = i;
    			}
    		}
        	Scalar firstHsv = circleHsvColor(circle.get(0, first), hsv);
        	Scalar secondHsv = circleHsvColor(circle.get(0, second), hsv);
        	if (firstHsv.val[1]>secondHsv.val[1]){
        		circle.put(0, second,circle.get(0, second)[0], circle.get(0, second)[1], circle.get(0, second)[2], WHITE);
        	}
        	else {
        		circle.put(0, first,circle.get(0, first)[0], circle.get(0, first)[1], circle.get(0, first)[2], WHITE);
        	}
    	}
    	
    	else if (som==1*RED){
    		for (int i=0; i<3; i++){
    			if (circle.get(0, i)[3] == NOCOLOR && first == -1){
    				first = i;
    			}
    			else if (circle.get(0, i)[3] == NOCOLOR){
    				second = i;
    			}
    		}
        	Scalar firstHsv = circleHsvColor(circle.get(0, first), hsv);
        	Scalar secondHsv = circleHsvColor(circle.get(0, second), hsv);
        	if (firstHsv.val[1]>secondHsv.val[1]){
        		circle.put(0, second,circle.get(0, second)[0], circle.get(0, second)[1], circle.get(0, second)[2], WHITE);
        		circle.put(0, first,circle.get(0, first)[0], circle.get(0, first)[1], circle.get(0, first)[2], YELLOW);
        	}
        	else {
        		circle.put(0, second,circle.get(0, second)[0], circle.get(0, second)[1], circle.get(0, second)[2], YELLOW);
        		circle.put(0, first,circle.get(0, first)[0], circle.get(0, first)[1], circle.get(0, first)[2], WHITE);
        	}
    	}
    }
    
    private void findAll(Mat circle, Mat hsv, double som){
    	Scalar firstHsv = circleHsvColor(circle.get(0, 0), hsv);
    	Scalar secondHsv = circleHsvColor(circle.get(0, 1), hsv);
    	Scalar thirdHsv = circleHsvColor(circle.get(0, 2), hsv);
    	int red = -1, yellow = -1;
    	if (firstHsv.val[1]<=secondHsv.val[1] && firstHsv.val[1]<=thirdHsv.val[1]){
    		circle.put(0, 0,circle.get(0, 0)[0], circle.get(0, 0)[1], circle.get(0, 0)[2], WHITE);
    		red = 1; yellow = 2;
    	}
    	else if (secondHsv.val[1]<=firstHsv.val[1] && secondHsv.val[1]<=thirdHsv.val[1]){
    		circle.put(0, 1,circle.get(0, 1)[0], circle.get(0, 1)[1], circle.get(0, 1)[2], WHITE);
    		red = 0; yellow = 2;
    	}
    	else {
    		circle.put(0, 2,circle.get(0, 2)[0], circle.get(0, 2)[1], circle.get(0, 2)[2], WHITE);
    		red = 0; yellow = 1;
    	}
    	
    	double dist1 = Math.abs(circleHsvColor(circle.get(0, red), hsv).val[0] - 30.);
    	double dist2 = Math.abs(circleHsvColor(circle.get(0, yellow), hsv).val[0] - 30.);
    	
    	if (dist1<dist2){
    		circle.put(0, red,circle.get(0, red)[0], circle.get(0, red)[1], circle.get(0, red)[2], YELLOW);
    		circle.put(0, yellow,circle.get(0, yellow)[0], circle.get(0, yellow)[1], circle.get(0, yellow)[2], RED);
    	}
    	else {
    		circle.put(0, red,circle.get(0, red)[0], circle.get(0, red)[1], circle.get(0, red)[2], RED);
    		circle.put(0, yellow,circle.get(0, yellow)[0], circle.get(0, yellow)[1], circle.get(0, yellow)[2], YELLOW);
    	}
    }
    
    private void findLastColor(Mat circle, Mat hsv, double som){
    	int last = -1;
    	double color = NOCOLOR;
    	boolean red = false, yellow = false, white = false;
    	
    	for (int i=0; i<3; i++){
    		if (circle.get(0,i)[3] == NOCOLOR){
    			last = i;
    		}
    		if (circle.get(0,i)[3] == WHITE){
    			white = true;
    		}
    		if (circle.get(0,i)[3] == RED){
    			red = true;
    		}
    		if (circle.get(0,i)[3] == YELLOW){
    			yellow = true;
    		}
    	}
    	
    	if (red && white) { color = YELLOW; }
    	if (red && yellow) { color = WHITE; }
    	if (yellow && white) { color = RED; }
    	
    	circle.put(0, last,circle.get(0, last)[0], circle.get(0, last)[1], circle.get(0, last)[2], color);
    }
    
    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
        return new Scalar(pointMatRgba.get(0, 0));
    }
    
    private boolean computeAreaColor(MotionEvent event){
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        int xOffset, yOffset;
        if(currentUseImportPicture){
        	xOffset = (mOpenCvImportImageView.getWidth() - cols) / 2;
            yOffset = (mOpenCvImportImageView.getHeight() - rows) / 2;
        }else{
        	xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
            yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
        }
        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;
        mTouchPoint.x = x;
        mTouchPoint.y = y;
        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
        Rect touchedRect = new Rect();
        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;
        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;
        Mat touchedRegionRgba = mRgba.submat(touchedRect);
        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;
        mDetector.setHsvColor(mBlobColorHsv);
        touchedRegionRgba.release();
        touchedRegionHsv.release();
        return true;
    }
    
    public ArrayList<Point> getRealTableVertices(){
    	ArrayList<Point> result = new ArrayList<Point>();
    	ArrayList<Point> realResult = new ArrayList<Point>();
    	int a=1,b=0; //Points qu'on va choisir

    	result = getListGoodPoint();
    	
    	double minDist = Math.sqrt((result.get(1).x-result.get(0).x)*(result.get(1).x-result.get(0).x)
    			+ (result.get(1).y-result.get(0).y)*(result.get(1).y-result.get(0).y));
    	for (int i=0; i<result.size(); i++){
    		for (int j=i+1; j<result.size(); j++){
	    		double dist = Math.sqrt((result.get(j).x-result.get(i).x)*(result.get(j).x-result.get(i).x)
	        			+ (result.get(j).y-result.get(i).y)*(result.get(j).y-result.get(i).y));
	    		if (dist<minDist){
	    			minDist=dist;
	    			a=i;
	    			b=j;
	    		}
    		}
    	}
    	if (mTable.getVoisinGauche(result.get(b)).x == result.get(a).x && mTable.getVoisinGauche(result.get(b)).y == result.get(a).y){
    		int tmp = a; a=b; b=tmp;
    	}
    	realResult.add(result.get(a));
    	realResult.add(result.get(b));
    	return realResult;
    }
    
    public ArrayList<Point> computeIntersections(ArrayList<Point> liste){
    	ArrayList<Point> reals = getListGoodPoint();
    	ArrayList<Integer> holes = getHoles();
    	ArrayList<ArrayList<Point>> lines = new ArrayList<ArrayList<Point>>();
    	ArrayList<Point> tmp = new ArrayList<Point>();
        int cols = mRgba.cols();
        int rows = mRgba.rows();
    	
        int xOffset, yOffset;
        if(currentUseImportPicture){
        	xOffset = (mOpenCvImportImageView.getWidth() - cols);
	        yOffset = (mOpenCvImportImageView.getHeight() - rows);
        }else{
        	xOffset = (mOpenCvCameraView.getWidth() - cols);
	        yOffset = (mOpenCvCameraView.getHeight() - rows);
        } 
        
    	Rect screen = new Rect(0,0,mOpenCvCameraView.getWidth()-xOffset, mOpenCvCameraView.getHeight()-yOffset);
    	
    	for (int i=0; i<mTableVertices.length; i++){
    		lines.add(new ArrayList<Point>());
    		lines.get(lines.size()-1).add(mTableVertices[i]);
    		if (i==mTableVertices.length-1){lines.get(lines.size()-1).add(mTableVertices[0]);}
    		else {lines.get(lines.size()-1).add(mTableVertices[i+1]);	}
    		lines.add(new ArrayList<Point>());
    		lines.get(lines.size()-1).add(mTableVertices[i]);
    		if (i==0){lines.get(lines.size()-1).add(mTableVertices[mTableVertices.length-1]);}
    		else {lines.get(lines.size()-1).add(mTableVertices[i-1]);}
    	}
    	for (int i=0; i<lines.size(); i++){
    		if (lines.get(i).get(0).x==lines.get(i).get(1).x || lines.get(i).get(0).y==lines.get(i).get(1).y){
    			lines.set(i, null);
    			continue;
    		}
    		int nbReals = 0;
    		for (int j=0; j<reals.size(); j++){
    			if (lines.get(i).get(0)==reals.get(j) || lines.get(i).get(1)==reals.get(j)){
    				nbReals++;
    			}
    		}
    		if (nbReals>1){
    			lines.set(i, null);
    		}
    	}
    	for (int i=0; i<lines.size(); i++){
    		for (int j=i+1; j<lines.size(); j++){
    			if (lines.get(i)==null){
    				break;
    			}
    			if (lines.get(j)==null){
    				continue;
    			}
    			int nbReals = 0;
    			int first=-1, second=-1;
    			for (int k=0; k<reals.size(); k++){
	    			if (lines.get(i).get(0).x==reals.get(k).x && lines.get(i).get(0).y==reals.get(k).y){
	    				nbReals++;
	    				if (first==-1){first=k;} else {second=k;}
	    			}
	    			if (lines.get(i).get(1).x==reals.get(k).x && lines.get(i).get(1).y==reals.get(k).y){
	    				nbReals++;
	    				if (first==-1){first=k;} else {second=k;}
	    			}
	    			if (lines.get(j).get(0).x==reals.get(k).x && lines.get(j).get(0).y==reals.get(k).y){
	    				nbReals++;
	    				if (first==-1){first=k;} else {second=k;}
	    			}
	    			if (lines.get(j).get(1).x==reals.get(k).x && lines.get(j).get(1).y==reals.get(k).y){
	    				nbReals++;
	    				if (first==-1){first=k;} else {second=k;}
	    			}
    			}
    			if (nbReals>1){
    				if (mTable.getVoisinGauche(reals.get(first)).x == reals.get(second).x && mTable.getVoisinGauche(reals.get(first)).y == reals.get(second).y
    				 || mTable.getVoisinDroite(reals.get(first)).x == reals.get(second).x && mTable.getVoisinDroite(reals.get(first)).y == reals.get(second).y){
    					continue;
    				}
    			}
    			Point r = intersection(lines.get(i).get(0), lines.get(i).get(1), lines.get(j).get(0), lines.get(j).get(1));
    			if (r!=null && !r.inside(screen)){
    				tmp.add(r);
    			}
    		}
    	}
    	//supprimer les doublons
    	for (int i=0; i<tmp.size(); i++){
    		for (int j=i+1; j<tmp.size(); j++){
    			if (tmp.get(i).x > tmp.get(j).x-1e-8 && tmp.get(i).x < tmp.get(j).x+1e-8 
    			 && tmp.get(i).y > tmp.get(j).y-1e-8 && tmp.get(i).y < tmp.get(j).y+1e-8 ){
    				tmp.set(i, null);
    				break;
    			}
    		}
    	}
    	
    	int k = 0;
    	for (int i=0; i<tmp.size(); i++){
    		if (tmp.get(i)!=null){
    			liste.add(holes.get(k),tmp.get(i));
    			k++;
    		}
    	}
    	if (decal>0 && mFirst!=1){
    		mFirst-=decal;
    		if (mFirst<0){
    			mFirst = 4-mFirst;
    		}
    	}
    	return liste;
    }
    
	 // Finds the intersection of two lines, or returns false.
	 // The lines are defined by (o1, p1) and (o2, p2).
	 public Point intersection(Point o1, Point p1, Point o2, Point p2){
	     Point x = new Point(o2.x - o1.x, o2.y - o1.y);
	     Point d1 = new Point(p1.x - o1.x, p1.y - o1.y);
	     Point d2 = new Point(p2.x - o2.x, p2.y - o2.y);
	
	     double cross = d1.x*d2.y - d1.y*d2.x;
	     if (Math.abs(cross) < /*EPS*/1e-8){
	         return null;
	     }
	     
	     double t1 = (x.x * d2.y - x.y * d2.x)/cross;
	     return new Point(o1.x+d1.x*t1, o1.y+d1.y*t1);
	 }
    
    public ArrayList<Point> getListGoodPoint(){
    	ArrayList<Point> result = new ArrayList<Point>();
    	for (int i=0; i<mTableVertices.length; i++){
    		if (mTableVertices[i].x > offSet && mTableVertices[i].x < width-offSet && mTableVertices[i].y > offSet && mTableVertices[i].y < height-offSet){
    			result.add(mTableVertices[i]);
    		}
    	}
    	return result;
    }
    
    private int decal=0;
    
    public ArrayList<Integer> getHoles(){
    	ArrayList<Integer> result = new ArrayList<Integer>();
    	int manque = 4-getListGoodPoint().size();
    	for (int i=0; i<mTableVertices.length; i++){
    		if (mTableVertices[i].x > offSet && mTableVertices[i].x < width-offSet && mTableVertices[i].y > offSet && mTableVertices[i].y < height-offSet){
    		}
    		else if (manque>0){
    			manque--;
    			if (mFirst>i){
    				decal=1;
    			}
    			if (i<3){
    				result.add(i);
    			}
    			else {
    				result.add(3);
    			}
    		}
    	}
    	return result;
    }
    
    
    /**On prend les deux points blancs les plus proches du coin k de la liste liste**/
    public List<MatOfPoint> getCLosestDots(int k, ArrayList<Point> liste, ArrayList<Point> dots, double penteBord){
        int first = 0, second =1;
        double dist1 = -1, dist2 = -1;
        
        for (int i=0; i<dots.size(); i++){
        	double dist3 = Math.sqrt((dots.get(i).x-liste.get(k).x)*(dots.get(i).x-liste.get(k).x)+(dots.get(i).y-liste.get(k).y)*(dots.get(i).y-liste.get(k).y));
        	ArrayList<Double> pentes = new ArrayList<Double>();
        	for (int j=0; j<dots.size(); j++){
        		if (i==j){continue;}
        		double pente = Math.abs((dots.get(j).y-dots.get(i).y)/(dots.get(j).x-dots.get(i).x));
        		pentes.add(Math.abs((double)1.-Math.abs(pente/penteBord)));
        	}
        	Collections.sort(pentes);
        	double mediane; 
        	if (pentes.size()%2!=0) { mediane = pentes.get(pentes.size()/2); }
        	else { mediane = pentes.get((pentes.size()/2)-1); } 
        	if ((penteBord<0.2 || mediane<0.1) && (dist1==-1 || dist3<dist1)){
        		dist2 = dist1;
        		second = first;
        		dist1=dist3;
        		first = i;
        	}
        	else if ((penteBord<0.2 || mediane<0.1) && (dist2==-1 || dist3<dist2)){
        		dist2=dist3;
        		second = i;
        	}
        }
        
        List<MatOfPoint> dotFinalContours = new ArrayList<MatOfPoint>();
        if ((dots.get(first).x-liste.get(k).x)*(dots.get(first).x-liste.get(k).x)+(dots.get(first).y-liste.get(k).y)*(dots.get(first).y-liste.get(k).y)
          > (dots.get(second).x-liste.get(k).x)*(dots.get(second).x-liste.get(k).x)+(dots.get(second).y-liste.get(k).y)*(dots.get(second).y-liste.get(k).y)){
        	int tmp = first; first = second; second = tmp;
        }
        dotFinalContours.add(new MatOfPoint(dots.get(first)));
        dotFinalContours.add(new MatOfPoint(dots.get(second)));
        return dotFinalContours;
    }
    
    public Point getTranformPoint(Point center){
    	Log.i("Point", mMatricePerspective.get(0,0)[0] + " " + mMatricePerspective.get(0,1)[0] + " " + mMatricePerspective.get(0,2)[0]);
    	double x, y,w;
    	x = mMatricePerspective.get(0,0)[0]*center.x + mMatricePerspective.get(0,1)[0]*center.y + mMatricePerspective.get(0,2)[0];
    	y = mMatricePerspective.get(1,0)[0]*center.x + mMatricePerspective.get(1,1)[0]*center.y + mMatricePerspective.get(1,2)[0];
    	w = mMatricePerspective.get(2,0)[0]*center.x + mMatricePerspective.get(2,1)[0]*center.y + mMatricePerspective.get(2,2)[0];
    	x/=w;
    	y/=w;
    	return new Point(x,y);
    }
    
    public ArrayList<ArrayList<Point>> harrisTracker(Mat gray, Mat mask, Mat model, Mat mask2, Mat model2, Rect zone1, Rect zone2){
    	ArrayList<Point> dots = new ArrayList<Point>();
    	ArrayList<Point> dots2 = new ArrayList<Point>();
    	Mat dst, dst_norm, dst_norm_scaled;
        dst = new Mat(mRgba.size(), CvType.CV_32FC1, new Scalar(0));
        dst_norm = new Mat();
        dst_norm_scaled = new Mat();
        
        Imgproc.cornerHarris(gray, dst, 2, 3, 0.01, Imgproc.BORDER_DEFAULT);
        Core.normalize(dst, dst_norm, 50, 255, Core.NORM_MINMAX, CvType.CV_32FC1, new Mat());
        Core.convertScaleAbs(dst_norm, dst_norm_scaled);
        
        int radius = mRgba.rows()/150;
        int dist = (3*radius)*(3*radius); /*!!!!!!!!!!!!!!!!*/
        int tresh = (int)Core.mean(dst_norm_scaled,mask).val[0]+1;
        ArrayList<Point> liste = new ArrayList<Point>();
        ArrayList<Point> liste2 = new ArrayList<Point>();
        
        if (zone1.x<0){zone1.x=0;}
        if (zone1.y<0){zone1.y=0;}
        if (zone2.x<0){zone2.x=0;}
        if (zone2.y<0){zone2.y=0;}
        if (zone1.br().x>mRgba.cols()){zone1.width-=zone1.br().x-mRgba.cols();}
        if (zone1.br().y>mRgba.rows()){zone1.height-=zone1.br().y-mRgba.rows();}
        if (zone2.br().x>mRgba.cols()){zone2.width-=zone2.br().x-mRgba.cols();}
        if (zone2.br().y>mRgba.rows()){zone2.height-=zone2.br().y-mRgba.rows();}
        
        for( int j = zone1.y+1; j < zone1.br().y; j++ ){ 
        	for( int i = zone1.x+1; i < zone1.br().x; i++ ){
                  if(model.get(j,i)[0]==255 && gray.get(j,i)[0]>20 && (int)dst_norm_scaled.get(j,i)[0] > tresh){
                	  Point p = new Point(i,j);
                	 /* boolean fusion = false;
                	  for (int k=0; k<liste.size(); k++){
                		  if ((liste.get(k).x-p.x)*(liste.get(k).x-p.x)+(liste.get(k).y-p.y)*(liste.get(k).y-p.y)<dist){
                			  liste.set(k, new Point((liste.get(k).x+p.x)/2, (liste.get(k).y+p.y)/2));
                			  fusion = true;
                			  break;
                		  }
                	  }*/
                	  //if (!fusion){
                		  liste.add(p);
                	  //}
                   }
        	}
        }
        for( int j = zone2.y+1; j < zone2.br().y; j++ ){ 
        	for( int i = zone2.x+1; i < zone2.br().x; i++ ){
                  if(model2.get(j,i)[0]==255 && gray.get(j,i)[0]>20 && (int)dst_norm_scaled.get(j,i)[0] > tresh){
                	  Point p = new Point(i,j);
                	/*  boolean fusion = false;
                	  for (int k=0; k<liste2.size(); k++){
                		  if ((liste2.get(k).x-p.x)*(liste2.get(k).x-p.x)+(liste2.get(k).y-p.y)*(liste2.get(k).y-p.y)<dist){
                			  liste2.set(k, new Point((liste2.get(k).x+p.x)/2, (liste2.get(k).y+p.y)/2));
                			  fusion = true;
                			  break;
                		  }
                	  }*/
                	  //if (!fusion){
                		  liste2.add(p);
                	  //}
                   }
                }
           }
        
        Imgproc.cvtColor(dst_norm_scaled, dst_norm_scaled, Imgproc.COLOR_GRAY2RGB);
        
        for( int i = 0; i < liste.size(); i++ ){
        	boolean suppr = false;
        	for (int j=i+1; j<liste.size();j++){
        		if (i==j){continue;}
        		if ((liste.get(i).x-liste.get(j).x)*(liste.get(i).x-liste.get(j).x)+(liste.get(i).y-liste.get(j).y)*(liste.get(i).y-liste.get(j).y)<dist*2){
        			suppr = true;
        		}
        	}
        	if (!suppr){
        		Core.circle( dst_norm_scaled, liste.get(i), radius, new Scalar(255,0,0), 2, 8, 0 );
        		dots.add(liste.get(i));
        	}
        }
        
        for( int i = 0; i < liste2.size(); i++ ){
        	boolean suppr = false;
        	for (int j=i+1; j<liste2.size();j++){
        		if (i==j){continue;}
        		if ((liste2.get(i).x-liste2.get(j).x)*(liste2.get(i).x-liste2.get(j).x)+(liste2.get(i).y-liste2.get(j).y)*(liste2.get(i).y-liste2.get(j).y)<dist*2){
        			suppr = true;
        		}
        	}
        	if (!suppr){
        		Core.circle( dst_norm_scaled, liste2.get(i), radius, new Scalar(0,255,0), 2, 8, 0 );
        		dots2.add(liste2.get(i));
        	}
        }
        
        takePicture(dst_norm_scaled, "harris.png");
        
        ArrayList<ArrayList<Point>> dotsFinals = new ArrayList<ArrayList<Point>>();
        dotsFinals.add(dots);
        dotsFinals.add(dots2);
        
        return dotsFinals;
    }
    
    public void drawContours(int first){
    	mFirst = first;
    	int flag = 0;
    	String lastColor="red";
    	for (int i=first; i<mTableVertices.length-1; i++){
    		if (mTable.out(i) && mTable.out(i+1)){
    			flag=3;
    		}else if (lastColor.equals("blue")){
    			flag=1; lastColor = "red";
    		}else { flag = 0; lastColor = "blue";}
    		drawLine(i, i+1, flag);
    	}
		if (mTable.out(mTableVertices.length-1) && mTable.out(0)){
			flag=3;
		}else if (lastColor.equals("blue")){
			flag=1; lastColor = "red";
		}else { flag = 0; lastColor = "blue";}
    	drawLine(mTableVertices.length-1, 0, flag);
    	for (int i=0; i<first; i++){
    		if (mTable.out(i) && mTable.out(i+1)){
    			flag=3;
    		}else if (lastColor.equals("blue")){
    			flag=1; lastColor = "red";
    		}else { flag = 0; lastColor = "blue";}
    		drawLine(i, i+1, flag);
    	}
    }
    
    public void drawLine(int i, int j, int flag){
    	if (flag==0){
    		Core.line(mRgba, mTable.get(i), mTable.get(j), new Scalar(255,0,0), 3);
    	}else if (flag==1){
    		Core.line(mRgba, mTable.get(i), mTable.get(j), new Scalar(0,0,255), 3);
    	}else {
    		Core.line(mRgba, mTable.get(i), mTable.get(j), new Scalar(0,0,0), 1);
    	}
    }
    
    public Mat setVerticesOfTable(ArrayList<Point> liste){
    	Mat table = new Mat(1,4,CvType.CV_32FC2);
    	int k = 0;
    	for (int i=mFirst; i<liste.size(); i++){
    		table.put(0, k, liste.get(i).x, liste.get(i).y);
    		k++;
    	}
    	for (int i=0; i<mFirst; i++){
    		table.put(0, k, liste.get(i).x, liste.get(i).y);
    		k++;
    	}
    	
    	Core.circle(mRgba, new Point(table.get(0,0)[0], table.get(0,0)[1]), 6, new Scalar(255,0,0), -1, 8, 0 );
    	Core.circle(mRgba, new Point(table.get(0,1)[0], table.get(0,1)[1]), 6, new Scalar(0,255,0), -1, 8, 0 );
    	Core.circle(mRgba, new Point(table.get(0,2)[0], table.get(0,2)[1]), 6, new Scalar(0,0,255), -1, 8, 0 );
    	Core.circle(mRgba, new Point(table.get(0,3)[0], table.get(0,3)[1]), 6, new Scalar(255,255,255), -1, 8, 0 );
    	
    	return table;
    }
    
    /*
     * Retourne la liste des boules avec leurs coordonnées respectives
     */
    public ArrayList<Pair<String,Pair<Double,Double>>> getPosition(){
    	ArrayList<Pair<String,Pair<Double,Double>>> listeBoules = new ArrayList<Pair<String,Pair<Double,Double>>>();
    	
    	int xOffset, yOffset;
        if(currentUseImportPicture){
        	xOffset = (mOpenCvImportImageView.getWidth() - mRgba.cols());
	        yOffset = (mOpenCvImportImageView.getHeight() - mRgba.rows());
        }else{
        	xOffset = (mOpenCvCameraView.getWidth() - mRgba.cols());
	        yOffset = (mOpenCvCameraView.getHeight() - mRgba.rows());
        }
        
        double xMin, xMax, yMin, yMax;
        if(currentUseImportPicture){
        	xMin = (mOpenCvImportImageView.getWidth()-xOffset)/8;
            xMax = mOpenCvImportImageView.getWidth()-xOffset-xMin;
            yMin = ((mOpenCvImportImageView.getHeight()-yOffset)-(3./8.)*(mOpenCvImportImageView.getWidth()-xOffset))/2;
            yMax = mOpenCvImportImageView.getHeight()-yOffset-yMin;
        }else{
        	xMin = (mOpenCvCameraView.getWidth()-xOffset)/8;
            xMax = mOpenCvCameraView.getWidth()-xOffset-xMin;
            yMin = ((mOpenCvCameraView.getHeight()-yOffset)-(3./8.)*(mOpenCvCameraView.getWidth()-xOffset))/2;
            yMax = mOpenCvCameraView.getHeight()-yOffset-yMin;
        }        
        
    	for (int i=0; i<mCircles.cols(); i++){
    		Point pProj = getTranformPoint(new Point(mCircles.get(0,i)[0],mCircles.get(0,i)[1]));
            double u = (pProj.x-xMin)/(xMax-xMin);
            double v = 1-(pProj.y-yMin)/(yMax-yMin);
    		if (mCircles.get(0,i)[3]==RED){
    			Pair<String,Pair<Double,Double>> red = new Pair<String,Pair<Double,Double>>("red",new Pair<Double,Double>(u,v));
    			listeBoules.add(red);
    		}
    		if (mCircles.get(0,i)[3]==YELLOW){
    			Pair<String,Pair<Double,Double>> red = new Pair<String,Pair<Double,Double>>("yellow",new Pair<Double,Double>(u,v));
    			listeBoules.add(red);
    		}
    		if (mCircles.get(0,i)[3]==WHITE){
    			Pair<String,Pair<Double,Double>> red = new Pair<String,Pair<Double,Double>>("white",new Pair<Double,Double>(u,v));
    			listeBoules.add(red);
    		}
    	}
		return listeBoules;
    }
    
    
    
    
    public boolean onCreateOptionsMenu(Menu menu) {  
        getMenuInflater().inflate(R.menu.main, menu);
        return true;  
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {  
        switch (item.getItemId()) {  
            case R.id.itemChooseWhite:
            	Toast.makeText(getApplicationContext(),"Vous avez la boule blanche", Toast.LENGTH_LONG).show();
            	MenuActivity.boule = 0;
            	return true;
           case R.id.itemChooseYellow:
                Toast.makeText(getApplicationContext(),"Vous avez la boule jaune", Toast.LENGTH_LONG).show();
                MenuActivity.boule = 1;
                return true;
           case R.id.itemChoosePicture:
        	   // on demande à l'utilisateur de choisir une image
        	   mState = State.ON_IMPORT_PICTURE;
               Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                       android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
               startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
               return true;
           case R.id.itemMainMenu:
        	// Passer du MainActivity au Menu
               Intent i = new Intent(MainActivity.this, MenuActivity.class);
               startActivity(i);
               finish();
               return true;
           default:
               return super.onOptionsItemSelected(item);
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG){
            	if(resultCode == RESULT_OK && data != null) {
                    importImageUri = data.getData();
                    importImage(importImageUri);
	            } else {
	                Toast.makeText(this, "Vous n'avez pas séléctionné d'image.", Toast.LENGTH_LONG).show();
	            	mState = State.ON_START;
	                mSwitchText = true;
	                mError = false;
	            }
            }
        } catch (Exception e) {
        	System.out.println("[TEST] Erreur onActivityResult");
        }
    }
    
    private void importImage(Uri importImageUri){
    	mState = State.ON_PICTURE_START;

        // on dit qu'on ne travaille plus sur la camera, mais sur une image importé
        currentUseImportPicture = true;
        
        // on affiche l'image sélectionnée
        mOpenCvImportImageView.setImageURI(null); // réinitialise l'imageView
        mOpenCvImportImageView.setImageURI(importImageUri);
        mOpenCvImportImageView.setVisibility(0);
        
        // on masque la caméraView
        mOpenCvCameraView.setVisibility(View.GONE);
        
        // on importe l'image dans la Mat mRgba (pour les calculs)
        ImportImageUriToMat(importImageUri);

        // on update les boutons à afficher
        mButtons[0].setVisibility(View.GONE);
    	mButtons[1].setVisibility(View.GONE);
    	mButtons[2].setVisibility(View.GONE);
    	mButtons[3].setVisibility(0); // on affiche le bouton annuler (les autres sont masqués)
    	
    	// on dit à l'utilisateur d'appuyer sur une zone vide de la table
    	TextView t = (TextView) findViewById(R.id.textColor);
	    t.setText("Touchez une zone vide sur la table");
        t.setBackgroundColor(0xff0000FF);
    }
    
    private void ImportImageUriToMat(Uri importImageUri){
    	// On récupère le chemin de l'image
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(importImageUri, filePathColumn, null, null, null);
        cursor.moveToFirst(); 
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String imgPath = cursor.getString(columnIndex);
        cursor.close();
        
        //System.out.println("[TEST] imgPath : " + imgPath);
        // on récupère l'image dans mRgba
        mRgba = Highgui.imread(imgPath);
        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2RGB); // because Highgui.imread upload image in B G R order.
    }
}