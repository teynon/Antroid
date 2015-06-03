package com.eynon.antroid.objects;

import java.util.ArrayList;
import java.util.List;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.R;
import com.eynon.antroid.enums.BlockType;
import com.eynon.antroid.enums.Task;
import com.eynon.antroid.model.PointF;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

public class Water extends Resource {
	private transient Bitmap graphic = null;
	private transient Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private transient Matrix matrix = null;
	private transient static List<Bitmap> bitmaps = null;
	private int type = 0;
	public static final int WeightPerUnit = 2;
	public static final double scaleSize = 20;
	public static final double Resistance = 20;
	
	public Water(float x, float y, LevelManager level, int lvl, int type) {
		width = 50;
		height = 50;
		interactionRadius = 5;
		left = x;
		top = y;
		targetable = true;
		_level = level;
		this.level = lvl;
		this.type = type;
		
		if (bitmaps == null) {
			bitmaps = new ArrayList<Bitmap>();
			bitmaps.add(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(level.Context.getResources(), R.drawable.water1), (int)50, (int)50, false));
		}
		
		graphic = bitmaps.get(type);
		selectPaint.setAlpha(200);
		
		matrix = new Matrix();
		matrix.setRotate(0);
		
		// Set default qty
		//SetQty(100);
	}
	
	private float scaleX = 1;
	private float scaleY = 1;
	
	public void SetQty(float qty) {
		this.qty = qty;
		width = (Math.sqrt(qty / Math.PI) * 2) * scaleSize;
		height = width;
		scaleX = (float) (width / graphic.getWidth());
		scaleY = (float) (height / graphic.getHeight());
		
		weight = (int)(WeightPerUnit * this.qty);
		
		if (qty <= 0) {
			// Remove the resource for GC.
			_level.RemoveObject(this);
		}
	}
	
	public float Harvest(Creature creature) {
		float harvestAmount = (float) ((float) creature.harvestStrength / Resistance);
		if (harvestAmount > qty) harvestAmount = qty;
		if (harvestAmount * WeightPerUnit > creature.carryCapacity - creature.carryWeight) harvestAmount = creature.carryCapacity - creature.carryWeight;
		
		SetQty(qty - harvestAmount);
		
		return harvestAmount;
	}

	@Override
	public void DoLogic() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void DoDraw(Canvas c, PointF offset) {
		matrix.reset();
		matrix.postScale(scaleX, scaleY);
		matrix.postTranslate((float)(left + offset.x - (width / 2)), (float)(top + offset.y - (height / 2)));
		c.drawBitmap(graphic, matrix, paint);
	}

	public void TriggerCollision(Ant obj) {
		obj.currentTask = Task.Gather;
	}

	@Override
	public Water factory() {
		Water r = new Water(left, top, _level, level, type);
		_level.AddObject(r, new PointF(left, top), width, height);
		return r;
	}
	
	@Override
	public void UpdateLocation(PointF point) {
		left = point.x;
		top = point.y;
		
		_level.RegisterObject(this, new PointF(left, top), width, height);
	}

	@Override
	public void DoDrop() {
		blocking = BlockType.PARTIAL;
		MergeCheck();
	}
	
	public void MergeCheck() {
		List<Object> objects = _level.FindObjects(this, new PointF((float)(left + (width / 2)), (float)(top + (height / 2))), (float)(width * 1.5));
		for (Object o : objects) {
			if (o instanceof Water && o != this && !o.carried) {
				// Join this water to that water.
				((Water) o).SetQty(((Water)o).qty + qty);
				
				// Remove the resource for GC.
				_level.RemoveObject(this);
				
				((Water) o).MergeCheck();
				break;
			}
		}
	}
}
