package com.eynon.antroid;

import java.util.concurrent.locks.Lock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.eynon.antroid.model.PointF;

import android.view.SurfaceHolder;

public class GameThread extends Thread {

	private int mCanvasWidth = 1000;
	private int mCanvasHeight = 1000;
	private SurfaceHolder holder;
	private boolean running = false;
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final int refresh_rate=100;
	public LevelManager level = null;
	
	private PointF distanceChanged = new PointF(0,0);
	
	public GameThread(SurfaceHolder hold, Context context) {
		holder = hold;
		//level = new LevelManager(context);
		//level.SetSize(mCanvasWidth, mCanvasHeight);
	}
	
	public void SetLevel(LevelManager lvl) {
		level = lvl;
	}
	
	@Override  
	public void run() {

		Canvas c = null;
		while (running) {
			try {
				synchronized (holder) {
					if (distanceChanged.x != 0 || distanceChanged.y != 0) {
						// Notify level of drag event.
						level.NotifyDrag(distanceChanged);
						distanceChanged.x = 0;
						distanceChanged.y = 0;
					}
					
					c = holder.lockCanvas();

					if (c != null) {
						level.DoLogic();
						level.DoDraw(c);
					}
					
					holder.wait(1);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				if (c != null) {
					holder.unlockCanvasAndPost(c);
				}
			}
		}
	}
	
	public void notifyDrag(PointF distance) {
		synchronized (holder) {
			distanceChanged.x += distance.x;
			distanceChanged.y += distance.y;
		}
	}
	
	public void notifyDrag(float x, float y) {
		synchronized (holder) {
			distanceChanged.x += x;
			distanceChanged.y += y;
		}
	}
	
	public void notifySingleTap(float x, float y) {
		synchronized (holder) {
			level.NotifySingleTap(x, y);
		}
	}
	
	public void notifyDoubleTap(float x, float y) {
		synchronized (holder) {
			level.NotifyDoubleTap(x, y);
		}
	}
	
	public void notifySelectArea(PointF p1, PointF p2) {
		synchronized (holder) {
			level.NotifySelectArea(p1, p2);
		}
	}
	
	public void notifySelectAreaRelease() {
		synchronized (holder) {
			level.NotifySelectAreaRelease();
		}
	}
	
	public void notifyDoSelect() {
		synchronized (holder) {
			level.NotifyDoSelect();
		}
	}
	
	public void setRunning(boolean b) {
		running = b;
	}
	
	public void setSurfaceSize(int width, int height) {  
		synchronized (holder) {  
			mCanvasWidth = width;  
			mCanvasHeight = height;
			if (level != null)
				level.SetSize(width, height);
        }  
	}
	
	public void doLogic() {
		
	}
	
	public void doDraw(Canvas c) {
		paint.setColor(Color.BLUE);
		c.drawRect(0, 0, 500, 50, paint);
	}
}
