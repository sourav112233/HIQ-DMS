package com.logicaldoc.gui.frontend.client.metadata.form;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.beans.GUIContact;
import com.logicaldoc.gui.common.client.beans.GUIEmail;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.common.client.validators.EmailValidator;
import com.logicaldoc.gui.frontend.client.services.FormService;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RichTextItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.EditCompleteEvent;
import com.smartgwt.client.widgets.grid.events.EditCompleteHandler;
import com.smartgwt.client.widgets.grid.events.EditorExitEvent;
import com.smartgwt.client.widgets.grid.events.EditorExitHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

/**
 * This is the form used to send form invitations
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.7
 */
public class FormInvitationDialog extends Window {

	private ValuesManager vm = new ValuesManager();

	private ListGrid recipientsGrid;

	private RichTextItem message;

	final SelectItem from = ItemFactory.newEmailFromSelector("from", null);

	public FormInvitationDialog(long formId) {
		super();

		addCloseClickHandler(new CloseClickHandler() {
			@Override
			public void onCloseClick(CloseClickEvent event) {
				destroy();
			}
		});

		setHeaderControls(HeaderControls.HEADER_LABEL, HeaderControls.CLOSE_BUTTON);
		setTitle(I18N.message("invitationtofillform"));
		setWidth(580);
		setHeight(430);
		setCanDragResize(true);
		setIsModal(true);
		setShowModalMask(true);
		centerInPage();
		setPadding(5);
		setAutoSize(false);

		SectionStack recipientsStack = prepareRecipientsGrid();

		final DynamicForm form = new DynamicForm();
		form.setValuesManager(vm);
		form.setWidth100();
		form.setHeight("*");
		form.setMargin(5);
		form.setTitleOrientation(TitleOrientation.LEFT);
		form.setNumCols(2);

		message = ItemFactory.newRichTextItemForEmail("message", "message", I18N.message("invitedyoutofillform"), null);
		message.setWidth("*");
		message.setHeight(150);
		message.setBrowserSpellCheck(true);
		message.setColSpan(2);

		form.setFields(from, message);

		final IButton send = new IButton();
		send.setTitle(I18N.message("send"));
		send.setMargin(3);
		send.setHeight(30);
		send.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				vm.validate();
				if (!vm.hasErrors()) {
					GUIEmail mail = new GUIEmail();
					mail.setFrom(new GUIContact(from.getValueAsString()));
					mail.setMessage(message.getValue().toString());

					List<String> to = new ArrayList<String>();
					List<String> cc = new ArrayList<String>();
					List<String> bcc = new ArrayList<String>();
					ListGridRecord[] records = recipientsGrid.getRecords();
					for (int i = 0; i < records.length; i++) {
						if (!recipientsGrid.validateCell(i, "email"))
							continue;

						ListGridRecord record = records[i];
						if (record.getAttribute("email") == null || record.getAttribute("type").trim().equals(""))
							continue;
						if ("to".equals(record.getAttribute("type")))
							to.add(record.getAttribute("email").trim());
						else if ("cc".equals(record.getAttribute("type")))
							cc.add(record.getAttribute("email").trim());
						else
							bcc.add(record.getAttribute("email").trim());
					}

					if (to.isEmpty() && cc.isEmpty()) {
						SC.warn(I18N.message("leastvalidrecipient"));
						return;
					}

					send.disable();

					List<GUIContact> tos = new ArrayList<GUIContact>();
					for (String email : to)
						tos.add(new GUIContact(email));
					mail.setTos(tos.toArray(new GUIContact[0]));

					List<GUIContact> ccs = new ArrayList<GUIContact>();
					for (String email : cc)
						ccs.add(new GUIContact(email));
					mail.setCcs(ccs.toArray(new GUIContact[0]));

					List<GUIContact> bccs = new ArrayList<GUIContact>();
					for (String email : bcc)
						bccs.add(new GUIContact(email));
					mail.setBccs(bccs.toArray(new GUIContact[0]));

					LD.contactingServer();

					FormService.Instance.get().invite(formId, mail, I18N.getLocale(), new AsyncCallback<Void>() {

						@Override
						public void onFailure(Throwable caught) {
							LD.clearPrompt();
							GuiLog.serverError(caught);
							send.enable();
							destroy();
						}

						@Override
						public void onSuccess(Void arg0) {
							LD.clearPrompt();
							send.enable();
							GuiLog.info(I18N.message("messagesent"));
							destroy();
						}
					});
				}
			}
		});

		HLayout buttons = new HLayout();
		buttons.setMembers(send);
		buttons.setHeight(30);

		addItem(recipientsStack);
		addItem(form);
		addItem(buttons);
	}

	private SectionStack prepareRecipientsGrid() {
		SectionStack sectionStack = new SectionStack();
		sectionStack.setWidth100();
		sectionStack.setHeight(150);
		sectionStack.setMargin(6);

		SectionStackSection section = new SectionStackSection("<b>" + I18N.message("recipients") + "</b>");
		section.setCanCollapse(false);
		section.setExpanded(true);

		ListGridField email = new ListGridField("email", I18N.message("email"));
		email.setWidth("*");
		email.setCanFilter(true);
		FormItem emailItem = ItemFactory.newEmailComboSelector("email", "email");
		emailItem.setRequired(true);
		emailItem.setWidth("*");
		emailItem.addKeyPressHandler(new KeyPressHandler() {

			@Override
			public void onKeyPress(KeyPressEvent event) {
				// Delete the row
				if (event.getKeyName().equals("Backspace")) {
					ListGridRecord selection = recipientsGrid.getSelectedRecord();
					if (selection.getAttribute("email") == null
							|| selection.getAttribute("email").toString().equals(""))
						if (recipientsGrid.getDataAsRecordList().getLength() > 1)
							recipientsGrid.removeSelectedData();
				}

			}
		});
		email.setEditorType(emailItem);
		email.setValidators(new EmailValidator());

		ListGridField type = new ListGridField("type", I18N.message(" "));
		type.setCanFilter(true);
		type.setWidth(50);
		type.setCanEdit(true);
		type.setEditorType(ItemFactory.newRecipientTypeSelector("type"));

		recipientsGrid = new ListGrid();
		recipientsGrid.setShowRecordComponents(true);
		recipientsGrid.setShowRecordComponentsByCell(true);
		recipientsGrid.setAutoFetchData(true);
		recipientsGrid.setCanEdit(true);
		recipientsGrid.setShowHeader(false);
		recipientsGrid.setWidth100();
		recipientsGrid.setEditEvent(ListGridEditEvent.CLICK);
		recipientsGrid.setFields(type, email);

		recipientsGrid.addEditCompleteHandler(new EditCompleteHandler() {

			@Override
			public void onEditComplete(EditCompleteEvent event) {
				addEmptyRow();
			}
		});

		recipientsGrid.addEditorExitHandler(new EditorExitHandler() {

			@Override
			public void onEditorExit(EditorExitEvent event) {
				addEmptyRow();
			}
		});

		recipientsGrid.setCellFormatter(new CellFormatter() {
			@Override
			public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
				if (value == null)
					return null;
				if (colNum == 0)
					return I18N.message(value.toString());
				else
					return value.toString();
			}
		});

		ListGridRecord record = new ListGridRecord();
		record.setAttribute("type", "to");
		record.setAttribute("email", "");
		recipientsGrid.setRecords(new ListGridRecord[] { record });

		final SelectItem contactsSelector = ItemFactory.newEmailSelector("contacts", "contacts");
		contactsSelector.setWidth(200);
		contactsSelector.addChangedHandler(new ChangedHandler() {

			@Override
			public void onChanged(ChangedEvent event) {
				ListGridRecord[] newSelection = contactsSelector.getSelectedRecords();
				if (newSelection == null || newSelection.length < 1)
					return;

				for (int i = 0; i < newSelection.length; i++) {
					ListGridRecord newRec = new ListGridRecord();
					newRec.setAttribute("email", newSelection[i].getAttributeAsString("email"));
					newRec.setAttribute("type", "to");

					// Iterate over the current recipients avoiding duplicates
					boolean duplicate = false;
					ListGridRecord[] currentRecipients = recipientsGrid.getRecords();
					for (int j = 0; j < currentRecipients.length; j++) {
						ListGridRecord rec = currentRecipients[j];
						if (rec.getAttributeAsString("email").contains(newRec.getAttributeAsString("email"))) {
							duplicate = true;
							break;
						}
					}

					if (!duplicate) {
						// Iterate over the current recipients finding an empty
						// slot
						boolean empty = false;
						for (int j = 0; j < currentRecipients.length; j++) {
							if (currentRecipients[j].getAttributeAsString("email").isEmpty()) {
								empty = true;
								currentRecipients[j].setAttribute("email", newRec.getAttributeAsString("email"));
								recipientsGrid.refreshRow(j);
								break;
							}
						}

						if (!empty)
							recipientsGrid.addData(newRec);
					}
				}
			}
		});

		DynamicForm addressbook = new DynamicForm();
		addressbook.setItems(contactsSelector);

		section.setItems(recipientsGrid, addressbook);
		sectionStack.setSections(section);

		return sectionStack;
	}

	private void addEmptyRow() {
		ListGridRecord[] records = recipientsGrid.getRecords();
		// Search for an empty record
		for (ListGridRecord rec : records) {
			if (rec.getAttribute("email") == null || rec.getAttribute("email").trim().equals(""))
				return;
		}

		ListGridRecord[] newRecords = new ListGridRecord[records.length + 1];
		for (int i = 0; i < records.length; i++)
			newRecords[i] = records[i];
		newRecords[records.length] = new ListGridRecord();
		newRecords[records.length].setAttribute("type", "to");
		newRecords[records.length].setAttribute("email", "");
		recipientsGrid.setRecords(newRecords);
	}
}