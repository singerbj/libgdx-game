package com.libgdx.game;

import java.util.Iterator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.TimeUtils;
import com.libgdx.entities.Player;

public class LibGdxGame extends ApplicationAdapter {
	private TiledMap map;
	private float mapWidth;
	private LoopingOrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private Texture koalaTexture;
	private Animation<TextureRegion> stand;
	private Animation<TextureRegion> walk;
	private Animation<TextureRegion> jump;
	private Player player;
	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject() {
			return new Rectangle();
		}
	};
	private Array<Rectangle> floorTiles = new Array<Rectangle>();
	private Array<Rectangle> ladderTiles = new Array<Rectangle>();

	private BitmapFont font;
	private SpriteBatch batch;

	private static final float GRAVITY = -45f;

	private boolean debug = true;
	private ShapeRenderer debugRenderer;
	private Rectangle[] debugTiles;

	@Override
	public void create() {
		// load the koala frames, split them, and assign them to Animations
		koalaTexture = new Texture("koalio.png");
		TextureRegion[] regions = TextureRegion.split(koalaTexture, 18, 26)[0];
		stand = new Animation(0, regions[0]);
		jump = new Animation(0, regions[1]);
		walk = new Animation(0.15f, regions[2], regions[3], regions[4]);
		walk.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);

		// load the map, set the unit scale to 1/16 (1 unit == 16 pixels)
		TmxMapLoader.Parameters parameters = new TmxMapLoader.Parameters();
		parameters.convertObjectToTileSpace = true;
		map = new TmxMapLoader().load("kenney.tmx", parameters);
		mapWidth = ((TiledMapTileLayer) map.getLayers().get(0)).getWidth();
		renderer = new LoopingOrthogonalTiledMapRenderer(map, 1 / 70f);

		// create an orthographic camera, shows us 30x20 units of the world
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 60f, 40f);
		camera.update();

		// create the Koala we want to move around the world
		player = new Player(1 / 16f * regions[0].getRegionWidth(), 1 / 16f * regions[0].getRegionHeight());
		player.position.set(20f, 10f);

		debugRenderer = new ShapeRenderer();

		font = new BitmapFont();
		batch = new SpriteBatch();
	}

	@Override
	public void render() {
		// clear the screen
		Gdx.gl.glClearColor(0.7f, 0.7f, 1.0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// get the delta time
		float deltaTime = Gdx.graphics.getDeltaTime();

		// update the koala (process input, collision detection, position update)
		updateKoala(deltaTime);

		// let the camera follow the koala, x-axis only
		camera.position.x = player.position.x;
		camera.position.y = player.position.y;
		camera.update();

		// set the TiledMapRenderer view based on what the
		// camera sees, and render the map
		renderer.setView(camera);
		renderer.render();

		// render the koala
		renderKoala(deltaTime);

		// render debug rectangles
		if (debug) {
			renderDebug();
			batch.begin();
			font.draw(batch, (int) Gdx.graphics.getFramesPerSecond() + " fps", 3, Gdx.graphics.getHeight() - 3);
			batch.end();
		}
	}

	private void updateKoala(float deltaTime) {
		//check for quit command
		if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
			System.exit(0);
		}		

		player.stateTime += deltaTime;

		// check input and apply to velocity & state
		if (Gdx.input.isKeyPressed(Keys.SPACE)) {
			if (player.grounded) {
				player.velocity.y += Player.JUMP_VELOCITY;
				player.state = Player.State.Jumping;
				player.grounded = false;
			} 
		}

		float speedMultiplier = 1f;
		if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) && (player.grounded || !player.onLadder)) {
			speedMultiplier = 1.6f;
		}

		if (Gdx.input.isKeyPressed(Keys.A)) {
			player.velocity.x -= 1 * speedMultiplier;
			if (player.velocity.x < -Player.MAX_VELOCITY * speedMultiplier) {
				player.velocity.x = -Player.MAX_VELOCITY * speedMultiplier;
			}
			if (player.grounded)
				player.state = Player.State.Walking;
			player.facesRight = false;
		}

		if (Gdx.input.isKeyPressed(Keys.D)) {
			player.velocity.x += 1 * speedMultiplier;
			if (player.velocity.x > Player.MAX_VELOCITY * speedMultiplier) {
				player.velocity.x = Player.MAX_VELOCITY * speedMultiplier;
			}
			if (player.grounded)
				player.state = Player.State.Walking;
			player.facesRight = true;
		}

		if (Gdx.input.isKeyJustPressed(Keys.B))
			debug = !debug;

		// apply gravity if we are falling
		player.velocity.add(0, GRAVITY * deltaTime);
		// set max falling speed
		if (player.velocity.y < -Player.MAX_VELOCITY * 5f) {
			player.velocity.y = -Player.MAX_VELOCITY * 5f;
		}

		// clamp the velocity to the maximum, x-axis only
		player.velocity.x = MathUtils.clamp(player.velocity.x, -Player.MAX_VELOCITY * speedMultiplier,
				Player.MAX_VELOCITY * speedMultiplier);

		// If the velocity is < 1, set it to 0 and set state to Standing
		if (Math.abs(player.velocity.x) < 1) {
			player.velocity.x = 0;
			if (player.grounded)
				player.state = Player.State.Standing;
		}

		// multiply by delta time so we know how far we go
		// in this frame
		player.velocity.scl(deltaTime);

		// perform collision detection & response, on each axis, separately
		// if the koala is moving right, check the tiles to the right of it's
		// right bounding box edge, otherwise check the ones to the left
		Rectangle koalaRect = rectPool.obtain();
		koalaRect.set(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);
		int startX, startY, endX, endY;
		if (player.velocity.x > 0) {
			startX = endX = (int) (player.position.x + Player.WIDTH + player.velocity.x);
		} else {
			startX = endX = (int) (player.position.x + player.velocity.x);
		}
		startY = (int) (player.position.y);
		endY = (int) (player.position.y + Player.HEIGHT);
		getTiles("walls", startX, startY, endX, endY, floorTiles);
		getTiles("ladders", startX, startY, endX, endY, ladderTiles);
		koalaRect.x += player.velocity.x;
		for (Rectangle tile : floorTiles) {
			if (koalaRect.overlaps(tile)) {
				player.velocity.x = 0;
				break;
			}
		}
		koalaRect.x = player.position.x;

		// if the koala is moving upwards, check the tiles to the top of its
		// top bounding box edge, otherwise check the ones to the bottom
		startX = (int) (player.position.x);
		endX = (int) (player.position.x + Player.WIDTH);

		getTiles("ladders", startX, (int) (player.position.y + player.velocity.y), endX,
				(int) (player.position.y + Player.HEIGHT + player.velocity.y), ladderTiles);

		player.onLadder = false;
		for (Rectangle tile : ladderTiles) {
			if (koalaRect.overlaps(tile)) {
				player.onLadder = true;
				player.state = Player.State.Standing;
				player.velocity.y = 0;
				break;
			}
		}

		if (player.onLadder) {
			if (Gdx.input.isKeyPressed(Keys.W)) {
				player.velocity.y += 5 * deltaTime;
				if (player.velocity.y > Player.MAX_VELOCITY) {
					player.velocity.y = Player.MAX_VELOCITY;
				}
			}

			if (Gdx.input.isKeyPressed(Keys.S)) {
				player.velocity.y -= 5 * deltaTime;
				if (player.velocity.y < -Player.MAX_VELOCITY) {
					player.velocity.y = -Player.MAX_VELOCITY;
				}
			}
		}

		if (player.velocity.y > 0) {
			startY = endY = (int) (player.position.y + Player.HEIGHT + player.velocity.y);
		} else {
			startY = endY = (int) (player.position.y + player.velocity.y);
		}

		getTiles("walls", startX, startY, endX, endY, floorTiles);
		koalaRect.y += player.velocity.y;
		for (Rectangle tile : floorTiles) {
			if (koalaRect.overlaps(tile)) {
				// we actually reset the koala y-position here
				// so it is just below/above the tile we collided with
				// this removes bouncing :)
				if (player.velocity.y <= 0) {
					player.position.y = tile.y + tile.height;
					// if we hit the ground, mark us as grounded so we can jump
					player.grounded = true;
				}
				player.velocity.y = 0;
				break;
			}
		}

		rectPool.free(koalaRect);

		// unscale the velocity by the inverse delta time and set
		// the latest position
		player.position.add(player.velocity);
		player.velocity.scl(1 / deltaTime);

		// Apply damping to the velocity on the x-axis so we don't
		// walk infinitely once a key was pressed
		player.velocity.x *= Player.DAMPING;
		
		//update the koalas alternate positions
		if(player.position.x < 0) {
			player.position.x = player.rightPosition.x;			
		}else if(player.position.x > mapWidth) {
			player.position.x = player.leftPosition.x;			
		}
		player.rightPosition.x = player.position.x + mapWidth;
		player.leftPosition.x = player.position.x - mapWidth;
	}
	
	private void getTiles(String layerName, int startX, int startY, int endX, int endY, Array<Rectangle> tiles) {
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
				cell = layer.getCell(x + layer.getWidth(), y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);					
				}
				cell = layer.getCell(x - layer.getWidth(), y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);					
				}
			}
		}
	}	

	private void renderKoala(float deltaTime) {
		// based on the koala state, get the animation frame
		TextureRegion frame = null;
		switch (player.state) {
		case Standing:
			frame = stand.getKeyFrame(player.stateTime);
			break;
		case Walking:
			frame = walk.getKeyFrame(player.stateTime);
			break;
		case Jumping:
			frame = jump.getKeyFrame(player.stateTime);
			break;
		}

		// draw the koala, depending on the current velocity
		// on the x-axis, draw the koala facing either right
		// or left
		Batch batch = renderer.getBatch();
		batch.begin();
		if (player.facesRight) {
			batch.draw(frame, player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);
			batch.draw(frame, player.leftPosition.x, player.position.y, Player.WIDTH, Player.HEIGHT);
			batch.draw(frame, player.rightPosition.x, player.position.y, Player.WIDTH, Player.HEIGHT);			
		} else {
			batch.draw(frame, player.position.x + Player.WIDTH, player.position.y, -Player.WIDTH, Player.HEIGHT);
			batch.draw(frame, player.leftPosition.x + Player.WIDTH, player.position.y, -Player.WIDTH, Player.HEIGHT);
			batch.draw(frame, player.rightPosition.x + Player.WIDTH, player.position.y, -Player.WIDTH, Player.HEIGHT);
		}
		batch.end();
	}

	private void renderDebug() {
		debugRenderer.setProjectionMatrix(camera.combined);
		debugRenderer.begin(ShapeType.Line);

		debugRenderer.setColor(Color.RED);
		debugRenderer.rect(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);

		debugRenderer.setColor(Color.YELLOW);
		TiledMapTileLayer wallsLayer = (TiledMapTileLayer) map.getLayers().get("walls");
		for (int y = 0; y <= wallsLayer.getHeight(); y++) {
			for (int x = 0; x <= wallsLayer.getWidth(); x++) {
				Cell cell = wallsLayer.getCell(x, y);
				if (cell != null) {
					if (camera.frustum.boundsInFrustum(x + 0.5f, y + 0.5f, 0, 1, 1, 0))
						debugRenderer.rect(x, y, 1, 1);
				}
			}
		}
		debugRenderer.setColor(Color.PURPLE);
		TiledMapTileLayer laddersLayer = (TiledMapTileLayer) map.getLayers().get("ladders");
		for (int y = 0; y <= laddersLayer.getHeight(); y++) {
			for (int x = 0; x <= laddersLayer.getWidth(); x++) {
				Cell cell = laddersLayer.getCell(x, y);
				if (cell != null) {
					if (camera.frustum.boundsInFrustum(x + 0.5f, y + 0.5f, 0, 1, 1, 0))
						debugRenderer.rect(x, y, 1, 1);
				}
			}
		}

		debugRenderer.setColor(Color.PINK);
		if (debugTiles != null) {
			for (int i = 0; i <= debugTiles.length - 1; i++) {
				Rectangle tile = debugTiles[i];
				if (camera.frustum.boundsInFrustum(tile.x + 0.5f, tile.y + 0.5f, 0, 1, 1, 0))
					debugRenderer.rect(tile.x, tile.y, 1, 1);
			}
		}

		debugRenderer.end();
	}

	@Override
	public void dispose() {
	}
}