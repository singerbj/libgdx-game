package com.libgdx.entities;

public class Gun {
	public final long MS_BETWEEN_SHOT = 55; //0; //100
	public final long MAX_AMMO = 30; //999999999; //30
	public final long MS_TO_RELOAD = 2000;
	
	public long currentAmmo = MAX_AMMO;
	public long lastShotTime = 0;
	
	public boolean reloading = false;
	public long reloadCompleteTime = -99999999;
	public float reloadPerentage = 0.0f;
	
	
	public boolean fireGun (long time) {
//		System.out.println("shotTime " + time + " - lastShotTime: " + lastShotTime + " - next shot allowed at: " + ((lastShotTime + MS_BETWEEN_SHOT) - time));
		if(!reloading && time > (lastShotTime + MS_BETWEEN_SHOT) && currentAmmo > 0) {
			System.out.println("shot fired! remaining ammo: " + currentAmmo);
			lastShotTime = time;
			currentAmmo = currentAmmo - 1;
			System.out.println("SHot fired! ammo left: " + currentAmmo);
			return true;
		}
		return false;
	}
	
	public void reload (long time) {
		if(!reloading && currentAmmo < MAX_AMMO) {
			reloading = true;
			reloadCompleteTime = time + MS_TO_RELOAD;
		}
	}
	
	public void updateReloadState (long time) {
		if(reloading == true) {
			System.out.println("reloadPerentage: " + (float)(time - (reloadCompleteTime - MS_TO_RELOAD))  + " / " + (float) MS_TO_RELOAD);
			reloadPerentage = (float)(time - (reloadCompleteTime - MS_TO_RELOAD)) / (float) MS_TO_RELOAD;
			if(reloadPerentage >= 1.0) {
				reloading = false;
				currentAmmo = MAX_AMMO;
				reloadPerentage = 0;
			}
		} else {
			reloadPerentage = 0;
		}
	}
}
