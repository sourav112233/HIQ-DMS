package com.logicaldoc.gui.frontend.client.metadata.template;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.beans.GUITemplate;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.widgets.EditingTabSet;
import com.logicaldoc.gui.frontend.client.services.TemplateService;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;

/**
 * This panel collects all template details
 * 
 * @author Matteo Caruso - LogicalDOC
 * @since 6.0
 */
public class TemplateDetailsPanel extends VLayout {

	protected GUITemplate template;

	protected Layout propertiesTabPanel;

	protected TemplatePropertiesPanel propertiesPanel;

	protected Layout securityTabPanel;

	protected TemplateSecurityPanel securityPanel;

	protected EditingTabSet tabSet;

	private TemplatesPanel templatesPanel;

	public TemplateDetailsPanel(TemplatesPanel panel) {
		super();
		this.templatesPanel = panel;

		setHeight100();
		setWidth100();
		setMembersMargin(10);

		Button saveButton = new Button(I18N.message("save"));
		saveButton.setAutoFit(true);
		saveButton.setMargin(2);
		saveButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onSave();
			}
		});
		saveButton.setLayoutAlign(VerticalAlignment.CENTER);

		HTMLPane spacer = new HTMLPane();
		spacer.setContents("<div>&nbsp;</div>");
		spacer.setWidth("70%");
		spacer.setOverflow(Overflow.HIDDEN);

		tabSet = new EditingTabSet(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onSave();
			}
		}, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (template.getId() != 0) {
					TemplateService.Instance.get().getTemplate(template.getId(), new AsyncCallback<GUITemplate>() {
						@Override
						public void onFailure(Throwable caught) {
							GuiLog.serverError(caught);
						}

						@Override
						public void onSuccess(GUITemplate template) {
							setTemplate(template);
						}
					});
				} else {
					setTemplate(new GUITemplate());
				}
			}
		});
		tabSet.setTabBarPosition(Side.TOP);
		tabSet.setTabBarAlign(Side.LEFT);
		tabSet.setWidth100();
		tabSet.setHeight100();

		Tab propertiesTab = new Tab(I18N.message("properties"));
		propertiesTabPanel = new HLayout();
		propertiesTabPanel.setWidth100();
		propertiesTabPanel.setHeight100();
		propertiesTab.setPane(propertiesTabPanel);
		tabSet.addTab(propertiesTab);

		Tab securityTab = new Tab(I18N.message("security"));
		securityTabPanel = new HLayout();
		securityTabPanel.setWidth100();
		securityTabPanel.setHeight100();
		securityTab.setPane(securityTabPanel);
		tabSet.addTab(securityTab);

		addMember(tabSet);
	}

	protected void refresh() {
		disableSave();

		ChangedHandler changeHandler = new ChangedHandler() {
			@Override
			public void onChanged(ChangedEvent event) {
				onModified();
			}
		};

		/*
		 * Prepare the standard properties tab
		 */
		if (propertiesPanel != null) {
			propertiesPanel.destroy();
			if (propertiesTabPanel.contains(propertiesPanel))
				propertiesTabPanel.removeMember(propertiesPanel);
		}
		propertiesPanel = new TemplatePropertiesPanel(template, changeHandler, this);
		propertiesTabPanel.addMember(propertiesPanel);

		/*
		 * Prepare the security tab
		 */
		if (securityPanel != null) {
			securityPanel.destroy();
			if (securityTabPanel.contains(securityPanel))
				securityTabPanel.removeMember(securityPanel);
		}
		securityPanel = new TemplateSecurityPanel(template, changeHandler);
		securityTabPanel.addMember(securityPanel);
	}

	public GUITemplate getTemplate() {
		return template;
	}

	public void setTemplate(GUITemplate template) {
		this.template = template;
		refresh();
	}

	private void disableSave() {
		tabSet.hideSave();
	}

	private void enableSave() {
		tabSet.displaySave();
	}

	public void onModified() {
		enableSave();
	}

	protected boolean validate() {
		boolean stdValid = propertiesPanel.validate();
		if (!stdValid)
			tabSet.selectTab(0);
		stdValid = securityPanel.validate();
		if (!stdValid)
			tabSet.selectTab(1);
		return stdValid;
	}

	protected void onSave() {
		if (validate()) {
			TemplateService.Instance.get().save(template, new AsyncCallback<GUITemplate>() {
				@Override
				public void onFailure(Throwable caught) {
					GuiLog.serverError(caught);
				}

				@Override
				public void onSuccess(GUITemplate result) {
					templatesPanel.updateRecord(result);
					setTemplate(result);
				}
			});
		}
	}
}
