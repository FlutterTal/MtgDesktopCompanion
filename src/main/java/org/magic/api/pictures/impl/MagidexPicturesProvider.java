package org.magic.api.pictures.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGPicturesCache;
import org.magic.api.interfaces.abstracts.AbstractPicturesProvider;
import org.magic.services.MTGControler;
import org.magic.tools.URLTools;

public class MagidexPicturesProvider extends AbstractPicturesProvider {

	@Override
	public STATUT getStatut() {
		return STATUT.DEV;
	}



	@Override
	public BufferedImage getPicture(MagicCard mc, MagicEdition me) throws IOException {

		String cardName = mc.getName().toLowerCase();
		MagicEdition edition = me;
		if (me == null)
			edition = mc.getCurrentSet();

		if (MTGControler.getInstance().getEnabled(MTGPicturesCache.class).getPic(mc, edition) != null) {
			logger.trace("cached " + mc + "(" + edition + ") found");
			return resizeCard(MTGControler.getInstance().getEnabled(MTGPicturesCache.class).getPic(mc, edition), newW, newH);
		}

		String cardSet = edition.getId();

		if (mc.getName().contains("//")) {
			cardName = cardName.replaceAll("//", "");
		}

		// This will properly escape the url
		URI uri;
		try {
			uri = new URI("http", "magidex.com", "/extstatic/card/" + cardSet.toUpperCase() + '/' + cardName + ".jpg",
					null, null);
		} catch (URISyntaxException e1) {
			throw new IOException(e1);
		}

		logger.debug("get card from " + uri.toURL());
		HttpURLConnection connection = URLTools.openConnection(uri.toURL());
	
		try {
			BufferedImage bufferedImage = ImageIO.read(connection.getInputStream());
			if (bufferedImage != null)
				MTGControler.getInstance().getEnabled(MTGPicturesCache.class).put(bufferedImage, mc, edition);
			return resizeCard(bufferedImage, newW, newH);
		} catch (Exception e) {
			logger.error(e);
			return getBackPicture();
		}
	}

	@Override
	public BufferedImage getSetLogo(String setID, String rarity) throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return "MagiDex";
	}

	@Override
	public BufferedImage extractPicture(MagicCard mc) throws IOException {
		return null;
	}

	

}
