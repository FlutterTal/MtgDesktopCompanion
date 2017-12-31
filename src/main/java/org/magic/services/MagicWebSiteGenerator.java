package org.magic.services;

import freemarker.core.ParseException;
import freemarker.template.*;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.MagicPrice;
import org.magic.api.interfaces.MagicDAO;
import org.magic.api.interfaces.MagicPricesProvider;
import org.utils.patterns.observer.Observable;

import java.io.*;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

public class MagicWebSiteGenerator extends Observable{
	
	Template template ;
	Configuration cfg ;
	MagicDAO dao;
	private String dest;
	private List<MagicPricesProvider> pricesProvider;
	private List<MagicCollection> cols;
	Logger logger = MTGLogger.getLogger(this.getClass());
	
	public MagicWebSiteGenerator(String template,String dest) throws IOException, ClassNotFoundException, SQLException {
		cfg = new Configuration(Configuration.VERSION_2_3_27);
		cfg.setDirectoryForTemplateLoading(new File(MTGConstants.MTG_TEMPLATES_DIR+"/"+template));
		cfg.setDefaultEncoding("UTF-8");
		//cfg.setNumberFormat("#");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER );
		cfg.setObjectWrapper( new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_27).build());
		dao=MTGControler.getInstance().getEnabledDAO();
		this.dest = dest;
		FileUtils.copyDirectory(new File(MTGConstants.MTG_TEMPLATES_DIR+"/"+template), new File(dest),new FileFilter() {
			public boolean accept(File pathname) {
				if(pathname.isDirectory())
					return true;
				
				if(pathname.getName().endsWith(".html"))
					return false;
				
				return true;
			}
		});
	}
	
	//TODO optimize
	public void generate(List<MagicCollection> cols,List<MagicPricesProvider> providers) throws TemplateException, IOException, SQLException
	{
		
		this.pricesProvider=providers;
		this.cols = cols;

		Template template = cfg.getTemplate("index.html");
			Writer out = new FileWriter(Paths.get(dest, "index.htm").toFile());
		
			Map<String,List<MagicCard>> root = new HashMap<String,List<MagicCard>>();
			for(MagicCollection col : cols)
				root.put(col.getName(), dao.getCardsFromCollection(col));
			
			template.process(root, out);
		
		generateCollectionsTemplate();
	}
	
	//lister les editions disponibles
	private void generateCollectionsTemplate() throws IOException, TemplateException, SQLException
	{
		Template template = cfg.getTemplate("page-col.html");
		
		for(MagicCollection col : cols){
			Map rootEd = new HashMap<>();
				rootEd.put("cols", cols);
				rootEd.put("colName", col.getName());
				Set<MagicEdition> eds = new HashSet<MagicEdition>();
				for(MagicCard mc : dao.getCardsFromCollection(col))
				{
					eds.add(mc.getEditions().get(0));
					generateCardsTemplate(mc);
				}
				
				rootEd.put("editions",eds);
				
				
				FileWriter out = new FileWriter(Paths.get(dest,"page-col-"+col.getName()+".htm").toFile());
				template.process(rootEd, out);
				
				//for(String ed : dao.getEditionsIDFromCollection(col))
				{
					generateEditionsTemplate(eds,col);
				}
				out.close();
				
		}
	}

	//lister les cartes disponibles dans la collection
	private void generateEditionsTemplate(Set<MagicEdition> eds,MagicCollection col) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, SQLException, TemplateException
	{
		Template cardTemplate = cfg.getTemplate("page-ed.html");
		Map rootEd = new HashMap<>();
			rootEd.put("cols",cols);
			rootEd.put("editions",eds);
			rootEd.put("col", col);
			rootEd.put("colName", col.getName());
			FileWriter out = null;
			for(MagicEdition ed : eds)
			{
				rootEd.put("cards", dao.getCardsFromCollection(col, ed));
				rootEd.put("edition", ed);
				out = new FileWriter(Paths.get(dest,"page-ed-"+col.getName()+"-"+ed.getId()+".htm").toFile());
				cardTemplate.process(rootEd, out);
			}
			out.close();
	}
	
	
	
	
	int i=0;
	private void generateCardsTemplate(MagicCard mc) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Template cardTemplate = cfg.getTemplate("page-card.html");
		
				Map rootEd = new HashMap<>();
				rootEd.put("card", mc);
				rootEd.put("cols", cols);
				
				List<MagicPrice> prices= new ArrayList<MagicPrice>();
				if(pricesProvider.size()>0)
				{
					for(MagicPricesProvider prov : pricesProvider)
					{
						try 
						{
							prices.addAll(prov.getPrice(mc.getEditions().get(0), mc));
						} 
						catch (Exception e) 
						{
							e.printStackTrace();
						}
					}
				}
				rootEd.put("prices", prices);
				FileWriter out = new FileWriter(Paths.get(dest,"page-card-"+mc.getId()+".htm").toFile());
				cardTemplate.process(rootEd, out);
				
				setChanged();
				notifyObservers(i++);
				out.close();
		
	}
}

