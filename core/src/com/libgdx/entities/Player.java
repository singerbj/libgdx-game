package com.libgdx.entities;

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

	public final Vector2 position = new Vector2();
	public final Vector2 leftPosition = new Vector2();
	public final Vector2 rightPosition = new Vector2();
	public final Vector2 velocity = new Vector2();
	public State state = State.Walking;
	public float stateTime = 0;
	public boolean facesRight = true;
	public boolean grounded = false;
	public boolean onLadder = false;
	
	public Player (float width, float height) {
		Player.WIDTH = width;
		Player.HEIGHT = height;
	}
	
	public Array<Vector2> getPositions () {
		Array<Vector2> positions = new Array<Vector2>();
		positions.add(position);
		positions.add(leftPosition);		
		positions.add(rightPosition);
		return positions;
	}
}
