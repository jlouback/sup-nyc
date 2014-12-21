package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.Event;

@WebServlet("/recommended_events_api")
public class RecommendedEventsApiController extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static final long ONE_WEEK_IN_MILLIS = 7L * 24L * 3600L * 1000L;
	
    public RecommendedEventsApiController() {
        super();
    }

    public class CustomComparator implements Comparator<Event> {
        @Override
        public int compare(Event o1, Event o2) {
            return (-1) * (o1.getLikeCount().compareTo(o2.getLikeCount()));
        }
    }
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	Long start = System.currentTimeMillis();
    	Long end = start + 3600*ONE_WEEK_IN_MILLIS;

    	List<Event> events = new ArrayList<Event>();
    	
    	for (String type : Event.TYPES) {
    		List<Event> aux = Event.loadAllInRange(type, start, end);
    		events.addAll(aux);
    	}

    	Collections.sort(events, new CustomComparator());
    	if (events.size() > 10) {
    		events = events.subList(0, 10);
    	}
    	
		response.getWriter().write(Event.toJson(events).toString());
	}
    
}
