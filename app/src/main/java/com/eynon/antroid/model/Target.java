package com.eynon.antroid.model;

import java.io.Serializable;

import com.eynon.antroid.enums.TargetType;
import com.eynon.antroid.enums.Task;
import com.eynon.antroid.level.Tunnel;
import com.eynon.antroid.objects.Buildable;
import com.eynon.antroid.objects.Object;

public class Target implements Serializable {
	public Object TargetObject;
	public PointF3D TargetPoint;
	public TargetType Type = TargetType.POINT;
	public Task TargetTask = Task.Forage;
	
	public Target(PointF3D target, Task task) {
		SetTargetPoint(target);
		TargetTask = task;
	}
	
	public Target(Object target, Task task) {
		SetTargetObject(target);
		TargetTask = task;
	}
	
	public Target(Buildable target, Task task) {
		SetTargetBuild(target);
		TargetTask = task;
	}
	
	public void SetTargetPoint(PointF3D target) {
		if (Type == Type.OBJECT) {
			Type = TargetType.POINT;
		}
		
		Type = TargetType.POINT;
		TargetPoint = target;
	}
	
	public void SetTargetObject(Object target) {
		Type = TargetType.OBJECT;
		TargetObject = target;
	}
	
	public void SetTargetBuild(Buildable target) {
		Type = TargetType.BUILD;
		TargetObject = target;
	}
	
	public float GetX() {
		switch (Type) {
		case POINT:
			return TargetPoint.x;
		case OBJECT:
		case BUILD:
			return TargetObject.left;
		}
		
		return 0;
	}
	
	public float GetY() {
		switch (Type) {
		case POINT:
			return TargetPoint.y;
		case OBJECT:
		case BUILD:
			return TargetObject.top;
		}
		
		return 0;
	}
	
	public float GetZ() {
		switch (Type) {
		case POINT:
			return TargetPoint.z;
		case OBJECT:
		case BUILD:
			return TargetObject.level;
		}
		
		return 0;
	}
	
	public PointF GetPointF() {
		switch (Type) {
		case POINT:
			return TargetPoint.GetPointF();
		case OBJECT:
		case BUILD:
			return new PointF(TargetObject.left, TargetObject.top);
		}
		
		return null;
	}
}
