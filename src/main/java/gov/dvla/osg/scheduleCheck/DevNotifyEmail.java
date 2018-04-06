package gov.dvla.osg.scheduleCheck;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import email.credentials;

public class DevNotifyEmail {

	private static final String credentialsFile = "//aiw//osg//resources//config//email.xml";
	private static final String contactsFile = "//aiw//osg//resources//config//contacts.xml";
	
	/**
	 * Constructs email from settings in the email config file and sends to Dev Team members.
	 * @param subjectLine Subject line of the email
	 * @param msgText Email text body
	 * @param recipients comma separated list of email addresses
	 */
	public static void send(String subjectLine, String msgText) throws MessagingException {
		
		// load SMTP configuration from config file
		credentials security;
		try {
			security = DevNotifyEmail.loadCredentials();
		} catch (JAXBException ex) {
			throw new MessagingException("Unable to load email config: " + credentialsFile);
		}
		
		String username = security.getUsername();
		String password = security.getPassword();
		String host = security.getHost();
		String port = security.getPort();
		String from = security.getFrom();
		
		// load dev team contact email addresses
		Address[] contacts;
		try {
			contacts = getContacts();
		} catch (Exception ex) {
			throw new MessagingException("Unable to load contacts file: " + contactsFile);
		}

		// Email salutation and signature lines
		String bodyHead = "Hello,\n\n";
		String bodyFoot = "\nPlease investigate ASAP\n\nThanks";

		// Setup mail server
		Properties properties = new Properties();
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", port);
		properties.put("mail.smtp.auth", "false");
		properties.put("mail.smtp.starttls.ename", "false");

		// Setup authentication, get session
		Session emailSession = Session.getInstance(properties, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(emailSession);
			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));
			// Set To: header field of the header.
			message.setRecipients(Message.RecipientType.TO, contacts);
			// Set Subject: header field
			message.setSubject(subjectLine);
			// Now set the actual message body
			message.setText(bodyHead + msgText + bodyFoot);
			// Send message
			Transport.send(message);
		} catch (MessagingException mex) {
			throw mex;
		}
	}

	/**
	 * Converts the XML in the SMTP config file into a credetials object
	 * @return SMTP configuration information.
	 * @throws JAXBException file does not contain valid XML.
	 */
	private static credentials loadCredentials() throws JAXBException {
		File file = new File(credentialsFile);
		JAXBContext jc = JAXBContext.newInstance(credentials.class);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (credentials) unmarshaller.unmarshal(file);
	}
	
	/**
	 * Gets the list of dev team members' email addresses from the config file.
	 * @return dev team email addresses.
	 * @throws IOException config file cannot be located.
	 * @throws AddressException file contains an invalid email address.
	 */
	private static Address[] getContacts() throws IOException, AddressException {
		List<String> list = Files.readAllLines(Paths.get(contactsFile));
		Address[] addresses = new Address[list.size()];
		for (int i = 0; i < list.size(); i++) {
		    addresses[i] = new InternetAddress(list.get(i));
		}
		return addresses;
	}
	
}
