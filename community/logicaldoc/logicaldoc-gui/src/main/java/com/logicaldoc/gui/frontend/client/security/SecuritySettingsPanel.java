package com.logicaldoc.gui.frontend.client.security;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Feature;
import com.logicaldoc.gui.common.client.Session;
import com.logicaldoc.gui.common.client.beans.GUISecuritySettings;
import com.logicaldoc.gui.common.client.beans.GUIUser;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.services.SecurityService;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.common.client.util.Util;
import com.logicaldoc.gui.common.client.widgets.FeatureDisabled;
import com.logicaldoc.gui.frontend.client.administration.AdminPanel;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.tab.Tab;

/**
 * This panel shows general security parameters.
 * 
 * @author Matteo Caruso - LogicalDOC
 * @since 6.0
 */
public class SecuritySettingsPanel extends AdminPanel {

	private ValuesManager vm = new ValuesManager();

	private GUISecuritySettings settings;

	private DynamicForm anonymousForm;

	private SelectItem anonymousUser;

	public SecuritySettingsPanel(GUISecuritySettings settings) {
		super("security");
		this.settings = settings;
	}

	@Override
	public void onDraw() {
		DynamicForm securityForm = new DynamicForm();
		securityForm.setValuesManager(vm);
		securityForm.setTitleOrientation(TitleOrientation.TOP);
		securityForm.setNumCols(1);

		final SpinnerItem pwdSize = ItemFactory.newSpinnerItem("pwdSize", "passwdsize", settings.getPwdSize());
		pwdSize.setRequired(true);
		pwdSize.setWidth(50);
		pwdSize.setMin(4);
		pwdSize.setStep(1);

		final SpinnerItem pwdExp = ItemFactory.newSpinnerItem("pwdExp", "passwdexpiration",
				settings.getPwdExpiration());
		pwdExp.setHint(I18N.message("daysafteruserdisabled"));
		pwdExp.setWrapTitle(false);
		pwdExp.setRequired(true);
		pwdExp.setWidth(50);
		pwdExp.setMin(0);
		pwdExp.setStep(10);

		final SpinnerItem pwdEnforce = ItemFactory.newSpinnerItem("pwdEnforce", "enforcepasswordhistory",
				settings.getPwdEnforceHistory());
		pwdEnforce.setHint(I18N.message("passwords"));
		pwdEnforce.setWrapTitle(false);
		pwdEnforce.setRequired(true);
		pwdEnforce.setWidth(50);
		pwdEnforce.setMin(0);
		pwdEnforce.setStep(1);

		final SpinnerItem maxInactivity = ItemFactory.newSpinnerItem("maxinactivity", "maxinactivity",
				settings.getMaxInactivity());
		maxInactivity.setRequired(false);
		maxInactivity.setHint(I18N.message("daysafteruserdisabled"));
		maxInactivity.setWidth(50);
		maxInactivity.setStep(1);

		final RadioGroupItem savelogin = ItemFactory.newBooleanSelector("savelogin", I18N.message("savelogin"));
		savelogin.setHint(I18N.message("saveloginhint"));
		savelogin.setValue(settings.isSaveLogin() ? "yes" : "no");
		savelogin.setWrapTitle(false);
		savelogin.setRequired(true);

		final RadioGroupItem ignorelogincase = ItemFactory.newBooleanSelector("ignorelogincase",
				I18N.message("ignorelogincase"));
		ignorelogincase.setValue(settings.isIgnoreLoginCase() ? "yes" : "no");
		ignorelogincase.setWrapTitle(false);
		ignorelogincase.setRequired(true);

		final RadioGroupItem alertnewdevice = ItemFactory.newBooleanSelector("alertnewdevice",
				I18N.message("alertloginfromnewdevice"));
		alertnewdevice.setValue(settings.isIgnoreLoginCase() ? "yes" : "no");
		alertnewdevice.setWrapTitle(false);
		alertnewdevice.setRequired(true);

		final RadioGroupItem allowSid = ItemFactory.newBooleanSelector("allowsid", I18N.message("allowsidinrequest"));
		allowSid.setValue(settings.isAllowSidInRequest() ? "yes" : "no");
		allowSid.setWrapTitle(false);
		allowSid.setRequired(true);
		allowSid.setDisabled(Session.get().isDemo());

		final RadioGroupItem forceSsl = ItemFactory.newBooleanSelector("forcessl", I18N.message("forcessl"));
		forceSsl.setValue(settings.isForceSsl() ? "yes" : "no");
		forceSsl.setWrapTitle(false);
		forceSsl.setRequired(true);
		forceSsl.setDisabled(Session.get().isDemo());

		final TextItem contentSecurityPolicy = ItemFactory.newTextItem("contentsecuritypolicy", "contentsecuritypolicy",
				settings.getContentSecurityPolicy());
		contentSecurityPolicy.setHint(I18N.message("contentsecuritypolicyhint"));
		contentSecurityPolicy.setWidth(400);

		if (Session.get().isDefaultTenant())
			securityForm.setFields(pwdSize, pwdExp, pwdEnforce, maxInactivity, savelogin, alertnewdevice,
					ignorelogincase, allowSid, forceSsl, contentSecurityPolicy);
		else
			securityForm.setFields(pwdSize, pwdExp, pwdEnforce, maxInactivity, savelogin, alertnewdevice);

		body.setMembers(securityForm);

		Tab menus = new Tab();
		menus.setTitle(I18N.message("menus"));
		menus.setPane(new MenusPanel());
		tabs.addTab(menus);

		Tab anonymous = prepareAnonymousTab(settings);
		tabs.addTab(anonymous);

		if (Feature.visible(Feature.GEOLOCATION)) {
			Tab geolocation = prepareGeolocationTab(settings);
			tabs.addTab(geolocation);
		}

		IButton save = new IButton();
		save.setTitle(I18N.message("save"));
		save.addClickHandler(new ClickHandler() {
			@SuppressWarnings("unchecked")
			public void onClick(ClickEvent event) {
				if (vm.validate()) {
					final Map<String, Object> values = (Map<String, Object>) vm.getValues();
					SecuritySettingsPanel.this.settings.setPwdExpiration((Integer) values.get("pwdExp"));
					SecuritySettingsPanel.this.settings.setPwdSize((Integer) values.get("pwdSize"));
					SecuritySettingsPanel.this.settings.setPwdEnforceHistory((Integer) values.get("pwdEnforce"));
					SecuritySettingsPanel.this.settings.setMaxInactivity((Integer) values.get("maxinactivity"));
					SecuritySettingsPanel.this.settings
							.setSaveLogin(values.get("savelogin").equals("yes") ? true : false);

					SecuritySettingsPanel.this.settings
							.setEnableAnonymousLogin(values.get("enableanonymous").equals("yes") ? true : false);
					SecuritySettingsPanel.this.settings
							.setAlertNewDevice(values.get("alertnewdevice").equals("yes") ? true : false);

					SecuritySettingsPanel.this.settings.setAnonymousKey((String) values.get("anonymousKey"));

					if (!SecuritySettingsPanel.this.settings.isEnableAnonymousLogin()) {
						SecuritySettingsPanel.this.settings.setAnonymousUser(null);
						anonymousUser.setValue((Long) null);
					} else if (SecuritySettingsPanel.this.settings.getAnonymousUser() == null) {
						SC.warn(I18N.message("selectanonymoususer"));
						return;
					}

					if (Session.get().isDefaultTenant()) {
						SecuritySettingsPanel.this.settings
								.setAllowSidInRequest(values.get("allowsid").equals("yes") ? true : false);

						SecuritySettingsPanel.this.settings
								.setIgnoreLoginCase(values.get("ignorelogincase").equals("yes") ? true : false);
						SecuritySettingsPanel.this.settings
								.setForceSsl(values.get("forcessl").equals("yes") ? true : false);
						SecuritySettingsPanel.this.settings
								.setContentSecurityPolicy(values.get("contentsecuritypolicy") != null
										? values.get("contentsecuritypolicy").toString()
										: null);

						SecuritySettingsPanel.this.settings
								.setGeolocationEnabled(values.get("geoEnabled").equals("yes") ? true : false);
						SecuritySettingsPanel.this.settings
								.setGeolocationCache(values.get("geoCache").equals("yes") ? true : false);
						SecuritySettingsPanel.this.settings.setGeolocationKey((String) values.get("geoKey"));
					}

					SecurityService.Instance.get().saveSettings(SecuritySettingsPanel.this.settings,
							new AsyncCallback<Boolean>() {

								@Override
								public void onFailure(Throwable caught) {
									GuiLog.serverError(caught);
								}

								@Override
								public void onSuccess(Boolean restartRequired) {
									GuiLog.info(I18N.message("settingssaved") + "  "
											+ I18N.message("settingsaffectnewsessions"), null);

									if (restartRequired.booleanValue())
										SC.warn(I18N.message("needrestart"));
								}
							});
				}
			}
		});

		addMember(save);
	}

	private Tab prepareGeolocationTab(GUISecuritySettings settings) {
		Tab geolocation = new Tab(I18N.message("geolocation"));

		if (!Feature.enabled(Feature.GEOLOCATION)) {
			geolocation.setPane(new FeatureDisabled());
			return geolocation;
		}

		DynamicForm geolocationForm = new DynamicForm();
		geolocationForm.setValuesManager(vm);
		geolocationForm.setTitleOrientation(TitleOrientation.TOP);
		geolocationForm.setNumCols(1);

		final RadioGroupItem enableGeolocation = ItemFactory.newBooleanSelector("geoEnabled",
				I18N.message("enablegeolocation"));
		enableGeolocation.setValue(settings.isGeolocationEnabled() ? "yes" : "no");
		enableGeolocation.setWrapTitle(false);
		enableGeolocation.setWrap(false);
		enableGeolocation.setRequired(true);

		final RadioGroupItem useCache = ItemFactory.newBooleanSelector("geoCache", I18N.message("usecache"));
		useCache.setValue(settings.isGeolocationCache() ? "yes" : "no");
		useCache.setWrapTitle(false);
		useCache.setWrap(false);
		useCache.setRequired(true);

		final TextItem licenseKey = ItemFactory.newTextItem("geoKey", "licensekey", settings.getGeolocationKey());
		licenseKey.setWidth(200);
		licenseKey.setHint(I18N.message("geolocationkeyhint"));

		final StaticTextItem geoDBversion = ItemFactory.newStaticTextItem("geoVersion", "dbversion",
				settings.getGeolocationDbVer());

		ButtonItem syncGeoDB = new ButtonItem("geoSyncDb", I18N.message("syncgeolocationdb"));
		syncGeoDB.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {

			@Override
			public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
				LD.contactingServer();
				SecurityService.Instance.get().syncGeolocationDB(licenseKey.getValueAsString(),
						new AsyncCallback<String>() {

							@Override
							public void onFailure(Throwable caught) {
								GuiLog.serverError(caught);
								LD.clearPrompt();
							}

							@Override
							public void onSuccess(String dbVer) {
								geoDBversion.setValue(dbVer);
								LD.clearPrompt();
							}
						});
			}
		});

		geolocationForm.setFields(enableGeolocation, useCache, licenseKey, geoDBversion, syncGeoDB);

		geolocation.setPane(geolocationForm);
		return geolocation;
	}

	private Tab prepareAnonymousTab(GUISecuritySettings settings) {
		Tab anonymous = new Tab(I18N.message("anonymous"));

		anonymousForm = new DynamicForm();
		anonymousForm.setValuesManager(vm);
		anonymousForm.setTitleOrientation(TitleOrientation.TOP);
		anonymousForm.setNumCols(1);
		final RadioGroupItem enableAnonymous = ItemFactory.newBooleanSelector("enableanonymous",
				I18N.message("enableanonymous"));
		enableAnonymous.setValue(settings.isEnableAnonymousLogin() ? "yes" : "no");
		enableAnonymous.setWrapTitle(false);
		enableAnonymous.setRequired(true);

		final StaticTextItem url = ItemFactory.newStaticTextItem("anonUrl", I18N.message("url"),
				GWT.getHostPageBaseURL() + "frontend.jsp?anonymous=login&tenant=" + Session.get().getTenantName()
						+ "&key=" + settings.getAnonymousKey());

		TextItem anonymousKey = ItemFactory.newSimpleTextItem("anonymousKey", "key", settings.getAnonymousKey());
		anonymousKey.setHintStyle("hint");
		anonymousKey.setRequired(true);
		anonymousKey.addChangedHandler(new ChangedHandler() {
			@Override
			public void onChanged(ChangedEvent event) {
				if (event.getValue() != null)
					url.setValue(Util.contextPath() + "frontend.jsp?anonymous=login&tenant="
							+ Session.get().getTenantName() + "&key=" + event.getValue().toString());
			}
		});

		anonymousUser = ItemFactory.newUserSelector("anonymousUser", "user", null, false, false);
		anonymousUser.setHint(I18N.message("anonymoususerhint"));
		anonymousUser.setHintStyle("hint");
		anonymousUser.addChangedHandler(new ChangedHandler() {
			@Override
			public void onChanged(ChangedEvent event) {
				if (anonymousUser.getSelectedRecord() == null) {
					SecuritySettingsPanel.this.settings.setAnonymousUser(null);
				} else {
					GUIUser u = new GUIUser();
					u.setId(Long.parseLong(anonymousUser.getSelectedRecord().getAttribute("id")));
					u.setUsername(anonymousUser.getSelectedRecord().getAttribute("username"));
					SecuritySettingsPanel.this.settings.setAnonymousUser(u);
				}
			}
		});
		if (SecuritySettingsPanel.this.settings.getAnonymousUser() != null)
			anonymousUser.setValue(Long.toString(SecuritySettingsPanel.this.settings.getAnonymousUser().getId()));

		anonymousForm.setItems(enableAnonymous, anonymousUser, anonymousKey, url);
		anonymous.setPane(anonymousForm);
		return anonymous;
	}
}
