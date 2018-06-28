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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.TimeUtils;
import com.libgdx.entities.Player;
import com.libgdx.helpers.DebugHelper;
import com.libgdx.helpers.RayCastHelper;

public class LibGdxGame extends ApplicationAdapter {
	private TiledMap map;
	private float mapWidth;
	private float mapHeight;
	private LoopingOrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private Texture playerTexture;
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

	private BitmapFont font;
	private SpriteBatch batch;

	private static final float GRAVITY = -45f;

	private boolean debug = true;
	private Array<Rectangle> debugTiles;
	private DebugHelper debugHelper;
	ShapeRenderer shapeRenderer;
	private RayCastHelper rayCastHelper;

	@Override
	public void create() {
		// load the player frames, split them, and assign them to Animations
		playerTexture = new Texture("koalio.png");
		TextureRegion[] regions = TextureRegion.split(playerTexture, 18, 26)[0];
		stand = new Animation<TextureRegion>(0, regions[0]);
		jump = new Animation<TextureRegion>(0, regions[1]);
		walk = new Animation<TextureRegion>(0.15f, regions[2], regions[3], regions[4]);
		walk.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);

		// load the map, set the unit scale to 1/16 (1 unit == 16 pixels)
		TmxMapLoader.Parameters parameters = new TmxMapLoader.Parameters();
		parameters.convertObjectToTileSpace = true;
		map = new TmxMapLoader().load("level1.tmx", parameters);
//		map = new TmxMapLoader().load("small.tmx", parameters);
		mapWidth = ((TiledMapTileLayer) map.getLayers().get(0)).getWidth();
		mapHeight = ((TiledMapTileLayer) map.getLayers().get(0)).getHeight();
		renderer = new LoopingOrthogonalTiledMapRenderer(map, 1 / 16f);

		// create an orthographic camera, shows us 30x20 units of the world
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 60f, 40f);
		camera.update();

		// create the Player we want to move around the world
		player = new Player(1 / 16f * regions[0].getRegionWidth(), 1 / 16f * regions[0].getRegionHeight());
		player.position.set(20f, 10f);

		font = new BitmapFont();
		batch = new SpriteBatch();

		rayCastHelper = new RayCastHelper();
		shapeRenderer = new ShapeRenderer();
		debugTiles = new Array<Rectangle>();
		debugHelper = new DebugHelper(shapeRenderer, camera, debugTiles, player, map);
	}

	@Override
	public void render() {
		// clear the screen
		Gdx.gl.glClearColor(0.7f, 0.7f, 1.0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// get the delta time
		float deltaTime = Gdx.graphics.getDeltaTime();

		// update the player (process input, collision detection, position update)
		updatePlayer(deltaTime);

		// let the camera follow the player, x-axis only
		camera.position.x = player.position.x + (Player.WIDTH / 2);
		camera.position.y = player.position.y + (Player.HEIGHT / 2);
		camera.update();

		// set the TiledMapRenderer view based on what the
		// camera sees, and render the map
		renderer.setView(camera);
		renderer.render();

		// render the player
		renderPlayer(deltaTime);

		// render debug rectangles
		if (debug) {
			debugHelper.renderDebug();
			batch.begin();
			font.draw(batch, (int) Gdx.graphics.getFramesPerSecond() + " fps", 3, Gdx.graphics.getHeight() - 3);
			batch.end();
		}
	}

	private void updatePlayer(float deltaTime) {
		// check for quit command
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
		
		player.gun.updateReloadState(TimeUtils.millis());
		if (Gdx.input.isKeyPressed(Keys.R)) {
			player.gun.reload(TimeUtils.millis());
		}

		if (Gdx.input.isTouched()) {
			if(player.gun.fireGun(TimeUtils.millis())) {
				Collision collision = null;
				for(Vector2 position : player.getPositions()) {
					if(collision != null) {
						break;
					}
					shapeRenderer.setProjectionMatrix(camera.combined);
					shapeRenderer.begin(ShapeType.Line);
					shapeRenderer.setColor(Color.BLACK);
					
					float playerCenterX = position.x + (Player.WIDTH / 2);
					float playerCenterY = position.y + (Player.HEIGHT / 2);
					Vector2 src = new Vector2(playerCenterX, playerCenterY);
					Vector3 literalDest = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
					camera.unproject(literalDest);
					Vector2 dest = new Vector2(literalDest.x + (position.x - player.position.x), literalDest.y);
					float theta = (float) (180.0 / Math.PI * Math.atan2(playerCenterX - dest.x, playerCenterY - dest.y));
					dest = new Vector2(playerCenterX, playerCenterY).add(new Vector2(20, 0).rotate(-theta - 90));
					
					collision = rayCastHelper.rayTest(src, dest, getTiles("walls", 0, 0, (int) mapWidth, (int) mapHeight));
					if(collision != null) {
						debugTiles.add(collision.getCollideableObject());
						shapeRenderer.line(src, collision.getCollisionPoint());
					}else{
						shapeRenderer.line(src, dest);
					}
					shapeRenderer.end();
				}
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

		if (Gdx.input.isKeyJustPressed(Keys.B)) {
			debug = !debug;
		}

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
		// if the player is moving right, check the tiles to the right of it's
		// right bounding box edge, otherwise check the ones to the left
		Rectangle playerRect = rectPool.obtain();
		playerRect.set(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);
		int startX, startY, endX, endY;
		if (player.velocity.x > 0) {
			startX = endX = (int) (player.position.x + Player.WIDTH + player.velocity.x);
		} else {
			startX = endX = (int) (player.position.x + player.velocity.x);
		}
		startY = (int) (player.position.y);
		endY = (int) (player.position.y + Player.HEIGHT);

		Array<Rectangle> floorTiles = new Array<Rectangle>();
		Array<Rectangle> ladderTiles = new Array<Rectangle>();
		floorTiles = getTiles("walls", startX, startY, endX, endY);
		ladderTiles = getTiles("ladders", startX, startY, endX, endY);
		playerRect.x += player.velocity.x;
		for (Rectangle tile : floorTiles) {
			if (playerRect.overlaps(tile)) {
				player.velocity.x = 0;
				break;
			}
		}
		playerRect.x = player.position.x;

		// if the player is moving upwards, check the tiles to the top of its
		// top bounding box edge, otherwise check the ones to the bottom
		startX = (int) (player.position.x);
		endX = (int) (player.position.x + Player.WIDTH);

		ladderTiles = getTiles("ladders", startX, (int) (player.position.y + player.velocity.y), endX,
				(int) (player.position.y + Player.HEIGHT + player.velocity.y));

		player.onLadder = false;
		for (Rectangle tile : ladderTiles) {
			if (playerRect.overlaps(tile)) {
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

		floorTiles = getTiles("walls", startX, startY, endX, endY);
		playerRect.y += player.velocity.y;
		for (Rectangle tile : floorTiles) {
			if (playerRect.overlaps(tile)) {
				// we actually reset the player y-position here
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

		rectPool.free(playerRect);

		// unscale the velocity by the inverse delta time and set
		// the latest position
		player.position.add(player.velocity);
		player.velocity.scl(1 / deltaTime);

		// Apply damping to the velocity on the x-axis so we don't
		// walk infinitely once a key was pressed
		player.velocity.x *= Player.DAMPING;

		// update the players alternate positions
		if (player.position.x < 0) {
			player.position.x = player.rightPosition.x;
		} else if (player.position.x > mapWidth) {
			player.position.x = player.leftPosition.x;
		}
		player.rightPosition.x = player.position.x + mapWidth;
		player.leftPosition.x = player.position.x - mapWidth;
		player.rightPosition.y = player.position.y;
		player.leftPosition.y = player.position.y;
	}

	private Array<Rectangle> getTiles(String layerName, int startX, int startY, int endX, int endY) {
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
				cell = layer.getCell(x + layer.getWidth() - 1, y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);
				}
				cell = layer.getCell(x - layer.getWidth() - 1, y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);
				}
			}
		}
		return tiles;
	}

	private void renderPlayer(float deltaTime) {
		// based on the player state, get the animation frame
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

		// draw the player, depending on the current velocity
		// on the x-axis, draw the player facing either right
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

	@Override
	public void dispose() {
	}
}