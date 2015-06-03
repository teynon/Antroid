package com.eynon.antroid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Log;

import com.eynon.antroid.enums.Action;
import com.eynon.antroid.enums.BlockType;
import com.eynon.antroid.enums.BuildableStatus;
import com.eynon.antroid.enums.Task;
import com.eynon.antroid.hud.Deselect;
import com.eynon.antroid.hud.Dig;
import com.eynon.antroid.hud.Panel;
import com.eynon.antroid.hud.TunnelRoom;
import com.eynon.antroid.hud.TunnelSubDown;
import com.eynon.antroid.hud.TunnelSubUp;
import com.eynon.antroid.level.Tunnel;
import com.eynon.antroid.level.Tunnel.TunnelType;
import com.eynon.antroid.level.Tunnel.TunnelUse;
import com.eynon.antroid.level.TunnelBuilder;
import com.eynon.antroid.model.CollisionDetection;
import com.eynon.antroid.model.MathHelpers;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.PointF3D;
import com.eynon.antroid.model.Rectangle;
import com.eynon.antroid.model.Size;
import com.eynon.antroid.objects.Ant;
import com.eynon.antroid.objects.AntHill;
import com.eynon.antroid.objects.Buildable;
import com.eynon.antroid.objects.Object;
import com.eynon.antroid.objects.Boundary;
import com.eynon.antroid.objects.Map;
import com.eynon.antroid.objects.Grid;
import com.eynon.antroid.objects.Pheromone;
import com.eynon.antroid.objects.Queen;
import com.eynon.antroid.objects.Resource;
import com.eynon.antroid.objects.Water;

public class LevelManager implements Serializable {

	public transient Context Context;

	private transient Paint BG = new Paint();
	private transient Paint SelectionBox = new Paint();
	private transient Paint DebugText = null;

	public static transient final boolean DebugMode = true;

	private List<Object> Objects;
	private List<Object> Resources;
	private List<Ant> Selection;
	private List<Pheromone> Pheromones;
	private List<Pheromone> PendingRemoval;
	private List<Panel> Panels = new ArrayList<Panel>();
	private List<Tunnel> SelectableRooms;
	private List<Tunnel> Tunnels;

	private List<Buildable> BuildQueue;

	// Used when the player is selecting a location to link the new room
	private Tunnel TemporaryDigRoom = null;
	private int Level = 0;
	private int Levels = -1;
	public int Team = 0;

	public boolean SelectActionLocation = false;
	public Action ActionPending = Action.NULL;

	private Boundary Boundary;
	private Map Map;
	private Grid Grid;

	private Size _size = new Size(1000, 1000);

	public Rectangle SelectionArea = null;
	public boolean DoSelection = false;
	public Panel panelSelection = new Panel(0, 0);
	public Panel digSubPanel = new Panel(0, 0);

	public long lastFrameTime;
	public long lastFrameRate;

	private PointF _offset = new PointF(0, 0);

	public LevelManager(Context context) {
		if (DebugMode) {
			DebugText = new Paint(Paint.ANTI_ALIAS_FLAG);
			DebugText.setTextSize(60);
			DebugText.setColor(Color.WHITE);
		}

		lastFrameRate = 0;
		lastFrameTime = SystemClock.elapsedRealtime();
		Context = context;
		panelSelection.margin.Bottom = (float) 50;
		Panels.add(panelSelection);
		Panels.add(digSubPanel);
		Deselect dsControl = new Deselect(this, 200, 200);
		Dig digControl = new Dig(this, 200, 200);
		digControl.SetSubPanel(digSubPanel);
		dsControl.margin.Left = 50;
		panelSelection.AddControl(dsControl);
		panelSelection.AddControl(digControl);

		digSubPanel.margin.Bottom = (float) 260;
		digSubPanel.margin.Left = 250;
		TunnelSubUp tunnelUp = new TunnelSubUp(this, 200, 200);
		digSubPanel.AddControl(tunnelUp);
		TunnelSubDown tunnelDown = new TunnelSubDown(this, 200, 200);
		digSubPanel.AddControl(tunnelDown);
		TunnelRoom tunnelRoom = new TunnelRoom(this, 200, 200);
		digSubPanel.AddControl(tunnelRoom);
		com.eynon.antroid.hud.Tunnel tunnelThrough = new com.eynon.antroid.hud.Tunnel(
				this, 200, 200);
		digSubPanel.AddControl(tunnelThrough);

		BG.setColor(Color.BLACK);
		SelectionBox.setColor(Color.argb(90, 0, 0, 200));

		Map = new Map(context, this);
		Grid = new Grid(Map.GetWidth(), Map.GetHeight(), 50);

		Boundary = new Boundary();
		Boundary.width = Map.GetWidth();
		Boundary.height = Map.GetHeight();

		Objects = new ArrayList<Object>();
		Resources = new ArrayList<Object>();
		Pheromones = new ArrayList<Pheromone>();
		PendingRemoval = new ArrayList<Pheromone>();
		Tunnels = new ArrayList<Tunnel>();
		SelectableRooms = new ArrayList<Tunnel>();
		BuildQueue = new ArrayList<Buildable>();

		Selection = new ArrayList<Ant>();

		// Queen queen = new Queen(50, 50, this, 0, 0, 0);
		// AddObject(queen, new PointF(queen.left, queen.top), queen.width,
		// queen.height);

		// Demo code...
		// =====================================================

		// Create tunnels.
		Tunnel t = DemoRandomTunnels(500, 500, new Random().nextInt(20) + 4,
				500, 400, 0);
		Tunnel p = t.AttachedTo.get(0);
		Tunnel dropTunnel = t;
		for (Tunnel tAttached : p.AttachedTo) {
			if (tAttached != t) {
				dropTunnel = tAttached;
			}
		}

		// Create our team of ants.
		DemoDropAnts((int) dropTunnel.Start.x, (int) dropTunnel.Start.y, 1, 0,
				-1);

		Tunnel t2 = DemoRandomTunnels(Map.GetWidth() - 500,
				Map.GetHeight() - 500, new Random().nextInt(4) + 4, 500, 400, 1);

		p = t2.AttachedTo.get(0);
		dropTunnel = t2;

		for (Tunnel tAttached : p.AttachedTo) {
			if (tAttached != t2) {
				dropTunnel = tAttached;
			}
		}

		// DemoDropAnts((int)dropTunnel.Start.x, (int)dropTunnel.Start.y, 10, 1,
		// -1);

		DemoRandomFood();

		// =====================================================

	}

	public void DemoDropAnts(int x, int y, int count, int team, int level) {

		for (int i = 0; i < count; i++) {
			Ant anty = new Ant(x, y, this, 0, team, level);
			AddObject(anty, new PointF(anty.left, anty.top), anty.width,
					anty.height);
		}
	}

	public Tunnel DemoRandomTunnels(int mainHillX, int mainHillY, int rooms,
			int maxRoomDistance, int minRoomSpacing, int team) {
		// Create the initial anthill.
		AntHill main = new AntHill(mainHillX, mainHillY, this, team, 0, true);

		AddObject(main, new PointF(mainHillX, mainHillY), main.width,
				main.height);
		Tunnel t = new Tunnel(this, new PointF3D(mainHillX, mainHillY, -1),
				new PointF3D(0, 0, 0), TunnelType.BasicRoom,
				(float) 200);
		t.level = -1;
		t.Purpose = TunnelUse.Lobby;

		Tunnel mainTunnel = t;

		AddTunnel(t);
		List<Tunnel> tunnels = new ArrayList<Tunnel>();
		tunnels.add(t);
		Random rand = new Random();

		for (int x = 0; x < rooms; x++) {
			int xRandom = 0;
			int yRandom = 0;

			Tunnel temp = null;

			boolean inRange = false;
			PointF3D newTunnel = null;
			while (!inRange) {
				temp = null;

				// Pick a random tunnel to attach to.
				while (temp == null || temp.Type == TunnelType.Path)
					temp = (tunnels.size() > 1) ? tunnels.get(rand
							.nextInt(tunnels.size() - 1)) : tunnels.get(0);

				xRandom = rand.nextInt(maxRoomDistance);
				yRandom = rand.nextInt(maxRoomDistance - xRandom);

				xRandom = (rand.nextBoolean()) ? xRandom *= -1 : xRandom;

				if (xRandom + yRandom < 100)
					yRandom = 100 - xRandom;

				yRandom = (rand.nextBoolean()) ? yRandom *= -1 : yRandom;

				xRandom = (int) (temp.Start.x + xRandom);
				yRandom = (int) (temp.Start.y + yRandom);

				inRange = true;

				newTunnel = new PointF3D(xRandom, yRandom, -1);

				if (!temp.CanAddTunnelLink(newTunnel))
					inRange = false;
				else if (temp.WithinBuildZone(newTunnel) != BuildableStatus.OK)
					inRange = false;
				else if (!ValidBuildLocation(newTunnel, (int) 100))
					inRange = false;
				else if (!CanBuildTunnelLink(temp, newTunnel))
					inRange = false;
				/*
				 * for (Tunnel tnl : Tunnels) { if (tnl.Type ==
				 * Tunnel.TunnelType.Path &&
				 * CollisionDetection.CheckLineIntersection
				 * (tnl.Start.GetPointF(), tnl.End.GetPointF(),
				 * temp.Start.GetPointF(), new PointF(xRandom, yRandom)) &&
				 * !tnl.Start.equals(temp.Start) && !tnl.End.equals(temp.Start)
				 * && !tnl.Start.equals(newTunnel) &&
				 * !tnl.End.equals(newTunnel)) inRange = false; else if
				 * (tnl.Type == Tunnel.TunnelType.Path) { // Get the tunnel
				 * rectangle. float angle =
				 * MathHelpers.GetAngleToPoint(tnl.Start.x, tnl.Start.y,
				 * tnl.End.GetPointF()); PointF p1 =
				 * MathHelpers.PointAtDistance(tnl.Width / 2, angle + 45); if
				 * (CollisionDetection.CheckCollisionRotatedRectangleCircle(new
				 * PointF(tnl.Start.x + p1.x, tnl.Start.y + p1.y), new
				 * PointF(tnl.Start.x - p1.x, tnl.Start.y - p1.y), new
				 * PointF(tnl.End.x + p1.x, tnl.End.y + p1.y), new
				 * PointF(tnl.End.x - p1.x, tnl.End.y - p1.y), new
				 * PointF(xRandom, yRandom), (float)main.width)) { inRange =
				 * false; } }
				 * 
				 * if (tnl.Type == Tunnel.TunnelType.BasicRoom &&
				 * (MathHelpers.Distance(xRandom, yRandom, tnl.Start.x,
				 * tnl.Start.y) <= minRoomSpacing || xRandom - main.width < 0 ||
				 * yRandom - main.width < 0)) inRange = false; }
				 */
			}

			// Place a random room and connect it with the main hill or another
			// random room.
			Tunnel t2 = new Tunnel(this, new PointF3D(xRandom, yRandom, -1),
					new PointF3D(0, 0, 0), TunnelType.BasicRoom,
					(float) 200);
			t.level = -1;
			AddTunnel(t2);
			Tunnel connector = new Tunnel(this, new PointF3D(xRandom, yRandom,
					-1), new PointF3D(temp.Start.x, temp.Start.y, -1),
					TunnelType.Path, (float) 100);
			connector.level = -1;
			AddTunnel(connector);
			tunnels.add(t2);

			t2.AttachTo(connector);
			temp.AttachTo(connector);
		}

		return t;
	}

	public boolean PointOnMap(PointF point) {
		return (point.x > 0 && point.y > 0 && point.x < Map.GetWidth() && point.y < Map
				.GetHeight());
	}

	public boolean CanBuildTunnelLink(Tunnel target, Tunnel room) {
		return CanBuildTunnelLink(target, room.Start);
	}

	public boolean CanBuildTunnelLink(Tunnel target, PointF3D start) {
		for (Tunnel tnl : Tunnels) {
			if (tnl.Type == Tunnel.TunnelType.Path
					&& CollisionDetection.CheckLineIntersection(
							tnl.Start.GetPointF(), tnl.End.GetPointF(),
							start.GetPointF(), target.Start.GetPointF())) {
				if (!tnl.Start.equals(start) && !tnl.End.equals(start)
						&& !tnl.Start.equals(target.Start)
						&& !tnl.End.equals(target.Start))
					return false;
			}
		}

		if (target.CanAddTunnelLink(start))
			return true;

		return false;
	}

	public boolean ValidBuildLocation(PointF3D mapPoint, int radius) {

		if (!PointOnMap(mapPoint.GetPointF()))
			return false;
		else if (!PointOnMap(new PointF(mapPoint.x - radius, mapPoint.y
				- radius)))
			return false;
		else if (!PointOnMap(new PointF(mapPoint.x + radius, mapPoint.y
				+ radius)))
			return false;

		BuildableStatus status = BuildableStatus.NOTINBUILDRANGE;

		// Check that the point is valid.
		for (Tunnel t : Tunnels) {
			BuildableStatus canBuildNearTunnel = t.WithinBuildZone(mapPoint);
			if (canBuildNearTunnel == BuildableStatus.TOOCLOSE) {
				return false;
			} else if (canBuildNearTunnel == BuildableStatus.OK) {
				status = BuildableStatus.OK;
			}
		}

		return (status == BuildableStatus.OK);
	}

	public void DemoRandomFood() {
		Random rand = new Random();
		int qty = rand.nextInt(50) + 50;

		for (int x = 0; x < qty; x++) {
			int xposition = rand.nextInt(Map.GetWidth() - 50) + 50;
			int yposition = rand.nextInt(Map.GetHeight() - 50) + 50;

			Water w = new Water(xposition, yposition, this, 0, 0);
			w.SetQty(rand.nextFloat() * 100);

			AddObject(w, new PointF(xposition, yposition), w.width, w.height);
		}
	}

	public void SetSize(float width, float height) {
		_size.Width = width;
		_size.Height = height;
		Log.i("Dimensions", "Width: " + width + " | Height: " + height);

		// Create HUD
		panelSelection.Top = height - panelSelection.Height
				- panelSelection.margin.Bottom + panelSelection.margin.Top;
		digSubPanel.Top = height - digSubPanel.Height
				- digSubPanel.margin.Bottom + digSubPanel.margin.Top;
		digSubPanel.Left = digSubPanel.margin.Left;
	}

	public void DoLogic() {
		for (Pheromone p : Pheromones) {
			p.DoLogic();
			if (p.strength <= 0)
				PendingRemoval.add(p);
		}

		for (Pheromone p : PendingRemoval) {
			Pheromones.remove(p);
		}
		PendingRemoval.clear();

		for (int x = 0; x < Objects.size(); x++) {
			if (Objects.get(x) != null) {
				Objects.get(x).DoLogic();
			} else {
				Log.i("Null object", x + " is null");
			}
		}
	}

	public void DoDraw(Canvas c) {

		c.drawRect(0, 0, _size.Width, _size.Height, BG);

		PointF screenOffset = new PointF(-_offset.x, -_offset.y);
		if (Level == 0) {
			Map.OnDraw(c);
		} else {
			if (ActionPending == ActionPending.DIGROOM) {
				for (Tunnel t : Tunnels) {
					if (!t.UnderConstruction && t.Type == TunnelType.BasicRoom
							&& t.IsOnScreen(screenOffset, _size))
						t.DoDrawBuildRadius(c, _offset);
				}

				for (Tunnel t : Tunnels) {
					if (t.Type == TunnelType.BasicRoom
							&& t.IsOnScreen(screenOffset, _size))
						t.DoDrawNoBuildRadius(c, _offset);
				}

			} else if (ActionPending == ActionPending.DIGROOMSELECTLINK
					|| ActionPending == ActionPending.TUNNEL
					|| ActionPending == ActionPending.TUNNELSELECTLINK) {
				for (Tunnel t : SelectableRooms) {
					t.DoDrawSelectable(c, _offset);
				}
			}

			for (Tunnel t : Tunnels) {
				if (t.Type == TunnelType.Path
						&& t.IsOnScreen(screenOffset, _size))
					t.DoDraw(c, _offset);
			}

			for (Tunnel t : Tunnels) {
				if (t.Type != TunnelType.Path
						&& t.IsOnScreen(screenOffset, _size))
					t.DoDraw(c, _offset);
			}
		}

		/*
		 * for (Pheromone pheromone : Pheromones) { if
		 * (pheromone.IsOnScreen(screenOffset, _size) && pheromone.level ==
		 * Level) { pheromone.DoDraw(c, _offset); } }
		 */

		for (Object creature : Selection) {
			if (creature.IsOnScreen(screenOffset, _size)
					&& creature.level == Level) {
				creature.DoDrawSelection(c, _offset);
			}
			if (creature.level == Level)
				creature.DoDrawTargetPath(c, _offset);
		}

		for (Object creature : Objects) {
			if (creature.IsOnScreen(screenOffset, _size)
					&& creature.level == Level) {
				creature.DoDraw(c, _offset);
				if (this.DebugMode)
					creature.onDebug(c, _offset);
			}
		}

		DrawSelection(c);

		// Draw boundary.
		Boundary.DoDraw(c, _offset);

		for (Panel panel : Panels) {
			panel.OnDraw(c, new PointF(0, 0), Selection, null);
		}

		if (DebugMode) {
			long frameTime = (SystemClock.elapsedRealtime() - lastFrameTime);
			lastFrameRate = (long) (lastFrameRate * 0.1 + (1000 / frameTime) * 0.9);
			lastFrameTime = SystemClock.elapsedRealtime();

			c.drawText(String.valueOf(lastFrameRate), 20, 60, DebugText);
		}
	}

	public void DrawSelection(Canvas c) {
		if (DoSelection && SelectionArea != null) {
			c.drawRect(SelectionArea.P1.x, SelectionArea.P1.y,
					SelectionArea.P2.x, SelectionArea.P2.y, SelectionBox);
		}
	}

	public Pheromone FindPheromone(PointF location, float radius) {
		return Grid.Collision(location, radius);
	}

	public List<Object> FindObjects(Object obj, PointF location, float radius) {
		return Grid.CollisionObjects(obj, location, radius);
	}

	public BlockType CheckCollision(Object obj, PointF location, double width,
			double height) {
		BlockType result = (!Boundary.CheckCollision(
				(float) (location.x - (obj.width / 2)),
				(float) (location.y - (obj.height / 2)),
				(float) (location.x + (obj.width / 2)),
				(float) (location.y + (obj.height / 2)), (float) obj.width,
				(float) obj.height)) ? BlockType.BLOCKING : BlockType.NONE;

		if (result != BlockType.BLOCKING && obj.level < 0) {
			result = BlockType.BLOCKING;
			// Check collision with tunnel
			for (Tunnel t : Tunnels) {
				if (!t.UnderConstruction
						&& t.CheckCollision(
								(float) (location.x - (obj.width / 2)),
								(float) (location.y - (obj.height / 2)),
								(float) (location.x + (obj.width / 2)),
								(float) (location.y + (obj.height / 2)))) {
					result = BlockType.NONE;
					break;
				}
			}
		}

		if (result != BlockType.BLOCKING) {
			result = Grid.Collision(obj, location, width, height);
		}

		return result;
	}

	public Tunnel GetTunnelCollision(PointF3D location, double width,
			double height) {
		for (Tunnel t : Tunnels) {
			if (!t.UnderConstruction
					&& t.level == location.z
					&& t.CheckCollision((float) (location.x - (width / 2)),
							(float) (location.y - (height / 2)),
							(float) (location.x + (width / 2)),
							(float) (location.y + (height / 2)))) {
				return t;
			}
		}

		return null;
	}

	public boolean CheckCollisionExcluding(Object obj, List<Object> Exclude,
			String excludeClass) {
		boolean result = !Boundary.CheckCollision(
				(float) (obj.left - (obj.width / 2)),
				(float) (obj.top - (obj.height / 2)),
				(float) (obj.left + (obj.width / 2)),
				(float) (obj.top + (obj.height / 2)), (float) obj.width,
				(float) obj.height);

		if (!result && obj.level < 0) {
			result = true;
			// Check collision with tunnel
			if (result) {
				for (Tunnel t : Tunnels) {
					if (t.CheckCollision((float) (obj.left - (obj.width / 2)),
							(float) (obj.top - (obj.height / 2)),
							(float) (obj.left + (obj.width / 2)),
							(float) (obj.top + (obj.height / 2)))) {
						result = false;
						break;
					}
				}
			}
		}

		if (!result && Grid.CollisionExcluding(obj, Exclude, excludeClass)) {
			result = true;
		}

		return result;
	}

	public List<Tunnel> GetTunnels(Object obj) {
		List<Tunnel> tunnels = new ArrayList<Tunnel>();

		boolean result = !Boundary.CheckCollision(obj.left, obj.top,
				(float) (obj.left + obj.width), (float) (obj.top + obj.height),
				(float) obj.width, (float) obj.height);

		if (!result && obj.level < 0) {
			result = true;
			// Check collision with tunnel
			for (Tunnel t : Tunnels) {
				if (t.CheckCollision((float) (obj.left - (obj.width / 2)),
						(float) (obj.top - (obj.height / 2)),
						(float) (obj.left + (obj.width / 2)),
						(float) (obj.top + (obj.height / 2)))) {
					tunnels.add(t);
				}
			}
		}

		return tunnels;
	}

	public List<Tunnel> GetTunnels() {
		return Tunnels;
	}

	public void AddToBuildQueue(Buildable obj) {
		if (!BuildQueue.contains(obj))
			this.BuildQueue.add(0, obj);
	}

	public List<Buildable> GetBuildQueue(int team) {
		List<Buildable> awaitingBuild = new ArrayList<Buildable>();

		for (Buildable b : BuildQueue) {
			if (b.getTeam() == team) {
				awaitingBuild.add(b);
			}
		}

		return awaitingBuild;
	}

	public Buildable GetNextInBuildQueue(int team) {
		for (Buildable b : BuildQueue) {
			if (b.getTeam() == team)
				return b;
		}

		return null;
	}

	public void RemoveFromBuildQueue(Buildable obj) {
		if (BuildQueue.contains(obj))
			this.BuildQueue.remove(obj);
	}

	public void AddObject(Object obj, PointF location, double width,
			double height) {
		Objects.add(obj);
		Grid.RegisterObject(obj, location, width, height);
	}

	public void AddTunnel(Tunnel t) {
		if (!Tunnels.contains(t))
			Tunnels.add(t);
	}

	public void RemoveTunnel(Tunnel t) {
		if (Tunnels.contains(t)) {
			Tunnels.remove(t);
		}
	}

	public void RemoveObject(Object obj) {
		if (Objects.contains(obj))
			Objects.remove(obj);
		if (Resources.contains(obj))
			Resources.remove(obj);
		Grid.DeregisterObject(obj);
	}

	public void RegisterObject(Object obj, PointF location, double width,
			double height) {
		Grid.RegisterObject(obj, location, width, height);
	}

	public Pheromone RegisterPheromone(float x, float y, float strength,
			int level, int team) {
		Pheromone p = new Pheromone(x, y, strength, level, team);

		p = Grid.RegisterPheremone(p);

		if (!Pheromones.contains(p)) {
			Pheromones.add(p);

			if (Pheromones.size() > 100) {
				// Increase the radius and start merging.
				Pheromone.MergeRadius = (Pheromones.size() > 50) ? 50
						: Pheromones.size();
			}
		}

		return p;
	}

	public void RemoveSelection(Object obj) {
		if (Selection.contains(obj)) {
			Selection.remove(obj);
		}
	}

	public void DeselectAll() {
		if (SelectActionLocation)
			StopAction();

		Selection.clear();
	}

	public void StopAction() {
		switch (ActionPending) {
		case DIGROOMSELECTLINK:
			if (TemporaryDigRoom != null) {
				RemoveTunnel(TemporaryDigRoom);
				TemporaryDigRoom = null;
			}
			break;
		case TUNNELSELECTLINK:
			TemporaryDigRoom = null;
			break;
		}
		SelectActionLocation = false;
		ActionPending = Action.NULL;
	}

	public void NotifyDrag(PointF drag) {
		_offset.x += drag.x;
		_offset.y += drag.y;
	}

	public Tunnel GetTunnelRoomAtPoint(PointF point, int level) {
		for (Tunnel t : Tunnels) {
			if (t.level == level
					&& !t.UnderConstruction
					&& t.Type == TunnelType.BasicRoom
					&& MathHelpers.Distance(t.Start.x, t.Start.y, point.x,
							point.y) < t.Width) {
				return t;
			}
		}

		return null;
	}

	// A single tap occurred. What do we do now?
	public void NotifySingleTap(float x, float y) {

		for (Panel p : Panels) {
			if (p.Active && p.onClick(new PointF(x, y))) {
				return;
			}
		}

		PointF3D mapPoint = new PointF3D(x - _offset.x, y - _offset.y, Level);
		if (SelectActionLocation) {
			switch (ActionPending) {
			case DIGROOM:
				BuildableStatus status = BuildableStatus.NOTINBUILDRANGE;

				if (ValidBuildLocation(mapPoint, 100)) {
					// Place a temporary room.
					TemporaryDigRoom = new Tunnel(this, mapPoint, mapPoint,
							TunnelType.BasicRoom, 200);
					TemporaryDigRoom.Temporary();
					AddTunnel(TemporaryDigRoom);

					// Have user choose connecting tunnel. (Tunnel must be
					// connected to build it.
					ActionPending = Action.DIGROOMSELECTLINK;

					// Determine selectable rooms
					SelectableRooms.clear();
					for (Tunnel t : Tunnels) {
						if (t != TemporaryDigRoom
								&& t.Type == TunnelType.BasicRoom
								&& !t.UnderConstruction) {
							// Check for path intersection.
							if (CanBuildTunnelLink(t, TemporaryDigRoom)
									&& t.WithinBuildZone(TemporaryDigRoom.Start) == BuildableStatus.OK) {
								SelectableRooms.add(t);
							}
						}
					}
				} else {
					// TODO: Display Error Message
				}
				break;
			case DIGROOMSELECTLINK:
				// Make the temporary room permanent and add a linking tunnel.
				// Then add building targets to the current selection.
				Tunnel targetTunnel = GetTunnelRoomAtPoint(
						mapPoint.GetPointF(), Level);

				if (targetTunnel != null
						&& SelectableRooms.contains(targetTunnel)) {
					TemporaryDigRoom.BeginConstruction();

					Tunnel connector = new Tunnel(this, TemporaryDigRoom.Start,
							targetTunnel.Start, TunnelType.Path, 100);
					connector.BeginConstruction();
					AddTunnel(connector);

					// Connect the tunnels.
					targetTunnel.AttachTo(connector);
					TemporaryDigRoom.AttachTo(connector);
					TemporaryDigRoom = null;

					// Have the active ants target the tunnel build point at the
					// end.
					if (connector.BuildPoints.size() > 0) {
						TunnelBuilder targetPoint = connector.BuildPoints
								.get(0);
						for (Ant creature : Selection) {
							creature.TriggerAction(ActionPending, new PointF3D(
									targetPoint.left, targetPoint.top,
									targetPoint.level));
							creature.SetDirectTarget(targetPoint, Task.Build);
						}
					}

					SelectActionLocation = false;
					ActionPending = Action.NULL;
				}

				break;
			case TUNNEL:
				// Get the target
				TemporaryDigRoom = GetTunnelRoomAtPoint(mapPoint.GetPointF(),
						Level);

				if (TemporaryDigRoom != null) {
					// Determine selectable rooms
					SelectableRooms.clear();
					for (Tunnel t : Tunnels) {
						if (t != TemporaryDigRoom
								&& t.Type == TunnelType.BasicRoom
								&& !t.UnderConstruction) {
							// Check for path intersection.
							if (CanBuildTunnelLink(t, TemporaryDigRoom)
									&& t.WithinBuildZone(TemporaryDigRoom.Start) == BuildableStatus.OK) {
								SelectableRooms.add(t);
							}
						}
					}

					ActionPending = Action.TUNNELSELECTLINK;
				}

				break;
			case TUNNELSELECTLINK:
				// Make the temporary room permanent and add a linking tunnel.
				// Then add building targets to the current selection.
				Tunnel tunnelToRoom = GetTunnelRoomAtPoint(
						mapPoint.GetPointF(), Level);

				if (tunnelToRoom != null
						&& SelectableRooms.contains(tunnelToRoom)) {

					Tunnel connector = new Tunnel(this, TemporaryDigRoom.Start,
							tunnelToRoom.Start, TunnelType.Path, 100);
					connector.BeginConstruction();
					AddTunnel(connector);

					// Connect the tunnels.
					tunnelToRoom.AttachTo(connector);
					TemporaryDigRoom.AttachTo(connector);
					TemporaryDigRoom = null;

					// Have the active ants target the tunnel build point at the
					// end.
					if (connector.BuildPoints.size() > 0) {
						TunnelBuilder targetPoint = connector.BuildPoints
								.get(connector.BuildPoints.size() - 1);
						for (Ant creature : Selection) {
							creature.TriggerAction(ActionPending, new PointF3D(
									targetPoint.left, targetPoint.top,
									targetPoint.level));
							creature.SetDirectTarget(targetPoint, Task.Build);
						}
					}

					SelectActionLocation = false;
					ActionPending = Action.NULL;
				}

				break;
			case DIGDOWN:
				AntHill hill = new AntHill(mapPoint.x, mapPoint.y, this, 0,
						Level, true);
				hill.BeginConstruction();
				AddObject(hill, mapPoint.GetPointF(), hill.width, hill.height);
				// Trigger action on selection.
				for (Object creature : Selection) {
					creature.TriggerAction(ActionPending, mapPoint);
					creature.SetTarget(hill, Task.Build);
				}

				SelectActionLocation = false;
				ActionPending = Action.NULL;
				break;
			}
		} else {
			// Search objects for a possible unit selection.
			for (Object creature : Objects) {
				if (creature instanceof Buildable) {
					if (((Buildable) creature).UnderConstruction
							&& creature.HitTest(mapPoint.GetPointF())) {
						for (Ant o : Selection) {
							o.SetDirectTarget((Buildable) creature, Task.Build);
							return;
						}
					}
				}

				if (creature.targetable && creature.level == Level
						&& creature.HitTest(mapPoint.GetPointF())
						&& Selection.size() > 0) {
					if (creature instanceof Resource) {
						for (Ant o : Selection)
							o.SetDirectTarget(creature, Task.Gather);
					} else {
						for (Ant o : Selection) {
							o.SetDirectTarget(creature, Task.Navigate);
						}
					}
					return;
				} else if (creature.selectable && creature.level == Level) {
					if (creature.HitTest(mapPoint.GetPointF())) {
						if (creature.getTeam() == Team) {
							if (!Selection.contains(creature)
									&& creature instanceof Ant) {
								// Select creature.
								Selection.add((Ant) creature);
								panelSelection.Enable();
								return;
							} else {
								// Unselect creature.For t
								Selection.remove(creature);
								return;
							}
						} else {
							// Attack!
							for (Object o : Selection) {
								if (o.level == creature.level) {
									((Ant) o).SetDirectTarget(creature,
											Task.Attack);
								}
							}
							return;
						}
					}
				}
			}

			// If creatures are selected, give creature that destination.
			if (Selection.size() > 0) {
				for (Object creature : Selection) {
					creature.RemoveTargetObject();
					((Ant) creature).SetDirectTarget(mapPoint.x, mapPoint.y,
							Level, Task.Navigate);
				}
			}
		}
	}

	public void NotifyDoubleTap(float x, float y) {
		Level--;
		if (Level < Levels)
			Level = 0;

		if (digSubPanel.Active)
			digSubPanel.Enable();
	}

	public void NotifySelectArea(PointF p1, PointF p2) {
		SelectionArea = new Rectangle();
		SelectionArea.P1.x = (p1.x < p2.x) ? p1.x : p2.x;
		SelectionArea.P1.y = (p1.y < p2.y) ? p1.y : p2.y;
		SelectionArea.P2.x = (p1.x < p2.x) ? p2.x : p1.x;
		SelectionArea.P2.y = (p1.y < p2.y) ? p2.y : p1.y;
		DoSelection = true;
	}

	public void NotifySelectAreaRelease() {
		DoSelection = false;
	}

	public void NotifyDoSelect() {
		DoSelection = false;
		PointF screenOffset = new PointF(-_offset.x, -_offset.y);
		if (SelectionArea != null) {
			// Search for objects in selection.
			for (Object creature : Objects) {
				if (creature.getTeam() == Team && creature.selectable
						&& creature.level == Level
						&& creature.IsOnScreen(screenOffset, _size)) {
					if (creature
							.CheckCollision(SelectionArea.P1.x - _offset.x,
									SelectionArea.P1.y - _offset.y,
									SelectionArea.P2.x - _offset.x,
									SelectionArea.P2.y - _offset.y)) {
						// Select the creature!
						if (!Selection.contains(creature)
								&& creature instanceof Ant) {
							// Select creature.
							Selection.add((Ant) creature);
							panelSelection.Enable();
						}
					}
				}
			}
		}

	}

	public void TriggerActionForSelection(Action action) {

		if (SelectActionLocation)
			StopAction();
		else {
			switch (action) {
			case TUNNEL:
				// Determine selectable rooms
				SelectableRooms.clear();
				for (Tunnel t : Tunnels) {
					if (t != TemporaryDigRoom && t.Type == TunnelType.BasicRoom
							&& !t.UnderConstruction) {
						// Check for path intersection.
						SelectableRooms.add(t);
					}
				}
			case DIGDOWN:
			case DIGROOM:
				SelectActionLocation = true;
				ActionPending = action;
				break;
			}
		}
	}

	public int get_selected() {
		return Selection.size();
	}

	public int get_level() {
		return Level;
	}

	public PointF GetOffset() {
		return _offset;
	}

	public Size GetScreenSize() {
		return _size;
	}
}
