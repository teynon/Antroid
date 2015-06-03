package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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

public class AntHill extends Buildable implements Serializable {
	private transient static List<Bitmap> anthill = null;
	private transient Bitmap Graphic = null;
	private transient Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private transient Matrix matrix = null;
	private float _angle = 0;
	public int type = 0;
	public AntHole linkedHole = null;
	
	public AntHill(float left, float top, LevelManager level, int team, int lvl, boolean immediate) {
		this.team = team;
		this.left = left;
		this.top = top;
		this._level = level;
		this.level = lvl;
		this.targetable = true;
		width = 20;
		height = 20;
		interactionRadius = 0;
		
		if (anthill == null) {
			anthill = new ArrayList<Bitmap>();
			anthill.add(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(level.Context.getResources(), R.drawable.anthole1), (int)100, (int)100, false));
		}
		
		blocking = BlockType.NONE;
		
		Graphic = anthill.get(type);
		matrix = new Matrix();
		matrix.setRotate(0);
		
		if (immediate) {
			CreateHole();
		}
	}
	
	@Override
	public void BeginConstruction() {
		super.BeginConstruction();
		if (linkedHole != null) {
			linkedHole.BeginConstruction();
		}
		paint.setAlpha(80);
	}
	
	@Override
	public void CompleteConstruction() {
		super.CompleteConstruction();
		
		// Add tunnel below if none exists.
		Tunnel t = _level.GetTunnelCollision(new PointF3D(left, top, level - 1), 0, 0);
		if (t == null) {
			// Add a tunnel.
			t = new Tunnel(_level, new PointF3D(left, top, level - 1), new PointF3D(0,0,0), TunnelType.BasicRoom, 200);
			t.Purpose = TunnelUse.Lobby;
			_level.AddTunnel(t);
		}
		
		if (linkedHole != null) {
			linkedHole.CompleteConstruction();
		}
		paint.setAlpha(255);
	}
	
	public void CreateHole() {
		linkedHole = new AntHole(left, top, _level, team, this.level-1, this);
		_level.AddObject(linkedHole, new PointF(left, top), 20, 20);
	}

	@Override
	public void DoLogic() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void DoDraw(Canvas c, PointF offset) {
		matrix.reset();
		matrix.setRotate((float) angle,Graphic.getWidth() / 2, Graphic.getHeight() / 2);
		matrix.postTranslate((float)left + offset.x - (Graphic.getWidth() / 2), (float)top + offset.y - (Graphic.getHeight() / 2));
		c.drawBitmap(Graphic, matrix, paint);
	}

	@Override
	public void TriggerCollision(Object obj) {
		if (!UnderConstruction) {
			obj.SetLevel(obj.level - 1);
			obj.Interaction(this);
			obj.left = (float) (left + ((height / 2) - (obj.width / 2)));
			obj.top = (float) (top + ((height / 2) - (obj.width / 2)));
		}
	}

	@Override
	public AntHill factory() {
		return new AntHill(left, top, _level, team, 0, true);
	}

}
