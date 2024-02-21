package com.logicaldoc.gui.frontend.client.reports;

import com.logicaldoc.gui.common.client.Feature;
import com.logicaldoc.gui.common.client.Menu;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.frontend.client.administration.AdminScreen;
import com.logicaldoc.gui.frontend.client.reports.custom.CustomReportsPanel;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * This panel shows the reports menu
 * 
 * @author Matteo Caruso - LogicalDOC
 * @since 6.1
 */
public class ReportsMenu extends VLayout {
	public ReportsMenu() {
		setMargin(10);
		setMembersMargin(5);
		setOverflow(Overflow.AUTO);

		Button lastChanges = new Button(I18N.message("lastchanges"));
		lastChanges.setWidth100();
		lastChanges.setHeight(25);
		lastChanges.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new LastChangesReport());
			}
		});
		if (Menu.enabled(Menu.LAST_CHANGES))
			addMember(lastChanges);

		Button lockedDocs = new Button(I18N.message("lockeddocs"));
		lockedDocs.setWidth100();
		lockedDocs.setHeight(25);
		lockedDocs.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new LockedDocsReport());
			}
		});
		if (Menu.enabled(Menu.LOCKED_DOCS))
			addMember(lockedDocs);

		Button deletedDocs = new Button(I18N.message("deleteddocs"));
		deletedDocs.setWidth100();
		deletedDocs.setHeight(25);
		deletedDocs.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new DeletedDocsReport());
			}
		});
		if (Menu.enabled(Menu.DELETED_DOCS))
			addMember(deletedDocs);

		Button archivedDocs = new Button(I18N.message("archiveddocs"));
		archivedDocs.setWidth100();
		archivedDocs.setHeight(25);
		archivedDocs.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new ArchivedDocsReport());
			}
		});

		if (Feature.visible(Feature.ARCHIVING) && Menu.enabled(Menu.PARAMETERS)) {
			addMember(archivedDocs);
			if (!Feature.enabled(Feature.ARCHIVING)) {
				archivedDocs.setDisabled(true);
				archivedDocs.setTooltip(I18N.message("featuredisabled"));
			}
		}

		Button duplicates = new Button(I18N.message("duplicates"));
		duplicates.setWidth100();
		duplicates.setHeight(25);
		duplicates.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new DuplicatesReport());
			}
		});

		Button deletedFolders = new Button(I18N.message("deletedfolders"));
		deletedFolders.setWidth100();
		deletedFolders.setHeight(25);
		deletedFolders.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new DeletedFoldersReport());
			}
		});
		if (Menu.enabled(Menu.DELETED_FOLDERS))
			addMember(deletedFolders);

		Button calendar = new Button(I18N.message("calendar"));
		calendar.setWidth100();
		calendar.setHeight(25);
		calendar.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new CalendarReport());
			}
		});

		Button downloadTickets = new Button(I18N.message("downloadtickets"));
		downloadTickets.setWidth100();
		downloadTickets.setHeight(25);
		downloadTickets.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new DownloadTicketsReport());
			}
		});

		Button subscriptions = new Button(I18N.message("subscriptions"));
		subscriptions.setWidth100();
		subscriptions.setHeight(25);
		subscriptions.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new SubscriptionsReport());
			}
		});

		Button apiCalls = new Button(I18N.message("apicalls"));
		apiCalls.setWidth100();
		apiCalls.setHeight(25);
		apiCalls.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new ApiCallsReport());
			}
		});

		Button customReports = new Button(I18N.message("customreports"));
		customReports.setWidth100();
		customReports.setHeight(25);
		customReports.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AdminScreen.get().setContent(new CustomReportsPanel());
			}
		});

		if (Feature.visible(Feature.DUPLICATES_DISCOVERY)) {
			addMember(duplicates);
			if (!Feature.enabled(Feature.DUPLICATES_DISCOVERY)) {
				duplicates.setDisabled(true);
				duplicates.setTooltip(I18N.message("featuredisabled"));
			}
		}

		if (Feature.visible(Feature.CALENDAR) && Menu.enabled(Menu.CALENDAR_REPORT)) {
			addMember(calendar);
			if (!Feature.enabled(Feature.CALENDAR)) {
				calendar.setDisabled(true);
				calendar.setTooltip(I18N.message("featuredisabled"));
			}
		}

		if (Menu.enabled(Menu.DOWNLOAD_TICKETS))
			addMember(downloadTickets);
		
		if (Feature.visible(Feature.AUDIT) && Menu.enabled(Menu.SUBSCRIPTIONS_REPORT)) {
			addMember(subscriptions);
			if (!Feature.enabled(Feature.AUDIT)) {
				subscriptions.setDisabled(true);
				subscriptions.setTooltip(I18N.message("featuredisabled"));
			}
		}

		if (Menu.enabled(Menu.DOWNLOAD_TICKETS))
			addMember(apiCalls);
		
		if (Feature.visible(Feature.CUSTOM_REPORTS) && Menu.enabled(Menu.CUSTOMREPORTS)) {
			addMember(customReports);
			if (!Feature.enabled(Feature.CUSTOM_REPORTS)) {
				customReports.setDisabled(true);
				customReports.setTooltip(I18N.message("featuredisabled"));
			}
		}
	}
}
