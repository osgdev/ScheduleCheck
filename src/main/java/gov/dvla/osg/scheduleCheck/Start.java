package gov.dvla.osg.scheduleCheck;

import static gov.dvla.osg.scheduleCheck.XMLUtils.*;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

import javax.mail.MessagingException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author OSG
 * @version 2.0.1
 */
public class Start {

	static final Logger LOG = LogManager.getLogger();
	
	static FileSchedule fs;
	static List<ExpectedFile> expectedFiles = new ArrayList<ExpectedFile>();

	public static void main(String[] args) throws Exception {
		// set debug mode for testing environment (Windows)
		boolean debug = false;
		File inpfile = null;
		File outfile = null;
		
		LOG.info("** STARTING **");
		
		if (debug) {
			if (args.length != 0) {
				LOG.fatal("Incorrect number of arguments");
				System.exit(1);
			}
			
		/************* CREATE TEST XML FILE ********************
		 createXmlFromScratch(new File("C:/temp/Schedule.xml")); 
		 System.exit(0);
		 /*****************************************************/
			inpfile = new File("C:/temp/Schedule.xml");
			outfile = new File("C:/temp/Schedule.out.xml");
			fs = unMarshallXml(inpfile);
		} else {
			  if (args.length != 2) {
				  LOG.fatal("Incorrect number of arguments");
				  LOG.fatal("Usage: java -jar progName input_file output_file");
				  LOG.info("** FINISHED **");
			  	System.exit(1); 
			  }
			  inpfile = new File(args[0]);
			  outfile = new File(args[1]);
			  fs = unMarshallXml(inpfile);
		}

		// Format of dates for reporting
		DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");
		DateTimeFormatter logFmt = DateTimeFormat.forPattern("dd-MM-yy HH:mm");
		DateTimeFormatter timeFmt = DateTimeFormat.forPattern("HH:mm");
		DateTime nextRunDate = DateTime.now();
		String lastRun = new DateTime(fs.getLastRun().toGregorianCalendar().getTime()).toString(fmt);

		expectedFiles = getExpectedFilesSinceLastRun();

		resolveExpectedJobs(getFilesSinceLastRun(), expectedFiles);

		LOG.info("RECIEVED FILES " + lastRun + " :");
		// print filenames out of files modified since last run date that match pattern
		for (File file : getFilesSinceLastRun()) {
			LOG.trace(file.getAbsolutePath() + "\t" + new DateTime(file.lastModified()).toString(fmt));
		}

		LOG.info("EXPECTED FILES SINCE " + lastRun + " :");
		for (ExpectedFile efs : expectedFiles) {
			efs.print();
		}

		LOG.info("RESOLVED FILES SINCE " + lastRun + " :");
		for (ExpectedFile efs : expectedFiles) {
			if (efs.isResolved()) {
				efs.print();
			}
		}
		
		// one email is sent to dev team members for every unresolved file
		LOG.info("UNRESOLVED FILES SINCE " + lastRun + " :");
		for (ExpectedFile efs : expectedFiles) {
			if (!efs.isResolved()) {
				efs.print();
				String subjectLine = "Schedule Check - unresolved file " + efs.getPath();
				String msg = nextRunDate.toString(logFmt)
						+ ":00 : "
						+ "/ipwdata/resources/applications/process/ScheduleCheck.ksh"
						+ " : 0000000 : S00003 : File Not Found "
						+ "(" + efs.getEarliest().toString(timeFmt) + "-" + efs.getLatest().toString(timeFmt) + ") : "
						+ efs.getFailMsg();
				
				try {
					DevNotifyEmail.send(subjectLine, msg);
				} catch (MessagingException e) {
					LOG.fatal(e);
				}
			}
		}
		
		createXml(fs, outfile, nextRunDate);

		LOG.info("** FINISHED **");
	}

	/**
	 * @param filesSinceLastRun
	 * @param expectedFilesSinceLastRun
	 */
	public static void resolveExpectedJobs(List<File> filesSinceLastRun, List<ExpectedFile> expectedFilesSinceLastRun) {
		for (ExpectedFile efs : expectedFilesSinceLastRun) {
			for (File file : filesSinceLastRun) {
				DateTime lm = new DateTime(file.lastModified());
				if ((matchWildcardString(file.getName(), efs.getPattern()))
						&& (lm.isAfter(efs.getEarliest()) || lm.isEqual(efs.getEarliest()))
						&& (lm.isBefore(efs.getLatest()) || lm.isEqual(efs.getLatest()))) {
					efs.setResolved(true);
				}
			}
		}
	}

	/**
	 * @param text
	 * @param pattern
	 * @return
	 */
	public static Boolean matchWildcardString(String text, String pattern) {
		String[] cards = pattern.split("\\*");

		for (String card : cards) {
			int idx = text.indexOf(card);
			if (idx == -1) {
				return false;
			}
			text = text.substring(idx + card.length());
		}
		return true;
	}

	/**
	 * @return
	 */
	public static List<ExpectedFile> getExpectedFilesSinceLastRun() {

		List<ExpectedFile> ef = new ArrayList<ExpectedFile>();
		XMLGregorianCalendar xgc = fs.getLastRun();
		Date lastRunDate = xgc.toGregorianCalendar().getTime();
		Date today = new Date();
		DateTime startDate = lastRunDate == null ? null : new DateTime(lastRunDate);
		DateTime endDate = new DateTime(today);
		DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm");

		for (FileSchedule.AppSchedule as : fs.getAppSchedule()) {
			for (FileSchedule.AppSchedule.EXPECTED exp : as.getEXPECTED()) {
				// Loop thru time since last run-date til today
				for (DateTime date = startDate; date.isBefore(endDate); date = date.plusHours(1)) {

					// Get the day of the week for day loop
					Calendar cal = Calendar.getInstance();
					cal.setTime(date.toDate());
					int day = cal.get(Calendar.DAY_OF_WEEK);
					int aDay = 0;
					// Adjust day int to ISO standard
					switch (day) {
						case 1: aDay = 7; break;
						case 2: aDay = 1; break;
						case 3: aDay = 2; break;
						case 4: aDay = 3; break;
						case 5: aDay = 4; break;
						case 6: aDay = 5; break;
						case 7: aDay = 6; break;
					}

					DateTime early = date;
					early = early.withTime(formatter.parseLocalTime(exp.getEARLIEST()));

					DateTime late = date;
					late = late.withTime(formatter.parseLocalTime(exp.getLATEST()));

					if (late.isBefore(early)) {
						late = late.plusDays(1);
					}

					// ------ DAYS
					if (exp.getMONTHS() == null) {
						if (exp.getDAYS().contains(Integer.toString(aDay))
								&& ((date.isAfter(early) || early.isEqual(date))
										&& (date.isBefore(late) || late.isEqual(date)))) {
							// Create ExpectedFile object
							ExpectedFile nef = new ExpectedFile(as.getPATH(), as.getFILEPATTERN(), 
									early, late, aDay, false, exp.getFAILMSG());
							ef.add(nef);
						}
					// MONTH
					} else { 
						// Split month tag into array
						String[] months = exp.getMONTHS().split(";");

						// LOOP THRU MONTHS
						for (String m : months) {

							String[] mon = m.split("\\.");

							int monthOfYear = Integer.parseInt(mon[0]);
							String[] daysOfMonth = mon[1].split(",");

							// LOOP THRU DAYS FOR EACH MONTH
							for (String d : daysOfMonth) {
								if ((date.getMonthOfYear() == monthOfYear
										&& date.getDayOfMonth() == Integer.parseInt(d))
										&& (date.isAfter(early) || early.isEqual(date))
										&& (date.isBefore(late) || late.isEqual(date))) {
									// Create ExpectedFile object
									ExpectedFile nef = new ExpectedFile(as.getPATH(), as.getFILEPATTERN(), early, late,
											aDay, false, exp.getFAILMSG());
									ef.add(nef);
								}
							}
						}
					}
				}
			}
		}
		
		Set<ExpectedFile> lhs = new HashSet<ExpectedFile>(ef);
		ef.clear();
		ef.addAll(lhs);

		return ef;

	}

	/**
	 * @return
	 */
	public static List<File> getFilesSinceLastRun() {

		List<File> ofa = new ArrayList<File>();
		for (FileSchedule.AppSchedule as : fs.getAppSchedule()) {
			File[] fa = listFiles(as.getPATH(), as.getFILEPATTERN());
			if (fa != null) {
				for (File file : fa) {
					ofa.add(file);
				}
			}
		}
		return ofa;
	}

	/**
	 * @param directory
	 * @param pattern
	 * @return
	 */
	public static File[] listFiles(String directory, String pattern) {
		File dir = new File(directory);
		FileFilter wcf = new WildcardFileFilter(pattern);
		try {
			File[] files = dir.listFiles(wcf);
			if (files != null) {
				Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
				return files;
			}
		} catch (Exception e) {
			LOG.fatal(e);
		}
		return null;
	}

	/**
	 * @param files
	 */
	public static void displayFiles(File[] files) {
		for (File file : files) {
			System.out.printf("File: %-20s Last Modified:" + new Date(file.lastModified()) + "\n", file.getName());
		}
	}

}
