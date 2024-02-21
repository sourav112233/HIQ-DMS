package com.logicaldoc.gui.frontend.client.reports;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Feature;
import com.logicaldoc.gui.common.client.beans.GUIHistory;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.util.GridUtil;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.common.client.widgets.FolderSelector;
import com.logicaldoc.gui.common.client.widgets.InfoPanel;
import com.logicaldoc.gui.common.client.widgets.grid.DateListGridField;
import com.logicaldoc.gui.common.client.widgets.grid.FileNameListGridField;
import com.logicaldoc.gui.common.client.widgets.grid.UserListGridField;
import com.logicaldoc.gui.frontend.client.administration.AdminPanel;
import com.logicaldoc.gui.frontend.client.services.SystemService;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.DateItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * This panel is used to show the last changes events.
 * 
 * @author Matteo Caruso - LogicalDOC
 * @since 6.0
 */
public class LastChangesReport extends AdminPanel {

	private Layout search = new VLayout();

	private Layout results = new VLayout();

	private VLayout lastchanges = new VLayout();

	private ValuesManager vm = new ValuesManager();

	private ListGrid histories;

	private InfoPanel infoPanel;

	private FolderSelector folder;

	public LastChangesReport() {
		super("lastchanges");
	}

	@Override
	public void onDraw() {
		HStack formsLayout = new HStack(10);

		DynamicForm form = new DynamicForm();
		form.setValuesManager(vm);
		form.setAlign(Alignment.LEFT);
		form.setTitleOrientation(TitleOrientation.LEFT);
		form.setNumCols(4);
		form.setWrapItemTitles(false);

		// Username
		SelectItem user = ItemFactory.newUserSelector("user", "user", null, false, false);
		user.setColSpan(4);

		// From
		DateItem fromDate = ItemFactory.newDateItem("fromDate", "from");

		// To
		DateItem tillDate = ItemFactory.newDateItem("tillDate", "till");

		// Session ID
		TextItem sessionId = ItemFactory.newTextItem("sid", "sid", null);
		sessionId.setColSpan(4);
		sessionId.setWidth(250);

		folder = new FolderSelector(null, true);
		folder.setColSpan(3);
		folder.setWidth(200);

		// Max results
		SpinnerItem displayMax = ItemFactory.newSpinnerItem("displayMax", "displaymax", 100, 5, null);
		displayMax.setHint(I18N.message("elements"));
		displayMax.setStep(10);

		ButtonItem searchButton = new ButtonItem();
		searchButton.setTitle(I18N.message("search"));
		searchButton.setAutoFit(true);
		searchButton.setEndRow(true);
		searchButton.setColSpan(2);
		searchButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onSearch();
			}
		});

		ButtonItem resetButton = new ButtonItem();
		resetButton.setTitle(I18N.message("reset"));
		resetButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				vm.clearValues();
			}
		});
		resetButton.setColSpan(2);
		resetButton.setAutoFit(true);
		resetButton.setEndRow(true);

		ButtonItem print = new ButtonItem();
		print.setTitle(I18N.message("print"));
		print.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				GridUtil.print(histories);
			}
		});
		print.setAutoFit(true);
		print.setEndRow(false);

		ButtonItem export = new ButtonItem();
		export.setTitle(I18N.message("export"));
		export.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				GridUtil.exportCSV(histories, false);
			}
		});
		if (!Feature.enabled(Feature.EXPORT_CSV)) {
			export.setDisabled(true);
			export.setTooltip(I18N.message("featuredisabled"));
		}
		export.setAutoFit(true);
		export.setEndRow(true);

		if (Feature.visible(Feature.EXPORT_CSV))
			form.setItems(user, sessionId, fromDate, tillDate, folder, displayMax, searchButton, resetButton, print,
					export);
		else
			form.setItems(user, sessionId, fromDate, tillDate, folder, displayMax, searchButton, resetButton, print);

		DynamicForm eventForm = new DynamicForm();
		eventForm.setValuesManager(vm);
		eventForm.setAlign(Alignment.LEFT);
		eventForm.setTitleOrientation(TitleOrientation.LEFT);
		eventForm.setNumCols(2);
		eventForm.setColWidths(1, "*");

		// Event
		SelectItem event = ItemFactory.newEventsSelector("event", I18N.message("event"), null, true, true, true, true);
		event.setColSpan(2);
		event.setEndRow(true);

		eventForm.setItems(event);

		formsLayout.addMember(form);
		formsLayout.addMember(eventForm);
		formsLayout.setMembersMargin(80);

		search.setMembersMargin(10);
		search.setMembers(formsLayout);
		search.setHeight("30%");
		search.setShowResizeBar(true);
		search.setWidth100();
		search.setMargin(10);

		ListGridField eventField = new ListGridField("event", I18N.message("event"), 200);
		eventField.setCanFilter(true);

		ListGridField date = new DateListGridField("date", "date");

		ListGridField userField = new UserListGridField("user", "userId", "user");
		userField.setCanFilter(true);
		userField.setAlign(Alignment.CENTER);

		FileNameListGridField name = new FileNameListGridField("name", "icon", I18N.message("name"), 150);
		name.setCanFilter(true);

		ListGridField folder = new ListGridField("folder", I18N.message("folder"), 100);
		folder.setCanFilter(true);

		ListGridField sid = new ListGridField("sid", I18N.message("sid"), 250);
		sid.setCanFilter(true);
		sid.setAlign(Alignment.CENTER);

		ListGridField docId = new ListGridField("docId", I18N.message("documentid"), 100);
		docId.setCanFilter(true);
		docId.setHidden(true);

		ListGridField folderId = new ListGridField("folderId", I18N.message("folderid"), 100);
		folderId.setCanFilter(true);
		folderId.setHidden(true);

		ListGridField userId = new ListGridField("userId", I18N.message("userid"), 100);
		userId.setCanFilter(true);
		userId.setHidden(true);

		ListGridField ip = new ListGridField("ip", I18N.message("ip"), 100);
		ip.setCanFilter(true);
		ip.setHidden(true);

		ListGridField device = new ListGridField("device", I18N.message("device"), 200);
		device.setHidden(true);
		ListGridField geolocation = new ListGridField("geolocation", I18N.message("geolocation"), 200);
		geolocation.setHidden(true);

		ListGridField username = new ListGridField("username", I18N.message("username"), 100);
		username.setCanFilter(true);
		username.setHidden(true);

		ListGridField comment = new ListGridField("comment", I18N.message("comment"), 200);
		comment.setCanFilter(true);
		comment.setHidden(true);

		ListGridField reason = new ListGridField("reason", I18N.message("reason"), 200);
		reason.setCanFilter(true);
		reason.setHidden(true);

		histories = new ListGrid();
		histories.setEmptyMessage(I18N.message("notitemstoshow"));
		histories.setWidth100();
		histories.setHeight100();
		histories.setFields(eventField, date, userField, name, folder, sid, docId, folderId, userId, username, ip,
				device, geolocation, comment, reason);
		histories.setSelectionType(SelectionStyle.SINGLE);
		histories.setShowRecordComponents(true);
		histories.setShowRecordComponentsByCell(true);
		histories.setCanFreezeFields(true);
		histories.setFilterOnKeypress(true);
		histories.setAutoFetchData(true);
		histories.sort("date", SortDirection.DESCENDING);

		results.addMember(histories);

		lastchanges.addMember(search, 0);

		// Prepare a panel containing a title and the documents list
		infoPanel = new InfoPanel("");
		lastchanges.addMember(infoPanel, 1);

		lastchanges.addMember(results, 2);

		body.setMembers(lastchanges);

		histories.addDoubleClickHandler(new DoubleClickHandler() {
			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				vm.setValue("sid", histories.getSelectedRecord().getAttributeAsString("sid"));
			}
		});
	}

	/**
	 * Gets the option items to choose events types
	 * 
	 * @return an array of select items
	 */
	public SelectItem[] getEventTypes() {
		List<SelectItem> items = new ArrayList<SelectItem>();

		return items.toArray(new SelectItem[0]);
	}

	@SuppressWarnings("unchecked")
	private void onSearch() {
		histories.setData(new ListGridRecord[0]);

		final Map<String, Object> values = (Map<String, Object>) vm.getValues();

		if (vm.validate()) {
			String[] eventValues = new String[0];
			if (values.get("event") != null) {
				String buf = values.get("event").toString().trim().toLowerCase();
				buf = buf.replace('[', ' ');
				buf = buf.replace(']', ' ');
				buf = buf.replace(" ", "");
				eventValues = buf.split(",");
			}

			Long userId = null;
			if (values.get("user") != null) {
				if (values.get("user") instanceof Long)
					userId = (Long) values.get("user");
				else
					userId = Long.parseLong(values.get("user").toString());
			}
			
			Date fromValue = null;
			if (values.get("fromDate") != null)
				fromValue = (Date) values.get("fromDate");
			Date tillValue = null;
			if (values.get("tillDate") != null)
				tillValue = (Date) values.get("tillDate");

			String sid = null;
			if (values.get("sid") != null)
				sid = (String) values.get("sid");

			int displayMaxValue = 0;
			try {
				if (values.get("displayMax") != null) {
					if (values.get("displayMax") instanceof Integer)
						displayMaxValue = (Integer) values.get("displayMax");
					else
						displayMaxValue = Integer.parseInt((String) values.get("displayMax"));
				}
			} catch (Throwable t) {

			}

			LD.contactingServer();
			SystemService.Instance.get().search(userId, fromValue, tillValue, displayMaxValue, sid, eventValues,
					folder.getFolderId(), new AsyncCallback<GUIHistory[]>() {

						@Override
						public void onFailure(Throwable caught) {
							LD.clearPrompt();
							GuiLog.serverError(caught);
						}

						@Override
						public void onSuccess(GUIHistory[] result) {
							LD.clearPrompt();

							if (result != null && result.length > 0) {
								ListGridRecord[] records = new ListGridRecord[result.length];
								for (int i = 0; i < result.length; i++) {
									ListGridRecord record = new ListGridRecord();
									record.setAttribute("event", I18N.message(result[i].getEvent()));
									record.setAttribute("date", result[i].getDate());
									record.setAttribute("user", result[i].getUsername());
									record.setAttribute("name", result[i].getFileName());
									record.setAttribute("folder", result[i].getPath());
									record.setAttribute("sid", result[i].getSessionId());
									record.setAttribute("docId", result[i].getDocId());
									record.setAttribute("folderId", result[i].getFolderId());
									record.setAttribute("userId", result[i].getUserId());
									record.setAttribute("ip", result[i].getIp());
									record.setAttribute("device", result[i].getDevice());
									record.setAttribute("geolocation", result[i].getGeolocation());
									record.setAttribute("username", result[i].getUserLogin());
									record.setAttribute("comment", result[i].getComment());
									record.setAttribute("reason", result[i].getReason());
									record.setAttribute("icon", result[i].getIcon());
									records[i] = record;
								}
								histories.setData(records);
							}
							lastchanges.removeMember(infoPanel);
							infoPanel = new InfoPanel("");
							infoPanel.setMessage(
									I18N.message("showelements", Integer.toString(histories.getTotalRows())));
							lastchanges.addMember(infoPanel, 1);
						}
					});
		}
	}
}