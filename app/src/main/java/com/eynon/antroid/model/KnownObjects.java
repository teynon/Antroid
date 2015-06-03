package com.eynon.antroid.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.eynon.antroid.objects.AntHill;
import com.eynon.antroid.objects.AntHole;

public class KnownObjects implements Serializable {
	public java.util.Map<Integer, List<AntHill>> KnownHills = new HashMap<Integer, List<AntHill>>();
	public java.util.Map<Integer, List<AntHole>> KnownHoles = new HashMap<Integer, List<AntHole>>();
	
	public KnownObjects() { }
	
	public void addKnownHole(AntHole hole) {
		if (!KnownHoles.containsKey(hole.level)) {
			// Add this one.
			List<AntHole> holes = new ArrayList<AntHole>();
			holes.add(hole);
			KnownHoles.put(hole.level, holes);
		}
		else {
			List<AntHole> holes = KnownHoles.get(hole.level);
			if (!holes.contains(hole)) {
				holes.add(hole);
			}
		}
	}
	
	public List<AntHole> getHolesAtLevel(int level) {
		if (KnownHoles.containsKey(level))
			return KnownHoles.get(level);
		else
			return new ArrayList<AntHole>();
	}
	
	public AntHole getNearestHole(int level, PointF point) {
		AntHole result = null;
		
		if (KnownHoles.containsKey(level)) {
			List<AntHole> holes = KnownHoles.get(level);
			
			for (AntHole h : holes) {
				if (result == null || MathHelpers.Distance(result.left, result.top, point.x, point.y) > MathHelpers.Distance(h.left, h.top, point.x, point.y))
					result = h;
			}
		}
		
		return result;
	}
	
	public void addKnownHill(AntHill hole) {
		if (!KnownHills.containsKey(hole.level)) {
			// Add this one.
			List<AntHill> holes = new ArrayList<AntHill>();
			holes.add(hole);
			KnownHills.put(hole.level, holes);
		}
		else {
			List<AntHill> holes = KnownHills.get(hole.level);
			if (!holes.contains(hole)) {
				holes.add(hole);
			}
		}
	}
	
	public List<AntHill> getHillsAtLevel(int level) {
		if (KnownHills.containsKey(level))
			return KnownHills.get(level);
		else
			return new ArrayList<AntHill>();
	}
	
	public AntHill getNearestHill(int level, PointF point) {
		AntHill result = null;
		
		if (KnownHills.containsKey(level)) {
			List<AntHill> holes = KnownHills.get(level);
			
			for (AntHill h : holes) {
				if (result == null || MathHelpers.Distance(result.left, result.top, point.x, point.y) > MathHelpers.Distance(h.left, h.top, point.x, point.y))
					result = h;
			}
		}
		
		return result;
	}
}