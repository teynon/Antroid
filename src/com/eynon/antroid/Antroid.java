package com.eynon.antroid;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;

public class Antroid extends Activity {

	Game theGame;
	private LevelManager level;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		if (savedInstanceState != null && savedInstanceState.containsKey("levelState"))
			level = (LevelManager) savedInstanceState.getSerializable("levelState");
		
		if (level == null) 
			level = new LevelManager(this);
		
		launchGame();
	}
	
	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		outState.putSerializable("levelState", level);
		super.onSaveInstanceState(outState);
	}



	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		launchGame();
	}
	
	@Override 
	protected void onStop() {
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void launchGame() {
		theGame = new Game(this, level);
		setContentView(theGame);
	}

}
