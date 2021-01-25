package com.testthree.frequency;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.testthree.frequency.utils.FrequencyUtils;

public class FrequencyProcessor {


	// process term frequency 2
	public  Stream<Document> processTermFreqStream(File[] inputFiles,Set<String> termSet) {
		List<Document> docs = new ArrayList<>();
		for(File file: inputFiles) {
			docs.add(processDocTermFreq(file,termSet));
		}

		return docs.stream();
	}
	
	public  Document processTermFreqStream(File inputFile,Set<String> termSet) {

			return processDocTermFreq(inputFile,termSet);

	}
	
	public  Stream<Document> processFinalDocs(File[] files, Set<String> termSet){
		return processTermFreqStream(files,termSet);		
		
	}
	
	
	public  Stream<Document> processFinalStats(File[] files,Set<String> termSet,Map<String,Integer> totalTermFreqInCorpus,Map<String,Integer> totalDocsWithTerm){
		Stream<Document> docs = processTermFreqStream(files,termSet);
		int numFiles = files.length;
    	Stream<Document> definitiveDocs = docs.map((doc) -> {
    		int totalWordsInDoc = doc.getWordCount();

		Map<String,Double> tfMap = new TreeMap<String,Double>();
		doc.getSearchTermFreq().forEach((k,v) -> {
			int termFreqInDoc = v;
			
			int docsWithTerm = totalDocsWithTerm.get(k);

			 double tfIdf = processTermDocWeight(termFreqInDoc, totalWordsInDoc, docsWithTerm, numFiles);//2 should be dynamic
			  tfMap.put(k, tfIdf);
			 doc.setTermTfIdfMap(tfMap);
		});
		return doc;
	});
    	return definitiveDocs;
	}


	private  Document processDocTermFreq(File file, Set<String> termSet) {
		Document doc = new Document();
		Map<String, Integer> searchTermMap = new TreeMap<>();
		try {
			Scanner input = new Scanner(file);
			int wordCount = 0;
			while (input.hasNext()) {
				String word = FrequencyUtils.clean(input.next());
				if (termSet.contains(word)) {
					searchTermMap.merge(word, 1, Integer::sum);
				}
				wordCount++;
			}
			doc.setName(file.getName());
			doc.setSearchTermFreq(searchTermMap);
			doc.setWordCount(wordCount);
			input.close();
		} catch (FileNotFoundException fnfe) {
			System.err.println("File not found.");
		} /*
			 * finally { input.close(); }
			 */
		return doc;
	}

	public  Map<String, Integer> processTotalTermFreq(Stream<Document> docs) {
		Map<String, Integer> totalTermFreq = new TreeMap<>();
		docs.forEach(doc -> {
			Map<String, Integer> keys = doc.getSearchTermFreq();
			keys.forEach((k, v) ->{ totalTermFreq.merge(k, v, Integer::sum);});
		});
		return totalTermFreq;
	}
	
	
	public  Map<String, Integer> processTotalTermFreq(Document doc,Map<String,Integer> totalTermFreq) {
			 Map<String, Integer>  searchTerms = doc.getSearchTermFreq();	
			 int wordCount = doc.getWordCount();
			System.out.println("Word count: " + wordCount);

			searchTerms.forEach((k, v) ->{ 
			totalTermFreq.merge(k, v, Integer::sum);});
		
		return totalTermFreq;
	}
	
	
	
	public  Map<String,Integer> processNumDocsTerm(File[] inputFiles, Set<String> termSet){
		Map<String, Integer> totalDocFreq = new TreeMap<>();
		Stream<Document> docs = processTermFreqStream(inputFiles,termSet);
		docs.forEach(doc -> {
			Map<String, Integer> keys = doc.getSearchTermFreq();
			keys.forEach((k, v) -> {
				if(termSet.contains(k)) {
					totalDocFreq.merge(k, 1, Integer::sum);
				}
				});
		});
		return totalDocFreq;
	}
	
	
	//calculate in how many docs the term appears
	public  Map<String,Integer> processNumDocsTerm(File inputFile, Set<String> termSet,Map<String,Integer> totalDocFreq){
		//Map<String, Integer> totalDocFreq = new HashMap<>();
		Document doc = processTermFreqStream(inputFile,termSet);

			Map<String, Integer> keys = doc.getSearchTermFreq();
			keys.forEach((k, v) -> {
				if(termSet.contains(k)) {
					totalDocFreq.merge(k, 1, Integer::sum);
				}
				});

		return totalDocFreq;
	}

	// public int processTotalTermsInDoc

	// calculate tf/idf per term per doc
	public  double processTermDocWeight(int termFreqInDoc, int totalWordsInDoc, int termFreqInCorpus, int totalDocs) {
		double termFreq = (double) termFreqInDoc / totalWordsInDoc;
//		System.out.print(totalTermsInDoc);
		double docFreq = (double) totalDocs / termFreqInCorpus;
		double idf = Math.log(docFreq);
		
		return termFreq * idf;
	}
	
	

	
	

}
