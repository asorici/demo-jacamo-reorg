package roombooking.core.logging;

import java.util.Date;

import roombooking.core.Room;


public class LoggedEnergy {
	private Date logDate;
	private int amount;
	private Room room;
	
	public LoggedEnergy(Date logDate, int amount, Room room) {
		this.logDate = logDate;
		this.amount = amount;
		this.room = room;
	}

	public Date getLogDate() {
		return logDate;
	}

	public int getAmount() {
		return amount;
	}

	public Room getRoom() {
		return room;
	}
}
