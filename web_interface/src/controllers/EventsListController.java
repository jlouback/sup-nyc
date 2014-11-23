package controllers;

import helpers.UserSessionHelper;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.Event;
import models.User;
import controllers.utils.FlashMessages;

@WebServlet("/events_list")
public class EventsListController extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public EventsListController() {
        super();
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// checks if user already logged in
    	User user = UserSessionHelper.getLoggedUser(request.getSession());
    	if (user != null) {
    		FlashMessages.extractFlashMessages(request);
    		String type = request.getParameter("type");
    		if (type == null) {	type = "party";	}
    		
    		List<Event> events = Event.loadAllFromUser(user, type);
    		
    		request.setAttribute("events", events);
    		request.setAttribute("type", type);
    		for (String eventType : Event.TYPES) {
    			request.setAttribute(eventType + "_event_count", user.getEventCount(eventType));
    		}
    		
    		request.getRequestDispatcher("/WEB-INF/views/events_list.jsp").forward(request, response);
    	}
    	else {
    		FlashMessages.addErrorMessage(request, "Please log in first.");
    		response.sendRedirect("home");
    	}
	}
}
