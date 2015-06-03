package com.eynon.antroid.model;

import com.eynon.antroid.enums.RotationDirection;

public class CollisionDetection {
	
	
	public static boolean CheckCollisionCircle(float x1, float y1, float x2, float y2, float r) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float rr = (float) (r);
		
		return (dx * dx) + (dy * dy) < rr * rr;
	}
	
	public static boolean CheckRectCollision(float x1, float y1, float x2, float y2, float ox1, float oy1, float ox2, float oy2) {
		return (ox1 < x2 && ox2 > x1 && oy1 < y2  && oy2 > y1);
	}
	
	public static boolean CheckPointInRect(float left, float top, float right, float bottom, float x, float y) {
		return (x > left && x < right && y > top && y < bottom);
	}
	
	public static boolean CheckRectCircleCollision(float x1, float y1, float width, float height, float cx, float cy, float cr) {
		x1 -= cr;
		y1 -= cr;
		width += (cr * 2);
		height += (cr * 2);
		
		return CheckPointInRect(x1, y1, x1 + width, y1 + height, cx, cy);
	}
	
	public static boolean CheckLineIntersection(PointF l1p1, PointF l1p2, PointF l2p1, PointF l2p2) {
		RotationDirection face1 = GetRotationDirection(l1p1, l1p2, l2p2);
		RotationDirection face2 = GetRotationDirection(l1p1, l1p2, l2p1);
		RotationDirection face3 = GetRotationDirection(l1p1, l2p1, l2p2);
		RotationDirection face4 = GetRotationDirection(l1p2, l2p1, l2p2);
		
		return ((face1 == RotationDirection.LINEAR && face2 == RotationDirection.LINEAR && face3 == RotationDirection.LINEAR && face4 == RotationDirection.LINEAR) || (face1 != face2 && face3 != face4));
	}
	
	public static RotationDirection GetRotationDirection(PointF p1, PointF p2, PointF p3) {
		float l1 = (p3.y - p1.y) * (p2.x - p1.x);
		float l2 = (p2.y - p1.y) * (p3.x - p1.x);
		
		if (l1 > l2) return RotationDirection.CLOCKWISE;
		else if (l1 < l2) return RotationDirection.COUNTERCLOCKWISE;
		
		return RotationDirection.LINEAR;
	}
	
	public static boolean CheckCollisionRotatedRectangleCircle(PointF r0, PointF r1, PointF r2, PointF r3, PointF circle, float cr) {
		// Rotate the rectangle.
		float Width = MathHelpers.Distance(r0.x, r0.y, r1.x, r1.y);
		float Height = MathHelpers.Distance(r0.x, r0.y, r3.x, r3.y);
		float angle = MathHelpers.GetAngleToPoint(r0.x, r0.y, r1);
		
		// Angle for new point.
		float angleNew = angle - MathHelpers.GetAngleToPoint(r0.x, r0.y, new PointF(r0.x + Width, r0.y));
		PointF angleFromOrigin = MathHelpers.PointAtDistance(MathHelpers.Distance(r0.x, r0.y, circle.x, circle.y), MathHelpers.GetAngleToPoint(r0.x, r0.y, circle) - angleNew);
		
		return CheckRectCircleCollision(r0.x, r0.y, Width, Height, r0.x + angleFromOrigin.x, r0.y + angleFromOrigin.y, cr);
	}
	
	public static boolean CheckCollisionRotatedRectangle(PointF r0, PointF r1, PointF r2, PointF r3, PointF circle, float cr) {
		// Rotate the rectangle.
		float Width = MathHelpers.Distance(r0.x, r0.y, r1.x, r1.y);
		float Height = MathHelpers.Distance(r0.x, r0.y, r3.x, r3.y);
		float angle = MathHelpers.GetAngleToPoint(r0.x, r0.y, r1);
		
		// Angle for new point.
		float angleNew = angle - MathHelpers.GetAngleToPoint(r0.x, r0.y, new PointF(r0.x + Width, r0.y));
		PointF angleFromOrigin = MathHelpers.PointAtDistance(MathHelpers.Distance(r0.x, r0.y, circle.x, circle.y), MathHelpers.GetAngleToPoint(r0.x, r0.y, circle) - angleNew);
		
		return CheckRectCircleCollision(r0.x, r0.y, Width, Height, r0.x + angleFromOrigin.x, r0.y + angleFromOrigin.y, cr);
	}
}
