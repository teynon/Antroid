package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.R;
import com.eynon.antroid.model.PointF;

public class Larvae extends Ant implements Serializable {

	public float Maturity = 0;
	public float MaturityCapacity = 1000;
	public float MaturityRate = (float) 0.8;
	protected transient static List<Bitmap> ant = null;
	
	public Larvae(float x, float y, LevelManager level, int type, int team,
			int lvl) {
		super(x, y, level, type, team, lvl);

		width = 26;
		height = 26;

		if (ant == null) {
			ant = new ArrayList<Bitmap>();
			ant.add(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(level.Context.getResources(), R.drawable.larvae1), (int)width, (int)height, false));
		}
		
		angle = (float) (Math.random() * 360);
		
		if (type >= ant.size()) {
			type = 0;
		}
		
		antGraphic = ant.get(type);
		matrix = new Matrix();
		matrix.setRotate(0);
		

		SetHealthColor();
	}

	
	@Override
	public void DoLogic() {
		if (!IsAlive) {
			return;
		}
		
		Maturity += MaturityRate;
		
		if (Maturity >= MaturityCapacity) {
			Hatch();
		}
		
		return;
	}
	
	@Override
	public void DoDraw(Canvas c, PointF offset) {
		// Draw health circle.
		if (IsAlive && _level.Team == team) {
			//c.drawCircle((float)left + offset.x, (float)top + offset.y, (float) width / 2, healthPaint);
			float width = (antGraphic.getWidth() * Health) / MaxHealth;
			c.drawRect((left + offset.x) - (antGraphic.getWidth() / 2),  (top + offset.y)- (antGraphic.getHeight() / 2), (left + offset.x) - (antGraphic.getWidth() / 2) + width,  (top + offset.y) - (antGraphic.getHeight() / 2) + 4, healthPaint);
		}
		
		matrix.reset();
		matrix.setRotate((float) angle, antGraphic.getWidth() / 2, antGraphic.getHeight() / 2);
		matrix.postTranslate((float)left + offset.x - (antGraphic.getWidth() / 2), (float)top + offset.y - (antGraphic.getHeight() / 2));
		c.drawBitmap(antGraphic, matrix, paint);
		
		// Debug draw circle for vision
		//PointF viewPoint = PointAtDistance(vision + visionOffset);//ScaleToDistance(AngleToSlope(angle), vision + visionOffset);
		//viewPoint.x += left;
		//viewPoint.y += top;
		//c.drawCircle(viewPoint.x + offset.x, viewPoint.y + offset.y, vision, selectPaint);
		
		//c.drawCircle((float)left + offset.x, (float)top + offset.y, (float) width - 5, paint);
	}
	
	public void Hatch() {
		// Hatch a specific kind of ant?
		Ant a = new Ant(left, top, _level, 0, team, level);
		_level.AddObject(a, new PointF(a.left, a.top), a.width, a.height);
		_level.RemoveObject(this);
	}

}
