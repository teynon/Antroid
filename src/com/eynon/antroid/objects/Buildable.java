package com.eynon.antroid.objects;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.R;
import com.eynon.antroid.model.PointF;

public abstract class Buildable extends Object {

	public float ConstructionProgress = 0;
	public float ConstructionSize = 10000;
	public boolean UnderConstruction = false;
	
	public void BeginConstruction() {
		UnderConstruction = true;
	}
	
	public void Temporary() { }
	
	public void CompleteConstruction() {
		UnderConstruction = false;
	}
	
	public void IncrementBuildProgress(float strength) {
		ConstructionProgress += strength;
		
		if (ConstructionProgress >= ConstructionSize) {
			CompleteConstruction();
		}
	}
}
