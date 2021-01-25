package com.testthree.frequency.utils;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

import com.testthree.frequency.Document;
import com.testthree.frequency.FrequencyProcessor;

public class FrequencyUtils {
	
	// read files
	public static File[] readFiles(String directory) {
		File[] files = new File(directory).listFiles();
		return files;
	}
	
	// read files 2
	public  Stream<File> readFileStream(String directory) {
		Stream<Path> paths = null;
		try {
			paths = Files.list(Paths.get(directory));
			// = (int)paths.count();
		} catch (IOException ioe) {
			System.out.println("File not found.\n");
		}
		return paths.map(file -> file.toFile());
	}
	
	
	public static String obtainConfigProperties() {
		String outputFile = "";
		try(InputStream input = FrequencyProcessor.class.getClassLoader().getResourceAsStream("config.properties")) {
			Properties prop = new Properties();
			
			if(input == null) {
				System.err.println("Config file not found");
				return outputFile;
			}			
			prop.load(input);
			outputFile = prop.getProperty("output.file");
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return outputFile;
	}
	
	public static void writeOutput(List<String> text, String outFile) {
		Path outputFile = Paths.get(outFile);
		try {
				Files.write(outputFile, text, CREATE,TRUNCATE_EXISTING);
		}catch(IOException e) {
			e.printStackTrace();
		}
			
	}
	
	//read the terms 
	public static Set<String> obtainTargetTerms(String file){
		File termFile = new File(file);
		Set<String> terms = new HashSet<>();
		try(Scanner input = new Scanner(termFile)){
			while(input.hasNext()) {
				String word = clean(input.next());
				terms.add(word);
			}
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return terms;
		
	}
	
	
	public  static void produceResults(Map<String,List<Document>> docGroup,String outFile) {
		List<String> outText = new ArrayList<>();		
		docGroup.forEach((nam,lis) ->{
			System.out.println(nam);
			outText.add(nam);
			lis.forEach(doc -> {
				doc.getTermTfIdfMap().forEach((ky,val) -> {
					System.out.printf("%4s %20.3f\n", ky,val);
					outText.add(String.format("%4s %20.3f\n", ky,val));
					FrequencyUtils.writeOutput(outText,outFile);
				});
			});
			System.out.println();
		});		
	}
	
	
	public static String clean(String text) {
		String cleanStr = "";
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isLetter(c)) {
				cleanStr = cleanStr + c;
			}
		}
		return cleanStr.toLowerCase();
	}

	// print files
	public void printFiles(File[] files) {
		for (File file : files)
			System.out.println(file.getName());
	}


}
