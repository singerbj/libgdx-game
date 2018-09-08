package network_entities;

import com.libgdx.entities.Player;
import com.libgdx.helpers.PlayerState;

public class NetworkPlayer {	
	public String id;
	public float positionX = 0f;
	public float positionY = 0f;
	public float velocityX = 0f;
	public float velocityY = 0f;
	public PlayerState state = null;
	public boolean facesRight = true;
	public long gunId = 0;
	public float lookAngle = 0;
	
	public NetworkPlayer() {}
	public NetworkPlayer(Player player) {
		this.id = player.id;
		this.positionX = player.position.x;
		this.positionY = player.position.y;
		this.velocityX = player.velocity.x;
		this.velocityY = player.velocity.y;
		this.state = player.state;
		this.facesRight = player.facesRight;
		this.gunId = player.gun.GUN_ID;
		this.lookAngle = player.lookAngle;
	}
}
