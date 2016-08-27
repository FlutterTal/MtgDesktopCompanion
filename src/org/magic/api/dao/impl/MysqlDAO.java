package org.magic.api.dao.impl;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.ShopItem;
import org.magic.api.interfaces.abstracts.AbstractMagicDAO;
import org.magic.services.MagicFactory;

public class MysqlDAO extends AbstractMagicDAO{

	static final Logger logger = LogManager.getLogger(MysqlDAO.class.getName());
    Connection con;
 
    @Override
    public String toString() {
    	return getName();
    }
    
	public MysqlDAO() throws ClassNotFoundException, SQLException {
	    super();	
		if(!new File(confdir, getName()+".conf").exists()){
			 props.put("DRIVER", "com.mysql.jdbc.Driver");
			 props.put("SERVERNAME","localhost");
			 props.put("SERVERPORT", "3306");
			 props.put("DB_NAME", "mtgdesktopclient");
			 props.put("LOGIN", "mtgdesktopclient");
			 props.put("PASSWORD", "mtgdesktopclient");
			 props.put("PARAMS", "?autoDeserialize=true");
		save();
		}
	}
	

	public void init() throws SQLException, ClassNotFoundException {
		 logger.debug("init " + getName());
		 Class.forName(props.getProperty("DRIVER"));
		 String url = "jdbc:mysql://"+props.getProperty("SERVERNAME")+":"+props.getProperty("SERVERPORT");
		 con=DriverManager.getConnection(url+"/"+props.getProperty("DB_NAME")+props.getProperty("PARAMS"),props.getProperty("LOGIN"),props.getProperty("PASSWORD"));
		 createDB();
	}

	 public boolean createDB()
	 {
		 try{
		 	logger.debug("Create table Cards");
		 	con.createStatement().executeUpdate("create table cards (ID varchar(250),name varchar(250), mcard BLOB, edition varchar(20), cardprovider varchar(50),collection varchar(250))");
		 	logger.debug("Create table Shop");
		 	con.createStatement().executeUpdate("create table shop (id varchar(250), statut varchar(250))");
		 	logger.debug("Create table collections");
		 	con.createStatement().executeUpdate("CREATE TABLE collections ( name VARCHAR(250))");
		 	logger.debug("populate collections");
			con.createStatement().executeUpdate("insert into collections values ('Library')");
		 	con.createStatement().executeUpdate("insert into collections values ('Needed')");
		 	con.createStatement().executeUpdate("insert into collections values ('For sell')");
		 	con.createStatement().executeUpdate("insert into collections values ('Favorites')");
			
		 	
		 	return true;
		 }
		 catch(SQLException e)
		 {
			 logger.debug(e);
			 return false;
		 }
		 
	 }
	
	
	@Override
	public void saveCard(MagicCard mc, MagicCollection collection) throws SQLException {
		logger.debug("saving " + mc +" in " + collection);
		
		PreparedStatement pst = con.prepareStatement("insert into cards values (?,?,?,?,?)");
		 pst.setString(1, mc.getName());
		 pst.setObject(2, mc);
		 pst.setString(3, mc.getEditions().get(0).getId());
		 pst.setString(4, "");
		 pst.setString(5, collection.getName());
		 
		 pst.executeUpdate();
	}

	@Override
	public void removeCard(MagicCard mc, MagicCollection collection) throws SQLException {
		logger.debug("remove " + mc + " from " + collection);
		PreparedStatement pst = con.prepareStatement("delete from cards where name=? and edition=? and collection=?");
		 pst.setString(1, mc.getName());
		 pst.setString(2, mc.getEditions().get(0).getId());
		 pst.setString(3, collection.getName());
		 pst.executeUpdate();

	}

	@Override
	public MagicCard loadCard(String name, MagicCollection collection) throws SQLException {
		logger.debug("load card " + name + " in " + collection);
		PreparedStatement pst=con.prepareStatement("select * from cards where collection= ? and name= ?");	
		pst.setString(1, collection.getName());
		pst.setString(2, name);
		ResultSet rs = pst.executeQuery();
		return (MagicCard) rs.getObject("mcard");
	}

	@Override
	public List<MagicCard> listCards() throws SQLException {
		logger.debug("list all cards");
		
		String sql ="select * from cards";
		
		PreparedStatement pst=con.prepareStatement(sql);	
		
		ResultSet rs = pst.executeQuery();
		List<MagicCard> list = new ArrayList<MagicCard>();
		while(rs.next())
		{
			list.add((MagicCard) rs.getObject("mcard"));
		}
	
	return list;
	}

	@Override
	public Map<String, Integer> getCardsCountGlobal(MagicCollection c) throws SQLException {
		String sql = "select edition, count(name) from cards where collection=? group by edition";
		PreparedStatement pst=con.prepareStatement(sql);	
		pst.setString(1, c.getName());
		ResultSet rs = pst.executeQuery();
		
		Map<String,Integer> map= new HashMap<String,Integer>();
		
		while(rs.next())
		{
			map.put(rs.getString(1), rs.getInt(2));
		}
		
		return map;
	}


	
	
	@Override
	public int getCardsCount(MagicCollection cols,MagicEdition me) throws SQLException {
		
		
		String sql = "select count(name) from cards ";
		
		if(cols!=null)
			sql+=" where collection = '" + cols.getName()+"'";
		
		if(me!=null)
			sql+=" and edition = '" + me.getId()+"'";
		
		logger.debug(sql);
		
		Statement st = con.createStatement();
		
		ResultSet rs = st.executeQuery(sql);
		rs.next();
		return rs.getInt(1);
	}

	@Override
	public List<MagicCard> getCardsFromCollection(MagicCollection collection) throws SQLException {
		return getCardsFromCollection(collection,null);
	}

	@Override
	public List<MagicCard> getCardsFromCollection(MagicCollection collection, MagicEdition me) throws SQLException {
		
		logger.debug("getCardsFromCollection " + collection + " " + me);
		
		String sql ="select * from cards where collection= ? and edition = ?";
		
		if(me==null)
			sql ="select * from cards where collection= ?";
		
		PreparedStatement pst=con.prepareStatement(sql);	
		pst.setString(1, collection.getName());
		
		if(me!=null)
			pst.setString(2, me.getId());
		
		ResultSet rs = pst.executeQuery();
		List<MagicCard> list = new ArrayList<MagicCard>();
		while(rs.next())
		{
			list.add((MagicCard) rs.getObject("mcard"));
		}
	
	return list;
	}

	@Override
	public List<String> getEditionsIDFromCollection(MagicCollection collection) throws SQLException {
		String sql ="select distinct(edition) from cards where collection=?";
		
		PreparedStatement pst=con.prepareStatement(sql);	
		pst.setString(1, collection.getName());
		ResultSet rs = pst.executeQuery();
		List<String> list = new ArrayList<String>();
		while(rs.next())
		{
			list.add(rs.getString("edition"));
		}
	
	return list;
	}

	@Override
	public MagicCollection getCollection(String name) throws SQLException {
		PreparedStatement pst=con.prepareStatement("select * from collections where name= ?");	
		pst.setString(1, name);
	
	ResultSet rs = pst.executeQuery();
	
	if(rs.next())
	{
		MagicCollection mc = new MagicCollection();
		mc.setName(rs.getString("name"));
		
		return mc;
	}
	
	return null;
	}

	@Override
	public void saveCollection(MagicCollection c) throws SQLException {

		PreparedStatement pst = con.prepareStatement("insert into collections values (?)");
		 pst.setString(1, c.getName());
		 
		 pst.executeUpdate();

	}

	@Override
	public void removeCollection(MagicCollection c) throws SQLException {
		
		if(c.getName().equals("Library"))
			throw new SQLException(c.getName() + " can not be deleted");
		
		PreparedStatement pst = con.prepareStatement("delete from collections where name = ?");
		 pst.setString(1, c.getName());
		 pst.executeUpdate();
		 
		 
		 pst = con.prepareStatement("delete from cards where collection = ?");
		 pst.setString(1, c.getName());
		 pst.executeUpdate();

	}

	@Override
	public List<MagicCollection> getCollections() throws SQLException {
		PreparedStatement pst=con.prepareStatement("select * from collections");	
		ResultSet rs = pst.executeQuery();
		List<MagicCollection> colls = new ArrayList<MagicCollection>();
		while(rs.next())
		{
			MagicCollection mc = new MagicCollection();
			mc.setName(rs.getString(1));
			colls.add(mc);
		}
		return colls;
	}

	@Override
	public void removeEdition(MagicEdition me, MagicCollection col) throws SQLException {
		logger.debug("remove " + me + " from " + col);
		PreparedStatement pst = con.prepareStatement("delete from cards where edition=? and collection=?");
		 pst.setString(1, me.getId());
		 pst.setString(2, col.getName());
		 pst.executeUpdate();

	}

	@Override
	public String getDBLocation() {
		return props.getProperty("URL")+"/"+props.getProperty("DB_NAME");
	}

	@Override
	public long getDBSize() {
		String sql = "SELECT Round(Sum(data_length + index_length) / 1024 / 1024, 1) FROM   information_schema.tables WHERE  table_schema = 'mtgdesktopclient'";
		PreparedStatement pst;
		try {
			pst = con.prepareStatement(sql);
			ResultSet rs = pst.executeQuery();
			return (long)rs.getDouble(1);
		} catch (SQLException e) {
			return 0;
		}	
	
	}

	@Override
	public String getName() {
		return "MySQL";
	}


	@Override
	public List<MagicDeck> listDeck() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void saveDeck(MagicDeck d) throws SQLException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deleteDeck(MagicDeck d) throws SQLException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public List<MagicCollection> getCollectionFromCards(MagicCard mc)throws SQLException{
		
		if(mc.getEditions().size()==0)
			throw new SQLException("No edition defined");
		
		PreparedStatement pst = con.prepareStatement("SELECT collection FROM cards WHERE name=? and edition=?");
		 pst.setString(1, mc.getName());
		 pst.setString(2, mc.getEditions().get(0).getId());
		 
		 ResultSet rs = pst.executeQuery();
		 List<MagicCollection> cols = new ArrayList<MagicCollection>();
		 while(rs.next())
		 {
			 MagicCollection col = new MagicCollection();
			 col.setName(rs.getString("collection"));
			 cols.add(col);
		 }
		 
		 return cols;
	}


	@Override
	public void saveShopItem(ShopItem mp, String string) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String getSavedShopItemAnotation(ShopItem id) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void backup(File f) {
		String dumpCommand = "mysqldump " + props.getProperty("DB_NAME") + " -h " + props.getProperty("SERVERNAME") + " -u " + props.getProperty("LOGIN") +" -p" + props.getProperty("PASSWORD");
		Runtime rt = Runtime.getRuntime();
		PrintStream ps;
		logger.info("begin Backup " + props.getProperty("DB_NAME"));
		
		try{
		Process child = rt.exec(dumpCommand);
		ps=new PrintStream(f);
		InputStream in = child.getInputStream();
		int ch;
		while ((ch = in.read()) != -1) 
		{
			ps.write(ch);
		}
		ps.close();
		logger.info("Backup " + props.getProperty("DB_NAME") + " done");
		
		}catch(Exception exc) {
			logger.error(exc);
		}
		
	}





}