package com.libgdx.game;

import java.io.IOException;
import java.util.HashMap;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.libgdx.entities.Gun;
import com.libgdx.entities.Player;
import com.libgdx.helpers.PlayerState;
import com.libgdx.helpers.Util;

public class Network {
	public Server server;
	public Client client;

	public static class PlayerConnectionRequest {
	}

	public static class PlayerConnectionResponse {
		public String id;
		public Vector2 position = new Vector2();;
	}

	public static class WorldDataResponse {
		public HashMap<String, Player> players = new HashMap<String, Player>();
	}

	public static class PlayerDataRequest {
		public Player player;

		public PlayerDataRequest() {
		}

		public PlayerDataRequest(Player player) {
			this.player = player;
		}
	}

	public class BetterListener extends Listener {
		public Player player;
		public HashMap<String, Player> players;
		public Array<Shot> shots;

		public BetterListener(Player localPlayer, HashMap<String, Player> players, Array<Shot> shots) {
			super();
			this.player = localPlayer;
			this.players = players;
			this.shots = shots;
		}
	}

	public Network(boolean isHost, final Player localPlayer, HashMap<String, Player> players, Array<Shot> shots) throws IOException {
		if (isHost) {
			server = new Server();
			registerClasses(server);
			server.start();
			server.bind(54545, 54745);

			server.addListener(new BetterListener(localPlayer, players, shots) {
				public void received(Connection connection, Object object) {
					if (object instanceof PlayerConnectionRequest) {
						System.out.println("=*= server recieved PlayerConnectionRequest");
						Player newPlayer = new Player();
						newPlayer.position.x = Util.randomInt(0, 50);
						newPlayer.position.y = 50;
						players.put(newPlayer.id, newPlayer);		
						System.out.println("=*=" + newPlayer.id);

						PlayerConnectionResponse response = new PlayerConnectionResponse();
						response.id = newPlayer.id;
						response.position.x = newPlayer.position.x;
						response.position.y = newPlayer.position.y;
						connection.sendTCP(response);
					} else if (object instanceof PlayerDataRequest) {
						System.out.println("=*= server recieved PlayerDataRequest");
						PlayerDataRequest request = (PlayerDataRequest) object;
						Player otherPlayer = players.get(request.player.id);
						if (otherPlayer != null && !otherPlayer.id.equals(player.id)) {
							// TODO: interpolate this or something?
							otherPlayer.velocity.x = request.player.velocity.x;
							otherPlayer.velocity.y = request.player.velocity.y;
							otherPlayer.position.x = request.player.position.x;
							otherPlayer.position.y = request.player.position.y;
						}
					} 
				}
			});
		}

		client = new Client();
		registerClasses(client);
		client.start();
		client.connect(5000, "127.0.0.1", 54545, 54745);

		client.addListener(new BetterListener(localPlayer, players, shots) {
			public void received(Connection connection, Object object) {
				if (object instanceof PlayerConnectionResponse) {
					PlayerConnectionResponse response = (PlayerConnectionResponse) object;
					System.out.println("=*= client recieved PlayerConnectionResponse");
					localPlayer.id = response.id.toString();
					localPlayer.position.x = response.position.x;
					localPlayer.position.y = response.position.y;
				} else if (object instanceof WorldDataResponse) {
					WorldDataResponse response = (WorldDataResponse) object;
					System.out.println("=*= client recieved WorldDataResponse");
					Player playerFromMap;
					String[] keys = (String[]) response.players.keySet().toArray();
					for (int i = 0; i < keys.length; i++) {
						playerFromMap = players.get(keys[i]);
						if (!playerFromMap.id.equals(player.id)) {
							// TODO: interpolate this or something?
							players.get(playerFromMap.id).velocity.x = playerFromMap.velocity.x;
							players.get(playerFromMap.id).velocity.y = playerFromMap.velocity.y;
							players.get(playerFromMap.id).position.x = playerFromMap.position.x;
							players.get(playerFromMap.id).position.y = playerFromMap.position.y;
						}
					}

				}
			}
		});

		client.sendTCP(new PlayerConnectionRequest());
	}

	public void sendPlayerData(Player player) {
		client.sendTCP(new PlayerDataRequest(player));
	}
	
	public void sendWorldData(Player player) {
		client.sendTCP(new PlayerDataRequest(player));
	}

	public void sendShotData() {

	}

	public void shutdownNetwork() {
		if (client != null) {
			client.stop();
			client.close();
			try {
				client.dispose();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (server != null) {
			server.stop();
			server.close();
			try {
				server.dispose();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void registerClasses(EndPoint clientOrServer) {
		Kryo kryo = clientOrServer.getKryo();
		kryo.register(PlayerConnectionRequest.class);
		kryo.register(PlayerConnectionResponse.class);
		kryo.register(PlayerDataRequest.class);
		kryo.register(WorldDataResponse.class);
		kryo.register(Vector2.class);
		kryo.register(Array.class);
		kryo.register(Object[].class);
		kryo.register(Player.class);
		kryo.register(Gun.class);
		kryo.register(PlayerState.class);
	}
}
