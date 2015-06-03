package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.R;
import com.eynon.antroid.level.Tunnel;
import com.eynon.antroid.level.Tunnel.TunnelType;
import com.eynon.antroid.level.Tunnel.TunnelUse;
import com.eynon.antroid.level.TunnelBuilder;
import com.eynon.antroid.model.MathHelpers;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.PointF3D;
import com.eynon.antroid.model.Size;
import com.eynon.antroid.model.KnownObjects;
import com.eynon.antroid.model.Target;
import com.eynon.antroid.objects.Object.CollisionType;
import com.eynon.antroid.objects.Object.DamageType;
import com.eynon.antroid.enums.Action;
import com.eynon.antroid.enums.BlockType;
import com.eynon.antroid.enums.TargetType;
import com.eynon.antroid.enums.Task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.SystemClock;
import android.util.Log;

public class Ant extends Creature implements Serializable {
	
	protected static Random rand = new Random();
	protected transient static List<Bitmap> ant = null;
	protected transient Bitmap antGraphic = null;
	protected transient Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	protected transient Matrix matrix = null;
	private float _angle = 0;
	protected boolean detourLeft = true;
	protected boolean lastDetourLeft = true;
	protected int detourCount = 0;
	protected int detourAttentionSpan = 0;
	protected int detourAttentionPadder = 0;
	protected int detourTotalCount = 0;
	public Object carriedObject = null;
	public float lastLeft = 0;
	public float lastTop = 0;
	public float lastAngle = 0;
	public float unresponsive = 0;
	public Pheromone lastPheromone = null;
	public java.util.Map<Integer, Long> Cooldown = new HashMap<Integer, Long>();
	
	public Tunnel KnownFood = null;
	public Tunnel TargetTunnel = null;
	
	// How far can the ant see?
	protected float vision = 200;
	protected float visionOffset = 70;
	protected Task defaultTask = Task.Forage;
	
	
	// Distance between droppings
	private float pheromoneDistance = 200;
	
	private float PheremoneStrength = 5;
	private final float PheremoneFood = 60;
	private final float PheremoneFight = 80;
	private final float PheremoneDefault = 2;
	
	// Amount of pheremone depends on the current action.
	// Probabilities of actions (go home, attack, flee, defend, ...)
	protected double[] probabilities = { 0.1, 0.2, 0.1, 0.1 };
	private float lastPheromoneDistance = 0;
	
	private float pheromoneThreshold = 15;
	
	public int BuildLevel = 0;
	public PointF3D BuildTarget = new PointF3D(0, 0, 0);
	
	public Ant(float x, float y, LevelManager level, int type, int team, int lvl) {
		MaxHealth = 100;
		Health = MaxHealth;
		collisionType = CollisionType.Circle;
		left = x;
		top = y;
		_level = level;
		width = 40;
		height = 40;
		speed = 5;
		strength = 400;
		weight = 40;
		selectable = true;
		IsAlive = true;
		this.team = team;
		this.level = lvl;
		
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
		
		ProcessLogic();

		// Update location of carried object.
		if (carriedObject != null) {
			// Get location of tip of ant.
			PointF front = PointAtDistance((float)(width / 2));
			front.x += left;
			front.y += top;
			((Resource)carriedObject).UpdateLocation(front);
		}
	}
	
	public void ProcessLogic() {
		
		if (lastLeft == left && lastTop == top && lastAngle == angle) {
			unresponsive++;
			if (unresponsive > 100) {
				if (LevelManager.DebugMode)
					Log.i("Ant Unresponsive", "Ant is unresponsive");
			}
			
			if (unresponsive > 200) {
				Ant oldAnt = this;
				Ant newAnt = factory();
				
				_level.RemoveObject(oldAnt);
				_level.AddObject(newAnt, new PointF(newAnt.left, newAnt.top), newAnt.width, newAnt.height);
				return;
			}
		}
		else {
			unresponsive = 0;
		}
	
		lastLeft = left;
		lastTop = top;
		lastAngle = angle;
		
		// Search ahead for collisions with pheromones
		PointF viewPoint = PointAtDistance(vision + visionOffset);
		viewPoint.x += left;
		viewPoint.y += top;
		
		Pheromone p = _level.FindPheromone(viewPoint, vision);
		// Check viewpoint for objects.
		List<Object> objectsInView = _level.FindObjects(this, viewPoint, vision);
		
		List<Tunnel> tunnels = _level.GetTunnels(this);
		
		Target _target = null;
		if (TargetList.size() > 0) {
			_target = TargetList.peek();
			currentTask = _target.TargetTask;
		}
		else {
			currentTask = defaultTask;
		}
		
		if (currentTask == Task.Forage && carriedObject != null && carriedObject instanceof Resource) {
			currentTask = Task.Retrieve;
		}
		
		boolean walk = true;
		
		// What is the ant doing?
		switch (currentTask) {
		case Build:
			// If target is in range and is a buildable object.
			if (_target.Type == TargetType.OBJECT && _target.TargetObject instanceof Buildable && ((Buildable)_target.TargetObject).UnderConstruction && TargetInRange(_target)) {
				BuildObject((Buildable)_target.TargetObject);
				walk = false;
			}
			else {
				Navigate();
			}
			break;
		case Forage:
			// If the ant is foraging, he shouldn't be carrying anything.
			if (carriedObject != null && carriedObject instanceof Resource) {
				DropCarried();
			}
			
			PheremoneStrength = PheremoneDefault;
			
			for (Object o : objectsInView) {
				if (o.getTeam() != team && o.getTeam() != 0 && o.IsAlive) {
					int decision = (int)(rand.nextInt(100));
					// Should we fight it?
					// - Pheromone strength in the area increases likelihood of a fight.
					if (p != null) {
						decision += p.strength;
					}
					
					if (decision > 70) {
						// Fight...
						SetTarget(o, Task.Attack);
					}
				}
				else if (o instanceof Water && ((level < 0 && ((Water)o).linkedRoom == null) || (level >= 0 && !o.carried))) {
					// Go get it!
					SetTarget(o, Task.Gather);
					p = null;
				}
				else if (o instanceof AntHill || o instanceof AntHole) {
					if (o.getTeam() == team) {
						if (Cooldown.containsKey(o.ObjectID)) {
							if (SystemClock.currentThreadTimeMillis() > Cooldown.get(o.ObjectID))
								Cooldown.remove(o.ObjectID);
						}
						
						if (o instanceof AntHill) this.KnownObjects.addKnownHill((AntHill)o);
						if (o instanceof AntHole) this.KnownObjects.addKnownHole((AntHole)o);
						if (o.level == level && rand.nextDouble() < probabilities[Action.GOHOME.ordinal()] && !Cooldown.containsKey(o.ObjectID)) {
							// Set it to the target.
							SetTarget(o, Task.Navigate);
							p = null;
						}
						else {
							AddCooldown(o);
						}
					}
				}
			}

			// Still foraging?
			if (currentTask == Task.Forage) {
				if (level < 0 && KnownObjects.getHolesAtLevel(level).size() > 0)  {
					// Set target to the hole to get to the surface.
					SetDirectTarget(KnownObjects.getNearestHole(level,  new PointF(left, top)), Task.Navigate);
				}
				if (p != null && p.strength > pheromoneThreshold) {
					// Move towards the pheromone!
					SetTarget(p.left, p.top, p.level, Task.Navigate);
				}
				else {
					ScanForPath();
				}
			}
			break;
		case Navigate:
			Navigate();
				/*
			if (_target != null) {
				// Houston, we have a problem.
				if (_target.GetZ() != level) {
					// TODO: Path planning to target.
					break;
				}
				
				if (_target.GetZ() == level && (
						(parentTask == Task.Build && MathHelpers.Distance(left, top, _target.left, _target.top) < buildRange + _target.interactionRadius) || 
						(parentTask != Task.Build && _target.CheckInteractionRange((float)(left - width / 2), (float)(top - height /2), (float)(left + width / 2), (float)(top + height /2)))
						)
					) {
					if (_target instanceof Resource) {
						currentTask = parentTask;
						((Resource)_target).TriggerCollision(this);
						_goalTarget = null;
						_target = null;
						_currentTarget = null;
						break;
					}
					else {
						_target.TriggerCollision(this);
						if (_target instanceof Buildable && !((Buildable) _target).UnderConstruction ||
							!(_target instanceof Buildable)) {
							AddCooldown(_target);
							if (_target instanceof AntHill) {
								this.KnownObjects.addKnownHill((AntHill)_target);
								this.KnownObjects.addKnownHole(((AntHill)_target).linkedHole);
								AddCooldown(((AntHill)_target).linkedHole);
							}
							else if (_target instanceof AntHole) 
							{
								this.KnownObjects.addKnownHill(((AntHole)_target).linkedHill);
								this.KnownObjects.addKnownHole((AntHole)_target);
								AddCooldown(((AntHole) _target).linkedHill);
							}
						}
						
						_target = null;

						if (currentTask == parentTask) currentTask = defaultTask;
						else currentTask = parentTask;
						
						if (_goalTarget != null && currentTask != Task.Build)
							SetDirectTarget(_goalTarget.x, _goalTarget.y, _goalTarget.z);
						
						break;
					}
				}
			}
			
			if (_currentTarget == null && _target != null) {
				SetTargetObject(_target);
			}
			else if (_target == null) {
				if (currentTask == parentTask) currentTask = defaultTask;
				else currentTask = parentTask;
			}*/
			break;
		case Attack:
			PheremoneStrength = PheremoneFight;
			if (carriedObject != null) DropCarried();
			if (_target == null || !_target.TargetObject.IsAlive) {
				RemoveTargetObject();
			}
			else {
				// If target is in range, attack!
				if (WithinAttackRange(_target)) {
					// Attack!
					_target.TargetObject.DoDamage(attackDamage[DamageType.Melee.ordinal()], DamageType.Melee, this);
				}
				else {
					Navigate();
				}
			}
			break;
		case Gather:
			if (WithinGatherRange(_target) && _target.TargetObject != null) {
				PheremoneStrength = PheremoneFood;
				walk = false;
				if (_target.TargetObject instanceof Resource) {
					if (carriedObject == null && foodRemaining < hungerCapacity * 0.7) {
						// Consume the food
						Consume(_target.TargetObject, ((Resource)_target.TargetObject).Harvest(this));
						if (((Resource) _target.TargetObject).qty <= 0) currentTask = defaultTask;
					}
					else {
						// Gather object until we can't carry anymore.
						if (carriedObject == null || !_target.TargetObject.getClass().equals(carriedObject.getClass())) {
							carriedObject = _target.TargetObject.factory();
							carriedObject.blocking = BlockType.NONE;
							carriedObject.carried = true;
						}
		
						// Can we carry more?
						((Resource) carriedObject).SetQty(((Resource) carriedObject).qty + ((Resource)_target.TargetObject).Harvest(this));
						this.carryWeight = carriedObject.weight;
						if (carryWeight >= carryCapacity || ((Resource)_target.TargetObject).qty <= 0) {	
							RemoveTargetObject();
							
							if (KnownObjects.getHillsAtLevel(level).size() > 0) {
								SetTarget(KnownObjects.getNearestHill(level, new PointF(left, top)), Task.Navigate);
							}
							else if (lastPheromone != null) {
								SetTarget(lastPheromone.left, lastPheromone.top, lastPheromone.level, Task.Navigate);
							}
						}
					}
				}
				else {
					CompleteCurrentTarget();
				}
			}
			else {
				Navigate();
			}
			break;
		case Retrieve:
			if (carriedObject == null) {
				RemoveTargetObject();
				break;
			}
			
			if (level < 0) {
				// Path planning.
				// Find an area to store the food.
				boolean inRange = false; 
				
				// Loop through the tunnels currently colliding with ant.
				for (Tunnel t : tunnels) {
					if ((t.Purpose == TunnelUse.Empty || t.Purpose == TunnelUse.Food) && (t.CapacityRemaining() > 0)) {
						// Move to center of room first
						inRange = true;
						if (KnownFood == null) { 
							KnownFood = t;
							t.Purpose = TunnelUse.Food;
						}

						if (t.GetContainedObject() != null) {
							SetCurrentTarget(t.GetContainedObject());
							
							if (TargetInRange(TargetList.peek())) {
								t.AddContainedObject(carriedObject);
								ClearCarried();
								CompleteCurrentTarget();
								return;
							}
						}
						else {
							SetCurrentTarget(t.Start.x, t.Start.y, t.level);
							
							if (PointInRange(t.Start.x, t.Start.y, left, top)) {
								// Drop item and return to foraging.
								this.DropCarried();
								CompleteCurrentTarget();
								return;
							}
						}
						break;
					}
				}
				
				if (!inRange && tunnels.size() > 0) {
					// If target is in range, pop it off the list and get the next one.
					if (_target != null && PointInRange(_target.GetX(), _target.GetY(), left, top)) {
						TargetList.pop();
						if (TargetList.size() > 0) {
							_target = TargetList.peek();
						}
						else
							_target = null;
					}
					
					if (_target == null || _target.TargetTask != Task.Retrieve) {
						
						if (KnownFood == null || KnownFood.CapacityRemaining() <= 0) {
							// Set a random room as the target.
							// If we are touching a room, use the room instead of a tunnel.
							
							// TODO: Convert to linked node search for open room.
							int get = -1;
							List<Tunnel> tSearch = _level.GetTunnels();
							for (int x = 0; x < tSearch.size(); x++) {
								if ((tSearch.get(x).Type == TunnelType.BasicRoom || get == -1) && tSearch.get(x).CapacityRemaining() > 0) {
									get = x;
									break;
								}
							}
							
							if (get > -1) {
								List<Tunnel> Waypoints = GetNearestNeighbor(tunnels.get(get));
								
								if (Waypoints != null) {
									AddRoute(Waypoints, Task.Retrieve);
								}
							}
						}
						else {
							List<Tunnel> Waypoints = GreedyBest_TunnelSearch(tunnels.get(0), KnownFood);
							
							if (Waypoints.isEmpty()) {
								SetCurrentTarget(KnownFood.Start.x, KnownFood.Start.y, KnownFood.level);
							}
							else {
								AddRoute(Waypoints, Task.Retrieve);
							}
						}
					}
				}
			}
			else {
				if (_target == null || _target.TargetObject == null || !(_target.TargetObject instanceof AntHill)) {
					// Bring object home.
					for (Object o : objectsInView) {
						if (o.getTeam() != team && o.getTeam() != 0 && o.IsAlive) {
							int decision = (int)(rand.nextInt(100));
							// Should we fight it?
							// - Pheromone strength in the area increases likelihood of a fight.
							if (p != null) {
								decision += p.strength;
							}
							
							if (decision > 70) {
								// Fight...
								DropCarried();
								SetTarget(o, Task.Attack);
							}
						}
						else if (o instanceof AntHill && o.getTeam() == team) {
							// Set it to the target.
							SetTarget(o, Task.Retrieve);
							p = null;
						}
					}
					
					if (currentTask == Task.Retrieve) {
						
						if (p != null) {
							// Move towards the pheromone!
							SetCurrentTarget(p.left, p.top, p.level);
						}
						else {
							ScanForPath();
						}
					}
				}
				else {

					if (KnownObjects.getHillsAtLevel(level).size() > 0) {
						SetTarget(KnownObjects.getNearestHill(level, new PointF(left, top)), Task.Navigate);
					}
					else if (lastPheromone != null) {
						SetTarget(lastPheromone.left, lastPheromone.top, lastPheromone.level, Task.Navigate);
					}
					else
						Navigate();
				}
			}
			
			break;
		case ScanForPath:
			if (this.PointInRange(_target.GetX(), _target.GetY(), left, top)) {
				TargetList.pop();
			}
			break;
			default:
				RemoveTargetObject();
				break;
		}
		
		if (TargetList.size() > 0) {
			_target = TargetList.peek();
			currentTask = _target.TargetTask;
		}
		else _target = null;
		
		if (_target != null && walk) {
			PointF step = GetNextStep(_target);
			if (Math.abs(step.x) > 0 || Math.abs(step.y) > 0) {
				BlockType blocked = _level.CheckCollision(this, new PointF((float)(left + step.x), (float)(top + step.y)), width, height);
				if (blocked != BlockType.BLOCKING) {
					detourCount = 0;
					detourAttentionPadder = 0;
					float rateOfSpeed = 1;
					if (blocked == BlockType.PARTIAL)
						rateOfSpeed = (float) 0.5;
					left += (step.x * rateOfSpeed);
					top += (step.y * rateOfSpeed);
					
					lastPheromoneDistance += MathHelpers.Distance(0, 0, step.x, step.y);
					if (lastPheromoneDistance > pheromoneDistance && level == 0) {
						lastPheromoneDistance = 0;
						
						lastPheromone = _level.RegisterPheromone(left,  top, PheremoneStrength, level, team);
					}
					
					_level.RegisterObject(this, new PointF((float)(left), (float)(top)), width, height);
					matrix.setRotate((float) angle, antGraphic.getWidth() / 2, antGraphic.getHeight() / 2);
	
					angleToTarget = MathHelpers.GetAngleToPoint(left, top, _target.GetPointF());
					
					if (Math.abs(angle - angleToTarget) < angleBuffer) {
						angle = angleToTarget;
					}
				}
				else {					
					if (Math.abs(angle - angleToTarget) < angleBuffer) {
						directNavigation = false;
					}
					
					if (detourCount > detourAttentionSpan && !directNavigation) { 
						detourCount = 0;
						detourAttentionPadder += 20;
					}
					if (detourTotalCount > targetAttentionSpan) {
						RemoveTargetObject();
						detourTotalCount = 0;
					}
					else {
						
					
						if (detourCount == 0) {
							int chances = rand.nextInt(100);
							detourLeft = (chances > 85) ? !lastDetourLeft : lastDetourLeft;
							lastDetourLeft = detourLeft;
							detourAttentionSpan = rand.nextInt(attentionSpan) + minAttention + detourAttentionPadder;
						}
						
						detourCount++;
						detourTotalCount++;
						
						float searchAngle = angle;
						
						if (detourLeft) searchAngle += pivotSpeed;
						else searchAngle -= pivotSpeed;
						
						PointF newDirection = null;
		
						if (searchAngle < 0) searchAngle = 360 + searchAngle;
						searchAngle = searchAngle % 360;
						
						newDirection = ScaleSpeed(PointF.AngleToSlope(searchAngle));
						newDirection.x = (newDirection.x * 3) + left;
						newDirection.y = (newDirection.y * 3) + top;
						angleToTarget = searchAngle;
						
						if (currentTask == Task.ScanForPath) SetCurrentTarget(newDirection.x, newDirection.y, level);
						else AddTarget(newDirection.x,  newDirection.y, level, Task.ScanForPath);
					}
				}
			}
		}
		
		return;
	}
	
	public void Navigate() {
		Target target = TargetList.peek();
		if (target != null) {
			// Figure out a route
			if (target.GetZ() != level) {
				if (!AddDirectTarget_path(target.GetX(), target.GetY(), target.GetZ(), target.TargetTask)) {
					// We can't get there.
					RemoveTargetObject();
				}
			}
			else if (TargetInRange(target)) {
				if (target.TargetObject != null) {
					// Notify collision
					target.TargetObject.TriggerCollision(this);
					
					// If its a hill, add it to the known list.
					if (target.TargetObject instanceof Buildable && !((Buildable) target.TargetObject).UnderConstruction ||	!(target.TargetObject instanceof Buildable)) {
						AddCooldown(target.TargetObject);
						if (target.TargetObject instanceof AntHill) {
							this.KnownObjects.addKnownHill((AntHill)target.TargetObject);
							this.KnownObjects.addKnownHole(((AntHill)target.TargetObject).linkedHole);
							AddCooldown(((AntHill)target.TargetObject).linkedHole);
						}
						else if (target.TargetObject instanceof AntHole) 
						{
							this.KnownObjects.addKnownHill(((AntHole)target.TargetObject).linkedHill);
							this.KnownObjects.addKnownHole((AntHole)target.TargetObject);
							AddCooldown(((AntHole) target.TargetObject).linkedHill);
						}
					}
				}
				
				TargetList.pop();
			}
			else if (target.TargetObject != null && target.TargetObject instanceof Tunnel && ((Tunnel)target.TargetObject).Type == TunnelType.Path) {
				// Pop it off the list.
				TargetList.pop();
			}
		}
	}
	
	public boolean TargetInRange(Target target) {
		if (target.GetZ() == level) {
			if (target.TargetObject != null) {
				if  (target.TargetTask == Task.Build && MathHelpers.Distance(left, top, target.GetX(), target.GetY()) < buildRange + target.TargetObject.interactionRadius)
					return true;
				else if (target.TargetTask != Task.Build && target.Type == TargetType.OBJECT && target.TargetObject.CheckInteractionRange((float)(left - width / 2), (float)(top - height /2), (float)(left + width / 2), (float)(top + height /2)))
					return true;
			}
			else if (PointInRange(target.GetX(), target.GetY(), left, top))
				return true;
		}
		
		return false;
	}
	
	public void AddRoute(List<Tunnel> tunnels, Task task) {
		for (Tunnel t : tunnels) {
			AddTarget(t, task);
		}
	}
	
	public void DropCarried() {
		if (carriedObject != null) {
			carriedObject.carried = false;
			
			// Check for a collision with other objects
			if (carriedObject instanceof Resource) {
				if (carriedObject.level < 0) {
					// Get the affected tunnel. If the tunnel
					// is a storage room, add it to the rooms storage object.
					List<Tunnel> tunnels = _level.GetTunnels(carriedObject);
					for (Tunnel t : tunnels) {
						if (t.Purpose == TunnelUse.Food) {
							t.AddContainedObject(carriedObject);
						}
					}
				}
				((Resource)carriedObject).DoDrop();
			}
			
			carriedObject = null;
		}
		carryWeight = 0;
	}
	
	public void ClearCarried() {
		carriedObject = null;
		carryWeight = 0;
	}
	
	@Override
	public void RemoveTargetObject() {
		super.RemoveTargetObject();

		currentTask = defaultTask;
	}
	
	public void ScanForPath() {
		// Randomness! Turn a bit until we find a pheromone. The ant should be able to use it's own trail to get back.
		if (detourCount > detourAttentionSpan * 2) detourCount = 0;
		
		if (detourCount == 0) {
			detourLeft = rand.nextBoolean();
			detourAttentionSpan = rand.nextInt(attentionSpan) + minAttention;
		}
		
		detourCount++;
		
		float searchAngle = angle;
		
		if (detourLeft) searchAngle += pivotSpeed * 2;
		else searchAngle -= pivotSpeed * 2;
		
		PointF newDirection = null;

		searchAngle = searchAngle % 360;
		
		newDirection = ScaleToDistance(PointF.AngleToSlope(searchAngle), 200);
		newDirection.x = (newDirection.x * 3) + left;
		newDirection.y = (newDirection.y * 3) + top;
		angleToTarget = searchAngle;

		if (currentTask == Task.ScanForPath) SetCurrentTarget(newDirection.x, newDirection.y, level);
		else AddTarget(newDirection.x,  newDirection.y, level, Task.ScanForPath);
	}
	
	public boolean PointInRange(float x, float y, float x2, float y2) {
		return (Math.abs(x - x2) < navigateProximity && Math.abs(y - y2) < navigateProximity);
	}
	
	public boolean WithinGatherRange(Target target) {
		return TargetInRange(target);
	}
	
	public boolean WithinAttackRange(Target target) {
		return PointInRange(target.GetX(), target.GetY(), left, top);
	}
	
	@Override
	public PointF GetNextStep(Target target) {
		PointF step = new PointF(0, 0);
		if (target != null) {
			if (!PointInRange(target.GetX(), target.GetY(), left, top)) {
				if (target != null) {
					// If the angle isn't set to the desired angle, we need to change direction.
					if (angleToTarget != angle) {
						/*if (Math.abs(angleToTarget - angle) > pivotSpeed) {
							angle = angleToTarget;
						}
						else */
						if ((angleToTarget > angle && angleToTarget - angle < 180) || (angleToTarget < angle && angle - angleToTarget > 180))
							angle += (detourCount > 0) ? pivotSpeed * 2 : pivotSpeed;
						else
							angle -= (detourCount > 0) ? pivotSpeed * 2 : pivotSpeed;
						
						if (angle > 360) angle -= 360;
						else if (angle < 0) angle += 360;
						
						// If distance is too small, slow down the walk distance.
						float multiplier = 1;
						if (DistanceToTarget() < 100) {
							multiplier = (float) 0.2;
						}
						step = ScaleSpeed(PointF.AngleToSlope(angle), multiplier);
						
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
		}
		
		return step;
	}
	
	@Override
	public void DoDamage(float amount, DamageType type, Object sender) {
		Health -= amount;
		
		if (Health <= 0) {
			Kill();
		}
		else {
			SetHealthColor();
			if (sender instanceof Creature && sender != this) {
				if (currentTask != Task.Attack) {
					SetTarget(sender, Task.Attack);
				}
			}
		}
	}
	
	public void Kill() {
		IsAlive = false;
		blocking = BlockType.NONE;
		selectable = false;
		_level.RemoveSelection(this);
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
	
	@Override
	public void DoDrawSelection(Canvas c, PointF offset) {
		c.drawCircle((float)left + offset.x, (float)top + offset.y, (float) width + 5, selectPaint);
	}

	@Override
	public void DoDrawTargetPath(Canvas c, PointF offset) {
		if (TargetList.size() > 0) {
			Target _currentTarget = TargetList.peek();
			// Draw line to current target.
			c.drawLine(left + offset.x, top + offset.y, _currentTarget.GetX() + offset.x, _currentTarget.GetY() + offset.y, debugPaint);
		}
	}

	@Override
	public Ant factory() {
		return new Ant(left, top, _level, 0, team, level);
	}
	
	@Override
	public void SetLevel(int lvl) {
		level = lvl;
		if (this.carriedObject != null) carriedObject.level = lvl;
	}
	
	// Go to a random neighbor room
	public List<Tunnel> GetNearestNeighbor(Tunnel current) {
		List<Tunnel> Used = new ArrayList<Tunnel>();
		List<Tunnel> Route = new ArrayList<Tunnel>();
		
		Tunnel initial = current;
		
		// Examine each node. Find the closest one that isn't used.
		Tunnel nextNode = current;
		
		if (current.AttachedTo.size() > 0) {
			do {
				nextNode = null;
				Tunnel next = current.AttachedTo.get(rand.nextInt(current.AttachedTo.size()));
				if (Used.contains(next)) {
					for (Tunnel nextN : current.AttachedTo) {
						if (Used.contains(nextN))
							continue; 
						
						nextNode = nextN;
					}
				}
				else nextNode = next;
				
				if (nextNode == null) {
					// If there is nothing in the route, the route is impossible.
					if (Route.size() == 0) break;
					
					// Set the current node as extinguished (No routes available.)
					Used.add(current);
					
					// Backtrack.
					nextNode = Route.get(Route.size() - 1);
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
			} while (current.Type != TunnelType.BasicRoom || current == initial);
			
			Route.add(current);
		}
		
		return Route;
	}
	
	public void Starving() {
		this.DoDamage((rand.nextFloat() * maxHungerDamage - minHungerDamage) + minHungerDamage, DamageType.Hunger, this);
	}
	
	public void Consume(Object obj, float qty) {
		if (obj instanceof Resource) {
			foodRemaining += ((Resource)obj).FillRate * qty;
			if (foodRemaining > hungerCapacity) foodRemaining = hungerCapacity;
		}
	}
	
	@Override
	public void TriggerAction(Action action, PointF3D location) {
		switch (action) {
		case DIGDOWN:
			SetTarget(location.x, location.y, location.z, Task.Build);
			break;
		case DIGROOMSELECTLINK:
			SetTarget(location.x, location.y, location.z, Task.BuildRoom);
		case DIGROOM:
			SetTarget(location.x, location.y, location.z, Task.BuildRoom);
			break;
		}
	}
	
	public void BuildObject(Buildable TargetBuildObject) {
		// TODO: Check if target is buildable.
		if (TargetBuildObject != null && !TargetBuildObject.UnderConstruction) {
			SetNextBuildTarget(TargetBuildObject);
		}
		else {
			if (TargetBuildObject != null && MathHelpers.Distance(left, top, TargetBuildObject.left, TargetBuildObject.top) < buildRange + TargetBuildObject.interactionRadius) {
				TargetBuildObject.IncrementBuildProgress((float)strength);
				
				// Completed!
				if (!TargetBuildObject.UnderConstruction) {
					SetNextBuildTarget(TargetBuildObject);
				}
			}
		}
	}
	
	public void SetNextBuildTarget(Buildable TargetBuildObject) {
		boolean clearTask = true;
		if (TargetBuildObject instanceof TunnelBuilder) {
			// Look for next tunnel to build.
			for (TunnelBuilder t : ((TunnelBuilder) TargetBuildObject).Neighbors) {
				if (t != null && t.UnderConstruction) {
					// Set it.
					SetTarget(t, Task.Build);
					clearTask = false;
				}
			}
			
			if (clearTask) {
				// See if the tunnel we just finished is connected to a tunnel that is under construction.
				Tunnel t = ((TunnelBuilder) TargetBuildObject).LinkedTunnel;
				for (Tunnel attached : t.AttachedTo) {
					if (attached.UnderConstruction && attached.BuildPoints != null && attached.BuildPoints.size() > 0) {
						SetTarget(attached.BuildPoints.get(0), Task.Build);
						clearTask = false;
						break;
					}
				}
			}
			
			if (clearTask) {
				// See if there are any builds waiting in the build queue.
				List<Buildable> target = _level.GetBuildQueue(team);
				if (target.size() > 0) {
					clearTask = false;
					// TODO: Check if route is available, set target.
					for (Buildable b : target) {
						Tunnel collision = _level.GetTunnelCollision(new PointF3D(b.left,  b.top, b.level), 1, 1);
						if (collision != null)
						{
							SetDirectTarget(b, Task.Build);
							clearTask = false;
							break;
						}
					}
				}
			}
		}
		
		if (clearTask) {
			RemoveTargetObject();
		}
	}
	
	@Override
	public boolean SetDirectTarget(float x, float y, float z, Task task) {		
		SetTarget(x, y, z, task);
		
		return AddDirectTarget_path(x, y, z, task);
	}
	
	public boolean SetDirectTarget(Object o, Task task) {
		SetTarget(o, task);
		return AddDirectTarget_path(o.left, o. top, o.level, task);
	}
	
	private boolean AddDirectTarget_path(float x, float y, float z, Task task) {
		boolean DidFindRoute = false;
		
		//DropCarried();

		// Set the object to this goal angle exactly.
		if (z == level)
			directNavigation = true;
		
		if (level == z && z < 0) {
			// Plan path to destination
			Tunnel currentTunnel = _level.GetTunnelCollision(new PointF3D(left, top, level), width, width);
			
			Tunnel t = _level.GetTunnelCollision(new PointF3D(x, y, z), 0, 0);
			if (t != null && currentTunnel != null) {
				List<Tunnel> Waypoints = GreedyBest_TunnelSearch(currentTunnel, t);
				if (Waypoints.size() > 0) {
					// Route works, set it
					AddRoute(Waypoints, task);
					DidFindRoute = true;
				}
			}
		}
		else if (level < z) {
			// Find nearest hole, then navigate from there
			Tunnel currentTunnel = _level.GetTunnelCollision(new PointF3D(this.left, this.top, this.level), width, width);
			List<AntHole> holes = KnownObjects.getHolesAtLevel(level);
			if (holes.size() > 0) {
				for (AntHole ah : holes) {
					Tunnel t = _level.GetTunnelRoomAtPoint(new PointF(ah.left, ah.top), level);
					if (t != null) {
						List<Tunnel> Waypoints = GreedyBest_TunnelSearch(currentTunnel, t);
						if (Waypoints.size() > 0) {
							// Route works, set it
							AddTarget(ah, task);
							AddRoute(Waypoints, task);
							DidFindRoute = true;
							break;
						}
					}
				}
			}
		}
		else if (level > z) {
			// Find hill near target, then path plan from there.
			Tunnel targetTunnel = _level.GetTunnelRoomAtPoint(new PointF(x, y), (int)z);
			if (targetTunnel != null) {
				List<AntHill> hills = KnownObjects.getHillsAtLevel(level);
				if (hills.size() > 0) {
					for (AntHill ah : hills) {
						Tunnel t = _level.GetTunnelRoomAtPoint(new PointF(ah.left, ah.top), level - 1);
						List<Tunnel> Waypoints = GreedyBest_TunnelSearch(t, targetTunnel);
						if (Waypoints.size() > 0) {
							// Route works, set the hill as the target.
							AddRoute(Waypoints, task);
							AddTarget(ah, task);
							DidFindRoute = true;
							break;
						}
					}
				}
			}
		}
		
		return DidFindRoute;
	}
	
	@Override
	public void Interaction(Object obj) {
		if (obj instanceof AntHill) {
		}
	}
	
	public void AddCooldown(Object obj) {
		Cooldown.put(obj.ObjectID, SystemClock.currentThreadTimeMillis() + 2000);
	}
}
