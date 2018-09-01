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
	}
	
	public static class WorldDataResponse {
		public Array<PlayerDataRequest> playerDataRequests = new Array<PlayerDataRequest>();
	}

	public static class PlayerDataRequest {
		public String playerId;
		public Vector2 position;
		public Vector2 velocity;
		
		public PlayerDataRequest() {}
		
		public PlayerDataRequest(String playerId, Vector2 position, Vector2 velocity) {
			this.playerId = playerId;
			this.position = position;
			this.velocity = velocity;
		}
	}

	public static class ShotDataRequest {
		public Player player;
	}

	public class BetterListener extends Listener {
		public Player player;
		public Array<Player> players;
		public Array<Shot> shots;

		public BetterListener(Player player, Array<Player> players, Array<Shot> shots) {
			super();
			this.player = player;
			this.players = players;
			this.shots = shots;
		}
	}

	public Network(boolean isHost, Player player, Array<Player> players, Array<Shot> shots) throws IOException {
		if (isHost) {
			server = new Server();
			registerServerClasses(server);
			server.start();
			server.bind(54545, 54745);

			server.addListener(new BetterListener(player, players, shots) {
				public void received(Connection connection, Object object) {
					if (object instanceof PlayerConnectionRequest) {
						System.out.println("=== server recieved PlayerConnectionRequest");
						PlayerConnectionResponse response = new PlayerConnectionResponse();
						response.id = UUID.randomUUID().toString();
						players.add(new Player(response.id, new Vector2(50, 50)));
						connection.sendTCP(response);
					} else if (object instanceof PlayerDataRequest) {
//						System.out.println("=== server recieved PlayerDataRequest");
						PlayerDataRequest request = (PlayerDataRequest) object;
						WorldDataResponse response = new WorldDataResponse();
						// TODO: loop this different, or use a map maybe?
						for (Player otherPlayer : players) {
							if (otherPlayer.id.equals(request.playerId) && !otherPlayer.id.equals(player.id)) {
								// TODO: interpolate this or something?
								otherPlayer.velocity.x = request.velocity.x;
								otherPlayer.velocity.y = request.velocity.y;
								otherPlayer.position.x = request.position.x;
								otherPlayer.position.y = request.position.y;
								break;
							}
							
							response.playerDataRequests.add(new PlayerDataRequest(otherPlayer.id, otherPlayer.position, otherPlayer.velocity)); 
						}
					} else if (object instanceof ShotDataRequest) {
						System.out.println("=== server recieved ShotDataRequest");
						// TODO: this
					}
				}
			});
		}

		client = new Client();
		registerClientClasses(client);
		client.start();
		client.connect(5000, "127.0.0.1", 54545, 54745);

		client.addListener(new BetterListener(player, players, shots) {
			public void received(Connection connection, Object object) {
				if (object instanceof PlayerConnectionResponse) {
					PlayerConnectionResponse response = (PlayerConnectionResponse) object;
					System.out.println("=== client recieved PlayerConnectionResponse");
					player.id = response.id.toString();
					player.position.x = 50;
					player.position.y = 50;
				} else if (object instanceof WorldDataResponse) {
					WorldDataResponse response = (WorldDataResponse) object;
//					System.out.println("=== client recieved WorldDataResponse");
					
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
		kryo.register(ShotDataRequest.class);
		kryo.register(WorldDataResponse.class);
		kryo.register(Vector2.class);
	}

	private void registerServerClasses(Server server) {
		Kryo kryo = server.getKryo();
		kryo.register(PlayerConnectionRequest.class);
		kryo.register(PlayerConnectionResponse.class);
		kryo.register(PlayerDataRequest.class);
		kryo.register(ShotDataRequest.class);
		kryo.register(WorldDataResponse.class);
		kryo.register(Vector2.class);
	}

}
