package com.libgdx.game;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.kryonet.Server;
import com.libgdx.entities.Level;
import com.libgdx.entities.Player;
import com.libgdx.helpers.DebugHelper;
import com.libgdx.helpers.PlayerState;
import com.libgdx.helpers.RayCastHelper;

public class LibGdxGame extends ApplicationAdapter {
	Array<String> args;
	
	private Level level;
	private OrthographicCamera camera;

	private Texture ak;
	private Sprite akSprite;

	private Player player;

	// network related variables
	private Network network;
	private Array<Shot> shots;
	private HashMap<String, Player> players;

	private BitmapFont font;
	private SpriteBatch batch;

	private static final float GRAVITY = -45f;

	private boolean debug = true;
	private Array<Rectangle> debugTiles;
	private DebugHelper debugHelper;
	ShapeRenderer shapeRenderer;
	private RayCastHelper rayCastHelper;

	private Texture playerTexture;
	private TextureRegion[] regions;
	public Animation<TextureRegion> stand;
	public Animation<TextureRegion> jump;
	public Animation<TextureRegion> walk;
	
	public LibGdxGame(String[] args) {
		super();
		this.args = Array.with(args);
	}

	@Override
	public void create() {
		// load the player frames, split them, and assign them to Animations

		ak = new Texture("ak47.png");
		akSprite = new Sprite(ak);

		// network related variables initialized
		shots = new Array<Shot>();
		players = new HashMap<String, Player>();
		player = new Player();
		players.put(player.id, player);
		try {
			network = new Network(args.contains("server", false), player, players, shots);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		playerTexture = new Texture("bettersquare.png");
		regions = TextureRegion.split(playerTexture, 18, 26)[0];
		stand = new Animation<TextureRegion>(0, regions[0]);

		jump = new Animation<TextureRegion>(0, regions[1]);
		walk = new Animation<TextureRegion>(0.15f, regions[2], regions[3], regions[4]);
		walk.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);

		// level = new Level("level1.tmx");
		// level = new Level("small.tmx");
		level = new Level("kenney.tmx");

		// create an orthographic camera
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 1920 / 25f, 1080 / 25f);
		camera.update();

		font = new BitmapFont();
		batch = new SpriteBatch();

		rayCastHelper = new RayCastHelper();
		shapeRenderer = new ShapeRenderer();
		debugTiles = new Array<Rectangle>();
		debugHelper = new DebugHelper(shapeRenderer, camera, debugTiles, player, level);

		// Cursor customCursor = Gdx.graphics.newCursor(new
		// Pixmap(Gdx.files.internal("cursor.png")), 0, 0);
		// Gdx.graphics.setCursor(customCursor);
	}

	@Override
	public void render() {
		// clear the screen
		Gdx.gl.glClearColor(0.7f, 0.7f, 1.0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		// get the delta time
		float deltaTime = Gdx.graphics.getDeltaTime();

		// set projection matrix whatever that is
		shapeRenderer.setProjectionMatrix(camera.combined);

		// handle non player inputs
		handleNonPlayerInputs();

		// update the player (process input, collision detection, position update)
		if (player != null) {
			updatePlayer(deltaTime);

			// let the camera follow the player, x-axis only
			camera.position.x = player.position.x + (Player.WIDTH / 2);
			camera.position.y = player.position.y + (Player.HEIGHT / 2);
			camera.update();

			// set the TiledMapRenderer view based on what the
			// camera sees, and render the map
			level.renderer.setView(camera);
			level.renderer.render();

			// render the player
			renderPlayers(deltaTime);

			// render the crosshair
			renderCrosshair(deltaTime);
			
			// render the hud
			renderHud(deltaTime);
			
			// render debug shapes
			if (debug) {
				debugHelper.renderDebug();
				batch.begin();
				font.draw(batch, (int) Gdx.graphics.getFramesPerSecond() + " fps", 3, Gdx.graphics.getHeight() - 3);
				batch.end();
			}
			
			if(network.server != null) {
				network.sendWorldData(players);
			} else {
				network.sendPlayerData(player);
			}
			
		}
	}

	private void handleNonPlayerInputs() {
		if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
			if (network != null) {
				network.shutdownNetwork();
			}
			System.exit(0);
		}
	}

	private void updatePlayer(float deltaTime) {
		// check for quit command
		player.stateTime += deltaTime;

		// check input and apply to velocity & state
		if (Gdx.input.isKeyPressed(Keys.SPACE)) {
			if (player.grounded) {
				player.velocity.y += Player.JUMP_VELOCITY;
				player.state = PlayerState.Jumping;
				player.grounded = false;
			}
		}

		player.gun.updateReloadState(TimeUtils.millis());
		if (Gdx.input.isKeyPressed(Keys.R)) {
			player.gun.reload(TimeUtils.millis());
		}

		float playerCenterX = player.position.x + (Player.WIDTH / 2);
		float playerCenterY = player.position.y + (Player.HEIGHT / 2);
		Vector2 src = new Vector2(playerCenterX, playerCenterY);
		Vector3 literalDest = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(literalDest);
		Vector2 dest = new Vector2(literalDest.x + (player.position.x - player.position.x), literalDest.y);
		float theta = (float) (180.0 / Math.PI * Math.atan2(playerCenterX - dest.x, playerCenterY - dest.y));
		player.lookAngle = (-theta - 90);

		for (Shot shot : shots) {
			if (shot.getAlphaModifier() > 0f) {
				shot.setAlphaModifier(shot.getAlphaModifier() - 0.03f);
				shapeRenderer.begin(ShapeType.Line);
				for (float i = -(level.mapWidth * 2); i <= (level.mapWidth * 2); i += level.mapWidth) {
					shapeRenderer.line(shot.getSource().x + i, shot.getSource().y, shot.getDest().x + i,
							shot.getDest().y, new Color(255f, 255f, 255f, 0.0f),
							new Color(255f, 255f, 255f, shot.getAlphaModifier()));
				}
				shapeRenderer.end();
			}
		}

		int index = 0;
		for (Shot shot : shots) {
			if (shot.getAlphaModifier() < 0.05f) {
				shots.removeIndex(index);
			}
			index += 1;
		}

		Shot shot = null;
		Shot tempShot = null;
		boolean gunFired = false;
		if (Gdx.input.isTouched()) {
			for (Vector2 position : player.getPositions()) {
				playerCenterX = position.x + (Player.WIDTH / 2);
				playerCenterY = player.position.y + (Player.HEIGHT / 2);
				src = new Vector2(playerCenterX, playerCenterY);
				literalDest = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
				camera.unproject(literalDest);
				dest = new Vector2(literalDest.x + (player.position.x - player.position.x), literalDest.y);
				player.lookAngle = (-theta - 90);
				dest = new Vector2(playerCenterX, playerCenterY)
						.add(new Vector2(player.gun.RANGE, 0).rotate(-theta - 90));

				if ((gunFired || player.gun.fireGun(TimeUtils.millis()))) {
					gunFired = true;
					tempShot = rayCastHelper.rayTest(player.stateTime, src, dest, getCollideables());
					if (tempShot != null && (shot == null || tempShot.getDistance() < shot.getDistance())) {
						shot = tempShot;
					}
					if (shot.getCollideableObject() != null) {
						debugTiles.add(shot.getCollideableObject().rectangle);
						if (shot.getCollideableObject().player != null) {
							shot.getCollideableObject().player.takeDamage(player.gun.DAMAGE);
						}
					}

				}
			}

			if (shot != null) {
				shots.add(shot);
				shapeRenderer.begin(ShapeType.Line);
				for (float i = -(level.mapWidth * 2); i <= (level.mapWidth * 2); i += level.mapWidth) {
					shapeRenderer.line(shot.getSource().x + i, shot.getSource().y, shot.getDest().x + i,
							shot.getDest().y, new Color(255f, 255f, 255f, 0.0f),
							new Color(255f, 255f, 255f, shot.getAlphaModifier()));
				}
			}
			shapeRenderer.end();

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
				player.state = PlayerState.Walking;
			// player.facesRight = false;
		}

		if (Gdx.input.isKeyPressed(Keys.D)) {
			player.velocity.x += 1 * speedMultiplier;
			if (player.velocity.x > Player.MAX_VELOCITY * speedMultiplier) {
				player.velocity.x = Player.MAX_VELOCITY * speedMultiplier;
			}
			if (player.grounded)
				player.state = PlayerState.Walking;
			// player.facesRight = true;
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
				player.state = PlayerState.Standing;
		}

		// multiply by delta time so we know how far we go
		// in this frame
		player.velocity.scl(deltaTime);

		// perform collision detection & response, on each axis, separately
		// if the player is moving right, check the tiles to the right of it's
		// right bounding box edge, otherwise check the ones to the left
		Rectangle playerRect = new Rectangle();
		playerRect.set(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);
		int startY, endX, endY;
		if (player.velocity.x > 0) {
			endX = (int) (player.position.x + Player.WIDTH + player.velocity.x);
		} else {
			endX = (int) (player.position.x + player.velocity.x);
		}
		startY = (int) (player.position.y);
		endY = (int) (player.position.y + Player.HEIGHT);

		playerRect.x += player.velocity.x;

		for (Collidable wall : level.walls) {
			if (playerRect.overlaps(wall.rectangle)) {
				player.velocity.x = 0;
				break;
			}
		}
		playerRect.x = player.position.x;

		endX = (int) (player.position.x + Player.WIDTH);

		// ladderTiles = getTiles("ladders", startX, (int) (player.position.y +
		// player.velocity.y), endX,
		// (int) (player.position.y + Player.HEIGHT + player.velocity.y));

		player.onLadder = false;
		for (Rectangle tile : level.ladders) {
			if (playerRect.overlaps(tile)) {
				player.onLadder = true;
				player.state = PlayerState.Standing;
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

		playerRect.y += player.velocity.y;
		for (Collidable wall : level.walls) {
			if (playerRect.overlaps(wall.rectangle)) {
				// we actually reset the player y-position here
				// so it is just below/above the tile we collided with
				// this removes bouncing :)
				if (player.velocity.y <= 0) {
					player.position.y = wall.rectangle.y + wall.rectangle.height;
					// if we hit the ground, mark us as grounded so we can jump
					player.grounded = true;
				}
				player.velocity.y = 0;
				break;
			}
		}

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
		} else if (player.position.x > level.mapWidth) {
			player.position.x = player.leftPosition.x;
		}

		player.updateLeftRightPositions(level.mapWidth);
	}

	private Array<Collidable> getCollideables() {
		Array<Collidable> collidables = new Array<Collidable>();
		collidables.addAll(level.walls);
		Player player;
		for(String key : players.keySet()) {
			player = players.get(key);
			if(!player.id.equals(this.player.id)) {
				collidables.addAll(player.getCollidables());
			}
		}
		return collidables;
	}

	private void renderPlayers(float deltaTime) {
		renderPlayer(deltaTime, player);
		renderGun(deltaTime, player);
		Player otherPlayer;
		for(String key : players.keySet()) {
			otherPlayer = players.get(key);
			if(otherPlayer.id != player.id) {
				otherPlayer.updateLeftRightPositions(level.mapWidth);
				renderPlayer(deltaTime, otherPlayer);
				renderGun(deltaTime, otherPlayer);
			}
		}
	}

	private void renderPlayer(float deltaTime, Player player) {
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
		Batch rendererBatch = level.renderer.getBatch();
		rendererBatch.begin();

		if (camera.unproject(new Vector3(Gdx.input.getX(), 0, 0)).x > player.position.x) {
			player.facesRight = true;
			rendererBatch.draw(frame, player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);
			rendererBatch.draw(frame, player.leftPosition.x, player.position.y, Player.WIDTH, Player.HEIGHT);
			rendererBatch.draw(frame, player.rightPosition.x, player.position.y, Player.WIDTH, Player.HEIGHT);
		} else {
			player.facesRight = false;
			rendererBatch.draw(frame, player.position.x + Player.WIDTH, player.position.y, -Player.WIDTH,
					Player.HEIGHT);
			rendererBatch.draw(frame, player.leftPosition.x + Player.WIDTH, player.position.y, -Player.WIDTH,
					Player.HEIGHT);
			rendererBatch.draw(frame, player.rightPosition.x + Player.WIDTH, player.position.y, -Player.WIDTH,
					Player.HEIGHT);
		}
		rendererBatch.end();
	}
	
	private void renderGun(float deltaTime, Player player) { // USE THIS IF CAMERA STAYS AT CONSTANT Y
		batch.begin();
		if (!player.facesRight) {
			akSprite.flip(false, true);
		}

		Vector3 projectedPlayer = camera.project(new Vector3(player.position.x, player.position.y, 0f));

		batch.draw(akSprite, projectedPlayer.x, projectedPlayer.y, 16f, 16f, akSprite.getWidth() / 10,
				akSprite.getHeight() / 10, 1f, 1f, player.lookAngle);

		// draw gun on left
		float projectedGunLeft = camera.project(new Vector3(
				camera.unproject(new Vector3((Gdx.graphics.getWidth() / 2) - 18, 0, 0)).x - level.mapWidth, 0, 0)).x;
		batch.draw(akSprite, projectedGunLeft, projectedPlayer.y, 16f, 16f, akSprite.getWidth() / 10,
				akSprite.getHeight() / 10, 1f, 1f, player.lookAngle);

		// draw gun on right
		float projectedGunRight = camera.project(new Vector3(
				camera.unproject(new Vector3((Gdx.graphics.getWidth() / 2) - 18, 0, 0)).x + level.mapWidth, 0, 0)).x;
		batch.draw(akSprite, projectedGunRight, projectedPlayer.y, 16f, 16f, akSprite.getWidth() / 10,
				akSprite.getHeight() / 10, 1f, 1f, player.lookAngle);

		batch.end();
		if (!player.facesRight) {
			akSprite.flip(false, true);
		}
	}

	private void renderCrosshair(float deltaTime) {
		// Gdx.input.setCursorCatched(true);
		shapeRenderer.begin(ShapeType.Line);
		shapeRenderer.setColor(Color.RED);
		Vector3 literalDest = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(literalDest);
		shapeRenderer.line(literalDest.x + 0.5f, literalDest.y, literalDest.x - 0.5f, literalDest.y);
		shapeRenderer.line(literalDest.x, literalDest.y + 0.5f, literalDest.x, literalDest.y - 0.5f);
		shapeRenderer.end();
	}
	
	private void renderHud(float deltaTime) {
		batch.begin();
		font.draw(batch, "Health: " + (player.health / Player.MAX_HEALTH) * 100, 3, Gdx.graphics.getHeight() - 20);
		batch.end();
	}

	@Override
	public void dispose() {
	}
}