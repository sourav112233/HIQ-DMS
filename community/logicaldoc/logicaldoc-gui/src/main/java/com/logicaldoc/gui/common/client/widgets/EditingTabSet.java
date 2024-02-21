package com.logicaldoc.gui.common.client.widgets;

import com.logicaldoc.gui.common.client.i18n.I18N;
import com.smartgwt.client.types.TabBarControls;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * A tabset capable of showing a save widget. Useful for build up details panels
 * with editing capabilities
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.2.3
 */
public class EditingTabSet extends TabSet {

	private HLayout savePanel = null;

	private Button saveButton;

	private Button cancelButton;

	public EditingTabSet(ClickHandler saveHandler, ClickHandler cancelHandler) {
		saveButton = new Button(I18N.message("save"));
		saveButton.setAutoFit(true);
		saveButton.setMargin(2);
		saveButton.addClickHandler(saveHandler);
		saveButton.setLayoutAlign(VerticalAlignment.CENTER);

		cancelButton = new Button(I18N.message("cancel"));
		cancelButton.setAutoFit(true);
		cancelButton.setMargin(2);
		cancelButton.setLayoutAlign(VerticalAlignment.CENTER);
		cancelButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				hideSave();
			}
		});
		if (cancelHandler != null)
			cancelButton.addClickHandler(cancelHandler);

		savePanel = new HLayout();
		savePanel.setHeight(20);
		savePanel.setMembersMargin(5);
		savePanel.setStyleName("warn");
		savePanel.setMembers(saveButton, cancelButton);

		savePanel.hide();

		setTabBarControls(savePanel, TabBarControls.TAB_SCROLLER, TabBarControls.TAB_PICKER);
	}

	public void displaySave() {
		if (saveButton != null)
			saveButton.setDisabled(false);
		if (cancelButton != null)
			cancelButton.setDisabled(false);
		setStyleName("warn");
		savePanel.show();
	}

	public void hideSave() {
		setStyleName(null);
		savePanel.hide();
	}

	public void disableSave() {
		if (saveButton != null)
			saveButton.setDisabled(true);
		if (cancelButton != null)
			cancelButton.setDisabled(true);
	}
}