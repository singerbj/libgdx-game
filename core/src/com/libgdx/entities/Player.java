package com.libgdx.entities;

import java.util.UUID;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Player {
	/** The player character, has state and state time, */

	public static float WIDTH;
	public static float HEIGHT;
	public static float MAX_VELOCITY = 5f;
	public static float JUMP_VELOCITY = 20;
	public static float DAMPING = 0.87f;

	public enum State {
		Standing, Walking, Jumping
	}

	public String id;
	public final Vector2 position = new Vector2();
	public final Vector2 leftPosition = new Vector2();
	public final Vector2 rightPosition = new Vector2();
	public final Vector2 velocity = new Vector2();
	public State state = State.Walking;
	public float stateTime = 0;
	public boolean facesRight = true;
	public boolean grounded = false;
	public boolean onLadder = false;
	public Gun gun = new Gun();
	public float lookAngle = 0;
	
	public Player() {
		System.out.println("-=-Player created");
		this.id = UUID.randomUUID().toString();
		Player.WIDTH = 1f; // 1 / 16f; // * regions[0].getRegionWidth();
		Player.HEIGHT = 2f; // 1 / 16f; // * regions[0].getRegionHeight();
	}
	
	public Player(Vector2 position) {
		System.out.println("Player created");
		this.id = UUID.randomUUID().toString();
		this.position.x = position.x;
		this.position.y = position.y;
		Player.WIDTH = 1f; // 1 / 16f; // * regions[0].getRegionWidth();
		Player.HEIGHT = 2f; // 1 / 16f; // * regions[0].getRegionHeight();
	}
	
	public Player(String id, Vector2 position) {
		System.out.println("Player created");
		this.id = id;
		this.position.x = position.x;
		this.position.y = position.y;
		Player.WIDTH = 1f; // 1 / 16f; // * regions[0].getRegionWidth();
		Player.HEIGHT = 2f; // 1 / 16f; // * regions[0].getRegionHeight();
	}

	public Array<Vector2> getPositions() {
		Array<Vector2> positions = new Array<Vector2>();
		positions.add(position);
		positions.add(leftPosition);
		positions.add(rightPosition);
		return positions;
	}

	public void updateLeftRightPositions(float mapWidth) {
		this.rightPosition.x = this.position.x + mapWidth;
		this.leftPosition.x = this.position.x - mapWidth;
		this.rightPosition.y = this.position.y;
		this.leftPosition.y = this.position.y;
	}
}
