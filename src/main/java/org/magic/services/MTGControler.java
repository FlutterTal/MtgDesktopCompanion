package org.magic.services;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.log4j.Logger;
import org.magic.api.beans.EnumCondition;
import org.magic.api.beans.MTGNotification;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.Wallpaper;
import org.magic.api.interfaces.MTGCardsExport;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGDao;
import org.magic.api.interfaces.MTGNotifier;
import org.magic.api.interfaces.MTGPlugin;
import org.magic.api.interfaces.MTGServer;
import org.magic.game.model.Player;
import org.magic.services.extra.CurrencyConverter;
import org.magic.services.extra.KeyWordProvider;
import org.magic.services.extra.LookAndFeelProvider;
import org.magic.tools.ImageUtils;

public class MTGControler {

	private static MTGControler inst;
	private KeyWordProvider keyWordManager;
	private XMLConfiguration config;
	private FileBasedConfigurationBuilder<XMLConfiguration> builder;
	private LanguageService langService;
	private CurrencyConverter currencyService;
	private LookAndFeelProvider lafService;
	private Logger logger = MTGLogger.getLogger(this.getClass());
	private MTGNotifier notifier;

	private MTGControler() {
		
		File conf = new File(MTGConstants.CONF_DIR, MTGConstants.CONF_FILENAME);
		if (!conf.exists())
			try {
				logger.info(conf+" file doesn't exist. creating one from default file");
				FileUtils.copyURLToFile(getClass().getResource("/default-conf.xml"),conf);
				logger.info("conf file created");
			} catch (IOException e1) {
				logger.error(e1);
			}

		Parameters params = new Parameters();
		builder = new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class).configure(params.xml()
				.setFile(conf).setSchemaValidation(false)
				.setValidating(false).setEncoding(MTGConstants.DEFAULT_ENCODING).setExpressionEngine(new XPathExpressionEngine()));

		try {
			config = builder.getConfiguration();
			
			PluginRegistry.inst().setConfig(config);
			
			keyWordManager = new KeyWordProvider();
			langService = new LanguageService();
			langService.changeLocal(getLocale());
			currencyService = new CurrencyConverter(get("currencylayer-access-api"));
			
		} catch (Exception e) {
			logger.error("error init", e);
		}
	}
	
	public void removeCard(MagicCard mc , MagicCollection collection) throws SQLException
	{
		MTGControler.getInstance().getEnabled(MTGDao.class).removeCard(mc, collection);
		if(MTGControler.getInstance().get("collections/stockAutoDelete").equals("true"))
		{ 
			MTGControler.getInstance().getEnabled(MTGDao.class).listStocks(mc, collection).forEach(st->{
				try{
					MTGControler.getInstance().getEnabled(MTGDao.class).deleteStock(st);	
				}
				catch(Exception e)
				{
					logger.error(e);
				}
			});
		}
		
	}
	
	public void saveCard(MagicCard mc , MagicCollection collection) throws SQLException
	{
		MTGControler.getInstance().getEnabled(MTGDao.class).saveCard(mc, collection);
		if(MTGControler.getInstance().get("collections/stockAutoAdd").equals("true"))
		{ 
			MagicCardStock st = getDefaultStock();
			st.setMagicCard(mc);
			st.setMagicCollection(collection);
			MTGControler.getInstance().getEnabled(MTGDao.class).saveOrUpdateStock(st);
		}
	}
	

	private MagicCardStock getDefaultStock() {
		String defaultBool = "false";
		MagicCardStock st = new MagicCardStock();
					   st.setSigned(Boolean.parseBoolean(get("collections/defaultStock/signed",defaultBool)));
					   st.setAltered(Boolean.parseBoolean(get("collections/defaultStock/altered",defaultBool)));
					   st.setFoil(Boolean.parseBoolean(get("collections/defaultStock/foil",defaultBool)));
					   st.setOversize(Boolean.parseBoolean(get("collections/defaultStock/oversized",defaultBool)));
					   st.setLanguage(get("collections/defaultStock/language","English"));
					   st.setCondition(EnumCondition.valueOf(get("collections/defaultStock/condition","NEAR_MINT")));
					   st.setQte(Integer.parseInt(get("collections/defaultStock/qty","1")));
		return st;
	}

	public CurrencyConverter getCurrencyService() {
		return currencyService;
	}

	public KeyWordProvider getKeyWordManager() {
		return keyWordManager;
	}
	
	public LookAndFeelProvider getLafService() {
		if (lafService != null) {
			return lafService;
		} else {
			lafService = new LookAndFeelProvider();
			return lafService;
		}
	}

	public LanguageService getLangService() {
		if (langService != null) {
			return langService;
		} else {
			langService = new LanguageService(Locale.ENGLISH);
			return langService;
		}
	}

	public static MTGControler getInstance() {
		if (inst == null)
			inst = new MTGControler();
		return inst;
	}

	public Dimension getCardsGameDimension() {
		int w = Integer.parseInt(get("/game/cards/card-width"));
		int h = Integer.parseInt(get("/game/cards/card-height"));
		return new Dimension(w, h);
	}

	public <T extends MTGPlugin> void addProperty(String path, Class<T> classname) {
		String[] k = path.split("/");

		String root = k[1];
		String elem = k[2];
		try {
			/*if(config.containsKey("/"+root))
			{
				config.addProperty(root+"/"+elem+"/class",classname.getName());
				config.addProperty(root+"/"+elem+"/enable",true);
			}
			else*/
				config.addProperty("/" + root + " " + elem + "/class", classname.getName());
				setProperty(classname.getDeclaredConstructor().newInstance(), false);
			
			logger.debug("add module " + path + " " + classname.getName());
				
		} catch (IllegalArgumentException e ) {
			logger.error("Error inserting : " + path + " for " + classname ,e);
		}
		catch( InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e)
		{
			logger.error("Error loading :"+ classname ,e);
		}
	}
	
	

	public Dimension getPictureProviderDimension() {
		int w = Integer.parseInt(get("/card-pictures-dimension/width"));
		int h = Integer.parseInt(get("/card-pictures-dimension/height"));
		return new Dimension(w, h);
	}

	public void setProperty(Object k, Object c) {
		try {
			String path = "";
			logger.debug("set " + k + " to " + c);

			if(k instanceof MTGPlugin){
				path = PluginRegistry.inst().getEntryFor(k).getXpath()+"[class='" + k.getClass().getName() + "']/enable";
				logger.trace(path);
			}
			else {
				path = k.toString();
			}

			config.setProperty(path, c);
			builder.save();
		} catch (Exception e) {
			logger.error("Error saving "+k+"="+ c, e);
		}
	}
	
	
	public MagicCard switchEditions(MagicCard mc, MagicEdition ed)
	{
		try {
			return getEnabled(MTGCardsProvider.class).searchCardByName(mc.getName(), ed, true).get(0);
		} catch (IOException e) {
			logger.error(mc +" is not found in " + ed);
			return mc;
		}
	}

	
	public Locale getLocale() {
		try {
			return LocaleUtils.toLocale(config.getString("locale"));
		} catch (Exception e) {
			logger.error("Could not load " + config.getString("locale"));
			return langService.getDefault();
		}
	}

	
	
	public String get(String prop, String defaut) {
		return config.getString(prop, defaut);
	}


	public String get(String prop) {
		return get(prop, "");
	}

	public Player getProfilPlayer() {
		Player p = new Player();
		p.setName(config.getString("/game/player-profil/name"));

		String url = config.getString("/game/player-profil/avatar");
		try {
			p.setIcon(ImageIO.read(new File(url)));
		} catch (Exception e) {
			logger.error("error loading icon player " + p + " "+e);
		}
		return p;
	}


	public boolean updateConfigMods() {
		return PluginRegistry.inst().updateConfigWithNewModule();
	}
	


	public boolean isRunning(MTGServer server) {
		for (MTGServer serv : listEnabled(MTGServer.class))
			if (serv.getName().equals(server.getName()))
				return serv.isAlive();

		return false;
	}

	
	public MTGCardsExport getAbstractExporterFromExt(File f) {
		String ext = FilenameUtils.getExtension(f.getAbsolutePath());

		for (MTGCardsExport ace : getPlugins(MTGCardsExport.class)) {
			if (ace.getFileExtension().endsWith(ext))
				return ace;
		}
		return null;
	}

	
	public void saveWallpaper(Wallpaper p) throws IOException {
		if (!MTGConstants.MTG_WALLPAPER_DIRECTORY.exists())
			MTGConstants.MTG_WALLPAPER_DIRECTORY.mkdir();

		ImageUtils.saveImage(p.getPicture(),
				new File(MTGConstants.MTG_WALLPAPER_DIRECTORY, p.getName() + "." + p.getFormat()), p.getFormat());

	}
	

	@SuppressWarnings("unchecked")
	public <T extends MTGPlugin> T getPlugin(String name,Class<T> type) {
		return PluginRegistry.inst().getPlugin(name,type);
	}
	
	
	public void notify(MTGNotification notif)
	{
		try {
			if(notifier==null)
				notifier=getPlugin(MTGConstants.DEFAULT_NOTIFIER_NAME, MTGNotifier.class);
			
			notifier.send(notif);
		} catch (IOException e) {
			logger.error(notif.getMessage());
		}
	}
	

	
	public <T extends MTGPlugin> List<T> getPlugins(Class<T> t)
	{
		return PluginRegistry.inst().listPlugins(t);
	}
	
	public <T extends MTGPlugin> T getEnabled(Class<T> t)
	{
		return PluginRegistry.inst().getEnabledPlugins(t);
	}
	
	public <T extends MTGPlugin> List<T> listEnabled(Class<T> t)
	{
		return PluginRegistry.inst().listEnabledPlugins(t);
	}
	

}
