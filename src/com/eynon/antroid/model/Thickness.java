package com.eynon.antroid.model;

import java.io.Serializable;

public class Thickness implements Serializable {
	public float Left;
	public float Top;
	public float Right;
	public Float Bottom;
	
	public Thickness(float left, float top, float right, float bottom) {
		Left = left;
		Top = top;
		Right = right;
		Bottom = bottom;
	}
}
