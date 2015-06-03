package com.eynon.antroid.hud;

import java.io.Serializable;
import java.util.List;

import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.Rectangle;
import com.eynon.antroid.model.Thickness;
import com.eynon.antroid.objects.Ant;
import com.eynon.antroid.objects.Object;

import android.graphics.Canvas;

public abstract class hud implements Serializable {
	
	public float Left = 0;
	public float Top = 0;
	public float Width = 0;
	public float Height = 0;
	public Thickness margin = new Thickness(0,0,0,0);
	public Thickness padding = new Thickness(0,0,0,0);
	public boolean Active = true;
	
	public hud() {
	
	}
	
	// Determine if clicked.
	public boolean onClick(PointF point) {
		
		return false;
	}
	
	public boolean HitTest(PointF point) {
		if (point.x >= Left && point.x <= Left + Width && point.y >= Top && point.y <= Top + Height) {
			return true;
		}
		
		return false;
	}
	
	public boolean HitTest(Rectangle HitBox, PointF point) {
		return (point.x >= HitBox.P1.x && point.x <= HitBox.P2.x && point.y >= HitBox.P1.y && point.y <= HitBox.P2.y); 
	}
	
	public void OnDraw(Canvas c, PointF offset, Panel p) {
		
	}
	
	public void PreDraw(Panel p) {
	}
	
	public boolean ValidForSelection(List<Ant> selection) {
		return true;
	}
	
	public void Disable() {
		Active = false;
	}
	
	public void Enable() {
		Active = true;
	}
	
	public void Toggle() {
		Active = !Active;
	}
}
