package org.magic.api.decksniffer.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Document;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.RetrievableDeck;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractDeckSniffer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.tools.URLTools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AetherhubDeckSniffer extends AbstractDeckSniffer {

	private static final String FORMAT = "FORMAT";
	private HttpClient httpclient;
	private Map<String,String> formats;
	private String postReqData="{\"draw\":1,\"columns\":[{\"data\":\"name\",\"name\":\"name\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false}},{\"data\":\"color\",\"name\":\"color\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false}},{\"data\":\"tags\",\"name\":\"tags\",\"searchable\":true,\"orderable\":false,\"search\":{\"value\":\"\",\"regex\":false}},{\"data\":\"likes\",\"name\":\"likes\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false}},{\"data\":\"views\",\"name\":\"views\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false}},{\"data\":\"comments\",\"name\":\"comments\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false}},{\"data\":\"updated\",\"name\":\"updated\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"7\",\"regex\":false}},{\"data\":\"updatedhidden\",\"name\":\"updatedhidden\",\"searchable\":false,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false}},{\"data\":\"popularity\",\"name\":\"popularity\",\"searchable\":false,\"orderable\":true,\"search\":{\"value\":\"\",\"regex\":false}}],\"order\":[{\"column\":8,\"dir\":\"desc\"}],\"start\":0,\"length\":50,\"search\":{\"value\":\"\",\"regex\":false}}";
	private String uriPost = "https://aetherhub.com/Meta/FetchMetaListAdv";

	public AetherhubDeckSniffer() {
		super();
		
		formats = new HashMap<>();	
		formats.put("All", "");
		formats.put("Standard", "?formatId=1");
		formats.put("Modern", "?formatId=2");
		formats.put("Commander", "?formatId=3");
		formats.put("Legacy", "?formatId=4");
		formats.put("Vintage", "?formatId=5");
		formats.put("MTG Arena", "?formatId=13");
		formats.put("Commander 1vs1", "?formatId=7");
		formats.put("Frontier", "?formatId=8");
		formats.put("Pauper", "?formatId=9");
		formats.put("Brawl", "?formatId=10");
		
		
		httpclient = HttpClients.custom().setUserAgent(MTGConstants.USER_AGENT)
				 .setRedirectStrategy(new LaxRedirectStrategy()).build();

	}

	@Override
	public String[] listFilter() {
		return formats.keySet().toArray(new String[formats.keySet().size()]);
	}

	@Override
	public MagicDeck getDeck(RetrievableDeck info) throws IOException {
		
		String uri="https://aetherhub.com/Deck/FetchDeckExport?deckId="+info.getUrl().getQuery().replace("id=","");
		
		logger.debug("get deck from " + info.getUrl());
		Document d =URLTools.extractHtml(info.getUrl().toString());
		String data = URLTools.extractAsString(uri);
		
		MagicDeck deck = new MagicDeck();
		deck.setName(info.getName());
		deck.setDescription(d.select("div.decknotes").text());
		
		boolean sideboard=false;
		data = RegExUtils.replaceAll(data,"\\\\r\\\\n","\n");
		data = RegExUtils.replaceAll(data,"\"","");
		String[] lines = data.split("\n");
		
		for(int i=1;i<lines.length;i++)
		{
			String line=lines[i].trim();
			
			if(line.startsWith("Sideboard") || line.startsWith("Maybeboard"))
			{
				sideboard=true;
			}
			else if(!StringUtils.isBlank(line))
			{
				
				Integer qte = Integer.parseInt(line.substring(0, line.indexOf(' ')));
				String cardName = line.substring(line.indexOf(' '), line.length()).trim();
				
				notify(cardName);
				if(sideboard)
					deck.getMapSideBoard().put(MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName(cardName, null, true).get(0), qte);
				else
					deck.getMap().put(MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName(cardName, null, true).get(0), qte);
			}
		}
		return deck;
	}

	@Override
	public List<RetrievableDeck> getDeckList() throws IOException {
		List<RetrievableDeck> list = new ArrayList<>();
		
		HttpPost p = new HttpPost(uriPost+formats.get(getString(FORMAT)));
				 p.addHeader("Content-Type", "application/json");
				 p.addHeader("user-agent",MTGConstants.USER_AGENT);
				 p.setEntity(new StringEntity(postReqData));
		
				 
		logger.debug("list decks at" + uriPost+formats.get(getString(FORMAT)));		 
				 
		HttpResponse resp = httpclient.execute(p);
		String ret = EntityUtils.toString(resp.getEntity());
		logger.trace(ret);
		JsonObject el = new JsonParser().parse(ret).getAsJsonObject();
		JsonArray arr = el.get("metadecks").getAsJsonArray();
		
		for(JsonElement je : arr)
		{
			RetrievableDeck d = new RetrievableDeck();
						    d.setAuthor(je.getAsJsonObject().get("username").getAsString());
						    d.setName(je.getAsJsonObject().get("name").getAsString());
						    d.setDescription(je.getAsJsonObject().get("updated").toString());
						    
						    JsonArray colors = je.getAsJsonObject().get("color").getAsJsonArray();
						    StringBuilder temp = new StringBuilder();
						    if (colors.get(0).getAsInt() > 0) {
	                           temp.append("{W}");
	                        }
	                        if (colors.get(1).getAsInt() > 0) {
	                        	temp.append("{U}");
	                        }
	                        if (colors.get(2).getAsInt() > 0) {
	                        	temp.append("{B}");
	                        }
	                        if (colors.get(3).getAsInt() > 0) {
	                        	temp.append("{R}");
	                        }
	                        if (colors.get(4).getAsInt() > 0) {
	                        	temp.append("{G}");
	                        }
	                        d.setColor(temp.toString());
	                        
	                        try {
								d.setUrl(new URI("https://aetherhub.com/Deck/Public?id="+je.getAsJsonObject().get("id").getAsInt()));
							} catch (URISyntaxException e) {
								logger.error(e);
							}
	                       
						    list.add(d);
		}
		return list;
	}


	@Override
	public String getName() {
		return "Aetherhub";
	}

	@Override
	public STATUT getStatut() {
		return STATUT.DEV;
	}

	@Override
	public void initDefault() {
		setProperty(FORMAT, "All");	
		
	}

	@Override
	public String getVersion() {
		return "0.3";
	}



}
