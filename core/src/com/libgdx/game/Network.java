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
	
	public static class PlayerConnectionRequest {}
	public static class PlayerConnectionResponse {
		public static String id;
    }
	public static class PlayerDataRequest {
		public static String playerId;
        public static Vector2 position;
        public static Vector2 velocity;
    }
	public static class ShotDataRequest {
        public static Player player;
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
		if(isHost) {
			server = new Server();
			registerServerClasses(server);
			server.start();
			server.bind(54545, 54745);
			
			server.addListener(new BetterListener(player, players, shots) {
			    public void received (Connection connection, Object object) {
				   if(object instanceof PlayerConnectionRequest){
					   PlayerConnectionResponse response = new PlayerConnectionResponse();
			           response.id = UUID.randomUUID().toString();
			           players.add(new Player(response.id));
			           connection.sendTCP(response);
			       } else if (object instanceof PlayerDataRequest) {
			    	   PlayerDataRequest request = (PlayerDataRequest) object;
			    	   //TODO: loop this different, or use a map maybe?
			    	   for(Player otherPlayer : players){
			    		   if(otherPlayer.id.equals(request.playerId) && !otherPlayer.id.equals(player.id)) {
			    			   //TODO: interpolate this or something?
			    			   otherPlayer.velocity.x = request.velocity.x;
			    			   otherPlayer.velocity.y = request.velocity.y;
			    			   otherPlayer.position.x = request.position.x;
			    			   otherPlayer.position.y = request.position.y;
			    			   break;
			    		   }
			    	   }
			       } else if(object instanceof ShotDataRequest){
			    	   //TODO: this
			       }
			    }
		    });
		}
		
	    client = new Client();
	    registerClientClasses(client);
	    client.start();
	    client.connect(5000, "127.0.0.1", 54545, 54745);
	    
	    client.addListener(new BetterListener(player, players, shots) {
		    public void received (Connection connection, Object object) {
			   if(object instanceof PlayerConnectionResponse){
				 player = new Player(UUID.randomUUID().toString());
		       }
		    }
	    });
	    
	    client.sendTCP(new PlayerConnectionRequest());
	}

	private void registerClientClasses(Client client) {
		Kryo kryo = client.getKryo();
	    kryo.register(PlayerConnectionRequest.class);
	    kryo.register(PlayerConnectionResponse.class);
	    kryo.register(PlayerDataRequest.class);
	    kryo.register(ShotDataRequest.class);
	}

	private void registerServerClasses(Server server) {
		Kryo kryo = server.getKryo();
	    kryo.register(PlayerConnectionRequest.class);
	    kryo.register(PlayerConnectionResponse.class);
	    kryo.register(PlayerDataRequest.class);
	    kryo.register(ShotDataRequest.class);
		
	}

}
