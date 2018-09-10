package com.libgdx.game.desktop;

import java.util.Arrays;

import org.mockito.Mockito;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Array;
import com.libgdx.game.Game;

public class DesktopLauncher {
	public static void main(String[] args) {
		System.out.println(Arrays.asList(args));
		
		if(Array.with(args).contains("server", false)) {
			System.out.println("$$$$$$ STARTING SERVER");
			Gdx.gl20 = Mockito.mock(GL20.class);
			Gdx.gl = Gdx.gl20;
			HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
	        new HeadlessApplication(new Game(args, true), config);
		}
		
		System.out.println("$$$$$$ STARTING CLIENT");
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Game";
//		System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
//		config.width = LwjglApplicationConfiguration.getDesktopDisplayMode().width;
//		config.height = LwjglApplicationConfiguration.getDesktopDisplayMode().height;
		config.width = 960;
		config.height = 540;
		config.vSyncEnabled = false; // Setting to false disables vertical sync
		config.foregroundFPS = 240; // Setting to 0 disables foreground fps throttling
		config.backgroundFPS = 240; // Setting to 0 disables background fps throttling
		new LwjglApplication(new Game(args, false), config);
	}
}