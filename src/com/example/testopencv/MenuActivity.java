package com.example.testopencv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class MenuActivity extends Activity {
	private enum Affichage {ACCUEIL, CHOIXBOULE};
	private Affichage affichage = Affichage.ACCUEIL;
	
	
	public enum Mode {MEILLEUR_COUP, ENTRAINEMENT, MULTIJOUEUR};
	public static Mode choix;
	public static int boule = 0; // 0 = blanche; 1 = jaune
	
	ImageButton meilleurCoupButton, entrainementButton, multijoueurButton, quitterButton;
	ImageButton bouleBlancheButton, bouleJauneButton, retourButton;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        System.out.println("[TEST] MenuActivity : onCreate");

        setContentView(R.layout.activity_menu);
        
        meilleurCoupButton = (ImageButton) findViewById(R.id.menu_meilleur_coup);
        meilleurCoupButton.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	choix = Mode.MEILLEUR_COUP;
		    	ChoixCouleurBoule();
		    }
        });
        
        entrainementButton = (ImageButton) findViewById(R.id.menu_entrainement);
        entrainementButton.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	choix = Mode.ENTRAINEMENT;
		    	boule = 0;
		    	Go();
		    	//Toast.makeText(getApplicationContext(),"Entraînement indisponible pour le moment", Toast.LENGTH_LONG).show();
		    }
        });
        
        multijoueurButton = (ImageButton) findViewById(R.id.menu_multijoueur);
        multijoueurButton.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	choix = Mode.MULTIJOUEUR;
		    	boule = 0;
		    	Go();
		    	//Toast.makeText(getApplicationContext(),"Multijoueur indisponible pour le moment", Toast.LENGTH_LONG).show();
		    }
        });
        
        quitterButton = (ImageButton) findViewById(R.id.menu_quitter);
        quitterButton.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	finish();
		    }
        });
        
        bouleBlancheButton = (ImageButton) findViewById(R.id.menu_boule_blanche);
        bouleBlancheButton.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	boule = 0;
		    	Go();
		    }
        });
        
        bouleJauneButton = (ImageButton) findViewById(R.id.menu_boule_jaune);
        bouleJauneButton.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	boule = 1;
		    	Go();
		    }
        });
        
        retourButton = (ImageButton) findViewById(R.id.menu_retour);
        retourButton.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	retour();
		    }
        });
    }
	
	public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK) {        	
        	return retour();
        }
        return false; // propagation de l'évènement
	}
	
	private boolean retour(){
		if(affichage == Affichage.CHOIXBOULE){
    		affichage = Affichage.ACCUEIL;
    		meilleurCoupButton.setVisibility(0);
    		entrainementButton.setVisibility(0);
    		multijoueurButton.setVisibility(0);
    		quitterButton.setVisibility(0);
    		
    		bouleBlancheButton.setVisibility(View.GONE);
    		bouleJauneButton.setVisibility(View.GONE);
    		retourButton.setVisibility(View.GONE);
    		
    		return true; // pas de propagation de l'évènement
    	}
		return false; // propagation de l'évènement
	}
	
	private void ChoixCouleurBoule(){
		affichage = Affichage.CHOIXBOULE;
		meilleurCoupButton.setVisibility(View.GONE);
		entrainementButton.setVisibility(View.GONE);
		multijoueurButton.setVisibility(View.GONE);
		quitterButton.setVisibility(View.GONE);
		
		bouleBlancheButton.setVisibility(0);
		bouleJauneButton.setVisibility(0);
		retourButton.setVisibility(0);
	}
	
	private void Go(){
		if(choix == Mode.MEILLEUR_COUP){
			// Passer du Menu à la MainActivity
	        Intent i = new Intent(MenuActivity.this, MainActivity.class);
	        startActivity(i);
	        finish();
		}else if(choix == Mode.ENTRAINEMENT || choix == Mode.MULTIJOUEUR){
			// Passer du Menu à la GameActivity
	        Intent i = new Intent(MenuActivity.this, GameActivity.class);
	        startActivity(i);
	        finish();
		}else{
			Toast.makeText(getApplicationContext(),"Une erreur a eu lieue. Contacter l'équipe de production", Toast.LENGTH_LONG).show();
		}
	}
}
