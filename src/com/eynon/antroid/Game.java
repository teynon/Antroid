package com.eynon.antroid;

import com.eynon.antroid.input.Gestures;
import com.eynon.antroid.model.PointF;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Game extends SurfaceView implements SurfaceHolder.Callback {

	private SurfaceHolder holder;
	private GameThread gameThread;
	private PointF distanceChanged = new PointF(0, 0);
	private PointF lastPoint = new PointF(0,0);
	private Context _Context;
	private Gestures gesture;
	private GestureDetector gestureDetector;
	private LevelManager level = null;
	
	public Game(Context context, LevelManager level) {
		super(context);
		_Context = context;
		holder = getHolder();
		
		this.level = level;
		
		holder.addCallback(this);
		gesture = new Gestures();
		gestureDetector = new GestureDetector(context, gesture);
	}

	public long AreaReleaseTimer = 0;
	public boolean SelectArea = false;
	public static final int SelectionLimit = 10000;
	
	/*public static String actionToString(int action) {
		
	    switch (action) {
	                
	        case MotionEvent.ACTION_DOWN: return "Down";
	        case MotionEvent.ACTION_MOVE: return "Move";
	        case MotionEvent.ACTION_POINTER_DOWN: return "Pointer Down";
	        case MotionEvent.ACTION_UP: return "Up";
	        case MotionEvent.ACTION_POINTER_UP: return "Pointer Up";
	        case MotionEvent.ACTION_OUTSIDE: return "Outside";
	        case MotionEvent.ACTION_CANCEL: return "Cancel";
	    }
	    return "";
	}*/
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int pointers = event.getPointerCount();
		int index = MotionEventCompat.getActionIndex(event);
		int action = MotionEventCompat.getActionMasked(event);
		//Log.i("Touch", actionToString(action));
		if (pointers > 1) {
			//Log.i("Touch", "Multi");
			SelectArea = true;
			PointF p1 = new PointF(event.getX(0), event.getY(0));
			PointF p2 = new PointF(event.getX(1), event.getY(1));
			
			if ((action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) && pointers == 2) {
				//Log.i("Touch", "Multi - Pointer Up");
				// Trigger selected area.
				AreaReleaseTimer = SystemClock.elapsedRealtime();
				gameThread.notifySelectAreaRelease();
				SelectArea = false;
				
			}
			else {
				//Log.i("Touch", "Notify Select Area");
				gameThread.notifySelectArea(p1, p2);
			}
		}
		else if (AreaReleaseTimer != 0) {
			//Log.i("Touch", "Single - Select Area");
			if ((action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) && SystemClock.elapsedRealtime() - AreaReleaseTimer < SelectionLimit) {

				//Log.i("Touch", "Single - Select Area - Up");
				gameThread.notifySelectAreaRelease();
				gameThread.notifyDoSelect();
				AreaReleaseTimer = 0;
				SelectArea = false;
			}
			
			if (SystemClock.elapsedRealtime() - AreaReleaseTimer > SelectionLimit) AreaReleaseTimer = 0;
		}
		else {
			//Log.i("Touch", "Single - Non Select");
			if (AreaReleaseTimer != 0 || SelectArea) {

				//Log.i("Touch", "Single - Non Select - Do Select");
				gameThread.notifySelectAreaRelease();
				gameThread.notifyDoSelect();
				SelectArea = false;
			}
			
			gestureDetector.onTouchEvent(event);
		}
		/*
		float x = event.getX();
		float y = event.getY();
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			lastPoint.x = x;
			lastPoint.y = y;
			break;
		case MotionEvent.ACTION_MOVE:
			distanceChanged.x += x - lastPoint.x;
			distanceChanged.y += y - lastPoint.y;
			break;
		case MotionEvent.ACTION_UP:
			break;
		}
		
		lastPoint.x = x;
		lastPoint.y = y;
		
		if (distanceChanged.x != 0 || distanceChanged.y != 0) {
			gameThread.notifyDrag(distanceChanged);
			distanceChanged.x = 0;
			distanceChanged.y = 0;
		}*/
		
		return true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		synchronized (holder) {
			if (gameThread == null) {
				gameThread = new GameThread(holder, _Context);
				gameThread.SetLevel(level);
				level.SetSize(width, height);
				gameThread.setRunning(true);
				gameThread.setSurfaceSize(width, height);
				gameThread.start();
				
				gesture.setGameThread(gameThread);
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		level = gameThread.level;
		gameThread.setRunning(false);
		boolean retry = true;
		while (retry) {
			try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
		}
		
	}
	
	public Thread getThread() {
		return gameThread;
	}

}
