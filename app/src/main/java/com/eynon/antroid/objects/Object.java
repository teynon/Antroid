package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.enums.Action;
import com.eynon.antroid.enums.BlockType;
import com.eynon.antroid.enums.Task;
import com.eynon.antroid.level.Tunnel;
import com.eynon.antroid.model.KnownObjects;
import com.eynon.antroid.model.MathHelpers;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.PointF3D;
import com.eynon.antroid.model.Rectangle;
import com.eynon.antroid.model.Size;
import com.eynon.antroid.model.Target;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.util.Log;

public abstract class Object implements Serializable {
	public enum CollisionType {
		Square,
		Circle
	}
	
	// Static object ID tracker.
	public static int NextObjectID = 0;
	
	protected LevelManager _level = null;
	
	public CollisionType collisionType = CollisionType.Square;
	
	// Position
	public float left = 0;
	public float top = 0;
	public transient List<Cell> cells = null;
	public int level = 0;
	
	public float angle = 0;
	public float angleToTarget = 0;
	
	// Selection
	public boolean selectable = false;
	public boolean targetable = false;
	protected float _hitBuffer = 30;
	protected float angleBuffer = 4;
	public Tunnel OwnedRoom = null;
	
	// Targeting
	public Stack<Target> TargetList = new Stack<Target>();
	//protected PointF3D _goalTarget = null;
	//protected Object _target = null;
	
	// Dimensions
	public double width = 0;
	public double height = 0;
	
	// Interaction Radius
	public double interactionRadius = 0;
	
	// Attributes
	public BlockType blocking = BlockType.BLOCKING;
	public boolean carryable = false;
	public boolean carried = false;
	public int weight = 0;
	public float speed = 1;
	public float pivotSpeed = 6;
	public int navigateProximity = 3;
	public float attackRange = 60;
	protected float buildRange = 20;
	public float[] attackDamage = { (float) 0.2, (float) 0.1 };
	public KnownObjects KnownObjects = new KnownObjects();
	public Task currentTask = Task.Forage;
	
	public int MaxHealth = 1000;
	public static int MinHealth = 1;
	
	public enum DamageType {
		Melee,
		Poison,
		Hunger
	}
	
	// Navigation
	// Attention span determines how long an ant will pursue an unknown goal.
	public int attentionSpan = 800;
	public int minAttention = 400;
	public int targetAttentionSpan = 300;
	public boolean directNavigation = false;

	protected int team = 0;
	
	// Livelihood
	public boolean IsAlive = true;
	public float Health = 0;
	public int ObjectID;
	
	public transient Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public transient Paint healthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public transient Paint hungerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public static transient Paint debugPaint = null;
	public static transient Paint debugActionPaint = null;
	
	public Object() {
		cells = new ArrayList<Cell>();
		paint.setColor(Color.RED);
		hungerPaint.setColor(Color.rgb(200, 150, 0));
		if (LevelManager.DebugMode) {
			debugPaint = new Paint();
			debugActionPaint = new Paint();
			debugPaint.setStyle(Paint.Style.STROKE);
			debugPaint.setColor(Color.YELLOW);
			debugActionPaint.setStyle(Paint.Style.STROKE);
			debugActionPaint.setColor(Color.WHITE);
		}
		
		ObjectID = NextObjectID;
		NextObjectID++;
	}
	
	public void SetHealthColor() {
		int Red = 0;
		int Green = 0;
		
		Red = (int) (255 - ((255 * Health) / MaxHealth));
		Green = (int) (((255 * Health) / MaxHealth));
		healthPaint.setColor(Color.rgb(Red, Green, 0));
		healthPaint.setAlpha(200);
	}
	
	public void setTeam(int team) {
		this.team = team;
	}
	
	public int getTeam() {
		return team;
	}

	// Create a new instance of the object.
	public abstract Object factory();
	
	public int Harvest(Object obj) {
		return 0;
	}
	
	public void Interaction(Object obj) {}
	
	// Process object logic
	public abstract void DoLogic();
	
	public abstract void DoDraw(Canvas c, PointF offset);
	
	public void DoDrawSelection(Canvas c, PointF offset) {}
	public void DoDrawTargetPath(Canvas c, PointF offset) {}
	
	public boolean IsOnScreen(PointF offset, Size screenSize) {
		if (left + width > offset.x && left - width < offset.x + screenSize.Width && top + height > offset.y && top - height < offset.y + screenSize.Height){
			return true;
		}
		
		return false;
	}
	
	public boolean HitTest(PointF hitArea) {
		if (left - width - _hitBuffer < hitArea.x && left + width + _hitBuffer > hitArea.x && top - height - _hitBuffer < hitArea.y && top + height + _hitBuffer > hitArea.y) {
			return true;
		}
		
		return false;
	}
	
	public PointF GetNextStep(Target target) {
		PointF step = new PointF(0, 0);
		if (target != null) {
			if (Math.abs(target.GetX() - left) < navigateProximity && Math.abs(target.GetY() - top) < navigateProximity) {
				if (TargetList.size() <= 0) {
					target = null;
				}
				else {
					target = TargetList.pop();
				}
			}
			
			if (target != null) {
				// If the angle isn't set to the desired angle, we need to change direction.
				if (angleToTarget != angle) {
					if (angleToTarget > angle && (angle + 360) - angleToTarget > 180)
						angle += pivotSpeed;
					else
						angle -= pivotSpeed;
					
					if (angle > 360) angle -= 360;
					else if (angle < 0) angle += 360;
					
					step = ScaleSpeed(PointF.AngleToSlope(angle));
					
				}
				else {
					// We're on track, lets go straight there.
					step.x = (float)(target.GetX() - left);
					step.y = (float)(target.GetY() - top);
					
					// Scale the distance moved to the speed.
					step = ScaleSpeed(step);
				}
			}
		}
		
		return step;
	}
	
	public PointF ScaleSpeed(PointF direction) {
		return ScaleSpeed(direction, 1);
	}
	
	public PointF ScaleSpeed(PointF direction, float multiplier) {
		float scale = (float) (speed / (Math.abs(direction.x) + Math.abs(direction.y)));
		scale *= multiplier;
		
		direction.x *= scale;
		direction.y *= scale;
		
		return direction;
	}
	
	public PointF ScaleToDistance(PointF direction, float distance) {
		float scale = (float) (distance / (Math.abs(direction.x) + Math.abs(direction.y)));
		
		direction.x *= scale;
		direction.y *= scale;
		
		return direction;
	}
	
	public PointF PointAtDistance(float distance) {
		PointF result = new PointF(0, 0);
		result.x = (float) (distance * Math.sin(MathHelpers.ToRadians(360 - angle)));
		result.y = (float) (distance * Math.cos(MathHelpers.ToRadians(360 - angle)));
		return result;
	}
	
	public void SetTarget(float x, float y, float z, Task task) {
		TargetList.clear();
		TargetList.add(new Target(new PointF3D(x, y, z), task));
		
		angleToTarget = MathHelpers.GetAngleToPoint(left, top, TargetList.peek().GetPointF());
		currentTask = task;
	}
	
	public void AddTarget(float x, float y, float z, Task task) {
		TargetList.add(new Target(new PointF3D(x, y, z), task));
		
		angleToTarget = MathHelpers.GetAngleToPoint(left, top, TargetList.peek().GetPointF());
	}
	
	public Rectangle GetRectangle() {
		return new Rectangle(this.left - this.width / 2, this.top - this.height / 2, this.left + this.width / 2, this.top + this.height / 2);
	}
	
	public void SetCurrentTarget(float x, float y, float z) {
		if (TargetList.size() > 0) {
			Target t = TargetList.peek();
			t.SetTargetPoint(new PointF3D(x, y, z));
			angleToTarget = MathHelpers.GetAngleToPoint(left, top, TargetList.peek().GetPointF());
		}
		else {
			SetTarget(x, y, z, Task.Navigate);
		}
	}
	
	public boolean SetDirectTarget(float x, float y, float z, Task task) {
		SetTarget(x, y, z, task);

		// Set the object to this goal angle exactly.
		if (z >= 0 && z == level)
			directNavigation = true;
		
		return true;
	}
	
	public void SetTarget(Object obj, Task task) {
		TargetList.clear();
		TargetList.add(new Target(obj, task));
		currentTask = task;
		angleToTarget = MathHelpers.GetAngleToPoint(left, top, TargetList.peek().GetPointF());
	}
	
	public void AddTarget(Object obj, Task task) {
		TargetList.add(new Target(obj, task));
		angleToTarget = MathHelpers.GetAngleToPoint(left, top, TargetList.peek().GetPointF());
	}
	
	public void SetCurrentTarget(Object obj) {
		if (TargetList.size() > 0) {
			Target t = TargetList.peek();
			t.SetTargetObject(obj);
		}
		else {
			SetTarget(obj, currentTask);
		}
		
		angleToTarget = MathHelpers.GetAngleToPoint(left, top, TargetList.peek().GetPointF());
		
	}
	
	public void CompleteCurrentTarget() {
		TargetList.pop();
	}
	
	public void RemoveTargetObject() {
		TargetList.clear();
	}
	
	public boolean CheckCollision(float x1, float y1, float x2, float y2) {
		return (left - (width / 2) < x2 && left + (width / 2) > x1 && top - (height / 2) < y2  && top + (height / 2) > y1);
	}
	
	public boolean CheckInteractionRange(float x1, float y1, float x2, float y2) {
		return (left - interactionRadius - (width / 2) < x2 && left + (width / 2) + interactionRadius > x1 && top - interactionRadius - (height / 2) < y2 && top + (height / 2) + interactionRadius > y1);
	}
	
	public boolean CheckCollisionCircle(float x1, float y1, float r) {
		float dx = left - x1;
		float dy = top - y1;
		float rr = (float) (r + (width / 2));
		
		return (dx * dx) + (dy * dy) < rr * rr;
	}
	
	public void onDebug(Canvas c, PointF offset) {
		c.drawRect((float)(left - (width / 2) + offset.x),  (float)(top - (height / 2) + offset.y), (float)(left + (width / 2) + offset.x), (float)(top + offset.y + (height / 2)), debugPaint);
		c.drawRect((float)(left - (width / 2) + offset.x + interactionRadius),  (float)(top - (height / 2) + offset.y + interactionRadius), (float)(left + offset.x + (width / 2) - interactionRadius), (float)(top + offset.y + (height / 2) - interactionRadius), debugActionPaint);
	}
	
	public float DistanceToTarget() {
		if (TargetList.size() <= 0) return 0;
		Target target = TargetList.peek();
		return (float) Math.sqrt(Math.pow(left - target.GetX(), 2) + Math.pow(top - target.GetY(), 2));
	}
	
	public void DoDamage(float amount, DamageType type, Object sender) {
		Health -= amount;
		
		if (Health <= 0) Die();
		else SetHealthColor();
	}
	
	public void Die() {
		IsAlive = false;
		carried = false;
	}
	
	public void TriggerCollision(Object obj) {
	}
	
	public void SetLevel(int lvl) {
		level = lvl;
	}
	
	public void TriggerAction(Action action, PointF3D location) {
	
	}
	
	// Input target and current in a reversed manner to get a stack that is in the proper order for the ant to follow.
	public List<Tunnel> GreedyBest_TunnelSearch(Tunnel target, Tunnel current) {
		List<Tunnel> Used = new ArrayList<Tunnel>();
		List<Tunnel> Route = new ArrayList<Tunnel>();
		
		// Examine each node. Find the closest one that isn't used.
		Tunnel nextNode = current;
		float nextNodeDistance = 0;
		
		if (current.AttachedTo.size() > 0) {
			while (current != target) {
				if (!Used.contains(current)) Used.add(current);
				nextNode = null;
				for (Tunnel next : current.AttachedTo) {
					if (Used.contains(next) || next.UnderConstruction)
						continue; 
					
					if (nextNode == null) {
						nextNode = next;
						nextNodeDistance = MathHelpers.Distance(target.Start.x, target.Start.y, nextNode.Start.x, nextNode.Start.y);
					}
					else {
						if (MathHelpers.Distance(target.Start.x,  target.Start.y,  next.Start.x, next.Start.y) < nextNodeDistance) {
							nextNode = next;
							nextNodeDistance = MathHelpers.Distance(current.Start.x,  current.Start.y,  next.Start.x, next.Start.y);
						}
					}
				}
				
				if (nextNode == null) {
					// If there is nothing in the route, the route is impossible.
					if (Route.size() == 0) break;
					
					// Set the current node as extinguished (No routes available.)
					Used.add(current);
					
					// Backtrack.
					current = Route.get(Route.size() - 1);
					Route.remove(Route.size() - 1);
				}
				else {
					// Set the current node to the route used.
					Route.add(current);
					
					// Dont let the next node come back.
					Used.add(current);
					
					// Set the current node to the next node.
					current = nextNode;
				}
			}
			
			if (current == target) 
				Route.add(current);
		}
		
		return Route;
	}
}
