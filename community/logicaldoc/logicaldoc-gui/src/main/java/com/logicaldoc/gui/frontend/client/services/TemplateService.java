package com.logicaldoc.gui.frontend.client.services;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.logicaldoc.gui.common.client.LDRpcRequestBuilder;
import com.logicaldoc.gui.common.client.ServerException;
import com.logicaldoc.gui.common.client.beans.GUITemplate;

/**
 * The client side stub for the Template Service. This service gives all needed
 * methods to handle templates.
 */
@RemoteServiceRelativePath("template")
public interface TemplateService extends RemoteService {
	/**
	 * Deletes a given template
	 * 
	 * @param templateId identifier of the template
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	public void delete(long templateId) throws ServerException;

	/**
	 * Creates or updates a template
	 * 
	 * @param template the template to save
	 * 
	 * @return the saved template
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	public GUITemplate save(GUITemplate template) throws ServerException;

	/**
	 * Loads a given template from the database
	 * 
	 * @param templateId identifier of the template
	 * 
	 * @return the template retrieved by the server application
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	public GUITemplate getTemplate(long templateId) throws ServerException;

	/**
	 * Counts the documents of a given template
	 * 
	 * @param templateId identifier of the template
	 * 
	 * @return number of documents referencing the template
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	public long countDocuments(long templateId) throws ServerException;

	public static class Instance {
		private static TemplateServiceAsync instance;

		public static TemplateServiceAsync get() {
			if (instance == null) {
				instance = GWT.create(TemplateService.class);
				((ServiceDefTarget) instance).setRpcRequestBuilder(new LDRpcRequestBuilder());
			}
			return instance;
		}
	}
}