package com.libgdx.helpers;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.libgdx.entities.Level;
import com.libgdx.entities.Player;
import com.libgdx.game.Shot;

public class GameGraphics {
	
	private BitmapFont font = new BitmapFont();
	private SpriteBatch batch = new SpriteBatch();
	ShapeRenderer shapeRenderer;
	public Texture gunImage = new Texture("ak47.png");
	public Sprite gunSprite = new Sprite(gunImage);	
	public Texture playerTexture = new Texture("bettersquare.png");
	public TextureRegion[] regions = TextureRegion.split(playerTexture, 18, 26)[0];
	public Animation<TextureRegion> stand = new Animation<TextureRegion>(0, regions[0]);
	public Animation<TextureRegion> jump = new Animation<TextureRegion>(0, regions[1]);
	public Animation<TextureRegion> walk = new Animation<TextureRegion>(0.15f, regions[2], regions[3], regions[4]);
	
	private DebugHelper debugHelper;
	
	public GameGraphics(OrthographicCamera camera, Array<Rectangle> debugTiles, Player player, Level level) {
		walk.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
		shapeRenderer = new ShapeRenderer();
		debugHelper = new DebugHelper(shapeRenderer, camera, debugTiles, player, level);
	}
	
	public void renderPlayers(float deltaTime, Camera camera, Level level, Player player, HashMap<String, Player> players) {
		shapeRenderer.setProjectionMatrix(camera.combined);
		renderPlayer(deltaTime, camera, level, player);
		renderGun(deltaTime, camera, level, player);
		Player otherPlayer;
		for(String key : players.keySet()) {
			otherPlayer = players.get(key);
			if(otherPlayer.id != player.id) {
				otherPlayer.updateLeftRightPositions(level.mapWidth);
				renderPlayer(deltaTime, camera, level, otherPlayer);
				renderGun(deltaTime, camera, level, otherPlayer);
			}
		}
	}
	
	public void renderPlayer(float deltaTime, Camera camera, Level level, Player player) {
		shapeRenderer.setProjectionMatrix(camera.combined);
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
	
	public void renderGun(float deltaTime, Camera camera, Level level, Player player) { // USE THIS IF CAMERA STAYS AT CONSTANT Y
		shapeRenderer.setProjectionMatrix(camera.combined);
		Batch batch = level.renderer.getBatch();
		batch.begin();
		if (!player.facesRight) {
			gunSprite.flip(false, true);
		}

		Vector3 projectedPlayer = camera.project(new Vector3(player.position.x, player.position.y, 0f));

		batch.draw(gunSprite, projectedPlayer.x, projectedPlayer.y, 16f, 16f, gunSprite.getWidth() / 10,
				gunSprite.getHeight() / 10, 1f, 1f, player.lookAngle);

		// draw gun on left
		float projectedGunLeft = camera.project(new Vector3(
				camera.unproject(new Vector3((Gdx.graphics.getWidth() / 2) - 18, 0, 0)).x - level.mapWidth, 0, 0)).x;
		batch.draw(gunSprite, projectedGunLeft, projectedPlayer.y, 16f, 16f, gunSprite.getWidth() / 10,
				gunSprite.getHeight() / 10, 1f, 1f, player.lookAngle);

		// draw gun on right
		float projectedGunRight = camera.project(new Vector3(
				camera.unproject(new Vector3((Gdx.graphics.getWidth() / 2) - 18, 0, 0)).x + level.mapWidth, 0, 0)).x;
		batch.draw(gunSprite, projectedGunRight, projectedPlayer.y, 16f, 16f, gunSprite.getWidth() / 10,
				gunSprite.getHeight() / 10, 1f, 1f, player.lookAngle);

		batch.end();
		if (!player.facesRight) {
			gunSprite.flip(false, true);
		}
	}
	
	public void renderShots(float deltaTime, Camera camera, Level level, Array<Shot> shots) {
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Line);
		for(Shot shot : shots) {
			for (float i = -(level.mapWidth * 2); i <= (level.mapWidth * 2); i += level.mapWidth) {
				shapeRenderer.line(shot.getSource().x + i, shot.getSource().y, shot.getDest().x + i,
						shot.getDest().y, new Color(255f, 255f, 255f, 0.0f),
						new Color(255f, 255f, 255f, shot.getAlphaModifier()));
			}		
		}
		shapeRenderer.end();
	}
	
	public void renderCrosshair(float deltaTime, Camera camera) {
		shapeRenderer.setProjectionMatrix(camera.combined);
		// Gdx.input.setCursorCatched(true);
		shapeRenderer.begin(ShapeType.Line);
		shapeRenderer.setColor(Color.RED);
		Vector3 literalDest = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(literalDest);
		shapeRenderer.line(literalDest.x + 0.5f, literalDest.y, literalDest.x - 0.5f, literalDest.y);
		shapeRenderer.line(literalDest.x, literalDest.y + 0.5f, literalDest.x, literalDest.y - 0.5f);
		shapeRenderer.end();
	}
	
	public void renderHud(float deltaTime, Camera camera, Player player) {
		shapeRenderer.setProjectionMatrix(camera.combined);
		batch.begin();
		font.draw(batch, "Health: " + (player.health / Player.MAX_HEALTH) * 100, 3, Gdx.graphics.getHeight() - 20);
		batch.end();
	}
	
	public void renderDebug(float deltaTime, Camera camera) {
		shapeRenderer.setProjectionMatrix(camera.combined);
		batch.begin();
		font.draw(batch, (int) Gdx.graphics.getFramesPerSecond() + " fps", 3, Gdx.graphics.getHeight() - 3);
		batch.end();
		debugHelper.renderDebug();
	}
}
