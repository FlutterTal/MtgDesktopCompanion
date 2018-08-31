package org.magic.api.indexer.impl;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.magic.api.beans.MagicCard;
import org.magic.api.exports.impl.JsonExport;
import org.magic.api.interfaces.abstracts.AbstractCardsIndexer;
import org.magic.api.interfaces.abstracts.AbstractMTGPlugin;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;



public class LuceneIndexer extends AbstractCardsIndexer {

	private Directory dir;
	private Analyzer analyzer ;
	private JsonExport serializer;
	
	public static void main(String[] args) throws IOException {
		LuceneIndexer t = new LuceneIndexer();
	
		if(t.open())
		{	
			//MTGControler.getInstance().getEnabledCardsProviders().init();
			//t.initIndex();
			MagicCard mc = new MagicCard();
				mc.setName("Emrakul, the Promised End");
				
			t.similarity(mc).entrySet().forEach(s->System.out.println("------"+s.getValue() +" " + s.getKey() + " ("+s.getKey().getCurrentSet()+")"));
			
			t.close();
		}
	}
	
	
	@Override
	public void initDefault() {
		setProperty("boost", "true");
		setProperty("minTermFreq", "1");
		setProperty("fields","text,color,types,cmc");
	}
	
	public LuceneIndexer() {
		super();
		serializer=new JsonExport();
		analyzer = new StandardAnalyzer();
	}
	
	public Map<MagicCard,Float> similarity(MagicCard mc) throws IOException 
	{
		Map<MagicCard,Float> ret = new LinkedHashMap<>(); 
		try (IndexReader indexReader = DirectoryReader.open(dir))
		{
			
		 IndexSearcher searcher = new IndexSearcher(indexReader);
		 Query query = new QueryParser("name", analyzer).parse("name:"+mc.getName());
		
		 logger.debug(query);
		 MoreLikeThis mlt = new MoreLikeThis(indexReader);
		 			  mlt.setFieldNames(getArray("fields"));
		 			  mlt.setAnalyzer(analyzer);
		 			  mlt.setMinTermFreq(getInt("minTermFreq"));
		 			  mlt.setBoost(getBoolean("boost"));

		 			  
		 TopDocs top = searcher.search(query, 1);
		 ScoreDoc d = top.scoreDocs[0];
		 Query like = mlt.like(d.doc);
		 logger.debug(like);
		 
		 TopDocs likes = searcher.search(like,50);
		 
		 
		 for(ScoreDoc l : likes.scoreDocs)
			{
			 Document doc = searcher.doc(l.doc);
			 ret.put(serializer.fromJson(MagicCard.class, doc.get("data")),l.score);
			}
		 
		 
		return ret;
		
		} catch (ParseException e) {
			logger.error(e);
		}
		return ret;
		
	}
	
	public void initIndex() throws IOException {
		
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		  				   iwc.setOpenMode(OpenMode.CREATE);
	    IndexWriter indexWriter = new IndexWriter(dir, iwc);
		 
		for(MagicCard mc : MTGControler.getInstance().getEnabledCardsProviders().searchCardByCriteria("name", "", null, false))
			 indexWriter.addDocument(toDocuments(mc));
		
		indexWriter.commit();
		indexWriter.close();
	}
	
	public boolean open(){
	    try 
        {
	    	dir = FSDirectory.open(Paths.get(MTGConstants.CONF_DIR.getAbsolutePath(),"index"));
            return true;
        } 
	    catch (Exception e) {
        	logger.error(e);
			return false;
		}
    }
	 
	public void close() throws IOException
	{
		dir.close();
	}
	
	private Document toDocuments(MagicCard mc) {
          Document doc = new Document();
          			
          		FieldType fieldType = new FieldType();
		          		fieldType.setStored(true);
		          		fieldType.setStoreTermVectors(true);
		          		fieldType.setTokenized(true);
		          		fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		          		
           		   doc.add(new Field("name", mc.getName(), fieldType));
           		   doc.add(new Field("cost", mc.getCost(),fieldType));
           		   doc.add(new Field("text", mc.getText(), fieldType));
           		   doc.add(new Field("type", mc.getFullType(), fieldType));
           		   doc.add(new Field("set",mc.getCurrentSet().getId(),fieldType));
           		   doc.add(new StringField("data",serializer.toJson(mc),Field.Store.YES));
           		   doc.add(new StoredField("cmc",mc.getCmc()));
           		   
	      		   for(String color:mc.getColors())
	      		   {
	      			   doc.add(new Field("color", color, fieldType));
	      		   }
	      		   
         return doc;
 	 }

	@Override
	public String getName() {
		return "Lucene";
	}

	@Override
	public PLUGINS getType() {
		return PLUGINS.INDEXER;
	}

	 
}