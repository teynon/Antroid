package com.eynon.antroid.objects;

import java.io.Serializable;

import com.eynon.antroid.model.PointF;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Boundary implements Serializable {
	public double left = 0;
	public double top = 0;
	public double width = 400;
	public double height = 400;
	public transient Paint paint = new Paint();
	
	public Boundary() {
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.STROKE);
	}
	
	public void DoDraw(Canvas c, PointF offset) {
		c.drawRect((float)(left + offset.x), (float)(top + offset.y), (float)(left + offset.x + width), (float)(top + offset.y + height), paint);
	}
	
	public boolean CheckCollision(float x1, float y1, float x2, float y2, float oWidth, float oHeight) {
		return (left < x2 - (oWidth) && 
				left + width > x1 + (oWidth) && 
				top < y2 - (oHeight) && 
				top + height > y1 + (oHeight));
	}
}
