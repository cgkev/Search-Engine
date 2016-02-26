package index;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

// Write Indexing here

// ********--Citing Sources--******** 
// Kevin
// Walking the file tree: https://docs.oracle.com/javase/tutorial/essential/io/walk.html#invoke
// Walking the file tree: http://stackoverflow.com/questions/10014746/the-correct-way-to-use-filevisitor-in-java


public class Indexing {
	static int counter = 0;
	static List<String> pathsOfHtml = new ArrayList<String>();
	
	public static void traverseAllFiles(String parentDirectory) throws IOException{
		Path startPath = Paths.get(parentDirectory);
		Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				// Validates if files name has .html
				if (file.getFileName().toString().contains(".html")) {
				  // add to List
					pathsOfHtml.add(file.toString());
				}
				return FileVisitResult.CONTINUE;
			}
		}); 
	}
	
	
	public static void main(String[] args) throws IOException {
		
		// Note: Path must be changed to work on your filesystem. 
		
		traverseAllFiles("C:\\Users\\Kevin\\Desktop\\en");
		System.out.println("Size: " + pathsOfHtml.size());
	}
}
