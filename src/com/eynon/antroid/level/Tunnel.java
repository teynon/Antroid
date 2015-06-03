package com.eynon.antroid.level;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.R;
import com.eynon.antroid.enums.BuildableStatus;
import com.eynon.antroid.model.CollisionDetection;
import com.eynon.antroid.model.MathHelpers;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.PointF3D;
import com.eynon.antroid.model.Size;
import com.eynon.antroid.objects.Buildable;
import com.eynon.antroid.objects.Object;
import com.eynon.antroid.objects.Resource;

public class Tunnel extends Buildable implements Serializable {
	
	public enum TunnelType {
		Path,
		BasicRoom
	}
	
	public enum TunnelDirection {
		Left,
		Right,
		Up,
		Down
	}
	
	public enum TunnelUse {
		Empty,
		Lobby,
		Food,
		Eggs,
		Queen,
		Rest,
		Social
	}
	
	private transient static List<Bitmap> tunnel = null;
	private transient Bitmap Graphic = null;
	private transient Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private transient Paint buildZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private transient Paint noBuildZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private transient Matrix matrix = null;
	private float Angle = 0;
	private LevelManager _level = null;
	public TunnelUse Purpose = TunnelUse.Empty;
	private float minimumTunnelAngleSpacing = 45;
	private float MaxBuildRadius = 800;
	private float MinBuildSeparationRadius = 200;
	private Object ContainedResource = null;
		
	public float Capacity = 15;
	public List<Object> Contents = new ArrayList<Object>();
	
	public PointF3D Start = new PointF3D(0, 0, 0);
	public PointF3D End = new PointF3D(0, 0, 0);
	public float Width = 0; // Width of room or width of tunnel.
	public TunnelType Type = TunnelType.BasicRoom;
	public List<Tunnel> AttachedTo = new ArrayList<Tunnel>();
	public List<TunnelBuilder> BuildPoints = new ArrayList<TunnelBuilder>();
	
	
	public Tunnel(LevelManager level, PointF3D start, PointF3D stop, TunnelType type, float width) {
		_level = level;
		this.level = -1;
		
		if (tunnel == null) {
			tunnel = new ArrayList<Bitmap>();
			tunnel.add(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(level.Context.getResources(), R.drawable.tunnel1), (int)50, (int)50, false));
			tunnel.add(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(level.Context.getResources(), R.drawable.tunnelroom1), (int)50, (int)50, false));
		}
		
		Graphic = tunnel.get(type.ordinal());
		matrix = new Matrix();
		paint.setColor(Color.RED);
		buildZonePaint.setColor(Color.GREEN);
		//buildZonePaint.setAlpha(120);
		noBuildZonePaint.setColor(Color.RED);
		//noBuildZonePaint.setAlpha(120);
		
		Start = start;
		left = start.x;
		top = start.y;
		End = stop;
		Type = type;
		Width = width;
		
		if (type == TunnelType.Path) {
			Angle = GetAngleToPoint(start.GetPointF(), stop.GetPointF());
			Purpose = TunnelUse.Lobby;
		}
	}
	
	public boolean CanAddTunnelLink(PointF3D linkPoint) {
		if (Type == TunnelType.BasicRoom) {			
			// Loop through each attached path and see if the angle is too close.
			float thisAngle = MathHelpers.GetAngleToPoint(linkPoint.x, linkPoint.y, this.Start.GetPointF());
			
			for (Tunnel t : AttachedTo) {
				float tAngle = 0;
				if (t.Start.equals(this.Start)) {
					tAngle = MathHelpers.GetAngleToPoint(t.End.x, t.End.y, this.Start.GetPointF());
				}
				else {
					tAngle = MathHelpers.GetAngleToPoint(t.Start.x, t.Start.y, this.Start.GetPointF());
				}
				
				if (Math.abs(tAngle - thisAngle) < minimumTunnelAngleSpacing) {
					return false;
				}
			}
		}
		return true;
	}
	
	public float CapacityRemaining() {
		if (ContainedResource == null) return Capacity;
		else if (ContainedResource instanceof Resource) {
			return Capacity - ((Resource)ContainedResource).qty;
		}
		
		return 0;
	}
	
	public Object GetContainedObject() {
		return ContainedResource;
	}
	
	public void AddContainedObject(Object o) {
		if (ContainedResource == null) {
			ContainedResource = o;
			if (o instanceof Resource) {
				((Resource) o).linkedRoom = this;
			}
		}
		else if (ContainedResource instanceof Resource && o instanceof Resource) {
			((Resource) ContainedResource).SetQty(((Resource)ContainedResource).qty + ((Resource)o).qty);
			
			// Destroy object.
			_level.RemoveObject(o);
		}
	}
	
	public BuildableStatus WithinBuildZone(PointF3D point) {
		float distance = MathHelpers.Distance(Start.x, Start.y, point.x, point.y);
		if (distance < MinBuildSeparationRadius) {
			return BuildableStatus.TOOCLOSE;
		}
		else if (distance > MaxBuildRadius || UnderConstruction)  {
			return BuildableStatus.NOTINBUILDRANGE;
		}
		else {
			
			return BuildableStatus.OK;
		}
	}
	
	@Override
	public void Temporary() {
		paint.setAlpha(80);
		UnderConstruction = true;
	}

	@Override
	public void BeginConstruction() {
		super.BeginConstruction();
		paint.setAlpha(80);
		
		if (Type == TunnelType.Path) {
			// Lay out the roadwork.
			
			float distance = MathHelpers.Distance(Start.x, Start.y, End.x, End.y);
			int numberBuildNodes = (int)(distance / 100) + 1;
			
			float angle = MathHelpers.GetAngleToPoint(End.x, End.y, Start.GetPointF());
			PointF pointToAdd = MathHelpers.PointAtDistance(100, angle);
			PointF nextPoint = new PointF(End.x, End.y);
			TunnelBuilder lastNode = null;
			
			for (int x = 0; x < numberBuildNodes; x++) {
				
				TunnelBuilder buildPoint = new TunnelBuilder(_level, nextPoint.x, nextPoint.y, this, level, (int)(Width / 2));
				if (lastNode != null) {
					lastNode.addNeighbor(buildPoint);
					buildPoint.addNeighbor(lastNode);
				}
				BuildPoints.add(buildPoint);
				_level.AddObject(buildPoint, nextPoint, 50, 50);
				
				nextPoint.x += pointToAdd.x;
				nextPoint.y += pointToAdd.y;
				
				lastNode = buildPoint;
			}
		}
		else {
			// Create a build node.
			TunnelBuilder buildNode = new TunnelBuilder(_level, Start.x, Start.y, this, level, (int)((Width / 2) + 5));
			BuildPoints.add(buildNode);
			_level.AddObject(buildNode, Start.GetPointF(), (Width + 20), (Width + 20));
		}
	}

	@Override
	public void CompleteConstruction() {
		super.CompleteConstruction();
		paint.setAlpha(255);
	}
	
	@Override
	public void IncrementBuildProgress(float strength) {
		ConstructionProgress += strength;
		
		if (ConstructionProgress >= ConstructionSize) {
			CompleteConstruction();
		}
	}
	
	public float GetAngleToPoint(PointF start, PointF end) {
		return ToDegrees((float) Math.atan2(start.y - end.y, start.x - end.x));
	}
	
	public float ToDegrees(float radians) {
		float result = (float) (radians * (180 / Math.PI)) + 90;
		if (result < 0) result += 360;
		else if (result > 360) result -= 360;
		
		return result;
	}
	
	public Tunnel AddTunnel(TunnelDirection dir, float distance, float width) {
		PointF3D start = new PointF3D(Start.x, Start.y, Start.z);
		PointF3D stop = new PointF3D(Start.x, Start.y, Start.z);
		switch (dir) {
		case Up:
			stop.y -= (Width / 2) - 50;
			start.y = stop.y - distance;
			break;
		case Down:
			start.y += (Width / 2) - 50;
			stop.y = start.y + distance;
			break;
		case Left:
			stop.x -= (Width / 2) - 50;
			start.x = stop.x - distance;
			break;
		case Right:
			start.x += (Width / 2) - 50;
			stop.x = start.x + distance;
			break;
		}
		Tunnel t = new Tunnel(_level, start, stop, TunnelType.Path, width);
		t.AttachTo(this);
		return t;
	}
	
	public Tunnel AddTunnel(float dir, float distance, float width) {
		float radius = Width / 2 - width / 2;
		Angle = dir;
		PointF3D start = new PointF3D(Start.x, Start.y, Start.z);
		PointF3D stop = new PointF3D(Start.x, Start.y, Start.z);
		
		
		// Get angle
		PointF end = MathHelpers.PointAtDistance(distance,  dir);
		stop.x += end.x;
		stop.y += end.y; 
				
		Tunnel t = new Tunnel(_level, start, stop, TunnelType.Path, width);
		t.AttachTo(this);
		return t;
	}
	
	public Tunnel Connect(Tunnel tunnel) {
		Tunnel result = AddTunnel(MathHelpers.GetAngleToPoint(Start.x, Start.y, tunnel.Start.GetPointF()), MathHelpers.Distance(Start.x, Start.y, tunnel.Start.x, tunnel.Start.y), 125);
		result.AttachTo(tunnel);
		return result;
	}
	
	public void AttachTo(Tunnel attach) {
		if (!AttachedTo.contains(attach)) {
			AttachedTo.add(attach);
		}
		
		if (!attach.AttachedTo.contains(this)) {
			attach.AttachedTo.add(this);
		}
	}
	
	public void Detach(Tunnel detach) {
		if (detach.AttachedTo.contains(this)) {
			detach.AttachedTo.remove(this);
		}
		
		if (AttachedTo.contains(detach)) {
			AttachedTo.remove(detach);
		}
	}
	
	public void DetachAll() {
		List<Tunnel> toRemove = new ArrayList<Tunnel>();
		
		for (Tunnel t : AttachedTo) {
			toRemove.add(t);
		}
		
		for (Tunnel t : toRemove) {
			Detach(t);
		}
	}
	
	public void DoDrawBuildRadius(Canvas c, PointF offset) {
		c.drawCircle(Start.x + offset.x, (float)Start.y + offset.y, MaxBuildRadius, buildZonePaint);
	}
	
	public void DoDrawNoBuildRadius(Canvas c, PointF offset) {
		c.drawCircle(Start.x + offset.x, (float)Start.y + offset.y, MinBuildSeparationRadius, noBuildZonePaint);
	}
	
	public void DoDrawSelectable(Canvas c, PointF offset) {
		c.drawCircle(Start.x + offset.x, (float)Start.y + offset.y, (Width / 2) + 20, buildZonePaint);
	}
	
	public void DoDraw(Canvas c, PointF offset) {
		matrix.reset();
		if (Type == TunnelType.BasicRoom) {
			matrix.postScale(Width / 50, Width / 50);
			matrix.postTranslate((float)Start.x + offset.x - (Width / 2), (float)Start.y + offset.y - (Width / 2));
			c.drawBitmap(Graphic, matrix, paint);
			
			if (_level.DebugMode) {
				c.drawCircle(Start.x + offset.x, (float)Start.y + offset.y, Width / 2, debugPaint);
			}
		}
		else {
			// Get 90 degrees from angle, convert to slope, and increase position by that amount.
			float TempAngle = (Angle < 270) ? Angle + 90: Angle - 270;
			PointF change = PointF.ScaleToDistance(PointF.AngleToSlope(Angle), Width / 2);
			
			matrix.postScale(Width / 50, PointF.Distance(Start.x, Start.y, End.x, End.y) / 50);
			
			if (Angle % 90 == 0) {

				matrix.postTranslate(0, -(Width / 2));
			}
			else {
				matrix.postTranslate(-(Width / 2), 0);
			}
			
			matrix.postRotate(Angle);
			matrix.postTranslate((float)Start.x + offset.x - change.x, (float)Start.y + offset.y - change.y);
			c.drawBitmap(Graphic, matrix, paint);
			
			// Draw line
			if (_level.DebugMode) {
				c.drawLine(Start.x + offset.x, Start.y + offset.y,  End.x + offset.x,  End.y + offset.y, paint);
			}
		}
		
		/*float ox1 = 0;
		float ox2 = 0;
		float oy1 = 0;
		float oy2 = 0;
		
		if (Type == TunnelType.BasicRoom) {
			ox1 = Start.x - (Width / 2);
			ox2 = Start.x + (Width / 2);
			oy1 = Start.y - (Width / 2);
			oy2 = Start.y + (Width / 2);
		}
		else if (Type == TunnelType.Path) {
			
			if (Angle == 0) {
				// Top to bottom
				ox1 = End.x - (Width / 2);
				ox2 = End.x + (Width / 2);
				oy1 = End.y;
				oy2 = Start.y;
			}
			else if (Angle == 90) {
				// Right to left
				ox1 = End.x;
				ox2 = Start.x;
				oy1 = End.y - (Width / 2);
				oy2 = End.y + (Width / 2);
			}
			else if (Angle == 180) {
				// Bottom to Top
				ox1 = Start.x - (Width / 2);
				ox2 = Start.x + (Width / 2);
				oy1 = Start.y;
				oy2 = End.y;
			}
			else if (Angle == 270) {
				// Left to Right
				ox1 = Start.x;
				ox2 = End.x;
				oy1 = End.y - (Width / 2);
				oy2 = End.y + (Width / 2);
			}
		}
		
		c.drawRect(ox1 + offset.x, oy1 + offset.y, ox2 + offset.x, oy2 + offset.y, paint);*/
	}
	
	public boolean IsOnScreen(PointF offset, Size screenSize) {
		if (Type == TunnelType.BasicRoom) { 
			if (Start.x + (MaxBuildRadius / 2) > offset.x && Start.x - (MaxBuildRadius / 2) < offset.x + screenSize.Width && Start.y + (MaxBuildRadius / 2) > offset.y && Start.y - (MaxBuildRadius / 2) < offset.y + screenSize.Height) {
				return true;
			}
		}
		else if (Type == TunnelType.Path) {
			if (LineIntersectsSquare(Start.x, Start.y, End.x, End.y, offset.x, offset.y, offset.x + screenSize.Width, offset.y + screenSize.Height) > 0) {
				return true;
			}
			
			if ((Start.x > offset.x - (MaxBuildRadius / 2) && Start.x < offset.x + screenSize.Width + (MaxBuildRadius / 2) && Start.y > offset.y - (MaxBuildRadius / 2) && Start.y < offset.y + screenSize.Height + (MaxBuildRadius / 2)) ||
					(End.x > offset.x - (MaxBuildRadius / 2) && End.x < offset.x + screenSize.Width + (MaxBuildRadius / 2) && End.y > offset.y - (MaxBuildRadius / 2) && End.y < offset.y + screenSize.Height + (MaxBuildRadius))) {
				return true;
			}
		}
		
		return false;
	}
	
	public float CalcY(float xval, float x0, float y0, float x1, float y1)
	{
	    if(x1 == x0) return Float.NaN;
	    return y0 + (xval - x0)*(y1 - y0)/(x1 - x0);
	}

	public float CalcX(float yval, float x0, float y0, float x1, float y1)
	{
	    if(x1 == x0) return Float.NaN;
	    return x0 + (yval - y0)*(y1 - y0)/(x1 - x0);
	}
	
	public boolean CheckCollision(float x1, float y1, float x2, float y2) {
		boolean result = false;
		
		if (Type == TunnelType.BasicRoom) {
			// Check all four points of object.
			if (CollisionDetection.CheckCollisionCircle(Start.x, Start.y, x1, y1, Width / 2) &&
				CollisionDetection.CheckCollisionCircle(Start.x, Start.y, x1, y2, Width / 2) &&
				CollisionDetection.CheckCollisionCircle(Start.x, Start.y, x2, y1, Width / 2) &&
				CollisionDetection.CheckCollisionCircle(Start.x, Start.y, x2, y2, Width / 2)) {
				result = true;
			}
			else result = false;
		}
		else if (Type == TunnelType.Path) {
			result = true;
			// Rotate point around origin in reverse.
			PointF origin = new PointF(Start.x, Start.y);
			float[] angles = new float[4];
			angles[0] = MathHelpers.GetAngleToPoint(origin.x, origin.y, new PointF(x1, y1));
			angles[1] = MathHelpers.GetAngleToPoint(origin.x, origin.y, new PointF(x1, y2));
			angles[2] = MathHelpers.GetAngleToPoint(origin.x, origin.y, new PointF(x2, y1));
			angles[3] = MathHelpers.GetAngleToPoint(origin.x, origin.y, new PointF(x2, y2));
			float[] points = new float[4];
			points[0] = MathHelpers.Distance(Start.x, Start.y, x1, y1);
			points[1] = MathHelpers.Distance(Start.x, Start.y, x1, y2);
			points[2] = MathHelpers.Distance(Start.x, Start.y, x2, y1);
			points[3] = MathHelpers.Distance(Start.x, Start.y, x2, y2);
			
			for (int x = 0; x < 4; x++) {
				// Subtract the rotation.
				angles[x] -= Angle;
				
				// Get the point and check if its within the unrotated box.
				PointF p = MathHelpers.PointAtDistance(points[x], angles[x]);
				p.x += Start.x;
				p.y += Start.y;
				
				if (!CollisionDetection.CheckPointInRect(Start.x - (Width / 2), Start.y, Start.x + (Width / 2), Start.y + MathHelpers.Distance(Start.x, Start.y, End.x, End.y), p.x, p.y)) {
					result = false;
					break;
				}
			}
		}
		
		return result;
	}

	public int LineIntersectsSquare(float x0, float y0, float x1, float y1, float left, float top, float right, float bottom)
	{
	    int intersections = 0;
	    if(CalcX(bottom, x0, y0, x1, y1) < right && CalcX(bottom, x0, y0, x1, y1) > left  ) intersections++;
	    if(CalcX(top   , x0, y0, x1, y1) < right && CalcX(top   , x0, y0, x1, y1) > left  ) intersections++;
	    if(CalcY(left  , x0, y0, x1, y1) < top   && CalcY(left  , x0, y0, x1, y1) > bottom) intersections++;
	    if(CalcY(right , x0, y0, x1, y1) < top   && CalcY(right , x0, y0, x1, y1) > bottom) intersections++;
	    return intersections;
	}

	@Override
	public Object factory() {
		return new Tunnel(_level, Start, End, Type, Width);
	}

	@Override
	public void DoLogic() {
		// TODO Auto-generated method stub
		
	}
}
