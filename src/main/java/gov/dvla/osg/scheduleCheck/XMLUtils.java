package gov.dvla.osg.scheduleCheck;

import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

/**
 * Collection of utility methods for working with / creating XML documentation
 * @author OSG
 *
 */
public class XMLUtils {
	
	private static final Logger LOG = LogManager.getLogger();
	
	/**
	 * Deserialises FileSchedule object to an XML file.
	 * @param inFs FileSchedule object to be writen out as XML.
	 * @param file Output file to write to.
	 * @param runDate next rundate.
	 */
	public static void createXml(FileSchedule inFs, File file, DateTime runDate) {

		try {
			// Convert time for XML
			Date date = runDate.toDate();
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(date);
			XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);

			inFs.setLastRun(xgc);
			// Create XML
			JAXBContext jc = JAXBContext.newInstance(FileSchedule.class);
			Marshaller jaxbMarshaller = jc.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "ScheduleCheck.xsd");
			jaxbMarshaller.marshal(inFs, file);
		} catch (DatatypeConfigurationException e) {
			LOG.fatal(e.getClass().getSimpleName() + " : " + e.getMessage());
		} catch (JAXBException e) {
			LOG.fatal(e.getClass().getSimpleName() + " : " + e.getMessage());
		}
	}
	
	/**
	 * Creates a File Schedule XML file containing dummy data for test purposes
	 * @param file output file to write XML to
	 */
	public static void createXmlFromScratch(File file) {

		// SPECIFY DETAILS
		String filePattern = "VEHICLES.HRC.*";
		String filePath = "/ipwdata/InputFileArchive/vehicles/HRC/";
		String days = "123456";
		String months = "1.12,31;5.21,30;";
		String earliest = "21:00";
		String latest = "23:00";
		String failMsg = "HRC SLA not met";
		XMLGregorianCalendar xgc = null;

		try {
			// Get todays date and convert for XML
			Date date = new Date();
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(date);
			xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		} catch (Exception ex) {
			LOG.fatal(ex.getClass().getSimpleName() + " : " + ex.getMessage());
		}

		// Create object factory
		ObjectFactory of = new ObjectFactory();
		// Create expected class using object factory method
		FileSchedule.AppSchedule.EXPECTED ex = of.createFileScheduleAppScheduleEXPECTED();
		FileSchedule.AppSchedule.EXPECTED ex2 = of.createFileScheduleAppScheduleEXPECTED();
		// set expected values
		ex.setDAYS(days);
		ex.setEARLIEST(earliest);
		ex.setLATEST(latest);
		ex.setFAILMSG(failMsg);

		ex2.setMONTHS(months);
		ex2.setEARLIEST(earliest);
		ex2.setLATEST(latest);
		ex2.setFAILMSG(failMsg);

		// Create app schedule using object factory
		FileSchedule.AppSchedule as = of.createFileScheduleAppSchedule();
		FileSchedule.AppSchedule as2 = of.createFileScheduleAppSchedule();
		// set pattern and path values
		as.setFILEPATTERN(filePattern);
		as.setPATH(filePath);
		as2.setFILEPATTERN(filePattern);
		as2.setPATH(filePath);

		// Create FileShedule
		FileSchedule fs = of.createFileSchedule();

		// Add expected object to app schedule
		as.getEXPECTED().add(ex);
		as.getEXPECTED().add(ex2);

		// set last run
		try {
			fs.setLastRun(xgc);
		} catch (Exception e) {
			LOG.fatal(e.getClass().getSimpleName());
			LOG.fatal(e.getMessage());
			System.exit(1);
		}

		// add app schedule object to file schedule object
		fs.getAppSchedule().add(as);
		fs.getAppSchedule().add(as2);
		try {
			// Create XML
			JAXBContext jc = JAXBContext.newInstance(FileSchedule.class);
			Marshaller jaxbMarshaller = jc.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "ScheduleCheck.xsd");
			jaxbMarshaller.marshal(fs, file);
		} catch (Exception e) {
			LOG.fatal(e);
		}
	}
	
	/**
	 * Converts XML file into FileSchedule object
	 * @param file File containing xml data
	 * @return deserialised XML
	 */
	public static FileSchedule unMarshallXml(File file) {
		FileSchedule fileSchedule = null;
		try {
			JAXBContext jc = JAXBContext.newInstance(FileSchedule.class);
			Unmarshaller jaxbUnmarshaller = jc.createUnmarshaller();
			fileSchedule = (FileSchedule) jaxbUnmarshaller.unmarshal(file);
			//LOG.trace(fileSchedule.getLastRun());
		} catch (Exception e) {
			LOG.fatal(e);
		}
		return fileSchedule;
	}
}
