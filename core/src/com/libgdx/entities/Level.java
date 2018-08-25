package com.libgdx.entities;

import java.util.HashMap;

import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.libgdx.game.LoopingOrthogonalTiledMapRenderer;

public class Level {
	public TiledMap map;
	public float mapWidth;
	public float mapHeight;
	public LoopingOrthogonalTiledMapRenderer renderer;
	public Array<Rectangle> walls = new Array<Rectangle>();
	public Array<Rectangle> ladders = new Array<Rectangle>();
	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject() {
			return new Rectangle();
		}
	};
	
	public Level(String mapName) {
		// load the map, set the unit scale to 1/16 (1 unit == 16 pixels)
		TmxMapLoader.Parameters parameters = new TmxMapLoader.Parameters();
		parameters.convertObjectToTileSpace = true;	
		map = new TmxMapLoader().load(mapName, parameters);
		mapWidth = ((TiledMapTileLayer) map.getLayers().get(0)).getWidth();
		mapHeight = ((TiledMapTileLayer) map.getLayers().get(0)).getHeight();
		renderer = new LoopingOrthogonalTiledMapRenderer(map, 1 / 70f);
		
		walls = getTiles("walls", 0, 0, (int) mapWidth, (int) mapHeight);
		ladders = getTiles("ladders", 0, 0, (int) mapWidth, (int) mapHeight);
	}
	
	public Array<Rectangle> getTiles(String layerName, int startX, int startY, int endX, int endY) {
		Array<Rectangle> tiles = new Array<Rectangle>();
		TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
		rectPool.freeAll(tiles);
		tiles.clear();
		for (int y = startY; y <= endY; y++) {
			for (int x = startX; x <= endX; x++) {
				Cell cell = layer.getCell(x, y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);
				}
			}
		}
		return tiles;
	}
}
