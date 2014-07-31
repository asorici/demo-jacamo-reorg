package roombooking.core.logging;

import java.util.Date;

import roombooking.core.request.Request.RequestType;


public class LoggedRequestType {
	private RequestType type;
	private int amount;
	private Date logDate;
	
	public LoggedRequestType(RequestType type, int amount, Date logDate) {
		this.type = type;
		this.amount = amount;
		this.logDate = logDate;
	}

	public RequestType getType() {
		return type;
	}

	public int getAmount() {
		return amount;
	}
	
	public Date getLogDate() {
		return logDate;
	}
}
