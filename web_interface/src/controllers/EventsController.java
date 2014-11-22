package controllers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/tweet_points")
public class EventsController extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public EventsController() {
        super();
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	if (request.getParameter("type") != null) {
			String keyword = (String) request.getParameter("type");
//			try {
				response.getWriter().write("{}");
//			} catch (JSONException e) {
//				e.printStackTrace();
//				response.getWriter().write("{}");
//			}
    	}
    	else {
    		response.getWriter().write("{}");
    	}
    	
	}
	

}
