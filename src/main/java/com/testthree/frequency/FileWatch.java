package com.testthree.frequency;

import java.io.*;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.testthree.frequency.utils.FrequencyUtils;


/**
 * Program that watches the directory for changes
 * @author petel
 *
 */
public class FileWatch {

	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private final boolean recursive;
	private boolean trace = false;
	private Map<String,Integer> totalTermFreqCorp;
	private Map<String,Integer> totalDocuWithTerms;


	public FileWatch(Path dir, boolean recursive) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		this.recursive = recursive;

		if (recursive) {
			System.out.format("Scanning %s ...\n", dir);
			registerAll(dir);
			System.out.println("Done.");
		} else {
			register(dir);
		}
		// enable trace after initial registration
		this.trace = true;
	}

	public Map<String, Integer> getTotalTermFreqCorp() {
		return totalTermFreqCorp;
	}

	public void setTotalTermFreqCorp(Map<String, Integer> totalTermFreqCorp) {
		this.totalTermFreqCorp = totalTermFreqCorp;
	}

	public Map<String, Integer> getTotalDocuWithTerms() {
		return totalDocuWithTerms;
	}

	public void setTotalDocuWithTerms(Map<String, Integer> totalDocuWithTerms) {
		this.totalDocuWithTerms = totalDocuWithTerms;
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * Register the given directory with the WatchService
	 */

	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update:  %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the
	 * WatchService.
	 */
	private void registerAll(final Path start) throws IOException {
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Process all events for keys queued to the watcher
	 */
	void processEvents( int timer, Set<String> terms,String outputFile,Map<String,Integer> tFreqCorp,Map<String,Integer> tDocWithTerms) {
		for (;;) {
			// wait for key to be signalled
			WatchKey key;
			File fileIn;
			
			try {
				key = watcher.poll(timer, TimeUnit.SECONDS);// .take();
			} catch (InterruptedException x) {
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();
				

				if (kind == OVERFLOW) {
					continue;
				}

				// context for directory entry even is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);
				fileIn = new File(child.toFile().getAbsolutePath());	
				FrequencyProcessor frPro = new FrequencyProcessor();
				
				System.out.println("File added: " + fileIn);
				
				Document doc = frPro.processTermFreqStream(fileIn, terms); // document term frequency stream
				
//				System.out.println("Previous: " + tFreqCorp);
				
//				docs.forEach(doc -> {
//					System.out.println("Search Terms Freq: " + doc.getSearchTermFreq());
//				});
				Map<String, Integer> totalTermFreqInCorpus = frPro.processTotalTermFreq(doc,tFreqCorp); // docs has been																									

				Map<String, Integer> totalDocsWithTerm = frPro.processNumDocsTerm(fileIn, terms,tDocWithTerms);				

				// print out event
				System.out.format("%s: %s\n", event.kind().name(), child);

				// if directory is createdk and watching recursively, then register it and its
				// sub-directories
				if (recursive && (kind == ENTRY_CREATE)) {
					try {
						if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
							registerAll(child);
						}
					} catch (IOException x) {
						//
					}
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}
	}

	static void usage() {
		System.err.println("usage: java FileWatch <dirToWatch> <fileTermsTT> <N> <period>"); //
		System.exit(-1);
	}

	public static void main(String[] args) throws IOException {
		// parse arguments
		if (args.length == 0 || args.length > 5)
			usage();
		

		
		boolean recursive = false;
		String dirToWatch = args[0];
		String termsTT = args[1];
		String topN = args[2];
		String period = args[3];
		
		FrequencyProcessor freqPro = new FrequencyProcessor();

		File[] filesIn = FrequencyUtils.readFiles(dirToWatch);

		String outputFile = FrequencyUtils.obtainConfigProperties();

		Set<String> tt = FrequencyUtils.obtainTargetTerms(termsTT);

		/* FILE PROCESSING */

		Stream<Document> docs = freqPro.processTermFreqStream(filesIn, tt); // document term frequency stream

		Path dir = Paths.get(dirToWatch);
		FileWatch watchdir = new FileWatch(dir, recursive);																					
		watchdir.setTotalTermFreqCorp(freqPro.processTotalTermFreq(docs));
		
		watchdir.setTotalDocuWithTerms(freqPro.processNumDocsTerm(filesIn, tt));
		
		Map<String, Integer> totalTermFreqInCorpus = watchdir.getTotalTermFreqCorp();
		Map<String, Integer>totalDocsWithTerm = watchdir.getTotalDocuWithTerms();
		Stream<Document> definitiveDocs = freqPro.processFinalStats(filesIn, tt,totalTermFreqInCorpus,
				totalDocsWithTerm);


		Map<String, List<Document>> res = definitiveDocs.collect(Collectors.groupingBy(Document::getName));


		FrequencyUtils.produceResults(res, outputFile);	
		
		
		int timePeriod = 0;
		try {
			timePeriod = Integer.parseInt(period);
		} catch (InputMismatchException ime) {
			System.err.println("Integer required for fourth argument (period to report).");
		}
		// register directory and process its events
		
		watchdir.processEvents(timePeriod,tt,outputFile,totalTermFreqInCorpus,totalDocsWithTerm);
	}

}
