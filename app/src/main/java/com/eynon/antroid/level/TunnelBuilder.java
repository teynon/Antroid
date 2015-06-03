package com.eynon.antroid.level;

import android.graphics.Canvas;

import com.eynon.antroid.LevelManager;
import com.eynon.antroid.enums.BlockType;
import com.eynon.antroid.level.Tunnel.TunnelType;
import com.eynon.antroid.model.MathHelpers;
import com.eynon.antroid.model.PointF;
import com.eynon.antroid.model.PointF3D;
import com.eynon.antroid.objects.Buildable;
import com.eynon.antroid.objects.Object;

public class TunnelBuilder extends Buildable {

	public Tunnel LinkedTunnel;
	public TunnelBuilder[] Neighbors = new TunnelBuilder[2];
	
	public TunnelBuilder(LevelManager lvl, float left, float top, Tunnel linkedTunnel, int level, int buildRadius) {
		this._level = lvl;
		this.left = left;
		this.top = top;
		LinkedTunnel = linkedTunnel;
		this.width = 0;
		this.height = 0;
		interactionRadius = buildRadius;
		this.level = level;
		this.blocking = BlockType.NONE;
		this.BeginConstruction();
		
		_level.AddToBuildQueue(this);
	}
	
	@Override
	public void CompleteConstruction() {
		_level.RemoveFromBuildQueue(this);
		UnderConstruction = false;
		if (LinkedTunnel.Type == TunnelType.Path) {
			// Fill the gap if two sides.
			if (Neighbors[0] != null && !Neighbors[0].LinkedTunnel.UnderConstruction && Neighbors[1] != null && !Neighbors[1].LinkedTunnel.UnderConstruction) {
				// If we are linking a gap, the tunnel is finished. Destroy the two tunnels and set the current tunnel to finished.
				this.LinkedTunnel.CompleteConstruction();
				Neighbors[0].LinkedTunnel.DetachAll();
				Neighbors[1].LinkedTunnel.DetachAll();
				_level.RemoveTunnel(Neighbors[0].LinkedTunnel);
				_level.RemoveTunnel(Neighbors[1].LinkedTunnel);
				Destroy(this);
			}
			else {
				// Check if a neighbor is linked to a finished tunnel. If so, extend that tunnel.
				if (Neighbors[0] != null && !Neighbors[0].LinkedTunnel.UnderConstruction) {
					ExtendTunnel(0, 1);
				}
				else if (Neighbors[1] != null && !Neighbors[1].LinkedTunnel.UnderConstruction) {
					ExtendTunnel(1, 0);
				}
				else if (Neighbors[0] != null && Neighbors[1] != null) {
					// Neighbors exist, but none have completed tunnels. Build a link between the two neighbors.
					PointF3D start = new PointF3D(Neighbors[0].left, Neighbors[0].top, Neighbors[0].level);
					PointF3D stop = new PointF3D(Neighbors[1].left, Neighbors[1].top, Neighbors[1].level);
					
					Tunnel connector = new Tunnel(_level, start, stop, TunnelType.Path, 100);
					this.LinkedTunnel = connector;
					
					_level.AddTunnel(connector);
				}
				else {
					// No tunnels exist yet. Create one and link it to this one. Extend to the next neighbor.
					Tunnel temporaryConnector = null;
					
					Tunnel LinkTo = null;
					// Figure out which tunnel to link to.
					for (Tunnel t : this.LinkedTunnel.AttachedTo)
					{
						if (t != null) {
							if (LinkTo == null || MathHelpers.Distance(this.left, this.top, t.left, t.top) < MathHelpers.Distance(LinkTo.left, LinkTo.top, this.left, this.top))
							{
								LinkTo = t;
							}
						}
					}
					
					if (Neighbors[0] != null) 
						temporaryConnector = new Tunnel(_level, LinkTo.Start, new PointF3D(Neighbors[0].left, Neighbors[0].top, Neighbors[0].level), TunnelType.Path, 100);
					else if (Neighbors[1] != null)
						temporaryConnector = new Tunnel(_level, LinkTo.Start, new PointF3D(Neighbors[1].left, Neighbors[1].top, Neighbors[1].level), TunnelType.Path, 100);
					
					if (temporaryConnector != null) {
						
						this.LinkedTunnel = temporaryConnector;
						temporaryConnector.AttachTo(LinkTo);
						_level.AddTunnel(temporaryConnector);
					}
				}
			}
		}
		else {
			// Linked to a room.
			this.LinkedTunnel.CompleteConstruction();
			Destroy(this);
		}
	}

	public void ExtendTunnel(int neighbor1, int neighbor2) {
		// If the other neighbor isn't null, expand the tunnel from 0 to the other neighbors points.
		if (Neighbors[neighbor2] != null) {
			if (Neighbors[neighbor1].LinkedTunnel.End.x == this.left && Neighbors[neighbor1].LinkedTunnel.End.y == this.top) {
				Neighbors[neighbor1].LinkedTunnel.End.set(Neighbors[neighbor2].left, Neighbors[neighbor2].top, Neighbors[neighbor2].level);
			}
			else {
				Neighbors[neighbor1].LinkedTunnel.Start.set(Neighbors[neighbor2].left, Neighbors[neighbor2].top, Neighbors[neighbor2].level);
			}
			this.LinkedTunnel = Neighbors[neighbor1].LinkedTunnel;
		}
		else {
			// End of the line. Activate the normal tunnel and destroy the neighbor tunnel builder nodes.
			this.LinkedTunnel.CompleteConstruction();
			Neighbors[neighbor1].LinkedTunnel.DetachAll();
			_level.RemoveTunnel(Neighbors[neighbor1].LinkedTunnel);
			Destroy(this);
		}
	}
	
	public void Destroy(TunnelBuilder parent) {
		for (int x = 0; x < 2; x++) {
			if (Neighbors[x] != null && Neighbors[x] != parent) {
				// Unregister and destroy the reference to the node.
				Neighbors[x].Destroy(this);
				Neighbors[x] = null;
			}
		}
		
		_level.RemoveObject(this);
	}
	
	public void addNeighbor(TunnelBuilder neighbor) {
		if (Neighbors[0] == null) Neighbors[0] = neighbor;
		else Neighbors[1] = neighbor;
	}
	
	@Override
	public Object factory() {
		return new TunnelBuilder(_level, left, top, LinkedTunnel, level, (int)interactionRadius);
	}

	@Override
	public void DoLogic() {	}

	@Override
	public void DoDraw(Canvas c, PointF offset) {
		// Invisible building object.
		if (_level.DebugMode) {
			//c.drawCircle(left + offset.x, top + offset.y, (float)width, debugPaint);
		}
	}
	
}
