package com.eynon.antroid.model;

import java.io.Serializable;

public class Rectangle implements Serializable {
	public PointF P1;
	public PointF P2;
	
	public Rectangle() {
		P1 = new PointF(0,0);
		P2 = new PointF(0,0);
	}
	
	public Rectangle(PointF p1, PointF p2) {
		P1 = p1;
		P2 = p2;
	}
	
	public Rectangle(double x1, double y1, double x2, double y2) {
		P1 = new PointF((float)x1, (float)y1);
		P2 = new PointF((float)x2, (float)y2);
	}
}
