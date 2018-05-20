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

public class LibGdxGame extends ApplicationAdapter {
	/** The player character, has state and state time, */
	static class Koala {
		static float WIDTH;
		static float HEIGHT;
		static float MAX_VELOCITY = 5f;
		static float JUMP_VELOCITY = 25f;
		static float DAMPING = 0.87f;

		enum State {
			Standing, Walking, Jumping
		}

		final Vector2 position = new Vector2();
		final Vector2 leftPosition = new Vector2();
		final Vector2 rightPosition = new Vector2();
		final Vector2 velocity = new Vector2();
		State state = State.Walking;
		float stateTime = 0;
		boolean facesRight = true;
		boolean grounded = false;
		boolean onLadder = false;
	}

	private TiledMap map;
	private float mapWidth;
	private LoopingOrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private Texture koalaTexture;
	private Animation<TextureRegion> stand;
	private Animation<TextureRegion> walk;
	private Animation<TextureRegion> jump;
	private Koala koala;
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

	private static final float GRAVITY = -2.5f;

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

		// figure out the width and height of the koala for collision
		// detection and rendering by converting a koala frames pixel
		// size into world units (1 unit == 16 pixels)
		Koala.WIDTH = 1 / 16f * regions[0].getRegionWidth();
		Koala.HEIGHT = 1 / 16f * regions[0].getRegionHeight();

		// load the map, set the unit scale to 1/16 (1 unit == 16 pixels)
		TmxMapLoader.Parameters parameters = new TmxMapLoader.Parameters();
		parameters.convertObjectToTileSpace = true;
		map = new TmxMapLoader().load("level1.tmx", parameters);
		mapWidth = ((TiledMapTileLayer) map.getLayers().get(0)).getWidth();
		renderer = new LoopingOrthogonalTiledMapRenderer(map, 1 / 16f);

		// create an orthographic camera, shows us 30x20 units of the world
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 60f, 40f);
		camera.update();

		// create the Koala we want to move around the world
		koala = new Koala();
		koala.position.set(3f, 10f);

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
		camera.position.x = koala.position.x;
		camera.position.y = koala.position.y;
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
		
		if (deltaTime == 0)
			return;

		if (deltaTime > 0.1f)
			deltaTime = 0.1f;

		koala.stateTime += deltaTime;

		// check input and apply to velocity & state
		if (Gdx.input.isKeyPressed(Keys.SPACE)) {
			if (koala.grounded) {
				koala.velocity.y += Koala.JUMP_VELOCITY;
				koala.state = Koala.State.Jumping;
				koala.grounded = false;
			} else {
				koala.velocity.y -= GRAVITY / 1.75;
			}
		}

		float speedMultiplier = 1f;
		if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) && (koala.grounded || !koala.onLadder)) {
			speedMultiplier = 1.6f;
		}

		if (Gdx.input.isKeyPressed(Keys.A)) {
			koala.velocity.x -= 1 * speedMultiplier;
			if (koala.velocity.x < -Koala.MAX_VELOCITY * speedMultiplier) {
				koala.velocity.x = -Koala.MAX_VELOCITY * speedMultiplier;
			}
			if (koala.grounded)
				koala.state = Koala.State.Walking;
			koala.facesRight = false;
		}

		if (Gdx.input.isKeyPressed(Keys.D)) {
			koala.velocity.x += 1 * speedMultiplier;
			if (koala.velocity.x > Koala.MAX_VELOCITY * speedMultiplier) {
				koala.velocity.x = Koala.MAX_VELOCITY * speedMultiplier;
			}
			if (koala.grounded)
				koala.state = Koala.State.Walking;
			koala.facesRight = true;
		}

		if (Gdx.input.isKeyJustPressed(Keys.B))
			debug = !debug;

		// apply gravity if we are falling
		koala.velocity.add(0, GRAVITY);
		// set max falling speed
		if (koala.velocity.y < -Koala.MAX_VELOCITY * 5f) {
			koala.velocity.y = -Koala.MAX_VELOCITY * 5f;
		}

		// clamp the velocity to the maximum, x-axis only
		koala.velocity.x = MathUtils.clamp(koala.velocity.x, -Koala.MAX_VELOCITY * speedMultiplier,
				Koala.MAX_VELOCITY * speedMultiplier);

		// If the velocity is < 1, set it to 0 and set state to Standing
		if (Math.abs(koala.velocity.x) < 1) {
			koala.velocity.x = 0;
			if (koala.grounded)
				koala.state = Koala.State.Standing;
		}

		// multiply by delta time so we know how far we go
		// in this frame
		koala.velocity.scl(deltaTime);

		// perform collision detection & response, on each axis, separately
		// if the koala is moving right, check the tiles to the right of it's
		// right bounding box edge, otherwise check the ones to the left
		Rectangle koalaRect = rectPool.obtain();
		koalaRect.set(koala.position.x, koala.position.y, Koala.WIDTH, Koala.HEIGHT);
		int startX, startY, endX, endY;
		if (koala.velocity.x > 0) {
			startX = endX = (int) (koala.position.x + Koala.WIDTH + koala.velocity.x);
		} else {
			startX = endX = (int) (koala.position.x + koala.velocity.x);
		}
		startY = (int) (koala.position.y);
		endY = (int) (koala.position.y + Koala.HEIGHT);
		getTiles(startX, startY, endX, endY, floorTiles);
		getLadderTiles(startX, startY, endX, endY, ladderTiles);
		koalaRect.x += koala.velocity.x;
		for (Rectangle tile : floorTiles) {
			if (koalaRect.overlaps(tile)) {
				koala.velocity.x = 0;
				break;
			}
		}
		koalaRect.x = koala.position.x;

		// if the koala is moving upwards, check the tiles to the top of its
		// top bounding box edge, otherwise check the ones to the bottom
		startX = (int) (koala.position.x);
		endX = (int) (koala.position.x + Koala.WIDTH);

		getLadderTiles(startX, (int) (koala.position.y + koala.velocity.y), endX,
				(int) (koala.position.y + Koala.HEIGHT + koala.velocity.y), ladderTiles);

		koala.onLadder = false;
		for (Rectangle tile : ladderTiles) {
			if (koalaRect.overlaps(tile)) {
				koala.onLadder = true;
				koala.state = Koala.State.Standing;
				koala.velocity.y = 0;
				break;
			}
		}

		if (koala.onLadder) {
			if (Gdx.input.isKeyPressed(Keys.W)) {
				koala.velocity.y += 1;
				if (koala.velocity.y > (Koala.MAX_VELOCITY * 0.02f)) {
					koala.velocity.y = (Koala.MAX_VELOCITY * 0.02f);
				}
			}

			if (Gdx.input.isKeyPressed(Keys.S)) {
				koala.velocity.y -= 1;
				if (koala.velocity.y < -(Koala.MAX_VELOCITY * 0.02f)) {
					koala.velocity.y = -(Koala.MAX_VELOCITY * 0.02f);
				}
			}
		}

		if (koala.velocity.y > 0) {
			startY = endY = (int) (koala.position.y + Koala.HEIGHT + koala.velocity.y);
		} else {
			startY = endY = (int) (koala.position.y + koala.velocity.y);
		}

		getTiles(startX, startY, endX, endY, floorTiles);
		koalaRect.y += koala.velocity.y;
		for (Rectangle tile : floorTiles) {
			if (koalaRect.overlaps(tile)) {
				// we actually reset the koala y-position here
				// so it is just below/above the tile we collided with
				// this removes bouncing :)
				if (koala.velocity.y <= 0) {
					koala.position.y = tile.y + tile.height;
					// if we hit the ground, mark us as grounded so we can jump
					koala.grounded = true;
				}
				koala.velocity.y = 0;
				break;
			}
		}

		rectPool.free(koalaRect);

		// unscale the velocity by the inverse delta time and set
		// the latest position
		koala.position.add(koala.velocity);
		koala.velocity.scl(1 / deltaTime);

		// Apply damping to the velocity on the x-axis so we don't
		// walk infinitely once a key was pressed
		koala.velocity.x *= Koala.DAMPING;
		
		//update the koalas alternate positions
		if(koala.position.x < 0) {
			koala.position.x = koala.rightPosition.x;			
		}else if(koala.position.x > mapWidth) {
			koala.position.x = koala.leftPosition.x;			
		}
		koala.rightPosition.x = koala.position.x + mapWidth;
		koala.leftPosition.x = koala.position.x - mapWidth;
	}
	
	private void getTiles(int startX, int startY, int endX, int endY, Array<Rectangle> floorTiles) {
		TiledMapTileLayer wallsLayer = (TiledMapTileLayer) map.getLayers().get("walls");
		rectPool.freeAll(floorTiles);
		floorTiles.clear();
		for (int y = startY; y <= endY; y++) {
			for (int x = startX; x <= endX; x++) {
				Cell cell = wallsLayer.getCell(x, y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					floorTiles.add(rect);					
				}
				cell = wallsLayer.getCell(x + wallsLayer.getWidth(), y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					floorTiles.add(rect);					
				}
				cell = wallsLayer.getCell(x - wallsLayer.getWidth(), y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					floorTiles.add(rect);					
				}
			}
		}
	}

	private void getLadderTiles(int startX, int startY, int endX, int endY, Array<Rectangle> ladderTiles) {
		TiledMapTileLayer ladderLayer = (TiledMapTileLayer) map.getLayers().get("ladders");
		rectPool.freeAll(ladderTiles);
		ladderTiles.clear();
		for (int y = startY; y <= endY; y++) {
			for (int x = startX; x <= endX; x++) {
				Cell cell = ladderLayer.getCell(x, y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					ladderTiles.add(rect);
				}
				cell = ladderLayer.getCell(x + ladderLayer.getWidth(), y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					ladderTiles.add(rect);					
				}
				cell = ladderLayer.getCell(x - ladderLayer.getWidth(), y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					ladderTiles.add(rect);					
				}
			}
		}
	}

	private void renderKoala(float deltaTime) {
		// based on the koala state, get the animation frame
		TextureRegion frame = null;
		switch (koala.state) {
		case Standing:
			frame = stand.getKeyFrame(koala.stateTime);
			break;
		case Walking:
			frame = walk.getKeyFrame(koala.stateTime);
			break;
		case Jumping:
			frame = jump.getKeyFrame(koala.stateTime);
			break;
		}

		// draw the koala, depending on the current velocity
		// on the x-axis, draw the koala facing either right
		// or left
		Batch batch = renderer.getBatch();
		batch.begin();
		if (koala.facesRight) {
			batch.draw(frame, koala.position.x, koala.position.y, Koala.WIDTH, Koala.HEIGHT);
			batch.draw(frame, koala.leftPosition.x, koala.position.y, Koala.WIDTH, Koala.HEIGHT);
			batch.draw(frame, koala.rightPosition.x, koala.position.y, Koala.WIDTH, Koala.HEIGHT);			
		} else {
			batch.draw(frame, koala.position.x + Koala.WIDTH, koala.position.y, -Koala.WIDTH, Koala.HEIGHT);
			batch.draw(frame, koala.leftPosition.x + Koala.WIDTH, koala.position.y, -Koala.WIDTH, Koala.HEIGHT);
			batch.draw(frame, koala.rightPosition.x + Koala.WIDTH, koala.position.y, -Koala.WIDTH, Koala.HEIGHT);
		}
		batch.end();
	}

	private void renderDebug() {
		debugRenderer.setProjectionMatrix(camera.combined);
		debugRenderer.begin(ShapeType.Line);

		debugRenderer.setColor(Color.RED);
		debugRenderer.rect(koala.position.x, koala.position.y, Koala.WIDTH, Koala.HEIGHT);

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