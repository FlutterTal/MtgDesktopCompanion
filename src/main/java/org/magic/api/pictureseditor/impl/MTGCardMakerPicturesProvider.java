package org.magic.api.pictureseditor.impl;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.abstracts.AbstractPicturesEditorProvider;
import org.magic.services.MTGConstants;
import org.magic.tools.CardsPatterns;
import org.magic.tools.URLTools;

public class MTGCardMakerPicturesProvider extends AbstractPicturesEditorProvider{

	@Override
	public MOD getMode() {
		return MOD.LOCAL;
	}

	public BufferedImage generatePictureForCard(MagicCard mc, BufferedImage pic) {
		BufferedImage cadre = getPicture(mc, null);
		Graphics g = cadre.createGraphics();
		g.drawImage(pic, 35, 68, 329, 242, null);
		g.dispose();
		return cadre;
	}

	public BufferedImage getPicture(MagicCard mc, MagicEdition ed) {
		try {

			URL url = getPictureURL(mc);
			return URLTools.extractImage(url);

		} catch (Exception e) {
			logger.error("Error reading pics for " + mc, e);
			return null;
		}

	}

	public int count(String manaCost, String item) {
		int count = 0;
		String regex = CardsPatterns.MANA_PATTERN.getPattern();
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(manaCost);
		while (m.find()) {
			if (m.group().equals(item))
				count++;
		}
		return count;
	}

	private int extractColorless(String manaCost) {
		try {
			return Integer.parseInt(manaCost.replaceAll("[^0-9]", ""));
		} catch (Exception e) {
			return 0;
		}

	}
	
	private String powerorloyalty(MagicCard mc) {
		
		if(extractColorless(mc.getCost()) > 0)
			return String.valueOf(extractColorless(mc.getCost()));
		else if (mc.getCost().contains("X"))
				return "X";
						
		return "0";
	}
	

	private URL getPictureURL(MagicCard mc) throws MalformedURLException, UnsupportedEncodingException {

		String color = "colorless";
		if (!mc.getColors().isEmpty())
			color = (mc.getColors().size() > 1 ? "Gold" : mc.getColors().get(0));

		if (color.equalsIgnoreCase("colorless"))
			color = "Gold";

		if (mc.getCost() == null)
			mc.setCost("");

		return new URL(
				"http://www.mtgcardmaker.com/mcmaker/createcard.php?" + "name="
						+ URLEncoder.encode(String.valueOf(mc.getName()), MTGConstants.DEFAULT_ENCODING.displayName()) + "&color=" + color + "&mana_r="
						+ (mc.getCost().contains("{R}") ? String.valueOf(count(mc.getCost(), "{R}")) : "0") + "&mana_u="
						+ (mc.getCost().contains("{U}") ? String.valueOf(count(mc.getCost(), "{U}")) : "0") + "&mana_g="
						+ (mc.getCost().contains("{G}") ? String.valueOf(count(mc.getCost(), "{G}")) : "0") + "&mana_b="
						+ (mc.getCost().contains("{B}") ? String.valueOf(count(mc.getCost(), "{B}")) : "0") + "&mana_w="
						+ (mc.getCost().contains("{W}") ? String.valueOf(count(mc.getCost(), "{W}")) : "0")
						+ "&mana_colorless="
						+ powerorloyalty(mc)
						+ "&picture=" + "&supertype=" + "&cardtype=" + URLEncoder.encode(mc.getFullType(), MTGConstants.DEFAULT_ENCODING.displayName())
						+ "&subtype=" + "&expansion=" + "&rarity=" + mc.getRarity() + "&cardtext="
						+ URLEncoder.encode(
								String.valueOf(mc.getText()), MTGConstants.DEFAULT_ENCODING.displayName())
						+ "&power=" + mc.getPower() + "&toughness=" + mc.getToughness() + "&artist="
						+ URLEncoder.encode(String.valueOf(mc.getArtist()), MTGConstants.DEFAULT_ENCODING.displayName()) + "&bottom="
						+ URLEncoder.encode("\u2122 & \u00A9 1993-" + Calendar.getInstance().get(Calendar.YEAR)
								+ " Wizards of the Coast LLC", MTGConstants.DEFAULT_ENCODING.displayName())
						+ "&set1=" + "&set2=" + "&setname=");

	}



	@Override
	public String getName() {
		return "MTGCard Maker";
	}

	@Override
	public void setFoil(Boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextSize(int size) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCenter(boolean center) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setColorIndicator(boolean selected) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColorAccentuation(String c) {
		// TODO Auto-generated method stub
		
	}


}
