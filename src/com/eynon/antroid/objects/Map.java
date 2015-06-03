package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

public class Map implements Serializable {

	public transient List<Bitmap> tileResources;
	public transient Bitmap defaultTile;
	private transient Paint _paint = new Paint();
	private transient Context _context;
	
	public int[][] Map;
	
	public float tileWidth = 500;
	public float tileHeight = 500;
	public int Width = 20;
	public int Height = 20;
	private LevelManager _level;
	
	public Map(Context context, LevelManager level) {
		_context = context;
		_level = level;
		defaultTile = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(_context.getResources(), R.drawable.terrain1), (int)tileWidth, (int)tileHeight, false);
		tileResources = new ArrayList<Bitmap>();
		tileResources.add(defaultTile);
		
		int defaultIndex = tileResources.indexOf(defaultTile);
		
		// Initialize map.
		Map = new int[Height][Width];
		for (int y = 0; y < Height; y++) {
			for (int x = 0; x < Width; x++) {
				Map[y][x] = defaultIndex;
			}
		}
		
	}
	
	public int GetWidth() {
		return (int) (tileWidth * Width);
	}
	
	public int GetHeight() {
		return (int) (tileHeight * Height);
	}
	
	public void GenerateRandomMap() {
	
	}
	
	public void OnDraw(Canvas c) {
		// Determine the first tile that is within screen view
		// and determine the offset for that tile.
		int tileX = 0;
		int tileY = 0;
		
		tileX = (int)Math.ceil((_level.GetOffset().x * -1) / tileWidth);
		tileY = (int)Math.ceil((_level.GetOffset().y * -1) / tileHeight);
		
		if (tileX > 0) tileX--;
		if (tileY > 0) tileY--;
		
		float tileOffsetX = (_level.GetOffset().x % tileWidth);
		float tileOffsetY = (_level.GetOffset().y % tileHeight);
		
		// If the grid hits a value exactly, the ceil won't round up.
		if ((_level.GetOffset().x * -1) % tileWidth == 0 && _level.GetOffset().x < 0) {
			//tileX++;
			tileOffsetX -= tileWidth;
		}
		if ((_level.GetOffset().y * -1) % tileHeight == 0 && _level.GetOffset().y < 0) {
			//tileY++;
			tileOffsetY -= tileHeight;
		}
		
		float offsetX = tileOffsetX;
		
		// Begin drawing tiles.
		for (int y = tileY; y < Height && y < tileY + (_level.GetScreenSize().Height / tileHeight) + 1; y++) {
			for (int x = tileX; x < Width && x < tileX + (_level.GetScreenSize().Width / tileWidth) + 1; x++) {
				if (y >= 0 && x >= 0) {
					c.drawBitmap(tileResources.get(Map[y][x]), offsetX,  tileOffsetY, _paint);
				}
				offsetX += tileWidth;
			}
			
			tileOffsetY += tileHeight;
			offsetX = tileOffsetX;
		}
	}
	
}
