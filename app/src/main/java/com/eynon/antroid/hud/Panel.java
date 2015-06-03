package com.eynon.antroid.hud;

import java.util.ArrayList;
import java.util.List;

import com.eynon.antroid.model.PointF;
import com.eynon.antroid.objects.Ant;
import com.eynon.antroid.objects.Object;

import android.graphics.Canvas;

public class Panel extends hud {

	public List<hud> controls = new ArrayList<hud>();
	public enum Direction {
		LeftToRight,
		RightToLeft,
		TopToBottom,
		BottomToTop
		
	}
	public Direction direction = Direction.LeftToRight;
		
	public Panel(float x, float y) {
		Enable();
	}
	
	
	public boolean onClick(PointF point) {
		boolean handled = false;
		if (Active){ 
			for (hud c : controls) {
				if (c.Active && c.onClick(point)) {
					handled = true;
					break;
				}
			}
		}
		
		return handled;
	}


	public void AddControl(hud control) {
		controls.add(control);
		if (control.Height > Height) Height = control.Height;
	}
	
	public void OnDraw(Canvas c, PointF offset, List<Ant> selection, Panel p) {
		if (Active) {
			PointF start = new PointF(offset.x, offset.y);
			offset.x += Left;
			offset.y += Top - padding.Bottom;
		
			for (hud control : controls) {
				control.PreDraw(this);
				if (control.Active && control.ValidForSelection(selection)) {
					offset.x += control.margin.Left;
					control.OnDraw(c, offset, this);
					
					switch (direction) {
					case LeftToRight:
						offset.x += control.Width + control.margin.Right;
						break;
					case RightToLeft:
						break;
					case TopToBottom:
						break;
					case BottomToTop:
						break;
					}
				}
				
			}
		}
	}
	
	@Override
	public void Enable() {
		super.Enable();
		for (hud control : controls) {
			control.Enable();
		}
	}
}
