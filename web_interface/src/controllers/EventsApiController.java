package controllers;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.Event;

@WebServlet("/events_api")
public class EventsApiController extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static final long ONE_WEEK_IN_MILLIS = 7L * 24L * 3600L * 1000L;
	
    public EventsApiController() {
        super();
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String type = request.getParameter("type");
    	
    	String startString = request.getParameter("start");
    	Long start = (startString == null ? System.currentTimeMillis() : Long.valueOf(startString));
    	
    	String endString = request.getParameter("end");
    	Long end = (endString == null ? start + ONE_WEEK_IN_MILLIS : Long.valueOf(endString));
    	
    	if (type != null) {	
			List<Event> events = Event.loadAllInRange(type, start, end);
			response.getWriter().write(Event.toJson(events).toString());
    	}
    	else {
    		response.getWriter().write("{}");
    	}
	}
    
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String type = request.getParameter("type");
    	String rangeKey = request.getParameter("range_key");
    	String action = request.getParameter("action");
    	String undoString = request.getParameter("undo");
    	boolean undo = false; 
    	if (undoString != null)
    		undo = true;
    	
    	if (type != null && rangeKey != null && action != null) {
    		Event event = Event.loadSingle(type, rangeKey);
    		if (event != null) {
    			if (undo == false) {
    				if (action.equalsIgnoreCase("like")) {
    					event.setLikeCount(event.getLikeCount() + 1);
    				} else if (action.equalsIgnoreCase("dislike")) {
    					event.setDislikeCount(event.getDislikeCount() + 1);
    				} else if (action.equalsIgnoreCase("going")) {
    					event.setGoingCount(event.getGoingCount() + 1);
    				} else {
    					returnError(response, "Invalid action: " + action);
    					return;
    				}
    				if (event.save()) {
    					returnOk(response);
    				} else {
    					returnError(response, "An error occured while trying to update event");
    				}
    			}
    			else {
    				if (action.equalsIgnoreCase("like")) {
    					if (event.getLikeCount() > 0)
    						event.setLikeCount(event.getLikeCount() - 1);
    				} else if (action.equalsIgnoreCase("dislike")) {
    					if (event.getDislikeCount() > 0)
    						event.setDislikeCount(event.getDislikeCount() - 1);
    				} else if (action.equalsIgnoreCase("going")) {
    					if (event.getGoingCount() > 0)
    						event.setGoingCount(event.getGoingCount() - 1);
    				} else {
    					returnError(response, "Invalid action: " + action);
    				}
    				if (event.save()) {
    					returnOk(response);
    				} else {
    					returnError(response, "An error occured while trying to update event");
    				}
    			}
    		}
    		else {
    			returnError(response, "There is no event with this type and range_key.");
    		}
    	}
    	else {
    		returnError(response, "Missing parameter type, range_key or action.");
    	}
	}
	
    private void returnError(HttpServletResponse response, String message) throws IOException {
    	response.getWriter().write("{\"status\":\"error\",\"reason\":\"" + message + "\"}");
    }
    
    private void returnOk(HttpServletResponse response) throws IOException {
    	response.getWriter().write("{\"status\":\"ok\"}");
    }

}
