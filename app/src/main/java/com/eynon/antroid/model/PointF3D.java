package com.eynon.antroid.model;

import java.io.Serializable;


public class PointF3D implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public float x = 0;
	public float y = 0;
	public float z = 0;
	
	public PointF3D() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}
	
	public PointF3D(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public PointF3D(PointF p) {
		this.x = p.x;
		this.y = p.y;
		this.z = 0;
	}
	
	public PointF GetPointF() {
		return new PointF(x, y);
	}
	
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public boolean equals(float x, float y, float z)
	{
		return (x == this.x && y == this.y && z == this.z);
	}
	
	public boolean equals (PointF3D point) {
		return equals(point.x, point.y, point.z);
	}
	
	public boolean equals(Object o) {
		return this == o;
	}
}
