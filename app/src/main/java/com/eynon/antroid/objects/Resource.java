package com.eynon.antroid.objects;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.level.Tunnel;
import com.eynon.antroid.model.PointF;

public abstract class Resource extends Object {
	private int type = 0;
	public float qty = 0;
	public static final double Resistance = 0.5;
	public static final double FillRate = 20;
	public Tunnel linkedRoom;

	public abstract void TriggerCollision(Ant obj);
	
	public abstract void SetQty(float qty);
	
	public abstract float Harvest(Creature creature);
	
	public abstract void UpdateLocation(PointF point);
	
	public abstract void DoDrop();
}
