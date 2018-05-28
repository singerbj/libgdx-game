package com.libgdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Collision {
	Vector2 collisionPoint;
	Rectangle collideableObject;
	
	public Collision (Vector2 collisionPoint, Rectangle collideableObject) {
		this.collisionPoint = collisionPoint;
		this.collideableObject = collideableObject;
	}

	public Vector2 getCollisionPoint() {
		return collisionPoint;
	}

	public void setCollisionPoint(Vector2 collisionPoint) {
		this.collisionPoint = collisionPoint;
	}

	public Rectangle getCollideableObject() {
		return collideableObject;
	}

	public void setCollideableObject(Rectangle collideableObject) {
		this.collideableObject = collideableObject;
	}
}
