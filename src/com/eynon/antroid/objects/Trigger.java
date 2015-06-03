package com.eynon.antroid.objects;

import java.io.Serializable;

import android.graphics.Canvas;

import com.eynon.antroid.model.PointF;

public class Trigger extends Object implements Serializable {

	public int Radius = 10;
	
	@Override
	public Object factory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void DoLogic() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void DoDraw(Canvas c, PointF offset) {
		// No drawing unless debug.
		
	}

}
