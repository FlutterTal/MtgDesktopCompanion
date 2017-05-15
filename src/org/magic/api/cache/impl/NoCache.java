package org.magic.api.cache.impl;

import java.awt.image.BufferedImage;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGPicturesCache;
import org.magic.api.interfaces.abstracts.AbstractMTGPicturesCache;
import org.magic.services.MTGControler;

public class NoCache extends AbstractMTGPicturesCache {

	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public String getName() {
		return "No Cache";
	}

	@Override
	public BufferedImage getPic(MagicCard mc,MagicEdition ed) {
		return null;
	}

	@Override
	public void put(BufferedImage im, MagicCard mc,MagicEdition ed) throws Exception {
		//Nothing to do

	}

}