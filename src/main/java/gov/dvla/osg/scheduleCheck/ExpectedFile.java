package gov.dvla.osg.scheduleCheck;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public class ExpectedFile {
	
	private static final Logger LOG = LogManager.getLogger();
	
	private String path;
	private String pattern;
	private DateTime earliest;
	private DateTime latest;
	private int dayInt;
	private boolean resolved;
	private String failMsg;
	
	public ExpectedFile(String path, String pattern, DateTime earliest, DateTime latest, int dayInt, boolean resolved, String failMsg) {
		this.path=path;
		this.pattern=pattern;
		this.earliest=earliest;
		this.latest=latest;
		this.dayInt=dayInt;
		this.resolved=resolved;
		this.failMsg=failMsg;
	}
	
	public String getFailMsg() {
		return failMsg;
	}
	public void setFailMsg(String failMsg) {
		this.failMsg = failMsg;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getPattern() {
		return pattern;
	}
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	public DateTime getEarliest() {
		return earliest;
	}
	public void setEarliest(DateTime earliest) {
		this.earliest = earliest;
	}
	public DateTime getLatest() {
		return latest;
	}
	public void setLatest(DateTime latest) {
		this.latest = latest;
	}
	public boolean isResolved() {
		return resolved;
	}
	public void setResolved(Boolean resolved) {
		this.resolved = resolved;
	}
	public String getDayLiteral(){
		String dayLiteral = "";
		switch (this.dayInt){
			case 1: dayLiteral = "Monday"; break;
			case 2: dayLiteral = "Tuesday"; break;
			case 3: dayLiteral = "Wednesday"; break;
			case 4: dayLiteral = "Thursday"; break;
			case 5: dayLiteral = "Friday"; break;
			case 6: dayLiteral = "Saturday"; break;
			case 7:	dayLiteral = "Sunday"; break;	
		}
		return dayLiteral;
	}
	
	public void print(){
		DateTimeFormatter frmfmt = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");
		DateTimeFormatter tofmt = DateTimeFormat.forPattern("HH:mm");
		LOG.info(this.path + this.pattern + " " + this.getDayLiteral() + "\t " + this.earliest.toString(frmfmt) + " - " + this.latest.toString(tofmt));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dayInt;
		result = prime * result
				+ ((earliest == null) ? 0 : earliest.hashCode());
		result = prime * result + ((latest == null) ? 0 : latest.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		result = prime * result + (resolved ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpectedFile other = (ExpectedFile) obj;
		if (dayInt != other.dayInt)
			return false;
		if (earliest == null) {
			if (other.earliest != null)
				return false;
		} else if (!earliest.equals(other.earliest))
			return false;
		if (latest == null) {
			if (other.latest != null)
				return false;
		} else if (!latest.equals(other.latest))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.equals(other.pattern))
			return false;
		if (resolved != other.resolved)
			return false;
		return true;
	}

}