<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="">
<meta name="author" content="">
<link rel="icon" href="assets/twiticon2.png">
<title>SupNYC</title>

<!-- Bootstrap core CSS -->
<link href="css/bootstrap.min.css" rel="stylesheet">
<!-- Silvio Moreto's select -->
<link href="css/bootstrap-select.css" rel="stylesheet">
<script type="text/javascript"
	src="https://maps.googleapis.com/maps/api/js?sensor=false"></script>
<script type="text/javascript"
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<script src="js/bootstrap.min.js"></script>
<script src="js/bootstrap-select.js"></script>
<script type="text/javascript">
	$('.selectpicker').selectpicker();
</script>
<link href="css/custom.css" rel="stylesheet">
<script>
	var map;
	function initialize() {
		var mapProp = {
			center : new google.maps.LatLng(40.7127837, -74.0059413),
			zoom : 11,
			mapTypeId : google.maps.MapTypeId.ROADMAP
		};
		map = new google.maps.Map(document.getElementById("googleMap"),
				mapProp);
	}
	
	google.maps.event.addDomListener(window, 'load', initialize);
	
	function clickGo() {
		if($("#keyword").val() == 0){
			return false;
		}
			var lat;
			var lng;
			$.getJSON("Sup", {keyword:$('#keyword').val()}, function(data) {
				if(data.success && data.markers.length > 0){
					var bounds = new google.maps.LatLngBounds ();
					for(var i = 0; i < data.markers.length; i++){
						var marker = data.markers[i].split(",");
		 				lat = marker[0];
		 				lng = marker[1];
		 				var location = new google.maps.LatLng(lat, lng);
		 				var marker = new google.maps.Marker({
		 					position : location,
		 					map : map
		 				});
		 				bounds.extend(location);
		 				marker.setMap(map);
					}
					map.fitBounds(bounds);
				}

			});
			return false; // prevents the page from refreshing before JSON is read from server response
	}
</script>
</head>

<body>

	<div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
		<div class="container">
			<div class="navbar-header">
				<a class="navbar-brand" href="#"
					style="padding: 0; padding-top: 4px;"><img class="navbar-icon"
					src="assets/twiticon.png" height="40" width="40"></a>
			</div>
			<div class="collapse navbar-collapse" style="float: right;">
				<ul class="nav navbar-nav">
					<li class="active"><a href="#">Home</a></li>
					<li><a href="#about">About</a></li>
				</ul>
			</div>
			<!--/.nav-collapse -->
		</div>
	</div>
	<div class="container fill">
		<div class="starter-template">
			<h1>Sup? NYC</h1>
			<p class="lead">A curated feed of of events</p>
		</div>
		<div id="main-content" class="fill">
			<form name="keyForm" action='Sup' method='get' target="googleMap">
				<select class="selectpicker" name="keyword" id="keyword">
					<option value="0">Select an event type</option>
					<option>Party</option>
					<option>Culture</option>
					<option>Dining</option>
					<option>Bars</option>
				</select>
				<button type="submit" class="btn" id="go" onclick="return clickGo();">Go</button>
			</form>
			<div id="googleMap" name="googleMap"></div>
			<input type="hidden" name="markers" value="someValue">
		</div>
	</div>
	<!-- /.container -->
</body>
</html>
