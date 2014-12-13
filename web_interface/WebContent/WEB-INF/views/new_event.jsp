<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<tags:layout activeTab="events_list">
	<h1>New Event</h1>
	<div class="row">
		<div class="col-xs-12 col-md-8 col-lg-6">
			<form role="form" action="new_event" method="post" id="new_event_form" enctype="multipart/form-data">
				<div class="form-group">
					<label for="type">Type</label>
					<select name="type" class="form-control">
						<option disabled selected> -- select type -- </option>
						<c:forEach var="event_type" items="${event_types}"> 
							<option value="${event_type}">${event_type}</option>
						</c:forEach>
					</select>
				</div>
				<div class="form-group">
					<label for="title">Title*</label>
					<input type="text" class="form-control" name="title" placeholder="Enter title" value="${event.title}" />
				</div>
				<div class="form-group">
					<label for="description">Description</label>
					<textarea class="form-control" name="description" placeholder="Enter description">${event.description}</textarea>
				</div>
				<div class="form-group">
					<label for="title">Address*</label>
					<div class="input-group">
						<input id="address" type="text" class="form-control" name="address" placeholder="Enter title" value="${event.addressNoCity}" />
					 	<span class="input-group-addon">New York, NY</span>
					</div>
				</div>
				<div class="form-group">
					<label for="start">Start*</label>
	                <div class='input-group date' id='start_datetimepicker'>
	                    <input type='text' name="start" placeholder="Enter start time" value="${event.formattedStart}" class="form-control" />
	                    <span class="input-group-addon"><span class="glyphicon glyphicon-calendar"></span></span>
	                </div>
	            </div>
	            <div class="form-group">
					<label for="start">End</label>
	                <div class='input-group date' id='end_datetimepicker'>
	                    <input type='text' name="end" placeholder="Enter end time" value="${event.formattedEnd}" class="form-control" />
	                    <span class="input-group-addon"><span class="glyphicon glyphicon-calendar"></span></span>
	                </div>
	            </div>
				<div class="form-group">
					<label for="video">Image</label>
					<input type="file" name="imagefile">
					<p class="help-block">Upload you image here</p>
				</div>
				<input id="latitude" type="hidden" name="latitude"/>
				<input id="longitude" type="hidden" name="longitude"/>
				<button type="submit" class="btn btn-primary">Submit</button>
				<a class="btn" href="events_list" role="button">Cancel</a>
			</form>
		</div>
		<div id="map-canvas" class="col-xs-12 col-md-4 col-lg-6" style="height:450px"></div>
	</div>
	
<script>

$(function () {
    $('#start_datetimepicker').datetimepicker();
    $('#end_datetimepicker').datetimepicker();
});

$(window).load(mapInit);

var geocoder;
var map;
var marker;

function mapInit() {
	geocoder = new google.maps.Geocoder();
    var mapOptions = {
      zoom: 11,
      center: new google.maps.LatLng(40.753, -74.002)
    }
    map = new google.maps.Map(document.getElementById("map-canvas"), mapOptions);
	
    function codeAddress(callback) {
        var address = $("#address").val() + ", New York, NY";
        
        geocoder.geocode({'address': address}, function(results, status) {
          	if (status == google.maps.GeocoderStatus.OK) {
          		var location = results[0].geometry.location;
            	
          		map.setCenter(location);

          		if (marker != undefined) {
          			marker.setMap(null);
          		}
          		
          		marker = new google.maps.Marker({
                	map: map,
                	position: location
           		});
            	
            	$("#latitude").val(location.lat());
            	$("#longitude").val(location.lng());
            	
            	if (callback) {
            		callback();
            	}
          	} 
          	else {
        		//alert("Geocode was not successful for the following reason: " + status);
        	}
        });
    }
    
    if ($("#address").val() != "") {
    	codeAddress();
    }
    
	// on select box change
	$("#address").change(function() { codeAddress(); });
	
	// trick to force geocoding if user presses enter when writting address
	$("#new_event_form").submit(function(event) {
		if($("#address").is(':focus')) {
			$("#address").focusout();
			event.preventDefault();
		}
	});
};
  

</script>
</tags:layout>