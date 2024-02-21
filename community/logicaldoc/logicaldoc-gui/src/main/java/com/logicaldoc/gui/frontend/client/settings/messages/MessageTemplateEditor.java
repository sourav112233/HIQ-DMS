package com.logicaldoc.gui.frontend.client.settings.messages;

import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.RichTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;

/**
 * This dialog is used to edit the message template
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.3.3
 */
public class MessageTemplateEditor extends Window {

	private ListGrid grid;

	private ListGridRecord record;

	private DynamicForm form = new DynamicForm();

	public MessageTemplateEditor(ListGrid grid, ListGridRecord record) {
		this.record = record;
		this.grid = grid;

		setHeaderControls(HeaderControls.HEADER_LABEL, HeaderControls.CLOSE_BUTTON);
		setTitle(I18N.message("messagetemplate") + " - " + record.getAttributeAsString("name"));
		setCanDragResize(true);
		setIsModal(true);
		setShowModalMask(true);
		setMargin(3);
		setWidth(670);
		setHeight(600);
		centerInPage();

		ToolStripButton save = new ToolStripButton();
		save.setTitle(I18N.message("save"));
		save.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
			@Override
			public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
				onSave();
				destroy();
			}
		});

		ToolStripButton close = new ToolStripButton();
		close.setTitle(I18N.message("close"));
		close.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
			@Override
			public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
				destroy();
			}
		});

		ToolStrip toolStrip = new ToolStrip();
		toolStrip.setHeight(20);
		toolStrip.setWidth100();
		toolStrip.addSpacer(2);
		toolStrip.addButton(save);
		toolStrip.addSeparator();
		toolStrip.addButton(close);

		addItem(toolStrip);

		TextAreaItem subject = ItemFactory.newTextAreaItemForAutomation("subject", "subject",
				record.getAttributeAsString("subject"), null, false);
		subject.setRequired(true);
		subject.setWidth("*");
		subject.setHeight(30);

		RichTextItem body = ItemFactory.newRichTextItemForAutomation("body", "body",
				record.getAttributeAsString("body"), null);
		body.setRequired(true);
		body.setWidth("*");
		body.setHeight("*");

		form.setWidth100();
		form.setHeight100();
		form.setTitleOrientation(TitleOrientation.TOP);
		form.setNumCols(1);
		form.setItems(subject, body);

		addItem(form);
	}

	private void onSave() {
		record.setAttribute("subject", form.getValueAsString("subject"));
		record.setAttribute("body", form.getValueAsString("body"));
		grid.refreshRow(grid.getRowNum(record));
	}
}