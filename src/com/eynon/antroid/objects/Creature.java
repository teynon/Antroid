package com.eynon.antroid.objects;

import java.io.Serializable;


public abstract class Creature extends Object implements Serializable {

	public int strength = 100;
	public int harvestStrength = 3;
	public int carryCapacity = 10;
	public int carryWeight = 0;
	public float consumptionRate = (float) 0.02;
	public float hungerCapacity = 1000;
	public float foodRemaining = hungerCapacity;
	public float minHungerDamage = (float) 0.1;
	public float maxHungerDamage = 1;
	
	public Creature() {
	}
	
}
