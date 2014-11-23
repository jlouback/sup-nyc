package controllers;

import helpers.EmailSenderHelper;
import helpers.UserSessionHelper;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.User;
import controllers.utils.FlashMessages;
import controllers.utils.StringRenderer;

@WebServlet("/home")
public class HomeController extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static final String FIRST_PAGE_URL = "events_list";
	private static final String HOME_VIEW = "/WEB-INF/views/home.jsp";
	
    public HomeController() {
        super();
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// checks if user already logged in
    	if (UserSessionHelper.getLoggedUser(request.getSession()) != null) {
    		response.sendRedirect(FIRST_PAGE_URL);
    	}
    	else {
	    	FlashMessages.extractFlashMessages(request);
	    	request.getRequestDispatcher(HOME_VIEW).forward(request, response);
    	}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String formName = request.getParameter("form_name");
	
		if (formName.equals("login")) {
			String username = request.getParameter("login_username");
			String password = request.getParameter("login_password");
			User user = User.loadFromDynamo(username);
		
			if (user != null && user.checkPassword(password)) {
				if (user.getActivated()) {
					if (UserSessionHelper.login(user, request.getSession())) {
						response.sendRedirect(FIRST_PAGE_URL);
					}
					else {
						FlashMessages.addErrorMessageNow(request, "An error occured during log in.");
						request.getRequestDispatcher(HOME_VIEW).forward(request, response);
					}
				} else {
					FlashMessages.addErrorMessageNow(request, "You have to activate your account first. Check your email.");
					request.getRequestDispatcher(HOME_VIEW).forward(request, response);
				}
			}
			else {
				User loginUser = new User();
				loginUser.setUsername(username);
				loginUser.setPassword(password);
				
				request.setAttribute("login_user", loginUser);
				FlashMessages.addErrorMessageNow(request, "Email or password is wrong.");
				request.getRequestDispatcher(HOME_VIEW).forward(request, response);
			}
		} 
		else {
			User user = User.buildFromForm(request.getParameterMap());
			if (user.save()) {
				// sends email for the user to confirm email address
				StringRenderer emailRenderer = new StringRenderer(response);
				request.setAttribute("user", user);
				request.getRequestDispatcher("/WEB-INF/views/signup_confirmation_email.jsp").forward(request, emailRenderer);
				EmailSenderHelper.getInstance().sendEmail(user.getEmail(), "Welcome to Sup NYC!", emailRenderer.getOutput());
				
				request.removeAttribute("user");
				FlashMessages.addSuccessMessageNow(request, "Account created successfully!<br>You will receive an email to confirm the validity of your email address.");
				request.getRequestDispatcher(HOME_VIEW).forward(request, response);
			}
			else {
				FlashMessages.addErrorMessageNow(request, user.getValidationErrorMessage());
				request.setAttribute("signup_user", user);
				request.getRequestDispatcher(HOME_VIEW).forward(request, response);
			}
		}
	}
	
}
