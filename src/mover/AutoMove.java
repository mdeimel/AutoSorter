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
 * First calculate the number of files, then have a percentage of completion in standard output
 * 
 * @author deimel
 *
 */
public class AutoMove {
	private static String pictureDirectory = null;
	private static String autoSortDirectory = null;
	private static BufferedWriter LOG_STREAM = null;
	private static Date TODAY = new Date();
	private static Integer NUM_OF_MONTHS_TOO_OLD = 6;
	private static String fs = System.getProperty("file.separator");
	
	private static void printHelp() {
		System.out.println("Please provide the directory the pictures reside in,");
		System.out.println("as well as the directory the pictures should be placed in.");
		System.out.println("Ex:");
		System.out.println("java mover.AutoMove /home/admin/SortedPictures /home/admin/PicturesToSort");
		System.out.println("  optional argument: age of picture (in months) to skip sorting, default is 6");
		System.out.println("  this option is used in case a camera's calendar has been reset, and newer");
		System.out.println("  pictures could potentially get sorted into older folders");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2 && args.length != 3) {
			printHelp();
			return;
		}
		else {
			pictureDirectory = args[0];
			autoSortDirectory = args[1];
			if (args.length == 3) {
				NUM_OF_MONTHS_TOO_OLD = Integer.parseInt(args[2]);
			}
		}
		
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
		File logDir = new File(autoSortDirectory+fs+"logs");
		if (!logDir.exists()) {
			if (!logDir.mkdir()) {
				throw new IOException("Logs directory could not be created at: " + logDir.getAbsolutePath());
			}
		}
		
		// Setup log file
		// Windows requires the file to be created first
		// Use semicolons(;) instead of colons(:) in time because colons are not allowed in Windows file names
		System.out.print("logFile path: " + logDir.getAbsolutePath()+fs+"AutoSort_"+(TODAY.getYear()+1900)+"-"+(TODAY.getMonth()+1)+"-"+TODAY.getDay()+"_"+TODAY.getHours()+";"+TODAY.getMinutes()+";"+TODAY.getSeconds()+".log");
//		File logFile = new File(logDir, "AutoSort_"+(TODAY.getYear()+1900)+"-"+(TODAY.getMonth()+1)+"-"+TODAY.getDay()+"_"+TODAY.getHours()+":"+TODAY.getMinutes()+":"+TODAY.getSeconds()+".log");
		File logFile = new File(logDir, "AutoMoveLog.txt");
		if (!logFile.exists()) {
			if (!logFile.createNewFile()) {
				throw new IOException("Could not create file: " + logFile.getAbsolutePath());
			}
		}
		FileWriter fstream = new FileWriter(logFile);
//		FileWriter fstream = new FileWriter(logDir+fs+"AutoSort_"+(TODAY.getYear()+1900)+"-"+(TODAY.getMonth()+1)+"-"+TODAY.getDay()+"_"+TODAY.getHours()+":"+TODAY.getMinutes()+":"+TODAY.getSeconds()+".log");
		LOG_STREAM = new BufferedWriter(fstream);
		log("Running...");
		
		// Where the real work is done
		autoSort(autoSortDir);
		
		LOG_STREAM.close();
	}
	
	private static void autoSort(File autoSortDir) throws IOException {
		log("Moving to directory: " + autoSortDir);
		for (File file : autoSortDir.listFiles()) {
			if (file.isDirectory()) {
				autoSort(file);
			}
			else if (isPictureOrVideo(file)) {
				Path path = Paths.get(file.getAbsolutePath());
				BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
				Date date = new Date(attributes.creationTime().toMillis());
				System.out.println("----------------------------------------");
				System.out.println("Path: " + file.getName());
				System.out.println("Creation Time: " + date.toGMTString());
				if (notTooOld(date)) {
					int year = date.getYear()+1900;
					File yearFolder = new File(pictureDirectory+fs+year);
					if (!yearFolder.exists()) {
						yearFolder.mkdir();
						log("Creating directory: " + yearFolder.getAbsolutePath());
					}
					String month = getMonth(date.getMonth(), year);
					File monthFolder = new File(yearFolder+fs+month);
					if (!monthFolder.exists()) {
						monthFolder.mkdir();
						log("Creating directory: " + monthFolder.getAbsolutePath());
					}
					// Check to see if file with same name already exists
					File newFileName = new File(monthFolder,  file.getName());
					if (newFileName.exists()) {
						// First check to see if the two files are the same (based on last modified time), if so, delete the existing file
						Path newFileNamePath = Paths.get(newFileName.getAbsolutePath());
						BasicFileAttributes newFileNameAttributes = Files.readAttributes(newFileNamePath, BasicFileAttributes.class);
						if (attributes.lastModifiedTime().toMillis()==newFileNameAttributes.lastModifiedTime().toMillis()) {
							log("File: " + file.getAbsolutePath() + " is a duplicate of: " + newFileName.getAbsolutePath() + ", and will be deleted.");
							if (!file.delete()) {
								logError("Could not delete: " + file.getAbsolutePath());
							}
							file = null;
							continue;
						}
						
						File incrementedNewFileName = newFileName;
						// If the files are not the same, increment the file name with a "_#" counter for uniqueness
						int counter = 0;
						int period = incrementedNewFileName.getName().lastIndexOf(".");
						String firstPartOfName = incrementedNewFileName.getName().substring(0, period);
						String extensionType = incrementedNewFileName.getName().substring(period);
						while (incrementedNewFileName.exists()) {
							incrementedNewFileName = new File(monthFolder, firstPartOfName+"_"+counter+extensionType);
							counter++;
						}
						logError("File: " + file.getAbsolutePath() + " already exists: " + newFileName.getAbsolutePath() + ", renaming to: " + incrementedNewFileName.getName());
						newFileName = incrementedNewFileName;
					}
					boolean success = file.renameTo(newFileName);
					if (success) {
						log("Moving: " + file.getName() + " to " + monthFolder);
					}
					else {
						logError("Problem with: " + file.getName());
					}
				}
				else {
					logError("Skipping: " + file.getName() + " because the file is more than " + NUM_OF_MONTHS_TOO_OLD + " months old.");
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
				logError("Could not delete directory: " + autoSortDir.getAbsolutePath());
			}
		}
	}
	
	/**
	 * Determines supported file types, currently: jpg, jpeg, png, mov, avi
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
	 * Preceed error messages with "ERROR:" for easier searching in log file
	 * @param message
	 * @throws IOException
	 */
	private static void logError(String message) throws IOException {
		log("ERROR: " + message);
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
