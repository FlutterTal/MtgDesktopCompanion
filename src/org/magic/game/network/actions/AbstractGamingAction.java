package org.magic.game.network.actions;

import java.io.Serializable;

public abstract class AbstractGamingAction implements Serializable {

	public static enum ACTIONS {JOIN, REQUEST_PLAY,RESPONSE,CHANGE_DECK,SPEAK,LIST_PLAYER,CHANGE_STATUS};
	
	
	private ACTIONS act;
	
	
	public ACTIONS getAct() {
		return act;
	}
	
	public void setAct(ACTIONS act) {
		this.act = act;
	}
	
}