package com.example.testopencv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GameActivity extends Activity {
	SolutionCanvasView canvas;
	
	@SuppressLint("ClickableViewAccessibility") 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		System.out.println("[TEST] GameActivity.onCreate");
		
		ImageView imagejoueur1 = (ImageView) findViewById(R.id.imagejoueur1);
		ImageView imagejoueur2 = (ImageView) findViewById(R.id.imagejoueur2);
		TextView  textejoueur1 = (TextView)  findViewById(R.id.textejoueur1);
		TextView  textejoueur2 = (TextView)  findViewById(R.id.textejoueur2);
		
		TextView  angle        = (TextView)  findViewById(R.id.angle);
		TextView  puissance    = (TextView)  findViewById(R.id.puissance);
		
		if(MenuActivity.choix == MenuActivity.Mode.ENTRAINEMENT){
			textejoueur1.setText("Joueur : 0 point");
			
			imagejoueur2.setVisibility(View.GONE);
			
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)textejoueur2.getLayoutParams();
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			textejoueur2.setLayoutParams(params);
			
			textejoueur2.setTextColor(Color.RED);
			textejoueur2.setText("0 coup raté");
			
			if(MenuActivity.boule == 1)
				imagejoueur1.setImageResource(R.drawable.bille_jaune);
		}else{ // MULTIJOUEUR
			textejoueur1.setText("Joueur 1 : 0 point");
			textejoueur2.setText("0 point : Joueur 2");
			if(MenuActivity.boule == 0){
				imagejoueur2.setAlpha((float)0.5);
				textejoueur2.setAlpha((float)0.5);
			}else{
				imagejoueur1.setAlpha((float)0.5);
				textejoueur1.setAlpha((float)0.5);
			}
		}
		
		// Canvas
		canvas = (SolutionCanvasView) findViewById(R.id.surface);
		canvas.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // pas d'accélération matérielle : sinon pointillé avec DashPathEffect ne fonctionne pas
		canvas.imagejoueur1 = imagejoueur1;
		canvas.imagejoueur2 = imagejoueur2;
		canvas.textejoueur1 = textejoueur1;
		canvas.textejoueur2 = textejoueur2;
		canvas.angleText    = angle;
		canvas.forceText    = puissance;
		// position initiale des règles du jeu
		canvas.drawBoule(0.25, 0.625, Color.WHITE);
    	canvas.drawBoule(0.25, 0.500, Color.YELLOW);
    	canvas.drawBoule(0.75, 0.500, Color.RED);
    	canvas.setAngle(0);
		canvas.setForce(50);
    	canvas.refresh();
    	
    	canvas.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if(!canvas.isAnimate())
					canvas.setAngle(event.getX() - canvas.getLeft(), event.getY() - canvas.getTop());
				return true; // pour pouvoir avoir l'évènement appelé dès que l'on bouge
			}
		});
    	
    	

    	Button jouer = (Button) findViewById(R.id.jouer);
    	jouer.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				if(!canvas.isAnimate()){
					canvas.lancerSimulation();
				}
			}
		});
    	
		Button moinsAngle = (Button) findViewById(R.id.moinsAngle);
		moinsAngle.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				if(!canvas.isAnimate()){
					canvas.setAngle(canvas.getAngle() -(1*Math.PI/180));
				}
			}
		});
		
		Button plusAngle = (Button) findViewById(R.id.plusAngle);
		plusAngle.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				if(!canvas.isAnimate()){
					canvas.setAngle(canvas.getAngle() +(1*Math.PI/180));
				}
			}
		});
		
		Button moinsPuissance = (Button) findViewById(R.id.moinsPuissance);
		moinsPuissance.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				if(!canvas.isAnimate()){
					canvas.setForce(canvas.getForce() -1);
				}
			}
		});
		
		Button plusPuissance = (Button) findViewById(R.id.plusPuissance);
		plusPuissance.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				if(!canvas.isAnimate()){
					canvas.setForce(canvas.getForce() +1);
				}
			}
		});
		
		/*NumberPicker np = (NumberPicker) findViewById(R.id.test);
		np.setMinValue(0);
		np.setMaxValue(100);*/
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	// Passer du GameActivity au MenuActivity
            Intent i = new Intent(GameActivity.this, MenuActivity.class);
            startActivity(i); 
            finish();
            return true; // pas de propagation de l'évenement
            
        }
		return false; // propagation de l'évenement
     }
}
