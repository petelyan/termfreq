package com.testthree.frequency;

import java.util.Map;

public class Document {
	private String name;
	private int wordCount;
	private Map<String,Integer> searchTermFreq;
	private Map<String,Double> termTfIdfMap;
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getWordCount() {
		return wordCount;
	}
	public void setWordCount(int wordCount) {
		this.wordCount = wordCount;
	}
	public Map<String, Integer> getSearchTermFreq() {
		return searchTermFreq;
	}
	public void setSearchTermFreq(Map<String, Integer> searchTermFreq) {
		this.searchTermFreq = searchTermFreq;
	}
	public Map<String, Double> getTermTfIdfMap() {
		return termTfIdfMap;
	}
	public void setTermTfIdfMap(Map<String, Double> termTfIdfMap) {
		this.termTfIdfMap = termTfIdfMap;
	}


}
