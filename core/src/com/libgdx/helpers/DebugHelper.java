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
import com.libgdx.entities.Player;

public class DebugHelper {
	OrthographicCamera camera;
	Array<Rectangle> debugTiles;
	Player player;
	TiledMap map;
	ShapeRenderer shapeRenderer;

	public DebugHelper(ShapeRenderer shapeRenderer, OrthographicCamera camera, Array<Rectangle> debugTiles, Player player, TiledMap map) {
		this.shapeRenderer = shapeRenderer;
		this.camera = camera;
		this.debugTiles = debugTiles;
		this.player = player;
		this.map = map;
	}

	public void renderDebug() {
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Line);

		shapeRenderer.setColor(Color.RED);
		shapeRenderer.rect(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);

		shapeRenderer.setColor(Color.YELLOW);
		TiledMapTileLayer wallsLayer = (TiledMapTileLayer) map.getLayers().get("walls");
		for (int y = 0; y <= wallsLayer.getHeight(); y++) {
			for (int x = 0; x <= wallsLayer.getWidth(); x++) {
				Cell cell = wallsLayer.getCell(x, y);
				if (cell != null) {
					if (camera.frustum.boundsInFrustum(x + 0.5f, y + 0.5f, 0, 1, 1, 0))
						shapeRenderer.rect(x, y, 1, 1);
				}
			}
		}
		shapeRenderer.setColor(Color.PURPLE);
		TiledMapTileLayer laddersLayer = (TiledMapTileLayer) map.getLayers().get("ladders");
		for (int y = 0; y <= laddersLayer.getHeight(); y++) {
			for (int x = 0; x <= laddersLayer.getWidth(); x++) {
				Cell cell = laddersLayer.getCell(x, y);
				if (cell != null) {
					if (camera.frustum.boundsInFrustum(x + 0.5f, y + 0.5f, 0, 1, 1, 0))
						shapeRenderer.rect(x, y, 1, 1);
				}
			}
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
