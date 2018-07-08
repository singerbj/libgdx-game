package com.libgdx.helpers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.libgdx.entities.Level;
import com.libgdx.entities.Player;

public class DebugHelper {
	OrthographicCamera camera;
	Array<Rectangle> debugTiles;
	Player player;
	ShapeRenderer shapeRenderer;
	Level level;

	public DebugHelper(ShapeRenderer shapeRenderer, OrthographicCamera camera, Array<Rectangle> debugTiles, Player player, Level level) {
		this.shapeRenderer = shapeRenderer;
		this.camera = camera;
		this.debugTiles = debugTiles;
		this.player = player;
		this.level = level;
	}

	public void renderDebug() {
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Line);

		shapeRenderer.setColor(Color.RED);
		shapeRenderer.rect(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);

		shapeRenderer.setColor(Color.YELLOW);
		for(Rectangle rect : level.walls) {
			shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
		}
		
		shapeRenderer.setColor(Color.PURPLE);
		for(Rectangle rect : level.ladders) {
			shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
		}
		
		shapeRenderer.setColor(Color.PINK);
		if (debugTiles != null) {
			for (int i = 0; i <= debugTiles.size - 1; i++) {
				Rectangle tile = debugTiles.get(i);
				if (camera.frustum.boundsInFrustum(tile.x + 0.5f, tile.y + 0.5f, 0, 1, 1, 0))
					shapeRenderer.rect(tile.x, tile.y, 1, 1);
			}
		}

		shapeRenderer.end();
		debugTiles.clear();
	}

}
