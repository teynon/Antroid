package com.eynon.antroid.model;

import java.io.Serializable;

public class PointF implements Serializable {
	public float x = 0;
	public float y = 0;
	
	public PointF(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public static float Distance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1,  2));
	}
	
	public static PointF AngleToSlope(float angle) {
		angle = angle % 360;
		
		PointF result = new PointF(0, 0);
		if (angle == 0) {
			result.x = 1;
			result.y = 0;
		}
		else if (angle == 90) {
			result.x = 0;
			result.y = 1;
		}
		else if (angle == 180) {
			result.x = -1;
			result.y = 0;
		}
		else if (angle == 270) {
			result.x = 0;
			result.y = -1;
		}
		else {
			result.x = (float) Math.tan(ToRadians(angle));
			result.y = 1;
			
			if (angle > 90 && angle < 270) {
				result.y = -1;
			}
			
			if (angle < 180) result.x = Math.abs(result.x) * -1;
			else result.x = Math.abs(result.x);
		}
		
		return result;
	}
	
	public static PointF ScaleToDistance(PointF direction, float distance) {
		float scale = (float) (distance / (Math.abs(direction.x) + Math.abs(direction.y)));
		
		direction.x *= scale;
		direction.y *= scale;
		
		return direction;
	}
	
	public static float ToRadians(float degrees) {
		return (float) (degrees * (Math.PI / 180));
	}
}
