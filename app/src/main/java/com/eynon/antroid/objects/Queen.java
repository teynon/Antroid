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
import android.graphics.Paint.Style;
import android.util.Log;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.R;
import com.eynon.antroid.enums.Task;
import com.eynon.antroid.level.Tunnel;
import com.eynon.antroid.level.Tunnel.TunnelType;
import com.eynon.antroid.level.Tunnel.TunnelUse;
import com.eynon.antroid.model.MathHelpers;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.PointF3D;
import com.eynon.antroid.objects.Object.CollisionType;
import com.eynon.antroid.objects.Object.DamageType;

public class Queen extends Ant implements Serializable {

	private transient static List<Bitmap> ant = null;
	private transient Bitmap antGraphic = null;
	private transient Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private transient Matrix matrix = null;
	
	public float Fertility = 0;
	public float FertilityPeak = 1000;
	public float FertilityHatchReduction = 100;
	public float EggDistance = -20;
	public float BirthCooldown = 60;
	public float CooldownTimer = 0;
	
	protected Task defaultTask = Task.Idle;
	
	public Queen(float x, float y, LevelManager level, int type, int team, int lvl) {
		super(x, y, level, type, team, lvl);
		defaultTask = Task.Idle;
		currentTask = defaultTask;
		MaxHealth = 1000;
		Health = MaxHealth;
		collisionType = CollisionType.Circle;
		width = 50;
		height = 50;
		speed = 5;
		strength = 800;
		weight = 100;
		selectable = true;
		IsAlive = true;
		this.team = team;
		
		selectPaint.setColor(Color.GREEN);
		selectPaint.setStyle(Style.STROKE);
		
		if (ant == null) {
			ant = new ArrayList<Bitmap>();
			ant.add(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(level.Context.getResources(), R.drawable.ant1), (int)width, (int)height, false));
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

		// Consume hunger
		foodRemaining -= consumptionRate;
		if (foodRemaining < 0) {
			Starving();
		}
		
		// What is the ant doing?
		switch (currentTask) {
		case Idle:
			if (level < 0) {
				SetTarget(left, top, level, Task.LayEggs);
			}
			else {
				Navigate();
			}
			break;
		// -----------------------------------------------------
		case LayEggs:
			// When fertilility is full, lay an egg.
			if (Fertility > FertilityHatchReduction) {
				LayEgg();
			}
			break;
		// -------------------------------------------------------------------------------------
		default:
			this.ProcessLogic();
			break;
		}
		
		return;
	}

	@Override
	public void DoDraw(Canvas c, PointF offset) {
		// Draw a circle...
		//c.drawBitmap(antGraphic, (float)left + offset.x - 20, (float)top + offset.y - 20, paint);
		//matrix.postTranslate((float)left + offset.x - 20, (float)top + offset.y - 20);
		
		// Draw health circle.
		if (IsAlive && _level.Team == team) {
			//c.drawCircle((float)left + offset.x, (float)top + offset.y, (float) width / 2, healthPaint);
			float width = (antGraphic.getWidth() * Health) / MaxHealth;
			c.drawRect((left + offset.x) - (antGraphic.getWidth() / 2),  (top + offset.y)- (antGraphic.getHeight() / 2), (left + offset.x) - (antGraphic.getWidth() / 2) + width,  (top + offset.y) - (antGraphic.getHeight() / 2) + 4, healthPaint);
			float hungerWidth = (antGraphic.getWidth() * foodRemaining) / hungerCapacity;
			c.drawRect((left + offset.x) - (antGraphic.getWidth() / 2),  (top + offset.y + 4)- (antGraphic.getHeight() / 2), (left + offset.x) - (antGraphic.getWidth() / 2) + hungerWidth,  (top + offset.y) - (antGraphic.getHeight() / 2) + 8, hungerPaint);
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
	
	public void LayEgg() {
		PointF eggArea = PointAtDistance(EggDistance);
		eggArea.x += left;
		eggArea.y += top;
		
		
		if (CooldownTimer <= 0) {
			float positionX = eggArea.x + (rand.nextInt(20) - 10);
			float positionY = eggArea.y + (rand.nextInt(20) - 10);
			// Create a new larvae.
			Larvae l = new Larvae(positionX, positionY, _level, 0, team, level);
			l.angle = rand.nextInt(360);
			List<Object> Exclude = new ArrayList();
			Exclude.add(this);
			if (!_level.CheckCollisionExcluding(l, Exclude, l.getClass().getName())) {
				_level.AddObject(l, new PointF(l.left, l.top), l.width, l.height);
				Fertility -= FertilityHatchReduction;
				CooldownTimer = BirthCooldown;
			}
		}
		else {
			CooldownTimer -= 0.5;
		}
	}
	
}
