package controllers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.MobileUser;

@WebServlet("/mobile_users_api")
public class MobileUsersApiController extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public MobileUsersApiController() {
        super();
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String email = request.getParameter("email");
    	String encryptedPassword = request.getParameter("encryptedPassword");
    	
    	if ((email != null) && (encryptedPassword != null)) {	
			MobileUser user = MobileUser.loadFromDynamo(email);
			
			if ((user != null) && (user.checkEncryptedPassword(encryptedPassword))) {
				returnOk(response);
			} else {
				returnError(response, "authentication failed");
			}
    	}
    	else {
    		returnError(response, "missing parameters in the request");
    	}
	}
    
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String email = request.getParameter("email");
    	String encryptedPassword = request.getParameter("encryptedPassword");
    	
    	
    	if (email != null && encryptedPassword != null) {	
    		MobileUser user = new MobileUser();
    		user.setEmail(email);
    		user.setEncryptedPassword(encryptedPassword);
    		
			if (user.valid()) {
				if (user.save()) {
					returnOk(response);
				} else {
					returnError(response, "email already exists");
				}
			} else {
				returnError(response, user.getValidationErrorMessage());
			}
    	}
    	else {
    		returnError(response, "missing parameters in the request");
    	}
	}
	
    private void returnError(HttpServletResponse response, String message) throws IOException {
    	response.getWriter().write("{\"status\":\"error\",\"reason\":\"" + message + "\"}");
    }
    
    private void returnOk(HttpServletResponse response) throws IOException {
    	response.getWriter().write("{\"status\":\"ok\"}");
    }

}
