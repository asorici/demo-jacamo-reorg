package roombooking.core.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RequestPool {
	private List<Request> requestQueue;
	
	public RequestPool() {
		requestQueue = new ArrayList<Request>();
	}
	
	public synchronized void pushRequest(Request req) {
		requestQueue.add(req);
	}
	
	public synchronized Request popRequest() {
		return requestQueue.remove(0);
	}
	
	public Request head() {
		return requestQueue.get(0);
	}
	
	public synchronized Request getRequestById(int id) {
		for (int i = 0; i < requestQueue.size(); i++) {
			Request req = requestQueue.get(i);
			if (req.getId() == id) {
				return req;
			}
		}
		
		return null;
	}
	
	public synchronized void sort() {
		Collections.sort(requestQueue, new Comparator<Request>() {

			@Override
			public int compare(Request r1, Request r2) {
				return r1.getSubmissionDate().compareTo(r2.getSubmissionDate());
			}
		});
	}
}
