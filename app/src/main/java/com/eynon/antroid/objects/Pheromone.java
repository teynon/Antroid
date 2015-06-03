package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.Size;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class Pheromone implements Serializable {

	public static transient Paint perimiterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public static boolean perimiterInitialized = false;
	public static float MergeRadius = 0;
	public transient Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public float left = 0;
	public float top = 0;
	public float strength = 0;
	public static float maxStrength = 500;
	public static float maxAlpha = 200;
	public static float minAlpha = 100;
	public float falloff = (float) 0.012;
	public float radius = 5;
	public static float maxRadius = 40;
	public static float minRadius = 2;
	public int level = 0;
	public transient List<Cell> cells = new ArrayList<Cell>();
	public int team = 0;
	
	public int[] colors = { Color.YELLOW, Color.RED, Color.BLUE, Color.GREEN, Color.WHITE, Color.GRAY };
	
	public Pheromone(float x, float y, float strength, int level, int team) {
		left = x;
		top = y;
		this.level = level;
		this.strength = strength;
		paint.setColor(colors[team]);
		//paint.setAlpha((int)strength);
		radius = ((maxRadius * strength) / maxStrength);
		if (!perimiterInitialized) {
			perimiterPaint.setColor(Color.YELLOW);
			perimiterPaint.setStyle(Style.STROKE);
			perimiterPaint.setStrokeWidth(2);
			perimiterInitialized = true;
		}
	}
	
	public void DoLogic() {
		strength -= falloff;
		if (strength > maxStrength) strength = maxStrength;
		if (strength > 0) {
			radius = (((maxRadius - minRadius) * strength) / maxStrength) + minRadius;
			float alpha = maxAlpha;
			if (strength < 100) { 
				alpha = ((maxAlpha - minAlpha) * strength / 100) + minAlpha;
			}
			//paint.setAlpha((int) alpha);
		}
		else {
			for (Cell c : cells) {
				c.Deregister(this);
			}
		}
	}
	
	public void addStrength(float str) {
		strength += str;
		radius = ((maxRadius * strength) / maxStrength);
	}
	
	public void DoDraw(Canvas c, PointF offset) {
		c.drawCircle(left + offset.x, top + offset.y, radius, paint);
		
		//c.drawCircle(left + offset.x, top + offset.y, radius + 3, perimiterPaint);
	}
	
	public boolean IsOnScreen(PointF offset, Size screenSize) {
		if (left + (strength / 2) > offset.x && left - (strength / 2) < offset.x + screenSize.Width && top + (strength / 2) > offset.y && top - (strength / 2) < offset.y + screenSize.Height){
			return true;
		}
		
		return false;
	}
	
	public boolean CheckCollisionCircle(float x1, float y1, float r) {
		float dx = left - x1;
		float dy = top - y1;
		float rr = (float) (r + radius + MergeRadius);
		
		return (dx * dx) + (dy * dy) < rr * rr;
	}
}
