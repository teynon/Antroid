package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.R;
import com.eynon.antroid.enums.BlockType;
import com.eynon.antroid.level.Tunnel;
import com.eynon.antroid.level.Tunnel.TunnelType;
import com.eynon.antroid.level.Tunnel.TunnelUse;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.PointF3D;

public class AntHole extends Buildable implements Serializable {
	private transient Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private float _angle = 0;
	public int type = 0;
	public AntHill linkedHill = null;
	
	public AntHole(float left, float top, LevelManager lvl, int team, int level, AntHill link) {
		this.team = team;
		this.left = left;
		this.top = top;
		this._level = lvl;
		this.targetable = true;
		width = 20;
		height = 20;
		interactionRadius = 4;
		linkedHill = link;
		
		blocking = BlockType.NONE;
		
		paint.setColor(Color.WHITE);
		paint.setAlpha(80);
		this.level = level;
	}
	
	@Override
	public void BeginConstruction() {
		super.BeginConstruction();
		if (linkedHill != null && !linkedHill.UnderConstruction) {
			linkedHill.BeginConstruction();
		}
		paint.setAlpha(20);
	}
	
	@Override
	public void CompleteConstruction() {
		super.CompleteConstruction();
		
		// Add tunnel below if none exists.
		if (linkedHill != null && linkedHill.UnderConstruction) {
			linkedHill.CompleteConstruction();
		}
		paint.setAlpha(80);
	}

	@Override
	public void DoLogic() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void DoDraw(Canvas c, PointF offset) {
		c.drawCircle(left + offset.x,  top + offset.y, (float)width, paint);
		if (_level.DebugMode) c.drawRect((float)(left - interactionRadius - (width / 2) + offset.x), (float)(top - interactionRadius - (height / 2) + offset.y), (float)(left + (width / 2) + interactionRadius + offset.x), (float)(top + (height / 2) + interactionRadius + offset.y), debugPaint);
	}
	
	@Override
	public boolean CheckInteractionRange(float x1, float y1, float x2, float y2) {
		return (left - interactionRadius - (width / 2) < x2 && left + (width / 2) + interactionRadius > x1 && top - interactionRadius - (height / 2) < y2 && top + (height / 2) + interactionRadius > y1);
	}

	@Override
	public void TriggerCollision(Object obj) {
		if (!UnderConstruction) {
			obj.SetLevel(obj.level + 1);
			obj.left = (float) (left);
			obj.top = (float) (top);
		}
	}

	@Override
	public AntHole factory() {
		return new AntHole(left, top, _level, team, level, linkedHill);
	}
	
}
