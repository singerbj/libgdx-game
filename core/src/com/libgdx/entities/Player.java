package com.libgdx.entities;

import java.util.UUID;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.libgdx.game.Collidable;
import com.libgdx.helpers.PlayerState;

public class Player {
	/** The player character, has state and state time, */

	public static float WIDTH;
	public static float HEIGHT;
	public static float MAX_VELOCITY = 5f;
	public static float JUMP_VELOCITY = 20;
	public static float DAMPING = 0.87f;
	public static float MAX_HEALTH = 1000f;

	public String id;
	public final Vector2 position = new Vector2();
	public final Vector2 leftPosition = new Vector2();
	public final Vector2 rightPosition = new Vector2();
	public final Vector2 velocity = new Vector2();
	public PlayerState state = PlayerState.Walking;
	public float stateTime = 0;
	public boolean facesRight = true;
	public boolean grounded = false;
	public boolean onLadder = false;
	public Gun gun = new Gun();
	public float lookAngle = 0;
	
	public float health = 1000;
	
	public Player() {
		System.out.println("-=1=-Player created");
		this.id = UUID.randomUUID().toString();
		Player.WIDTH = 1f; // 1 / 16f; // * regions[0].getRegionWidth();
		Player.HEIGHT = 2f; // 1 / 16f; // * regions[0].getRegionHeight();
	}
	
	public Player(Player newPlayer) {
		this.id = newPlayer.id;
		this.position.x = newPlayer.position.x;
		this.position.y = newPlayer.position.y;
		this.leftPosition.x = newPlayer.leftPosition.x;
		this.leftPosition.y = newPlayer.leftPosition.y;
		this.rightPosition.x = newPlayer.rightPosition.x;
		this.rightPosition.y = newPlayer.rightPosition.y;
		this.velocity.x = newPlayer.velocity.x;
		this.velocity.y = newPlayer.velocity.y;
		this.state = newPlayer.state;
		this.stateTime = newPlayer.stateTime;
		this.facesRight = newPlayer.facesRight;
		this.grounded = newPlayer.grounded;
		this.onLadder = newPlayer.onLadder;
		this.gun = newPlayer.gun;
		this.lookAngle = newPlayer.lookAngle;
		this.health = newPlayer.health;
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
	
//	public Array<Rectangle> getRects(){
//		Array<Rectangle> rects = new Array<Rectangle>();
//		rects.add(new Rectangle(this.position.x, this.position.y, Player.WIDTH, Player.HEIGHT));
//		rects.add(new Rectangle(this.leftPosition.x, this.position.y, Player.WIDTH, Player.HEIGHT));
//		rects.add(new Rectangle(this.rightPosition.x, this.position.y, Player.WIDTH, Player.HEIGHT));
//		return rects;
//	}

	public Array<Collidable> getCollidables() {
		Array<Collidable> collidables = new Array<Collidable>();
		collidables.add(new Collidable(new Rectangle(this.position.x, this.position.y, Player.WIDTH, Player.HEIGHT), this));
		collidables.add(new Collidable(new Rectangle(this.leftPosition.x, this.position.y, Player.WIDTH, Player.HEIGHT), this));
		collidables.add(new Collidable(new Rectangle(this.rightPosition.x, this.position.y, Player.WIDTH, Player.HEIGHT), this));
		return collidables;	
	}

	public void takeDamage(long damage) {
		if (damage < this.health ) {
			this.health = this.health - damage;
		} else {
			this.health = 0;
		}
	}
}
