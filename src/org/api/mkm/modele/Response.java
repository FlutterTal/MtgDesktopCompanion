package org.api.mkm.modele;

import java.util.List;

public class Response {
	
	List<Product> product;
	List<Article> article;
	List<Link> links;
	
	
	
	
	public List<Product> getProduct() {
		return product;
	}
	public void setProduct(List<Product> product) {
		this.product = product;
	}
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	public List<Article> getArticle() {
		return article;
	}
	public void setArticle(List<Article> article) {
		this.article = article;
	}
	
	
	
	

}
