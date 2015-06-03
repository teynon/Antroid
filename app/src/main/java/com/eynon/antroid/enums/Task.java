package com.eynon.antroid.enums;

public enum Task {
	Navigate,
	NavigateTunnel, // Move through a tunnel a bit smarter using references.
	Forage, // Follow trails (default task)
	Gather, // Gather resource
	Retrieve, // Retrieve object
	Return, // Return home
	Attack,
	Defend,
	Follow,
	Scout, // Search for food - don't follow trails under or over a range.
	FollowPath, // Follow a waypoint / pre-planned path
	Idle,
	LayEggs,
	Build,
	BuildRoom,
	ScanForPath
}
