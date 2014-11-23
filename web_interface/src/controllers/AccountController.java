package controllers;

import helpers.UserSessionHelper;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import controllers.utils.FlashMessages;
import models.User;

@WebServlet("/account")
public class AccountController extends HttpServlet {

	private static final long serialVersionUID = 1L;
    
    public AccountController() {
        super();
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// checks if user already logged in
    	User user = UserSessionHelper.getLoggedUser(request.getSession());
    	if (user != null) {
    		request.setAttribute("user", user);
    		request.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(request, response);
    	}
    	else {
    		FlashMessages.addErrorMessage(request, "Please log in first.");
    		response.sendRedirect("home");
    	}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		User user = UserSessionHelper.getLoggedUser(request.getSession());
		
//		String username = request.getParameter("username");
		String currentPassword = request.getParameter("current-password");
		String newPassword = request.getParameter("new-password");
		String newPasswordConfirmation = request.getParameter("new-password-confirmation");
		
		if (!currentPassword.isEmpty() || !newPassword.isEmpty() || !newPasswordConfirmation.isEmpty()) {
			if (currentPassword.isEmpty() || !user.checkPassword(currentPassword)) {
				FlashMessages.addErrorMessageNow(request, "Current password is incorrect.");
			}
			else if (newPassword.isEmpty() || newPasswordConfirmation.isEmpty() || !newPassword.equals(newPasswordConfirmation)) {
				FlashMessages.addErrorMessageNow(request, "New password confirmation does not match new password.");
			}
			else {
//				user.setUsername(username);
				user.setPassword(newPassword);
				if(user.save()) {
					FlashMessages.addSuccessMessageNow(request, "Account updated");
				} else {
					FlashMessages.addErrorMessageNow(request, user.getValidationErrorMessage());
				}
			}
		}
		else {
//			user.setUsername(username);
//			if(user.save()) {
//				FlashMessages.addSuccessMessageNow(request, "Name updated");
//			} else {
//				FlashMessages.addErrorMessageNow(request, user.getValidationErrorMessage());
//			}
		}
		request.setAttribute("user", user);
		request.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(request, response);
	}		
						
}
