<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<tags:layout activeTab="events_list">

<div class="row">
	<ul class="nav nav-pills nav-justified">
	  <li role="presentation" class="${type eq 'party' ? 'active' : ''}"><a href="events_list?type=party">Party <span class="badge">${party_event_count}</span></a></li>
	  <li role="presentation" class="${type eq 'bar' ? 'active' : ''}"><a href="events_list?type=bar">Bar <span class="badge">${bar_event_count}</span></a></li>
	  <li role="presentation" class="${type eq 'dining' ? 'active' : ''}"><a href="events_list?type=dining">Dining <span class="badge">${dining_event_count}</span></a></li>
	  <li role="presentation" class="${type eq 'culture' ? 'active' : ''}"><a href="events_list?type=culture">Culture <span class="badge">${culture_event_count}</span></a></li>
	</ul>
</div>
<div class="row">
	<div class="col-xs-12 list-group">
		<c:forEach var="event" items="${events}">
		  	<a href="#" class="list-group-item row">
		    	<div class="col-xs-12 col-md-2">
		    		<c:choose>
					      <c:when test='${not empty event.imageUrl}'>
					        <img src="${event.imageUrl}" class="img-rounded" height=100px>
					      </c:when>
					      <c:otherwise>
					      	<img src="resources/images/thumb_242x200.svg" class="img-rounded" height=100px>
					      </c:otherwise>
					</c:choose>
				</div>
		    	<div class="col-xs-12 col-md-8">
			    	<h4 class="list-group-item-heading">${event.title}</h4>
					<p class="list-group-item-text">${event.description}</p>
					<p class="list-group-item-text">Location: ${event.address}</p>
					<p class="list-group-item-text">Begins at ${event.formattedStart}</p>
					<c:if test='${not empty event.formattedEnd}'>
						<p class="list-group-item-text">Ends at ${event.formattedEnd}</p>
					</c:if>
				</div>
				<div class="col-xs-12 col-md-2">
					<span class="label label-success" style="display:block;font-size:100%">Likes ${event.likeCount}</span>
					<span class="label label-danger" style="display:block;font-size:100%">Dislikes ${event.dislikeCount}</span>
					<span class="label label-primary" style="display:block;font-size:100%">Going ${event.goingCount}</span>
				</div>
			</a>
		</c:forEach>
	</div>
</div>

</tags:layout>




