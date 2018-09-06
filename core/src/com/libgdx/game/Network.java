package com.libgdx.game;

import java.io.IOException;
import java.util.UUID;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.libgdx.entities.Player;

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
		public Array<PlayerDataRequest> playerDataRequests = new Array<PlayerDataRequest>();
//		public Array<PlayerDataRequest> playerDataRequests = new Array<PlayerDataRequest>();
	}

	public static class PlayerDataRequest {
		public String playerId;
		public Vector2 position = new Vector2();
		public Vector2 velocity;

		public PlayerDataRequest() {
		}

		public PlayerDataRequest(String playerId, Vector2 position, Vector2 velocity) {
			this.playerId = playerId;
			this.position = position;
			this.velocity = velocity;
		}
	}

	public class BetterListener extends Listener {
		public Player player;
		public Array<Player> players;
		public Array<Shot> shots;

		public BetterListener(Player localPlayer, Array<Player> players, Array<Shot> shots) {
			super();
			this.player = localPlayer;
			this.players = players;
			this.shots = shots;
		}
	}

	public Network(boolean isHost, Player localPlayer, Array<Player> players, Array<Shot> shots) throws IOException {
		if (isHost) {
			server = new Server();
			registerServerClasses(server);
			server.start();
			server.bind(54545, 54745);

			server.addListener(new BetterListener(localPlayer, players, shots) {
				public void received(Connection connection, Object object) {
					if (object instanceof PlayerConnectionRequest) {
						System.out.println("=*= server recieved PlayerConnectionRequest");
						Player newPlayer = new Player(new Vector2(50, 50));
						players.add(newPlayer);		
						System.out.println("=*=" + newPlayer.id);

						PlayerConnectionResponse response = new PlayerConnectionResponse();
						response.id = newPlayer.id;
						response.position.x = newPlayer.position.x;
						response.position.y = newPlayer.position.y;
						connection.sendTCP(response);
					} else if (object instanceof PlayerDataRequest) {
						// System.out.println("=*= server recieved PlayerDataRequest");
						PlayerDataRequest request = (PlayerDataRequest) object;
						WorldDataResponse response = new WorldDataResponse();
						// TODO: loop this different, or use a map maybe?
						Player otherPlayer;
						for (int i = 0; i < players.size; i++) {
							otherPlayer = players.get(i);
							if (otherPlayer.id.equals(request.playerId)){ // && !otherPlayer.id.equals(player.id)) {
								// TODO: interpolate this or something?
								otherPlayer.velocity.x = request.velocity.x;
								otherPlayer.velocity.y = request.velocity.y;
								otherPlayer.position.x = request.position.x;
								otherPlayer.position.y = request.position.y;
								break;
							}
							response.playerDataRequests.add(new PlayerDataRequest(otherPlayer.id, otherPlayer.position, otherPlayer.velocity));
						}
						connection.sendTCP(response);
					} 
//					else if (object instanceof ShotDataRequest) {
//						System.out.println("=*= server recieved ShotDataRequest");
//						// TODO: this
//					}
				}
			});
		}

		client = new Client();
		registerClientClasses(client);
		client.start();
		client.connect(5000, "127.0.0.1", 54545, 54745);

		client.addListener(new BetterListener(localPlayer, players, shots) {
			public void received(Connection connection, Object object) {
				if (object instanceof PlayerConnectionResponse) {
					PlayerConnectionResponse response = (PlayerConnectionResponse) object;
					System.out.println("=*= client recieved PlayerConnectionResponse");
//					player = new Player(response.id.toString(), response.position);
					player.id = response.id.toString();
					player.position.x = response.position.x;
					player.position.y = response.position.y;
				} else if (object instanceof WorldDataResponse) {
					WorldDataResponse response = (WorldDataResponse) object;
//					System.out.println("=*= client recieved WorldDataResponse");
					PlayerDataRequest playerDataRequest;
					Player otherPlayer;
					boolean found;
					for (int i = 0; i < response.playerDataRequests.size; i++) {
						playerDataRequest = response.playerDataRequests.get(i);
						found = false;
						for (int j = 0; j < players.size; j++) {
							otherPlayer = players.get(j);
							if (otherPlayer.id.equals(playerDataRequest.playerId) && !otherPlayer.id.equals(player.id)) {
								// TODO: interpolate this or something?
								otherPlayer.velocity.x = playerDataRequest.velocity.x;
								otherPlayer.velocity.y = playerDataRequest.velocity.y;
								otherPlayer.position.x = playerDataRequest.position.x;
								otherPlayer.position.y = playerDataRequest.position.y;
								found = true;
								break;
							}
						}
						
						if (!found) {
							players.add(new Player(playerDataRequest.playerId, playerDataRequest.position));
						}
					}

				}
			}
		});

		client.sendTCP(new PlayerConnectionRequest());
	}

	public void sendPlayerData(String playerId, Vector2 playerPosition, Vector2 playerVelocity) {
		client.sendTCP(new PlayerDataRequest(playerId, playerPosition, playerVelocity));
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

	private void registerClientClasses(Client client) {
		Kryo kryo = client.getKryo();
		kryo.register(PlayerConnectionRequest.class);
		kryo.register(PlayerConnectionResponse.class);
		kryo.register(PlayerDataRequest.class);
		kryo.register(WorldDataResponse.class);
		kryo.register(Vector2.class);
		kryo.register(Array.class);
		kryo.register(Object[].class);
	}

	private void registerServerClasses(Server server) {
		Kryo kryo = server.getKryo();
		kryo.register(PlayerConnectionRequest.class);
		kryo.register(PlayerConnectionResponse.class);
		kryo.register(PlayerDataRequest.class);
		kryo.register(WorldDataResponse.class);
		kryo.register(Vector2.class);
		kryo.register(Array.class);
		kryo.register(Object[].class);
	}

}
