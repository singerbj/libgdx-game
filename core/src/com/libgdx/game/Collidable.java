package com.libgdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.libgdx.entities.Player;

public class Collidable {
	public Rectangle rectangle;
	public Player player;

	public Collidable (Rectangle rectangle, Player player) {
		this.rectangle = rectangle;
		this.player = player;
	}
}
