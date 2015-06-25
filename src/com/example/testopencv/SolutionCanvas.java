package com.example.testopencv;

import simulation.Simulation;
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
import android.widget.ImageButton;

public class SolutionCanvas extends Activity {		
	SolutionCanvasView canvas;
	double blancheX, blancheY, rougeX, rougeY, jauneX, jauneY;
	private boolean showMenu = true;
	private ImageButton returnButton, inverseButton, startAnimationButton, stopAnimationButton;
	
	@SuppressLint("ClickableViewAccessibility") public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_solution);
		System.out.println("[TEST] ShowSolutionActivity.onCreate");
		
		SendMessage.context = this; // pour vérifier la connection internet pour l'envoi de stats
		
		Intent intent = getIntent();
		if (intent != null) {
			blancheX = intent.getDoubleExtra("blancheX", 0);
			blancheY = intent.getDoubleExtra("blancheY", 0);
			rougeX = intent.getDoubleExtra("rougeX", 0);
			rougeY = intent.getDoubleExtra("rougeY", 0);
			jauneX = intent.getDoubleExtra("jauneX", 0);
			jauneY = intent.getDoubleExtra("jauneY", 0);
		}

		// Bouton retour
		returnButton = (ImageButton)findViewById(R.id.imageButtonRetour);
		returnButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("[TEST] ShowSolutionActivity : Return Click");
				// Passer du ShowSolutionActivity à la MainActivity
                Intent i = new Intent(SolutionCanvas.this, MainActivity.class);
                startActivity(i); 
                finish();
			}
		});
		
		// Inverser
		inverseButton = (ImageButton)findViewById(R.id.imageButtonInverse);
		inverseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("[TEST] ShowSolutionActivity : Inverse");
				canvas.inverseBillard();
			}
		});
		
		// Animation
		startAnimationButton = (ImageButton)findViewById(R.id.imageButtonStartAnimation);
		startAnimationButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("[TEST] ShowSolutionActivity : Animation Start/Pause");
				canvas.StartStopAnimation();
			}
		});
		stopAnimationButton = (ImageButton)findViewById(R.id.imageButtonStopAnimation);
		stopAnimationButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("[TEST] ShowSolutionActivity : Animation Stop");
				canvas.StopAnimation();
			}
		});
		
		// Canvas
		canvas = (SolutionCanvasView) findViewById(R.id.surface);
		canvas.inverseButton   		= inverseButton;
		canvas.startAnimationButton = startAnimationButton;
		canvas.stopAnimationButton  = stopAnimationButton;
		canvas.drawBoule(blancheX, blancheY, 	Color.WHITE);
    	canvas.drawBoule(jauneX, 	 jauneY, 	Color.YELLOW);
    	canvas.drawBoule(rougeX,	 rougeY, 	Color.RED);
    	canvas.refresh();
    	
    	canvas.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				showMenu = !showMenu;
				refreshMenu();
				return false; // propagation de l'évenement
			}
		});
    	
    	// Simulation : Task
    	Simulation s = new Simulation();
    	if(MenuActivity.boule == 0) // l'utilisateur a la boule blanche
    		s.updateParam(canvas, 280, blancheX, blancheY, jauneX, jauneY, rougeX, rougeY);
    	else // l'utilisateur a la boule jaune
    		s.updateParam(canvas, 280, jauneX, jauneY, blancheX, blancheY, rougeX, rougeY);
    	s.execute();
	}
	
	public void refreshMenu(){
		if(showMenu){					
			returnButton.setVisibility(0);
			if(canvas.getAngle() != -1){
				inverseButton.setVisibility(0);
				startAnimationButton.setVisibility(0);
				if(canvas.isAnimate())
					stopAnimationButton.setVisibility(0);
			}
		}else{
			returnButton.setVisibility(View.GONE);
			inverseButton.setVisibility(View.GONE);
			startAnimationButton.setVisibility(View.GONE);
			stopAnimationButton.setVisibility(View.GONE);
		}
		canvas.showMenu = showMenu;
		canvas.refresh();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	// Passer du ShowSolutionActivity à la MainActivity
            Intent i = new Intent(SolutionCanvas.this, MainActivity.class);
            startActivity(i); 
            finish();
            return true; // pas de propagation de l'évenement
            
        }else if(keyCode == KeyEvent.KEYCODE_MENU){
        	showMenu = !showMenu;
			refreshMenu();
			return true; // pas de propagation de l'évenement
        }
		return false; // propagation de l'évenement
     }	
}
