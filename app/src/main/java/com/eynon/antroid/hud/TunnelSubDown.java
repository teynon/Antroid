package com.eynon.antroid.hud;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.R;
import com.eynon.antroid.enums.Action;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.Rectangle;
import com.eynon.antroid.objects.Ant;
import com.eynon.antroid.objects.Object;
import com.eynon.antroid.objects.Queen;

public class TunnelSubDown extends hud {

	private transient Bitmap graphic;
	private transient Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private LevelManager level;
	private Rectangle HitBox = new Rectangle();

	public TunnelSubDown(LevelManager lvl, int width, int height) {
		level = lvl;
		Height = height;
		Width = width;
		paint.setAlpha(100);
		graphic = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(level.Context.getResources(), R.drawable.dig_down), (int)width, (int)height, false);
	}

	
	@Override
	public void OnDraw(Canvas c, PointF offset, Panel p) {
		HitBox.P1.x = Left + offset.x;
		HitBox.P1.y = Top + offset.y;
		HitBox.P2.x = HitBox.P1.x + Width;
		HitBox.P2.y = HitBox.P1.y + Height;
		
		if (level.get_selected() > 0) {
			c.drawBitmap(graphic, Left + offset.x,  Top + offset.y, paint);
		}
		else {
			Disable();
			p.Disable();
		}
	}
	
	@Override
	public boolean ValidForSelection(List<Ant> selection) {
		boolean result = true;
		for (Object o : selection) {
			if (o instanceof Queen) {
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean onClick(PointF point) {
		if (HitTest(HitBox, point)) {
			level.TriggerActionForSelection(Action.DIGDOWN);
			return true;
		}
		
		return false;
	}
}
