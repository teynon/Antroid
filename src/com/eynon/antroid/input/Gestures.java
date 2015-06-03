package com.eynon.antroid.input;

import com.eynon.antroid.GameThread;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class Gestures extends GestureDetector.SimpleOnGestureListener {

	private GameThread gameThread = null;
	
	public void setGameThread(GameThread thread) {
		gameThread = thread;
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		if (gameThread != null)
			gameThread.notifyDrag(-distanceX, -distanceY);
		
		return super.onScroll(e1, e2, distanceX, distanceY);
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		// Notify tap.
		if (gameThread != null)
			gameThread.notifySingleTap(e.getX(), e.getY());
		
		return super.onSingleTapConfirmed(e);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		// Notify tap.
		if (gameThread != null)
			gameThread.notifyDoubleTap(e.getX(), e.getY());
		
		return super.onDoubleTap(e);
	}

	
}
