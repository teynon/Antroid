package com.eynon.antroid.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cell implements Serializable {
	public List<Object> Objects;
	public List<Pheromone> Pheromones;
	public int Row = 0;
	public int Col = 0;
	
	public Cell(int row, int col) {
		Objects = new ArrayList<Object>();
		Pheromones = new ArrayList<Pheromone>();
		Row = row;
		Col = col;
	}
	
	public void Register(Pheromone p) {
		if (!Pheromones.contains(p)) {
			Pheromones.add(p);
			
			// Add back link
			p.cells.add(this);
		}
	}
	
	public void Deregister(Pheromone p) {
		if (Pheromones.contains(p)) {
			Pheromones.remove(p);
		}
	}
	
	public void Register(Object obj) {
		if (!Objects.contains(obj)) {
			Objects.add(obj);
			
			// Add back link
			obj.cells.add(this);
		}
	}
	
	public void Deregister(Object obj) {
		if (Objects.contains(obj)) {
			Objects.remove(obj);
		}
		
		// Remove back link
		if (obj.cells.contains(this)) {
			obj.cells.remove(this);
		}
	}
}
