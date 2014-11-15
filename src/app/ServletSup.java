package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

@SuppressWarnings("serial")
public class ServletSup extends HttpServlet{
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	    response.setContentType("application/json");
	    PrintWriter out = response.getWriter();
	    try {
	    //Get the entered keyword, search for appropriate events
	    String key = request.getParameter("keyword");
	    DatabaseHelper dbHelper = new DatabaseHelper().withCredentials(new AWSCredentialsProviderChain(
	            new InstanceProfileCredentialsProvider(),
	            new ClasspathPropertiesFileCredentialsProvider()).getCredentials());
	    List<String> locations = dbHelper.getEventLocations(dbHelper.getEventsByType(key));
	    JSONArray array = new JSONArray(locations);
	    //Populate with latitude and longitude of events
	    JSONObject result = new JSONObject();
	    result.put("success", true);
	    result.put("markers",array);
	    String jsonResult = result.toString();
	    out.println(jsonResult);
	    } catch (Exception ex) {
	        out.println("There was an error.");
	    } finally {
	        out.flush();
	        out.close();
	    }
	}

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }


}
