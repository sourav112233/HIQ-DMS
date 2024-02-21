package com.logicaldoc.gui.frontend.client.security;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Session;
import com.logicaldoc.gui.common.client.beans.GUIParameter;
import com.logicaldoc.gui.common.client.beans.GUIUser;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.frontend.client.services.SettingService;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * This panel shows the firewall settings.
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 6.5
 */
public class FirewallPanel extends VLayout {

	private ValuesManager vm = new ValuesManager();

	private GUIUser user;

	private ChangedHandler changedHandler;

	private DynamicForm form;

	public FirewallPanel(GUIUser user, ChangedHandler changedHandler) {
		super();
		this.user = user;
		this.changedHandler = changedHandler;

		setWidth100();
		setHeight100();
		setMembersMargin(20);

		init();
		setMembers(form);
	}

	public FirewallPanel() {
		setWidth100();
		setMembersMargin(5);
		setMargin(5);

		init();

		Tab tab = new Tab();
		tab.setTitle(I18N.message("firewall"));
		tab.setPane(form);

		TabSet tabs = new TabSet();
		tabs.setWidth100();
		tabs.setHeight100();
		tabs.setTabs(tab);

		IButton save = new IButton();
		save.setAutoFit(true);
		save.setTitle(I18N.message("save"));
		save.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				onSave();
			}
		});

		setMembers(tabs, save);
	}

	private void init() {
		form = new DynamicForm();
		form.setValuesManager(vm);
		form.setTitleOrientation(TitleOrientation.TOP);
		if (user != null)
			form.setNumCols(2);
		else
			form.setNumCols(1);

		RadioGroupItem enabled = ItemFactory.newBooleanSelector("eenabled", "enabled");
		enabled.setValue(Session.get().getConfigAsBoolean("firewall.enabled"));
		enabled.setWrapTitle(false);
		enabled.setRequired(true);
		if (changedHandler != null)
			enabled.addChangedHandler(changedHandler);
		if (Session.get().isDemo())
			enabled.setDisabled(true);
		
		
		final TextAreaItem whitelist = ItemFactory.newTextAreaItem("whitelist", "whitelist", null);
		whitelist.setHeight(120);
		whitelist.setWidth(350);
		whitelist.setHint(I18N.message("blacklisthint"));
		if (changedHandler != null)
			whitelist.addChangedHandler(changedHandler);
		if (Session.get().isDemo())
			whitelist.setDisabled(true);

		final TextAreaItem blacklist = ItemFactory.newTextAreaItem("blacklist", "blacklist", null);
		blacklist.setHeight(120);
		blacklist.setWidth(350);
		blacklist.setHint(I18N.message("blacklisthint"));
		if (changedHandler != null)
			blacklist.addChangedHandler(changedHandler);
		if (Session.get().isDemo())
			blacklist.setDisabled(true);

		if (user == null) {
			/*
			 * We are operating on general filters
			 */
			form.setItems(enabled, whitelist, blacklist);
			SettingService.Instance.get().loadSettingsByNames(
					new String[] { "firewall.enabled", "firewall.whitelist", "firewall.blacklist" },
					new AsyncCallback<GUIParameter[]>() {
						@Override
						public void onFailure(Throwable caught) {
							GuiLog.serverError(caught);
						}

						@Override
						public void onSuccess(GUIParameter[] params) {
							enabled.setValue("true".equals(params[0].getValue()) ? "yes" : "no");
							whitelist.setValue(params[1].getValue().replace(',', '\n'));
							blacklist.setValue(params[2].getValue().replace(',', '\n'));
						}
					});
		} else {
			/*
			 * We are operating on user's specific filters
			 */
			form.setItems(whitelist, blacklist);
			whitelist.setValue(user.getIpWhitelist() != null ? user.getIpWhitelist().replace(',', '\n') : "");
			blacklist.setValue(user.getIpBlacklist() != null ? user.getIpBlacklist().replace(',', '\n') : "");
		}
	}

	public void onSave() {
		String enabled = "yes".equals(vm.getValueAsString("eenabled")) ? "true" : "false";
		String whitelist = vm.getValueAsString("whitelist");
		String blacklist = vm.getValueAsString("blacklist");

		GUIParameter[] params = new GUIParameter[3];

		params[0] = new GUIParameter("firewall.enabled", enabled);
		params[1] = new GUIParameter("firewall.whitelist",
				whitelist != null ? whitelist.replace('\n', ',').replaceAll(" ", "") : null);
		params[2] = new GUIParameter("firewall.blacklist",
				blacklist != null ? blacklist.replace('\n', ',').replaceAll(" ", "") : null);

		SettingService.Instance.get().saveSettings(params, new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				GuiLog.serverError(caught);
			}

			@Override
			public void onSuccess(Void params) {
				if (user == null)
					GuiLog.info(I18N.message("settingssaved"), null);
			}
		});
	}

	public boolean validate() {
		String whitelist = vm.getValueAsString("whitelist");
		String blacklist = vm.getValueAsString("blacklist");
		user.setIpWhitelist(whitelist != null ? whitelist.replace('\n', ',').replaceAll(" ", "") : null);
		user.setIpBlacklist(blacklist != null ? blacklist.replace('\n', ',').replaceAll(" ", "") : null);
		return true;
	}
}