package com.libgdx.game.desktop;

import java.util.Arrays;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.libgdx.game.LibGdxGame;

public class DesktopLauncher {
	public static void main(String[] args) {
		System.out.println(Arrays.asList(args));
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
		new LwjglApplication(new LibGdxGame(args), config);
	}
}