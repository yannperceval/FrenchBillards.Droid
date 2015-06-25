package com.example.testopencv;

import java.util.ArrayList;

import simulation.Simulation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class SolutionCanvasView extends SurfaceView implements SurfaceHolder.Callback {
	private static final int 	sizeBandeCm 	= 10;
	private static final int 	sizeDemiBandeCm = sizeBandeCm/2;
	private static final double	sizeBordureCm 	= 8.81; // bordure du billard sur l'image
	
	private double w, h;
	private final int 			type 			= 280;	
	private double 				heightBillardCm; // (SANS BANDE)
	private double 				widthBillardCm;  // (SANS BANDE)
	private static final double sizeBouleCm 	= 6.15;

	private SurfaceHolder mHolder = null;
	
	private Paint paint = new Paint();
	
	private Rect billard = new Rect();
	private double widthBillard,	// = (billard.right  - billard.left) (AVEC BANDE)
		   		   heightBillard;	// = (billard.height - billard.top)  (AVEC BANDE)
	
	private int margin;
	private int border;
	private int sizeOfBoule;
	private int radiusOfBoule;
	
	public boolean showMenu = true;
	
	// boulemove : affichage des boules en mouvement (tous les ticks de la simulation)
	private ArrayList<Pair<Integer, Pair<Double, Double>>> boulemove; // Pair<Couleur, Pair<x, y>>
	private double cxb = -1, cyb = -1;
	private double cxr = -1, cyr = -1;
	private double cxj = -1, cyj = -1;
	private double angle = -1; // solution trouvée par le simulateur : angle de tir en radian
	private double force = -1; // solution trouvée par le simulateur : force entre 0 et 100
	private boolean showTrouver = false;
	
	public ImageButton startAnimationButton;
	public ImageButton stopAnimationButton;
	private boolean animation = false;
	public boolean isAnimate(){ return animation; }
	private boolean pause = false;
	private int waitOnEndAnim;
	private int posAnim;
	
	public ImageButton inverseButton;
	private boolean inverse = false;
	
	private static final boolean dessin = false;	
	
	private boolean initCanvas = false;
	
	
	// for game (entrainement + multijoueur)
	public ImageView imagejoueur1, imagejoueur2;
	public TextView  textejoueur1, textejoueur2;
	private int scoreJoueur1 = 0,
				scoreJoueur2 = 0;
	public TextView  angleText = null;
	public TextView  forceText = null;
	
	public boolean coupReussi = false;
	
	
	Paint paintTiret;
	
	
	public SolutionCanvasView(Context context) {
		super(context);
		init();
	}
	public SolutionCanvasView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init();
	}
	public SolutionCanvasView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
		init();
	}
	private void init(){
		mHolder = getHolder();
		mHolder.addCallback(this);
		boulemove = new ArrayList<Pair<Integer, Pair<Double,Double>>>();
		
		switch (type)
	    {
	        case 310 : h = 142.3; w = 284.5;
	            break;
	        case 280 : h = 127.0; w = 254.0;
	            break;
	        case 260 : h = 115.0; w = 230.0;
	            break;
	        default :  h = 127.0; w = 254.0;
	    }
		heightBillardCm = h;
		widthBillardCm  = w;
		
		
		paintTiret = new Paint();
		paintTiret.setARGB(127, 0, 0,0);
		paintTiret.setStyle(Style.STROKE);
		paintTiret.setPathEffect(new DashPathEffect(new float[] {50.f, 20.f}, 0));
	}
	
	/**
	 * @brief Pivote le billard de 180° à l'affichage
	 */
	public void inverseBillard(){
		inverse = !inverse;
		if(cxb >= 0) cxb = 1 - cxb;
		if(cyb >= 0) cyb = 1 - cyb;
		if(cxr >= 0) cxr = 1 - cxr;
		if(cyr >= 0) cyr = 1 - cyr;
		if(cxj >= 0) cxj = 1 - cxj;
		if(cyj >= 0) cyj = 1 - cyj;
		if(angle >= 0) angle = (angle + Math.PI) % (2*Math.PI);
		for(int i = 0; i != boulemove.size(); ++i){
			Pair<Integer, Pair<Double, Double>> pos = boulemove.get(i);
			boulemove.set(i, new Pair<Integer, Pair<Double, Double>>(pos.first, new Pair<Double, Double>(1-pos.second.first, 1-pos.second.second)));
		}
		refresh();
	}
	
	/**
	 * @brief Arrête immédiatement l'animation du meilleur coup à jouer
	 */
	public void StopAnimation(){
		if(MenuActivity.choix == MenuActivity.Mode.MEILLEUR_COUP){
			startAnimationButton.setImageResource(R.drawable.anim_start);
			stopAnimationButton.setVisibility(View.GONE);
		}else{
			if(boulemove.size() >= 3){
				for(int i = boulemove.size()-3; i != boulemove.size(); ++i){
					Pair<Integer, Pair<Double, Double>> pos = boulemove.get(i);
					if(pos.first == Color.WHITE){
						cxb = pos.second.first;
						cyb = pos.second.second;
					}else if(pos.first == Color.YELLOW){
						cxj = pos.second.first;
						cyj = pos.second.second;					
					}else if(pos.first == Color.RED){
						cxr = pos.second.first;
						cyr = pos.second.second;					
					}
				}
			}
			boulemove.clear();
			if(coupReussi){
				if(MenuActivity.choix == MenuActivity.Mode.ENTRAINEMENT){
					textejoueur1.setText("Joueur : " + ++scoreJoueur1  + " point " + (scoreJoueur1>1?"s":""));
				}else{ // == MenuActivity.Mode.MULTIJOUEUR
					if(MenuActivity.boule==0){
						textejoueur1.setText("Joueur 1 : " + ++scoreJoueur1  + " point " + (scoreJoueur1>1?"s":""));
					}else{
						textejoueur2.setText(++scoreJoueur2 + " point " + (scoreJoueur2>1?"s":"") + " : Joueur 2");
					}
				}
			}else{
				if(MenuActivity.choix == MenuActivity.Mode.ENTRAINEMENT){
					// on utilise scoreJoueur2 pour compter le nombre de coups ratés lorsqu'on est en entraînement
					textejoueur2.setText(++scoreJoueur2 + " coup" + (scoreJoueur2>1?"s":"") + " raté" + (scoreJoueur2>1?"s":""));
				}else{ // == MenuActivity.Mode.MULTIJOUEUR
					MenuActivity.boule = MenuActivity.boule==0?1:0; // changement de joueur
					if(MenuActivity.boule==0){
						imagejoueur1.setAlpha((float)1.0);
						imagejoueur2.setAlpha((float)0.5);
						textejoueur1.setAlpha((float)1.0);
						textejoueur2.setAlpha((float)0.5);
					}else{
						imagejoueur1.setAlpha((float)0.5);
						imagejoueur2.setAlpha((float)1.0);
						textejoueur1.setAlpha((float)0.5);
						textejoueur2.setAlpha((float)1.0);
					}
				}
			}
		}
		animation = false;
		pause = false;
		refresh();
	}
	/**
	 * @brief Démarre ou met en pause l'animation du meilleur coup à jouer
	 */
	public void StartStopAnimation(){
		if(MenuActivity.choix == MenuActivity.Mode.MEILLEUR_COUP){
			if(animation){
				pause = !pause;
				if(pause)	startAnimationButton.setImageResource(R.drawable.anim_start);
				else		startAnimationButton.setImageResource(R.drawable.anim_pause);
				refresh();
				return;
			}
			stopAnimationButton.setVisibility(0);
			startAnimationButton.setImageResource(R.drawable.anim_pause);
		}
		
		animation = true;		
		posAnim = 0; 
	    
	    final Handler handler = new Handler();
	    
	    final int delay = 50;
		waitOnEndAnim = 2000/delay; // wait 2000 ms after end of animation
		if(MenuActivity.choix != MenuActivity.Mode.MEILLEUR_COUP)
			waitOnEndAnim = -1;
	    
	    final Runnable runnable = new Runnable() {
			public void run() {
				//System.out.println("[TEST] Animation : posAnim=" + posAnim);
				if(posAnim < boulemove.size()-1){
	        		if(!pause){
	        			posAnim += 3;
	        			if(posAnim >= boulemove.size())
	        				posAnim = boulemove.size()-1;
		        		refresh();
	        		}
	        	}else if(waitOnEndAnim-- <= 0){
	        		//System.out.println("[TEST] Animation : END");
	        		if(!pause)
	        			StopAnimation();
		        }
		        
		        if(animation) handler.postDelayed(this, delay);			
			}
	    };
	    //System.out.println("[TEST] Animation : START");
		handler.postDelayed(runnable, delay);
	}

	public void lancerSimulation(){
		// Simulation : Task
    	Simulation s = new Simulation();
    	if(MenuActivity.boule == 0) // l'utilisateur a la boule blanche
    		s.updateParam(this, type, cxb, 1-cyb, cxj, 1-cyj, cxr, 1-cyr);
    	else // l'utilisateur a la boule jaune
    		s.updateParam(this, type, cxj, 1-cyj, cxb, 1-cyb, cxr, 1-cyr);
    	s.executeEtAfficheCoup(force, angle);
	}
	
	private Bitmap bitmap_billard_280;
	private Bitmap bitmap_bille_blanche, bitmap_bille_jaune, bitmap_bille_rouge;
	private int widthCanvas = 0;

	private void initOnDraw(Canvas canvas){
		margin = canvas.getWidth()/20;
		border = margin/10;
		double aw, ah;
		if(dessin){
			aw = (widthBillardCm + sizeBandeCm)/(canvas.getWidth()  - 2*margin);
			ah = (heightBillardCm + sizeBandeCm)/(canvas.getHeight() - 2*margin);

			double bw = aw>=ah?1:(aw/ah);
			double bh = ah>=aw?1:(ah/aw);
			aw = bw; ah = bh;
			
			widthBillard  = (canvas.getWidth()  - 2*margin)*aw; // taille avec bande
			heightBillard = (canvas.getHeight() - 2*margin)*ah; // taille avec bande
			
			billard.left	= margin + (int)(((canvas.getWidth()  - 2*margin)  - widthBillard)/2);
			billard.top		= margin + (int)(((canvas.getHeight()  - 2*margin) - heightBillard)/2);
		}else{
			aw = (widthBillardCm + sizeBandeCm + sizeBordureCm*2)/canvas.getWidth();
			ah = (heightBillardCm + sizeBandeCm + sizeBordureCm*2)/canvas.getHeight();
			
			double bw = aw>=ah?1:(aw/ah);
			double bh = ah>=aw?1:(ah/aw);
			aw = bw; ah = bh;
			
			aw *= widthBillardCm  / (widthBillardCm  + sizeBandeCm + sizeBordureCm*2);
			ah *= heightBillardCm / (heightBillardCm + sizeBandeCm + sizeBordureCm*2);
			
			widthBillard  = canvas.getWidth()*aw;  // taille sans bande
			heightBillard = canvas.getHeight()*ah; // taille sans bande
			
			billard.left	= (int)((canvas.getWidth()  - widthBillard ) / 2);
			billard.top		= (int)((canvas.getHeight() - heightBillard) / 2);
		}		
		
		billard.right	= canvas.getWidth() - billard.left;
		billard.bottom	= canvas.getHeight() - billard.top;
		
		if(dessin){
			sizeOfBoule = (int) (widthBillard * sizeBouleCm / (widthBillardCm + sizeBandeCm));
		}else{
			sizeOfBoule = (int) (widthBillard * sizeBouleCm / widthBillardCm);
		}
		radiusOfBoule = sizeOfBoule/2;
		
		// on charge les images
		if(!dessin){
			bitmap_billard_280 = BitmapFactory.decodeResource(getResources(), R.drawable.billard_280);
			bitmap_billard_280 = Bitmap.createScaledBitmap(bitmap_billard_280, 
					(int)(widthBillard  * ( widthBillardCm + sizeBandeCm + 2*sizeBordureCm) /  widthBillardCm), 
					(int)(heightBillard * (heightBillardCm + sizeBandeCm + 2*sizeBordureCm) / heightBillardCm),false);
		
			bitmap_bille_blanche = BitmapFactory.decodeResource(getResources(), R.drawable.bille_blanche);
			bitmap_bille_blanche = Bitmap.createScaledBitmap(bitmap_bille_blanche, sizeOfBoule, sizeOfBoule, false);
			
			bitmap_bille_jaune = BitmapFactory.decodeResource(getResources(), R.drawable.bille_jaune);
			bitmap_bille_jaune = Bitmap.createScaledBitmap(bitmap_bille_jaune, sizeOfBoule, sizeOfBoule, false);
			
			bitmap_bille_rouge = BitmapFactory.decodeResource(getResources(), R.drawable.bille_rouge);
			bitmap_bille_rouge = Bitmap.createScaledBitmap(bitmap_bille_rouge, sizeOfBoule, sizeOfBoule, false);
		}
	}
	
	@SuppressLint("DrawAllocation") 
	protected void onDraw(Canvas canvas){
		if(!initCanvas || widthCanvas != canvas.getWidth()){
			widthCanvas = canvas.getWidth(); // juste pour tester
			initCanvas = true;
			initOnDraw(canvas);
		}
		
		// billard
		afficherBillard(canvas);

		// on affiche les boules (emplacements initiaux des boules si animation en cours)
		if(MenuActivity.choix == MenuActivity.Mode.MEILLEUR_COUP || !animation){
			afficherBoulesOrigines(canvas);
		}

		// tracé des boules (trace et animation)
		afficherAnimationBoules(canvas);
		
		// affichage de la queue
		if(!animation){
			afficherQueue(canvas);
		}
		
		// afficher les messages lorsqu'un coup est disponible ou si on est encore en cours de calcul
		if(MenuActivity.choix == MenuActivity.Mode.MEILLEUR_COUP){
			afficherMessage(canvas);
		}
	}
	
	private void afficherBillard(Canvas canvas){
		// billard version dessin
		if(dessin){
			paint.setColor(Color.BLACK);
			paint.setStrokeWidth(3);
			canvas.drawRect(billard.left, billard.top, billard.right, billard.bottom, paint);
			
			paint.setColor(Color.argb(255, 43, 135, 60));
			canvas.drawRect(billard.left +border, billard.top +border, billard.right -border, billard.bottom -border, paint);
		
		// billard version image
		}else{
			Matrix matrix = new Matrix();
			matrix.preTranslate((float)(billard.left - (widthBillard * (sizeDemiBandeCm + sizeBordureCm) / widthBillardCm)), (float)(billard.top - (heightBillard * (sizeDemiBandeCm + sizeBordureCm) / heightBillardCm)));
			canvas.drawBitmap(bitmap_billard_280, matrix, null);
		}
	}
	private void afficherMessage(Canvas canvas){
		if(angle < 0){
			String txt = "Calcul en cours...";
			int sizeOfText = margin*2/3;
			if(paint.measureText(txt) > canvas.getWidth()/3)
				sizeOfText = determineMaxTextSize(txt, canvas.getWidth()/3);
			
			paint.setColor(Color.argb(220, 0, 0, 0));
			canvas.drawRect(
					canvas.getWidth()/3 					-margin*2, 
					canvas.getHeight()/2 	-sizeOfText/2 	-margin, 
					canvas.getWidth()*2/3					+margin*2, 
					canvas.getHeight()/2 	+sizeOfText/2 	+margin, paint);
			
			paint.setColor(Color.WHITE);			
			paint.setTextSize(sizeOfText);
			canvas.drawText(txt, canvas.getWidth()/3 + ((canvas.getWidth()/3) - paint.measureText(txt))/2, canvas.getHeight()/2 +sizeOfText/4, paint);
		}else{
			// affichage du message de réussite
			double vraiAngle = inverse?((angle + Math.PI) % (2*Math.PI)):angle;
			String coup = "Angle=" + (int)(vraiAngle/Math.PI*180) + "°  - Force=" + (int)(force)+ "%";
			if(showTrouver){
				String txt = "Calcul fini - " + coup;
				int sizeOfText = margin*2/3;
				if(paint.measureText(txt) > canvas.getWidth()/3)
					sizeOfText = determineMaxTextSize(txt, canvas.getWidth()/3);
				
				paint.setColor(Color.argb(220, 0, 0, 0));
				canvas.drawRect(
						canvas.getWidth()/3 					-margin*2, 
						canvas.getHeight()/2 	-sizeOfText/2 	-margin, 
						canvas.getWidth()*2/3					+margin*2, 
						canvas.getHeight()/2 	+sizeOfText/2 	+margin, paint);
				
				paint.setColor(Color.argb(220, 90, 167, 45));
				paint.setTextSize(sizeOfText);
				canvas.drawText(txt, canvas.getWidth()/3 + ((canvas.getWidth()/3) - paint.measureText(txt))/2, canvas.getHeight()/2 +sizeOfText/4, paint);
			}else if(showMenu){
				int sizeOfText = determineMaxTextSize(coup, canvas.getWidth()/4);				
				paint.setTextSize(sizeOfText);
				
				paint.setColor(Color.argb(180, 0, 0, 0));
				canvas.drawRect(
						0, 
						0, 
						margin + paint.measureText(coup), 
						margin + sizeOfText -sizeOfText/4, paint);				
				
				paint.setColor(Color.argb(220, 255, 255, 255));
				canvas.drawText(coup, margin/2, margin/2 + sizeOfText/2, paint);
			}
		}
	}
	private void afficherAnimationBoules(Canvas canvas){
		//final int nbBoulesAffOnAnimation = (MenuActivity.choix == MenuActivity.Mode.MEILLEUR_COUP)?3:1;
		
		for(int i = 0; i != boulemove.size(); ++i){
			if(animation && i < posAnim -3 && i > posAnim)
				continue;
			
			Pair<Integer, Pair<Double, Double>> pos = boulemove.get(i);
			
			//if(MenuActivity.choix == MenuActivity.Mode.MEILLEUR_COUP){
				if(animation){
			    	     if(i == posAnim   || i == posAnim-1 || i == posAnim-2) paint.setAlpha(255);
			    	else if(i == posAnim-3 || i == posAnim-4 || i == posAnim-5) paint.setAlpha(128);
			    	else if(i == posAnim-6 || i == posAnim-7 || i == posAnim-8) paint.setAlpha(64);
			    	else continue;
			    }else{
			    	paint.setAlpha(64);
			    }
			/*}else{
				paint.setAlpha(255);
			}*/
			
			if(dessin){
					 if(pos.first == Color.WHITE) 	paint.setColor(Color.argb(64, 255, 255, 255));
				else if(pos.first == Color.RED) 	paint.setColor(Color.argb(64, 255,   0,   0));
				else if(pos.first == Color.YELLOW) 	paint.setColor(Color.argb(64, 255, 255,   0));
				else continue;
			    
				canvas.drawCircle((float)(billard.left + widthBillard*pos.second.first), 
						(float)(billard.top + heightBillard*pos.second.second), 
						radiusOfBoule, paint);
			}else{
				Matrix matrix = new Matrix();
				matrix.preTranslate(
						(float)(billard.left + widthBillard*pos.second.first   - radiusOfBoule), 
						(float)(billard.top  + heightBillard*pos.second.second - radiusOfBoule));
				if(pos.first == Color.WHITE) 		canvas.drawBitmap(bitmap_bille_blanche, matrix, paint);
				else if(pos.first == Color.RED) 	canvas.drawBitmap(bitmap_bille_rouge,   matrix, paint);
				else if(pos.first == Color.YELLOW) 	canvas.drawBitmap(bitmap_bille_jaune,   matrix, paint);
				else continue;
			}
		}
	}
	@SuppressWarnings("unused")
	private void afficherBoulesOrigines(Canvas canvas){
		if(dessin && animation) paint.setStyle(Paint.Style.STROKE); // si animation : uniquement contour des boules de départ
		if(animation) paint.setAlpha(64);
		else		  paint.setAlpha(255);
		
		// boule blanche
		if(cxb > 0 && cyb > 0){
			if(dessin){
				paint.setColor(Color.WHITE);
				canvas.drawCircle(
						(float)(billard.left + widthBillard*cxb), 
						(float)(billard.top  + heightBillard*cyb), 
						radiusOfBoule, paint);
			}else{
				Matrix matrix = new Matrix();
				matrix.preTranslate(
						(float)(billard.left + widthBillard*cxb  - radiusOfBoule), 
						(float)(billard.top  + heightBillard*cyb - radiusOfBoule));
				canvas.drawBitmap(bitmap_bille_blanche, matrix, paint);
			}
		}
		
		// boule rouge
		if(cxr > 0 && cyr > 0){
			if(dessin){
				paint.setColor(Color.RED);
				canvas.drawCircle(
						(float)(billard.left + widthBillard*cxr), 
						(float)(billard.top  + heightBillard*cyr), 
						radiusOfBoule, paint);
			}else{
				Matrix matrix = new Matrix();
				matrix.preTranslate(
						(float)(billard.left + widthBillard*cxr  - radiusOfBoule), 
						(float)(billard.top  + heightBillard*cyr - radiusOfBoule));
				canvas.drawBitmap(bitmap_bille_rouge, matrix, paint);
			}
		}
	
		// boule jaune
		if(cxj > 0 && cyj > 0){
			if(dessin){
				paint.setColor(Color.YELLOW);
				canvas.drawCircle(
						(float)(billard.left + widthBillard*cxj), 
						(float)(billard.top + heightBillard*cyj), 
						radiusOfBoule, paint);
			}else{
				Matrix matrix = new Matrix();
				matrix.preTranslate(
						(float)(billard.left + widthBillard*cxj  - radiusOfBoule), 
						(float)(billard.top  + heightBillard*cyj - radiusOfBoule));				
				canvas.drawBitmap(bitmap_bille_jaune, matrix, paint);
			}
		}
		
		if(dessin && animation) paint.setStyle(Paint.Style.FILL); // rétabli le remplissage
	}
	private void afficherQueue(Canvas canvas){
		if(angle < 0) return;
		if((MenuActivity.boule == 0 && cxb < 0 && cyb < 0) || 
		   (MenuActivity.boule == 1 && cxj < 0 && cyj < 0))
				return;
		
		double cx = (MenuActivity.boule == 0)?cxb:cxj;
		double cy = (MenuActivity.boule == 0)?cyb:cyj;
		
		double angleOpo, angleCal;
		if(angle == 0){ // problème : je pense que angleOpo est approximé si j'utilise le else -> angleOpo est considéré comme < PI au lieu d'être égal à PI
			angleOpo = Math.PI;
			angleCal = 0;
		}else{
			angleOpo = (angle + Math.PI) % (2*Math.PI);
			angleCal = angleOpo % (Math.PI/2);
		}
		
		double x1 = billard.left + widthBillard*cx, 
			   y1 = (billard.top + heightBillard*cy);
		double x1t = x1, y1t = y1;
		double x2 = x1, y2 = y1;
		final int longueurQueue = (int) (widthBillard/2);
		final int hauteurQueue = (longueurQueue*43/960);
		if(angleOpo < Math.PI/2){
			x1 += radiusOfBoule * Math.cos(angleCal);
			y1 -= radiusOfBoule * Math.sin(angleCal);
			x1t -= radiusOfBoule * Math.cos(angleCal);
			y1t += radiusOfBoule * Math.sin(angleCal);
			if(dessin){
				x2 += (longueurQueue + radiusOfBoule) * Math.cos(angleCal);
				y2 -= (longueurQueue + radiusOfBoule) * Math.sin(angleCal);
			}
		}else if (angleOpo < Math.PI){
			x1 -= radiusOfBoule * Math.sin(angleCal);
			y1 -= radiusOfBoule * Math.cos(angleCal);
			x1t += radiusOfBoule * Math.sin(angleCal);
			y1t += radiusOfBoule * Math.cos(angleCal);
			if(dessin){
				x2 -= (longueurQueue + radiusOfBoule) * Math.sin(angleCal);
				y2 -= (longueurQueue + radiusOfBoule) * Math.cos(angleCal);
			}	
		}else if (angleOpo < Math.PI*3/2){
			x1 -= radiusOfBoule * Math.cos(angleCal);
			y1 += radiusOfBoule * Math.sin(angleCal);
			x1t += radiusOfBoule * Math.cos(angleCal);
			y1t -= radiusOfBoule * Math.sin(angleCal);
			if(dessin){
				x2 -= (longueurQueue + radiusOfBoule) * Math.cos(angleCal);
				y2 += (longueurQueue + radiusOfBoule) * Math.sin(angleCal);
			}
		}else{
			x1 += radiusOfBoule * Math.sin(angleCal);
			y1 += radiusOfBoule * Math.cos(angleCal);
			x1t -= radiusOfBoule * Math.sin(angleCal);
			y1t -= radiusOfBoule * Math.cos(angleCal);
			if(dessin){
				x2 += (longueurQueue + radiusOfBoule) * Math.sin(angleCal);
				y2 += (longueurQueue + radiusOfBoule) * Math.cos(angleCal);
			}
		}
		
		// queue version dessin
		if(dessin){
			paint.setColor(Color.argb(255, 95, 65, 45));
			paint.setStrokeWidth(10);
			canvas.drawLine((float)x1, (float)y1, (float)x2, (float)y2, paint);
		
		// queue version image
		}else{					
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.queue);
			bitmap = Bitmap.createScaledBitmap(bitmap, longueurQueue, hauteurQueue, false);
			Matrix matrix = new Matrix();
			matrix.preTranslate((float) x1 -longueurQueue, (float) y1 -hauteurQueue/2);
			matrix.postRotate((float)(360 - (angle/Math.PI*180)) % 360, (float) x1, (float) y1);
			canvas.drawBitmap(bitmap, matrix, null);
		}
		
		// on affiche le tracé de la boule
		if(MenuActivity.choix != MenuActivity.Mode.MEILLEUR_COUP){
			final double diagBillard = Math.sqrt(Math.pow(widthBillard, 2) + Math.pow(heightBillard, 2));			
			final int x1px = (int) (cx*widthBillard);
			final int y1px = (int) (cy*heightBillard);
			final int x2px = x1px + (int) (Math.cos(angle) * diagBillard);
			final int y2px = y1px - (int) (Math.sin(angle) * diagBillard);
			Point p;
			
			// test collision avec la bande du dessus
			p = intersection(
					x1px, y1px, x2px, y2px,
					0, 0, (int)widthBillard, 0);
			if(p == null){
				// test collision avec la bande du dessous
				p = intersection(
						x1px, y1px, x2px, y2px,
						0, (int)heightBillard, (int)widthBillard, (int)heightBillard);
				if(p == null){
					// test collision avec la bande de gauche
					p = intersection(
							x1px, y1px, x2px, y2px,
							0, 0, 0, (int)heightBillard);
					if(p == null){
						// test collision avec la bande de droite
						p = intersection(
								x1px, y1px, x2px, y2px,
								(int)widthBillard, 0, (int)widthBillard, (int)heightBillard);
						
					}
				}
			}
			
			// p contient forcement la position du point d'intersection
			paintTiret.setStrokeWidth(hauteurQueue/8); // -> dimension relative à la taille de l'image
			if(p != null)
				canvas.drawLine((float)x1t, (float)y1t, (float)(billard.left + p.x), (float)(billard.top + p.y), paintTiret);
		}
	}
	
	/**
	 * @brief Computes the intersection between two segments. 
	 * @autor http://www.ahristov.com/tutorial/geometry-games/intersection-segments.html
	 * 
	 * @param x1 Starting point of Segment 1
	 * @param y1 Starting point of Segment 1
	 * @param x2 Ending point of Segment 1
	 * @param y2 Ending point of Segment 1
	 * @param x3 Starting point of Segment 2
	 * @param y3 Starting point of Segment 2
	 * @param x4 Ending point of Segment 2
	 * @param y4 Ending point of Segment 2
	 * @return Point where the segments intersect, or null if they don't
	 */
	public Point intersection(
	    int x1,int y1,int x2,int y2, 
	    int x3, int y3, int x4,int y4){
	    
		int d = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4);
	    if (d == 0) return null;
	    
	    int xi = ((x3-x4)*(x1*y2-y1*x2)-(x1-x2)*(x3*y4-y3*x4))/d;
	    int yi = ((y3-y4)*(x1*y2-y1*x2)-(y1-y2)*(x3*y4-y3*x4))/d;
	    
	    Point p = new Point(xi,yi);
	    if (xi < Math.min(x1,x2) || xi > Math.max(x1,x2)) return null;
	    if (xi < Math.min(x3,x4) || xi > Math.max(x3,x4)) return null;
	    return p;
	}
	
	private int determineMaxTextSize(String str, float maxWidth)
	{
	    int size = 0;       
	    Paint paint = new Paint();

	    do {
	        paint.setTextSize(++size);
	    } while(paint.measureText(str) < maxWidth);

	    return size;
	}
	
	
	/**
	 * @param cx : position x absolue (en cm) de la boule par rapport au bord gauche (SANS bande)
	 * @param cy : position y absolue (en cm) de la boule par rapport au bord bas (SANS bande)
	 * @param color
	 */
	public void drawBouleMove(double cx, double cy, int color){
		cx = (cx + sizeDemiBandeCm) / (widthBillardCm + sizeBandeCm);
		cy = 1 - ((cy + sizeDemiBandeCm) / (heightBillardCm + sizeBandeCm));
		if(MenuActivity.boule == 1){
			     if(color == Color.WHITE)  color = Color.YELLOW;
			else if(color == Color.YELLOW) color = Color.WHITE;
		}
		
		boulemove.add(new Pair<Integer, Pair<Double, Double>>(color, new Pair<Double, Double>(cx, cy)));
	}
	/**
	 * @param cx : position x relative (entre 0 et 1) de la boule par rapport au bord gauche (AVEC bande)
	 * @param cy : position y relative (entre 0 et 1) de la boule par rapport au bord bas (AVEC bande)
	 * @param color
	 */
	public void drawBoule(double cx, double cy, int color){
		cy = 1 - cy; // par rapport à en haut (et plus par rapport à en bas)
		if(color == Color.WHITE){
			this.cxb = cx;
			this.cyb = cy;
		}else if(color == Color.RED){
			this.cxr = cx;
			this.cyr = cy;
		}else if(color == Color.YELLOW){
			this.cxj = cx;
			this.cyj = cy;
		}
	}
	
	
	public double getForce(){
		return force;
	}
	/**
	 * @param force : entre 0 et 100
	 */
	public void setForce(double force){
		if(force < 0) force = 0;
		else if(force > 100) force = 100;
		this.force = force;
		if(forceText != null)
			forceText.setText(Math.round(this.force) + "");
		refresh();
	}
	
	public double getAngle(){
		return angle;
	}
	/**
	 * @brief Angle passé en paramètre est celui de la direction du coup à jouer
	 * @param angle : en radian
	 */
	public void setAngle(double angle){
		this.angle = (2*Math.PI + angle) % (2*Math.PI);
		showTrouver = true;
		
		if(angleText != null)
			angleText.setText(Math.round(this.angle*180/Math.PI) + "°");
		
		refresh();
		
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				showTrouver = false;
				refresh();
			}
		}, 3000);
	}
	/**
	 * @brief Angle calculé entre la boule du joueur et la position passé en paramètre
	 * @param x : en px par rapport au canvas (0 à gauche)
	 * @param y : en px par rapport au canvas (0 en haut)
	 */
	public void setAngle(double x, double y){
		double cx = (MenuActivity.boule == 0)?cxb:cxj;
		double cy = (MenuActivity.boule == 0)?cyb:cyj;
		if(cx < 0 || cy < 0) return;
		// on décalle par rapport à la marge entre le canvas et le billard
		x -= billard.left;
		y -= billard.top;
		// on transforme en coordonnées relatives
		x = x/widthBillard;
		y = y/heightBillard;
		// on calcul l'angle
		double hyp, adj;
		hyp = distancePoints(widthBillard*cx, heightBillard*cy,  widthBillard*x,  heightBillard*y);
		if((x > cx && y < cy) || (x < cx && y > cy)){
			adj = distancePoints(widthBillard*cx, heightBillard*cy,  widthBillard*x, heightBillard*cy);
		}else{
			adj = distancePoints(widthBillard*cx, heightBillard*cy, widthBillard*cx, heightBillard*y);
		}
		angle = Math.acos(adj/hyp);
		if(x > cx && y < cy){
			//angle += 0;
		}else if(y < cy){
			angle += Math.PI/2;
		}else if(x < cx){
			angle += Math.PI;
		}else{
			angle += 3*Math.PI/2;
		}
		
		if(angleText != null)
			angleText.setText(Math.round(angle*180/Math.PI) + "°");

		// on raffraichi
		refresh();
	}
	
	public double distancePoints(double x1, double y1, double x2, double y2){
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}
	
	public void hideMsg(){
		showTrouver = false;
	}	
	public void refresh(){
		SolutionCanvasView.this.invalidate();
	}

	public void surfaceCreated(SurfaceHolder holder) { }
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int  height) { }
	public void surfaceDestroyed(SurfaceHolder holder) { }
}
