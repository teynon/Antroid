package com.eynon.antroid.model;

import java.io.Serializable;

public class Size implements Serializable {
	public float Width;
	public float Height;
	
	public Size(float width, float height) {
		Width = width;
		Height = height;
	}
}
