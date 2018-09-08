package com.libgdx.entities;

import java.util.UUID;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.libgdx.game.Collidable;
import com.libgdx.helpers.PlayerState;

import network_entities.NetworkPlayer;

public class Player {
	/** The player character, has state and state time, */

	public static float WIDTH = 1f;  // 1 / 16f; // * regions[0].getRegionWidth()
	public static float HEIGHT = 2f; // 1 / 16f; // * regions[0].getRegionHeight();
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
	}
	
	public Player(NetworkPlayer networkPlayer) {
		System.out.println("-=2=-Player created");
		this.id = networkPlayer.id;
		this.position.x = networkPlayer.positionX;
		this.position.y = networkPlayer.positionY;
		this.velocity.x = networkPlayer.velocityX;
		this.velocity.y = networkPlayer.velocityY;
		this.state = networkPlayer.state;
		this.facesRight = networkPlayer.facesRight;
//		this.gunId = networkPlayer.gun.GUN_ID; // TODO: fix this by making a map of all the weapons
		this.lookAngle = networkPlayer.lookAngle;
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
	
	public NetworkPlayer getNetworkPlayer() {		 
		return new NetworkPlayer(this);		
	}
}
