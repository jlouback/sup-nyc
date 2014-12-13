package controllers;

import helpers.S3Helper;
import helpers.UserSessionHelper;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import models.Event;
import models.User;
import controllers.utils.FlashMessages;

@WebServlet("/new_event")
@MultipartConfig(fileSizeThreshold=1024*1024*2, // 2MB
                 maxFileSize=1024*1024*2,      // 2MB
                 maxRequestSize=1024*1024*10)   // 10MB
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
	    		
	    		if (event.valid()) { 
		    		Part imagefilepart = request.getPart("imagefile");
		    		if ((imagefilepart != null) && imagefilepart.getSize() > 0) {
		    			event.setImageUrl("https://s3.amazonaws.com/" + S3Helper.IMAGES_BUCKET + "/" + event.getRangeKeyBeginningWithStart().replaceAll("\\s", "_"));
		    		}
	    		}
	    		
	    		if (event.save()) {
	    			user.incrementEventCount(event.getType());
	    			user.save();
	    			
	    			Part imagefilepart = request.getPart("imagefile");
	    			if ((imagefilepart != null) && imagefilepart.getSize() > 0) {
	    				InputStream is = imagefilepart.getInputStream();
	    				String contentType = imagefilepart.getContentType();
	    				String key = event.getRangeKeyBeginningWithStart().replaceAll("\\s", "_");
	    				S3Helper.getInstance().uploadImage(S3Helper.IMAGES_BUCKET, key, is, contentType);
	    			}
	    			
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
