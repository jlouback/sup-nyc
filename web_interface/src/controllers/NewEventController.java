package controllers;

import helpers.UserSessionHelper;

import java.io.IOException;
import java.text.ParseException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.Event;
import models.User;
import controllers.utils.FlashMessages;

@WebServlet("/new_event")
public class NewEventController extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static final String NEW_EVENT_VIEW = "/WEB-INF/views/new_event.jsp";
	
    public NewEventController() {
        super();
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// checks if user already logged in
    	if (UserSessionHelper.getLoggedUser(request.getSession()) != null) {
    		FlashMessages.extractFlashMessages(request);
    		
    		Event event = new Event();
    		request.setAttribute("event", event);
    		request.setAttribute("event_types", Event.TYPES);
    		
    		request.getRequestDispatcher(NEW_EVENT_VIEW).forward(request, response);
    	}
    	else {
    		FlashMessages.addErrorMessage(request, "Please log in first.");
    		response.sendRedirect("home");
    	}
	}
    
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// checks if user already logged in
    	User user = UserSessionHelper.getLoggedUser(request.getSession());
    	if (user != null) {
    		Event event;
			try {
				event = Event.buildFromForm(request.getParameterMap());
	    		event.setHostUsername(user.getUsername());
	    		
	    		if (event.save()) {
	    			user.incrementEventCount(event.getType());
	    			user.save();
	    			
	    			FlashMessages.addSuccessMessage(request, "Event created");
		    		response.sendRedirect("events_list");
	    		}
	    		else {
	    			FlashMessages.addErrorMessageNow(request, event.getValidationErrorMessage());
	    			request.setAttribute("event", event);
	    			request.setAttribute("event_types", Event.TYPES);
	    			request.getRequestDispatcher(NEW_EVENT_VIEW).forward(request, response);
	    		}
			} catch (ParseException e) {
				e.printStackTrace();
				FlashMessages.addErrorMessage(request, "An error occurred parsing the parameters.");
	    		response.sendRedirect("new_event");
			}
    	}
    	else {
    		FlashMessages.addErrorMessage(request, "Please log in first.");
    		response.sendRedirect("home");
    	}
    	
    	
    }
}
