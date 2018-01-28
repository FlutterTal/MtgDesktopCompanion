package org.magic.gui.models;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.MagicPrice;
import org.magic.api.interfaces.MagicPricesProvider;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;

public class CardsPriceTableModel extends DefaultTableModel {

	
	private static final long serialVersionUID = 1L;
	private transient Logger logger = MTGLogger.getLogger(this.getClass());
	private transient List<MagicPricesProvider> providers;
	private transient List<MagicPrice> prices;
	
	
	String[] columns= new String[]{
								MTGControler.getInstance().getLangService().getCapitalize("WEBSITE"),
								MTGControler.getInstance().getLangService().getCapitalize("PRICE"),
								MTGControler.getInstance().getLangService().getCapitalize("CURRENCY"),
								MTGControler.getInstance().getLangService().getCapitalize("SELLER"),
								MTGControler.getInstance().getLangService().getCapitalize("QUALITY"),
								MTGControler.getInstance().getLangService().getCapitalize("CARD_LANGUAGE"),
								MTGControler.getInstance().getLangService().getCapitalize("URL"),
	};
	public void addPrice(MagicPrice p)
	{
		prices.add(p);
		fireTableDataChanged();
	}
	
	
	private void addPrice(MagicCard mc, MagicEdition me)
	{
		for(MagicPricesProvider prov : providers)
		{
			try {
				if(prov.isEnable())
				{
					List<MagicPrice> list = prov.getPrice(me, mc);
					
					if(list!=null && !list.isEmpty())
						prices.addAll(list);
					
					fireTableDataChanged();
				}
			} catch (Exception e) {
				logger.error("Error",e);
				
			}
		}
		fireTableDataChanged();
	}
	
	
	public void init(MagicCard mc,MagicEdition me)
	{
		prices.clear();
		addPrice(mc, me);
		
	}
	

	public CardsPriceTableModel() {
		providers = new ArrayList<>();
		prices=new ArrayList<>();
		providers=MTGControler.getInstance().getPricers();
	}
	
	public List<MagicPricesProvider> getProviders() {
		return providers;
	}
	
	
	public void setProvider(MagicPricesProvider provider)
	{
		providers.clear();
		providers.add(provider);
	}
	
	
	@Override
	public String getColumnName(int column) {
		return columns[column];
	}
	
	@Override
	public int getRowCount() {
		if(prices!=null)
			return prices.size();
		else
			return 0;
	}
	
	@Override
	public int getColumnCount() {
		return columns.length;
	}
	
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch(columnIndex)
		{
		case 0:return String.class;
		case 1 : return Double.class;
		case 2: return String.class;
		case 3 : return String.class;
		case 4 : return String.class;
		case 5 : return String.class;
		default : return URL.class;
		}
	}
	
	
	@Override
	public Object getValueAt(int row, int column) {
		try{
			
		MagicPrice mp = prices.get(row);
		
		switch(column)
		{
			case 0: return mp.getSite();
			case 1 : return mp.getValue();
			case 2: return mp.getCurrency();
			case 3 : return mp.getSeller();
			case 4 : return mp.getQuality();
			case 5 : return mp.getLanguage();
			case 6 : return mp.getUrl();
		default : return 0;
		}
		}catch(IndexOutOfBoundsException ioob)
		{
			return null;
		}
	}

	
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}


	public void clear() {
		prices.clear();
		fireTableDataChanged();
		
	}

	
	
}
