package com.eynon.antroid.model;

public class MathHelpers {
	
	public static PointF PointAtDistance(float distance, float angle) {
		PointF result = new PointF(0, 0);
		result.x = (float) (distance * Math.sin(ToRadians(360 - angle)));
		result.y = (float) (distance * Math.cos(ToRadians(360 - angle)));
		return result;
	}
	
	public static float GetAngleToPoint(float left, float top, PointF point) {
		return ToDegrees((float) Math.atan2(top - point.y, left - point.x));
	}
	
	public static float Distance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1,  2));
	}
	
	public static float ToRadians(float degrees) {
		return (float) (degrees * (Math.PI / 180));
	}
	
	public static float ToDegrees(float radians) {
		float result = (float) (radians * (180 / Math.PI)) + 90;
		if (result < 0) result += 360;
		else if (result > 360) result -= 360;
		
		return result;
	}
	
}
