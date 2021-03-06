package org.magic.api.exports.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractCardExport;
import org.magic.services.MTGControler;
import org.magic.tools.UITools;

public class Apprentice2DeckExport extends AbstractCardExport {

	@Override
	public STATUT getStatut() {
		return STATUT.DEV;
	}

	@Override
	public String getName() {
		return "Apprentice";
	}

	@Override
	public String getFileExtension() {
		return ".dec";
	}


	@Override
	public void export(MagicDeck deck, File dest) throws IOException {
		StringBuilder temp = new StringBuilder();
		int c = 0;
		for (MagicCard mc : deck.getMap().keySet()) {
			temp.append("MD,");
			temp.append(deck.getMap().get(mc) + ",");
			temp.append("\"" + mc.getName() + "\",");
			temp.append(mc.getCurrentSet().getId());
			temp.append("\n");
			notify(c);
		}
		for (MagicCard mc : deck.getMapSideBoard().keySet()) {
			temp.append("SB,");
			temp.append(deck.getMapSideBoard().get(mc) + ",");
			temp.append("\"" + mc.getName() + "\",");
			temp.append(mc.getCurrentSet().getId());
			temp.append("\n");
			notify(c);
		}

		try (FileWriter out = new FileWriter(dest)) {
			out.write(temp.toString());
		}

	}

	@Override
	public MagicDeck importDeck(String f,String name) throws IOException {
			MagicDeck deck = new MagicDeck();
			deck.setName(name);

			int ecart = 0;
			
			int count=0;
			for(String line : UITools.stringLineSplit(f)) 
			{
				line = line.trim();
				if (!line.startsWith("//")) {
					String[] elements = line.split(getString("SEPARATOR"));
					MagicEdition ed = null;
					try {
						ed = new MagicEdition();
						ed.setId(elements[3]);
					} catch (Exception e) {
						ed = null;
						ecart = 1;
					}
					String cname = elements[2 - ecart].replaceAll("\"", "");
					MagicCard mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName( cname, ed, true).get(0);
					Integer qte = Integer.parseInt(elements[1 - ecart]);
					notify(mc);
					
					if (line.startsWith("SB"))
						deck.getMapSideBoard().put(mc, qte);
					else
						deck.getMap().put(mc, qte);
					
					notify(count++);

				}
			}
			return deck;
		

	}


	@Override
	public void initDefault() {
		setProperty("VERSION", "2.0");
		setProperty("SEPARATOR", ",");

	}

	@Override
	public String getVersion() {
		return getProperty("VERSION", "2.0");
	}
}
