package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.eynon.antroid.enums.BlockType;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.objects.Object.CollisionType;


public class Grid implements Serializable {

	public float Width = 0;
	public float Height = 0;
	public int CellSize = 0;
	public int Rows = 0;
	public int Columns = 0;
	
	public Cell[][] Cells;
	
	public Grid(float width, float height, int cellSize) {
		Width = width;
		Height = height;
		CellSize = cellSize;
		
		// Start building the cells.
		Columns = (int) (Width / CellSize);
		Rows = (int) (Height / CellSize);
		Cells = new Cell[Rows][Columns];
	}
	
	public Pheromone RegisterPheremone(Pheromone p) {
		int StartCol = (int) Math.floor((p.left - (p.strength / 2)) / CellSize);
		int StartRow = (int) Math.floor((p.top - (p.strength / 2)) / CellSize);
		int EndCol = (int) Math.ceil((p.left + (p.strength / 2)) / CellSize);
		int EndRow = (int) Math.ceil((p.top + (p.strength / 2)) / CellSize);
		
		if (StartCol < 0) StartCol = 0;
		if (StartRow < 0) StartRow = 0;

		Pheromone collision = FindPheromoneCollision(p);
		if (collision == p) {
			for (int y = StartRow; y < EndRow && y < Rows; y++) {
				for (int x = StartCol; x < EndCol && x < Columns; x++) {
					if (Cells[y][x] == null)
						Cells[y][x] = new Cell(y, x);
					
					Cells[y][x].Register(p);
				}
			}
		}
		else p = collision;
		
		return p;
	}
	
	public Pheromone FindPheromoneCollision(Pheromone p) {
		int StartCol = (int) Math.floor((p.left - (p.strength / 2)) / CellSize);
		int StartRow = (int) Math.floor((p.top - (p.strength / 2)) / CellSize);
		int EndCol = (int) Math.ceil((p.left + (p.strength / 2)) / CellSize);
		int EndRow = (int) Math.ceil((p.top + (p.strength / 2)) / CellSize);
		
		if (StartCol < 0) StartCol = 0;
		if (StartRow < 0) StartRow = 0;

		Pheromone collision = null;
		for (int y = StartRow; y < EndRow && y < Rows; y++) {
			if (collision != null) break;
			for (int x = StartCol; x < EndCol && x < Columns; x++) {
				if (collision != null) break;
				if (Cells[y][x] != null) {
					for (Pheromone o : Cells[y][x].Pheromones) {
						if (o.CheckCollisionCircle(p.left, p.top, p.radius) && o.strength + p.strength < Pheromone.maxStrength) {
							collision = o;
							break;
						}
					}
				}
			}
		}
		
		if (collision != null) {
			CombinePheromone(collision, p);
			//collision = FindPheromoneCollision(collision);
			return collision;
		}
		
		return p;
	}
	
	public void CombinePheromone(Pheromone to, Pheromone from) {
		to.addStrength(from.strength);
		
		// Remove cells in the from pheromone.
		List<Cell> cells = from.cells;
		for (Cell c : cells) {
			c.Deregister(from);
		}
	}

	public Pheromone Collision(PointF location, float radius) {
		Pheromone result = null;
		
		// Get the cells that contain this unit.
		int StartCol = (int) Math.floor((location.x - radius) / CellSize);
		int StartRow = (int) Math.floor((location.y - radius) / CellSize);
		int EndCol = (int) Math.ceil((location.x + radius) / CellSize);
		int EndRow = (int) Math.ceil((location.y + radius) / CellSize);
		
		if (StartCol < 0) StartCol = 0;
		if (StartRow < 0) StartRow = 0;

		List<Pheromone> objects = new ArrayList<Pheromone>();
		// Find possible objects for collisions.
		for (int y = StartRow; y < EndRow && y < Rows; y++) {
			for (int x = StartCol; x < EndCol && x < Columns; x++) {
				if (Cells[y] != null && Cells[y][x] != null) {
					for (Pheromone o : Cells[y][x].Pheromones) {
						if (!objects.contains(o))
							objects.add(o);
					}
				}
			}
		}
		
		// Check object for collision.
		for (Pheromone o : objects) {
			if ((result == null || o.strength > result.strength) && o.CheckCollisionCircle(location.x, location.y, radius)) {
				result = o;
			}
		}
		
		return result;
	}

	public List<Object> CollisionObjects(Object obj, PointF location, float radius) {
		List<Object> result = new ArrayList<Object>();
		
		// Get the cells that contain this unit.
		int StartCol = (int) Math.floor((location.x - radius) / CellSize);
		int StartRow = (int) Math.floor((location.y - radius) / CellSize);
		int EndCol = (int) Math.ceil((location.x + radius) / CellSize);
		int EndRow = (int) Math.ceil((location.y + radius) / CellSize);
		
		if (StartCol < 0) StartCol = 0;
		if (StartRow < 0) StartRow = 0;

		List<Object> objects = new ArrayList<Object>();
		// Find possible objects for collisions.
		for (int y = StartRow; y < EndRow && y < Rows; y++) {
			for (int x = StartCol; x < EndCol && x < Columns; x++) {
				if (Cells[y] != null && Cells[y][x] != null) {
					for (Object o : Cells[y][x].Objects) {
						if (obj.level == o.level && !objects.contains(o))
							objects.add(o);
					}
				}
			}
		}
		
		// Check object for collision.
		for (Object o : objects) {
			if (o.CheckCollisionCircle(location.x, location.y, radius)) {
				result.add(o);
			}
		}
		
		return result;
	}
	
	public void DeregisterObject(Object obj) {
		int StartCol = 0;
		int StartRow = 0;
		int EndCol = Columns - 1;
		int EndRow = Rows - 1;
		
		if (StartCol < 0) StartCol = 0;
		if (StartRow < 0) StartRow = 0;

		List<Cell> remove = new ArrayList<Cell>();
		
		for (Cell c : obj.cells) {			
			//if (c.Row < StartRow || c.Row > EndRow || c.Col < StartCol || c.Col > EndCol) {
				// Remove.
				remove.add(c);
			//}
		}
		
		for (Cell c : remove) {
			c.Deregister(obj);
		}
	}
	
	public void RegisterObject(Object obj, PointF location, double width, double height) {
		int StartCol = (int) Math.floor((location.x - width / 2) / CellSize);
		int StartRow = (int) Math.floor((location.y - height / 2) / CellSize);
		int EndCol = (int) Math.ceil((location.x + width / 2) / CellSize);
		int EndRow = (int) Math.ceil((location.y + height / 2) / CellSize);
		
		if (StartCol < 0) StartCol = 0;
		if (StartRow < 0) StartRow = 0;

		List<Cell> remove = new ArrayList<Cell>();
		
		for (Cell c : obj.cells) {			
			if (c.Row < StartRow || c.Row > EndRow || c.Col < StartCol || c.Col > EndCol) {
				// Remove.
				remove.add(c);
				//c.Deregister(obj);
			}
		}
		
		for (Cell c : remove) {
			c.Deregister(obj);
		}
		
		remove = null;
		
		for (int y = StartRow; y <= EndRow && y < Rows; y++) {
			for (int x = StartCol; x <= EndCol && x < Columns; x++) {
				if (Cells[y][x] == null)
					Cells[y][x] = new Cell(y, x);
				
				Cells[y][x].Register(obj);
			}
		}
	}
	
	public BlockType Collision(Object obj, PointF location, double width, double height) {
		BlockType result = BlockType.NONE;
		
		// Get the cells that contain this unit.
		int StartCol = (int) Math.floor((location.x - (width / 2)) / CellSize);
		int StartRow = (int) Math.floor((location.y - (height / 2)) / CellSize);
		int EndCol = (int) Math.ceil((location.x + width) / CellSize);
		int EndRow = (int) Math.ceil((location.y + height) / CellSize);
		
		if (StartCol < 0) StartCol = 0;
		if (StartRow < 0) StartRow = 0;

		// Find possible objects for collisions.
		List<Object> objects = new ArrayList<Object>();
		for (int y = StartRow; y < EndRow && y < Rows; y++) {
			for (int x = StartCol; x < EndCol && x < Columns; x++) {
				if (Cells[y] != null && Cells[y][x] != null) {
					for (Object o : Cells[y][x].Objects) {
						if (obj != o && (o.blocking == BlockType.BLOCKING || o.blocking == BlockType.PARTIAL) && obj.level == o.level && !objects.contains(o)) 
							objects.add(o);
					}
				}
			}
		}
		
		// Check object for collision.
		for (Object o : objects) {
			if (o.collisionType == CollisionType.Circle && obj.collisionType == CollisionType.Circle) {
				if (o.blocking == BlockType.BLOCKING) {
					if (o != obj && o.CheckCollisionCircle(location.x, location.y, (float) (o.width / 2)) && !o.CheckCollisionCircle(obj.left,  obj.top,  (float)(obj.width / 2))) {
						result = BlockType.BLOCKING;
						break;
					}
				}
				else {
					if (o != obj && o.CheckCollisionCircle(location.x, location.y, (float) (o.width / 2))) {
						result = o.blocking;
					}
				}
			}
			else {
				if (o.blocking == BlockType.BLOCKING) {
					if (o != obj && o.CheckCollision((float)(location.x - obj.width / 2), (float)(location.y - obj.height / 2), (float)(location.x + obj.width / 2), (float)(location.y + obj.height / 2)) && !o.CheckCollision((float)(obj.left - obj.width / 2), (float)(obj.top - obj.height / 2), (float)(obj.left + obj.width / 2), (float)(obj.top + obj.height / 2))) {
						result = BlockType.BLOCKING;
						break;
					}
				}
				else {
					if (o != obj && o.CheckCollision((float)(location.x - obj.width / 2), (float)(location.y - obj.height / 2), (float)(location.x + obj.width / 2), (float)(location.y + obj.height / 2))) {
						result = o.blocking;
					}
				}
			}
		}
		
		return result;
	}
	
	public boolean CollisionExcluding(Object obj, List<Object> excluded, String ExcludedType) {
		boolean result = false;
		
		// Get the cells that contain this unit.
		int StartCol = (int) Math.floor((obj.left - (obj.width / 2)) / CellSize);
		int StartRow = (int) Math.floor((obj.top - (obj.height / 2)) / CellSize);
		int EndCol = (int) Math.ceil((obj.left + obj.width) / CellSize);
		int EndRow = (int) Math.ceil((obj.top + obj.height) / CellSize);
		
		if (StartCol < 0) StartCol = 0;
		if (StartRow < 0) StartRow = 0;

		// Find possible objects for collisions.
		List<Object> objects = new ArrayList<Object>();
		for (int y = StartRow; y < EndRow && y < Rows; y++) {
			for (int x = StartCol; x < EndCol && x < Columns; x++) {
				if (Cells[y] != null && Cells[y][x] != null) {
					for (Object o : Cells[y][x].Objects) {
						if (o.blocking == BlockType.BLOCKING && obj.level == o.level && !objects.contains(o) && !excluded.contains(o) && o.getClass().getName() != ExcludedType)
							objects.add(o);
					}
				}
			}
		}
		
		// Check object for collision.
		for (Object o : objects) {
			if (o.collisionType == CollisionType.Circle && obj.collisionType == CollisionType.Circle) {
				if (o != obj && o.CheckCollisionCircle(obj.left, obj.top, (float) (o.width / 2))) {
					result = true;
					break;
				}
			}
			else {
				if (o != obj && o.CheckCollision(obj.left, obj.top, (float)(obj.left + obj.width), (float)(obj.top + obj.height))) {
					result = true;
					break;
				}
			}
		}
		
		return result;
	}
}
