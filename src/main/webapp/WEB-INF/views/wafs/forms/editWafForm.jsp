<%@ include file="/common/taglibs.jsp"%>
	
<div class="modal-header">
	<h4 id="myModalLabel">Edit WAF <c:out value="${ waf.name }"/></h4>
</div>
<form:form id="wafForm${ waf.id }" style="margin-bottom:0px;" modelAttribute="waf" method="post" action="${ fn:escapeXml(updateUrl) }">
	<div class="modal-body">
		<table>
			<tbody>
			    <tr>
					<td class="no-color">Name</td>
					<td class="inputValue no-color">
						<form:input style="margin:5px;" id="nameInput" path="name" cssClass="focus" size="50" maxlength="50" value="${ waf.name }"/>
					</td>
					<td class="no-color" style="padding-left: 5px">
						<form:errors path="name" cssClass="errors" />
					</td>
				</tr>
				<tr>
					<td class="no-color">Type</td>
					<td class="inputValue no-color">
						<form:select style="margin:5px;" id="typeSelect" path="wafType.id">
							<c:forEach var="type" items="${ wafTypeList }">
								<option value="${ type.id }"
								<c:if test="${ type.name == waf.wafType.name }">
									selected=selected
								</c:if>
								><c:out value="${ type.name }"/></option>
							</c:forEach>
						</form:select>
					</td>
					<td class="no-color" style="padding-left: 5px">
						<form:errors path="wafType.id" cssClass="errors" />
					</td>
				</tr>
			</tbody>
		</table>
	</div>
	<div class="modal-footer">
		<button id="closeEditWafModalButton${ waf.id }" class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
		<a id="submitWafEditModal${ waf.id }" class="modalSubmit btn btn-primary" data-success-div="appWafDiv">Update WAF</a>
	</div>
</form:form>
