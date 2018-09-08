package com.libgdx.game;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.libgdx.entities.Player;
import com.libgdx.helpers.PlayerState;
import com.libgdx.helpers.Util;

import network_entities.NetworkPlayer;

public class Network {
	public Server server;
	public Client client;

	public static class PlayerConnectionRequest {}

	public static class PlayerConnectionResponse {
		public NetworkPlayer networkPlayer;
		
		public PlayerConnectionResponse() {}
		public PlayerConnectionResponse(Player player) {
			this.networkPlayer = new NetworkPlayer(player);
		}
	}

	public static class WorldDataResponse {
		public HashMap<String, NetworkPlayer> networkPlayers = new HashMap<String, NetworkPlayer>();
		
		public WorldDataResponse() {}
		public WorldDataResponse(HashMap<String, Player> players) {
			Set<String> playerKeys = players.keySet();
			for(String playerKey : playerKeys) {
				networkPlayers.put(playerKey, new NetworkPlayer(players.get(playerKey)));
			}
		}
	}

	public static class PlayerDataRequest {
		public NetworkPlayer networkPlayer;

		public PlayerDataRequest() {}
		public PlayerDataRequest(Player player) {
			this.networkPlayer = new NetworkPlayer(player);
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

						PlayerConnectionResponse response = new PlayerConnectionResponse(newPlayer);
						connection.sendTCP(response);
					} else if (object instanceof PlayerDataRequest) {
//						System.out.println("=*= server recieved PlayerDataRequest");
						PlayerDataRequest request = (PlayerDataRequest) object;
						Player otherPlayer = players.get(request.networkPlayer.id);
						if (otherPlayer != null && !otherPlayer.id.equals(player.id)) {
							// TODO: interpolate this or something?
							otherPlayer.velocity.x = request.networkPlayer.velocityX;
							otherPlayer.velocity.y = request.networkPlayer.velocityY;
							otherPlayer.position.x = request.networkPlayer.positionX;
							otherPlayer.position.y = request.networkPlayer.positionY;
							otherPlayer.state = request.networkPlayer.state;
							otherPlayer.facesRight = request.networkPlayer.facesRight;
//							otherPlayer.gunId = request.networkPlayer.gun.GUN_ID; // TODO: fix this by making a map of all the weapons
							otherPlayer.lookAngle = request.networkPlayer.lookAngle;
						}
					} 
				}
			});
		} else {

			client = new Client();
			registerClasses(client);
			client.start();
			client.connect(5000, "127.0.0.1", 54545, 54745);
	
			client.addListener(new BetterListener(localPlayer, players, shots) {
				public void received(Connection connection, Object object) {
					if (object instanceof PlayerConnectionResponse) {
						PlayerConnectionResponse response = (PlayerConnectionResponse) object;
						System.out.println("=*= client recieved PlayerConnectionResponse");
						localPlayer.id = response.networkPlayer.id;
						localPlayer.position.x = response.networkPlayer.positionX;
						localPlayer.position.y = response.networkPlayer.positionY;
					} else if (object instanceof WorldDataResponse) {
						if(player.id != null) {
							WorldDataResponse response = (WorldDataResponse) object;
	//						System.out.println("=*= client recieved WorldDataResponse");
							NetworkPlayer networkPlayerFromMap;
							Set<String> keys = response.networkPlayers.keySet();
							for (String key : keys) {
								networkPlayerFromMap = response.networkPlayers.get(key);
								if (!networkPlayerFromMap.id.equals(player.id)) {
									// TODO: interpolate this or something?
									Player playerToUpdate = players.get(networkPlayerFromMap.id);
									if(playerToUpdate != null) {
	//									System.out.println("=*= client updated player in map");
										playerToUpdate.velocity.x = networkPlayerFromMap.velocityX;
										playerToUpdate.velocity.y = networkPlayerFromMap.velocityY;
										playerToUpdate.position.x = networkPlayerFromMap.positionX;
										playerToUpdate.position.y = networkPlayerFromMap.positionY;
										playerToUpdate.state = networkPlayerFromMap.state;
										playerToUpdate.facesRight = networkPlayerFromMap.facesRight;
//										playerToUpdate.gunId = networkPlayerFromMap.gun.GUN_ID; // TODO: fix this by making a map of all the weapons
										playerToUpdate.lookAngle = networkPlayerFromMap.lookAngle;
									} else {
	//									System.out.println("=*= client added new player to map");
										Player newPlayer = new Player(networkPlayerFromMap);
										players.put(newPlayer.id, newPlayer);
									}
								}
							}
						}
					}
				}
			});
	
			client.sendTCP(new PlayerConnectionRequest());
		}
	}

	public void sendPlayerData(Player player) {
		client.sendTCP(new PlayerDataRequest(player));
	}
	
	public void sendWorldData(HashMap<String, Player> players) {
		if(server != null) {
			WorldDataResponse worldDataRespose = new WorldDataResponse(players);
			server.sendToAllTCP(worldDataRespose);
		}
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
		kryo.register(NetworkPlayer.class);
		kryo.register(PlayerState.class);
		kryo.register(HashMap.class);
	}
}
