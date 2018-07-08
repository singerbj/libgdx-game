package com.libgdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Shot {
	private Vector2 source;
	private Vector2 dest;
	private Rectangle collideableObject;
	private float distance;
	
	public Shot (Vector2 source, Vector2 dest, Rectangle collideableObject) {
		this.source = source;
		this.dest = dest;
		this.collideableObject = collideableObject;
		this.distance = Vector2.dst(this.source.x, this.source.y, this.dest.x, this.dest.y);
	}
	
	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}
	
	public Vector2 getSource() {
		return source;
	}

	public void setSource(Vector2 source) {
		this.source = source;
	}
	
	public Vector2 getDest() {
		return dest;
	}

	public void setDest(Vector2 dest) {
		this.dest = dest;
	}

	public Rectangle getCollideableObject() {
		return collideableObject;
	}

	public void setCollideableObject(Rectangle collideableObject) {
		this.collideableObject = collideableObject;
	}
}
