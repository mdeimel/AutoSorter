package mover;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

/**
 * DONE - Don't move file if another with the same name exists
 * DONE - Log output in time-stamped file
 * DONE - Recursively look through folders to find files -- delete directory if no more files exist
 * DONE - Don't transfer a file if it is older than ? months or ? years
 * 
 * @author deimel
 *
 */
public class AutoMove {
	private static String pictureDirectory = "/home/deimel/FakePictures";
	private static String autoSortDirectory = "/home/deimel/FakePictures/AutoSort";
	private static BufferedWriter LOG_STREAM = null;
	private static Date TODAY = new Date();
	private static int NUM_OF_MONTHS_TOO_OLD = 6;
	
	public static void main(String[] args) throws IOException {
		// Make sure directory for pictures exists
		File picturesDir = new File(pictureDirectory);
		if (!picturesDir.exists()) {
			throw new FileNotFoundException("Pictures directory expected to be found at: " + picturesDir.getPath());
		}
		
		// Make sure directory to sort exists
		File autoSortDir = new File(autoSortDirectory);
		if (!autoSortDir.exists()) {
			throw new FileNotFoundException("AutoSort directory expected to be found at: " + autoSortDir.getPath());
		}
		
		// Setup logging directory
		File logDir = new File(autoSortDirectory+"/logs");
		if (!logDir.exists()) {
			if (!logDir.mkdir()) {
				throw new IOException("Logs directory could not be created at: " + logDir.getAbsolutePath());
			}
		}
		
		// Setup log file
		FileWriter fstream = new FileWriter(logDir+"/AutoSort_"+(TODAY.getYear()+1900)+"-"+(TODAY.getMonth()+1)+"-"+TODAY.getDay()+"_"+TODAY.getHours()+":"+TODAY.getMinutes()+":"+TODAY.getSeconds()+".log");
		LOG_STREAM = new BufferedWriter(fstream);
		log("Running...");
		
		// Where the real work is done
		autoSort(autoSortDir);
		
		LOG_STREAM.close();
	}
	
	private static void autoSort(File autoSortDir) throws IOException {
		for (File file : autoSortDir.listFiles()) {
			if (file.isDirectory()) {
				autoSort(file);
			}
			else if (isPictureOrVideo(file)) {
				Path path = Paths.get(file.getAbsolutePath());
				BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
				Date date = new Date(attributes.creationTime().toMillis());
				if (notTooOld(date)) {
					int year = date.getYear()+1900;
					File yearFolder = new File(pictureDirectory+"/"+year);
					if (!yearFolder.exists()) {
						yearFolder.mkdir();
						log("Creating directory: " + yearFolder.getAbsolutePath());
					}
					String month = getMonth(date.getMonth(), year);
					File monthFolder = new File(yearFolder+"/"+month);
					if (!monthFolder.exists()) {
						monthFolder.mkdir();
						log("Creating directory: " + monthFolder.getAbsolutePath());
					}
					// Check to see if file with same name already exists
					File newFileName = new File(monthFolder,  file.getName());
					if (newFileName.exists()) {
						log("ERROR: File already exists: " + newFileName.getAbsolutePath());
					}
					else {
						boolean success = file.renameTo(newFileName);
						if (success) {
							log("Moving: " + file.getName() + " to " + monthFolder);
						}
						else {
							log("Problem with: " + file.getName());
						}
					}
				}
				else {
					log("Skipping: " + file.getName() + " because the file is more than " + NUM_OF_MONTHS_TOO_OLD + " months old.");
				}
			}
		}
		// If directory is empty, delete it
		if (autoSortDir.listFiles().length == 0) {
			boolean success = autoSortDir.delete();
			if (success) {
				log("Deleting directory: " + autoSortDir.getAbsolutePath());
			}
			else {
				log("ERROR: Could not delete directory: " + autoSortDir.getAbsolutePath());
			}
		}
	}
	
	/**
	 * Determines supported file types
	 * @param file
	 * @return
	 */
	private static boolean isPictureOrVideo(File file) {
		String fileName = file.getName().toLowerCase();
		if (fileName.endsWith(".jpg") ||
				fileName.endsWith(".jpeg") ||
				fileName.endsWith(".png") ||
				fileName.endsWith(".mov") ||
				fileName.endsWith(".avi")) {
			return true;
		}
		return false;
	}
	
	/**
	 * Get the folder name based on the given month and year
	 * @param monthInt
	 * @param year
	 * @return
	 */
	private static String getMonth(int monthInt, int year) {
		String month = "";
		switch(monthInt) {
		case(0): month=year+"-01 January"; break;
		case(1): month=year+"-02 February"; break;
		case(2): month=year+"-03 March"; break;
		case(3): month=year+"-04 April"; break;
		case(4): month=year+"-05 May"; break;
		case(5): month=year+"-06 June"; break;
		case(6): month=year+"-07 July"; break;
		case(7): month=year+"-08 August"; break;
		case(8): month=year+"-09 September"; break;
		case(9): month=year+"-10 October"; break;
		case(10): month=year+"-11 November"; break;
		case(11): month=year+"-12 December"; break;
		default: month="BAD_VALUE";
		}
		return month;
	}
	
	/**
	 * Log system messages to standard output and to a log file
	 * @param message
	 * @throws IOException
	 */
	private static void log(String message) throws IOException {
		LOG_STREAM.write(message+"\n");
		System.out.println(message);
	}
	
	/**
	 * Determines if a picture is older than the specified number of months,
	 * intended to prevent pictures from a camera where the date was reset
	 * from being placed in the wrong (old) directories.
	 * @param pictureDate
	 * @return
	 */
	private static boolean notTooOld(Date pictureDate) {
		long daysOld = (TODAY.getTime() - pictureDate.getTime()) / 86400000; // 1000 * 60 * 60 * 24 = Number of milliseconds in a day
		if (daysOld > (NUM_OF_MONTHS_TOO_OLD * 31)) { // Generously assume 31 days in a month
			return false;
		}
		else {
			return true;
		}
	}
}
