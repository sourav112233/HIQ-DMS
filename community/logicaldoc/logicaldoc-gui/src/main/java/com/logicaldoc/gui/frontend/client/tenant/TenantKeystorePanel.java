package com.logicaldoc.gui.frontend.client.tenant;

import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Constants;
import com.logicaldoc.gui.common.client.beans.GUIKeystore;
import com.logicaldoc.gui.common.client.beans.GUITenant;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.common.client.util.Util;
import com.logicaldoc.gui.frontend.client.services.SignService;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.ColorItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.MatchesFieldValidator;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * Shows the tenant's keystore informations.
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 7.7.2
 */
public class TenantKeystorePanel extends VLayout {

	private DynamicForm form = new DynamicForm();

	private DynamicForm signatureForm = new DynamicForm();

	private ValuesManager vm = new ValuesManager();

	private long tenantId;

	private VLayout layout = null;

	private GUIKeystore keystore = null;

	public TenantKeystorePanel(final long tenantId) {
		this.tenantId = tenantId;
		init();
	}

	void init() {
		if (tenantId <= 0) {
			setMembers(TenantsPanel.SELECT_TENANT);
		} else {
			setWidth100();
			setHeight100();
			setMembersMargin(20);

			SignService.Instance.get().loadKeystore(tenantId, new AsyncCallback<GUIKeystore>() {

				@Override
				public void onFailure(Throwable caught) {
					GuiLog.serverError(caught);
				}

				@Override
				public void onSuccess(GUIKeystore keystore) {
					TenantKeystorePanel.this.keystore = keystore;
					if (keystore == null) {
						TenantKeystorePanel.this.keystore = new GUIKeystore();
						TenantKeystorePanel.this.keystore.setTenantId(tenantId);
					}
					refresh();
				}
			});
		}
	}

	public TenantKeystorePanel(GUITenant tenant) {
		this(tenant.getId());
	}

	public void refresh() {
		boolean ksAlreadyGenerated = keystore != null && keystore.getId() != 0L;
		vm.clearValues();
		vm.clearErrors(false);

		if (form != null)
			form.destroy();

		if (layout != null) {
			layout.destroy();
		}

		layout = new VLayout();
		layout.setWidth100();

		form = new DynamicForm();
		form.setValuesManager(vm);
		form.setWrapItemTitles(false);
		form.setTitleOrientation(TitleOrientation.TOP);
		form.setNumCols(2);

		TextItem localCAalias = ItemFactory.newSimpleTextItemPreventAutocomplete("localCAalias", "localcaalias",
				keystore != null ? keystore.getOrganizationAlias() : null);
		localCAalias.setRequired(true);
		localCAalias.setSelectOnFocus(true);
		localCAalias.setWrapTitle(false);

		TextItem password = ItemFactory.newPasswordItemPreventAutocomplete("password", "keystorepasswd", null);
		password.setRequired(true);
		password.setWrapTitle(false);
		PasswordItem passwordAgain = ItemFactory.newPasswordItemPreventAutocomplete("passwordagain",
				"keystorepasswdagain", null);
		passwordAgain.setRequired(true);
		passwordAgain.setWrapTitle(false);
		MatchesFieldValidator validator = new MatchesFieldValidator();
		validator.setOtherField("passwordagain");
		validator.setErrorMessage(I18N.message("passwordnotmatch"));
		password.setValidators(validator);

		StaticTextItem details = ItemFactory.newStaticTextItem("details", "details",
				(keystore != null && keystore.getOrganizationDN() != null)
						? (keystore.getOrganizationDN() + " " + I18N.message("validtill") + ": "
								+ I18N.formatDate(keystore.getOrganizationExpire()))
						: I18N.message("error").toLowerCase());
		details.setWrap(false);
		details.setColSpan(2);

		SpinnerItem validity = ItemFactory.newSpinnerItem("validity", "validity",
				keystore != null ? keystore.getValidity() : 2);
		validity.setHint(I18N.message("years"));
		validity.setRequired(true);
		validity.setMin(1);
		validity.setMax(10);
		validity.setStep(1);

		TextItem countryCode = ItemFactory.newTextItem("countryCode", "countrycode", null);
		countryCode.setLength(2);
		countryCode.setHint("C");
		countryCode.setRequired(true);
		countryCode.setWrapTitle(false);

		TextItem organization = ItemFactory.newTextItem("organization", "organization", null);
		organization.setHint("O");
		organization.setRequired(true);
		organization.setWrapTitle(false);

		TextItem organizationalUnit = ItemFactory.newTextItem("organizationalUnit", "organizationalunit", null);
		organizationalUnit.setHint("OU");
		organizationalUnit.setRequired(true);
		organizationalUnit.setWrapTitle(false);

		TextItem keytoolCommand = ItemFactory.newTextItem("keytoolCommand", "Keytool", keystore.getKeytoolPath());
		keytoolCommand.setWidth(400);
		keytoolCommand.setRequired(true);
		keytoolCommand.setColSpan(2);

		TextItem opensslCommand = ItemFactory.newTextItem("opensslCommand", "OpenSSL", keystore.getOpenSSLPath());
		opensslCommand.setWidth(400);
		opensslCommand.setRequired(true);
		opensslCommand.setColSpan(2);

		signatureForm = new DynamicForm();
		signatureForm.setValuesManager(vm);
		signatureForm.setWrapItemTitles(false);
		signatureForm.setTitleOrientation(TitleOrientation.TOP);
		signatureForm.setNumCols(2);
		signatureForm.setIsGroup(true);
		signatureForm.setWidth(1);
		signatureForm.setGroupTitle(I18N.message("signature"));

		TextItem exprx = ItemFactory.newTextItem("exprx", "exprx", keystore.getSignX());
		exprx.setWidth(300);

		TextItem expry = ItemFactory.newTextItem("expry", "expry", keystore.getSignY());
		expry.setWidth(300);

		TextItem width = ItemFactory.newTextItem("width", "width", keystore.getSignWidth());
		width.setWidth(300);

		final ColorItem color = ItemFactory.newColorItemPicker("color", "color", keystore.getSignFontColor(), true,
				null);

		final RadioGroupItem visual = ItemFactory.newBooleanSelector("visual", "visual");
		visual.setValue(keystore.isSignVisual() ? "yes" : "no");

		SpinnerItem opacity = ItemFactory.newSpinnerItem("opacity", "opacity", keystore.getSignOpacity(), 1, 100);

		SpinnerItem fontSize = ItemFactory.newSpinnerItem("fontsize", "fontsize", keystore.getSignFontSize(), 1, 9999);

		signatureForm.setItems(visual, exprx, color, expry, opacity, width, fontSize);

		HLayout buttons = new HLayout();
		buttons.setMembersMargin(5);
		buttons.setMargin(3);

		IButton createNew = new IButton(I18N.message("generatenewkeystore"));
		createNew.setAutoFit(true);
		createNew.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (validate()) {
					LD.contactingServer();
					SignService.Instance.get().generateNewKeystore(keystore, new AsyncCallback<Void>() {
						@Override
						public void onFailure(Throwable caught) {
							LD.clearPrompt();
							GuiLog.serverError(caught);
						}

						@Override
						public void onSuccess(Void arg) {
							LD.clearPrompt();
							init();
						}
					});
				}
			}
		});

		IButton save = new IButton(I18N.message("save"));
		save.setAutoFit(true);
		save.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (validate()) {
					LD.contactingServer();
					SignService.Instance.get().saveKeystore(keystore, new AsyncCallback<Void>() {
						@Override
						public void onFailure(Throwable caught) {
							LD.clearPrompt();
							GuiLog.serverError(caught);
						}

						@Override
						public void onSuccess(Void arg) {
							LD.clearPrompt();
						}
					});
				}
			}
		});

		IButton export = new IButton(I18N.message("export"));
		export.setAutoFit(true);
		export.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Util.download(Util.contextPath() + "export-keystore?tenantId=" + tenantId);
			}
		});

		IButton delete = new IButton(I18N.message("ddelete"));
		delete.setAutoFit(true);
		delete.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				SC.ask(I18N.message("deletekeystorewarn"), new BooleanCallback() {

					@Override
					public void execute(Boolean value) {
						if (value)
							SignService.Instance.get().deleteKeystore(tenantId, new AsyncCallback<Void>() {
								@Override
								public void onFailure(Throwable caught) {
									GuiLog.serverError(caught);
								}

								@Override
								public void onSuccess(Void arg) {
									init();
								}
							});
					}
				});

			}
		});

		IButton _import = new IButton(I18N.message("iimport"));
		_import.setAutoFit(true);
		_import.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				KeystoreUploader uploader = new KeystoreUploader(TenantKeystorePanel.this);
				uploader.show();
			}
		});

		IButton downloadCert = new IButton(I18N.message("downloadcert"));
		downloadCert.setAutoFit(true);
		downloadCert.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Util.download(Util.contextPath() + "export-keystore?cert=true&tenantId=" + tenantId);
			}
		});

		/*
		 * Two invisible fields to 'mask' the real credentials to the browser
		 * and prevent it to auto-fill the username and password we really use.
		 */
		TextItem fakeUsername = ItemFactory.newTextItem("prevent_autofill", "prevent_autofill", "xyz");
		fakeUsername.setCellStyle("nodisplay");
		PasswordItem fakePassword = ItemFactory.newPasswordItem("password_fake", "password_fake", "xyz");
		fakePassword.setCellStyle("nodisplay");

		if (ksAlreadyGenerated) {
			password.setRequired(false);
			passwordAgain.setRequired(false);
			if (tenantId == Constants.TENANT_DEFAULTID)
				form.setItems(fakeUsername, fakePassword, details, validity, localCAalias, password, passwordAgain,
						keytoolCommand, opensslCommand);
			else
				form.setItems(fakeUsername, fakePassword, details, validity, fakeUsername, fakePassword, localCAalias,
						password, passwordAgain);
			layout.addMember(form);
			layout.addMember(signatureForm);

			buttons.setMembers(save, export, downloadCert, _import, delete);
		} else {
			Label label = new Label(I18N.message("keystorenotgenerated"));
			label.setHeight(50);
			layout.addMember(label);

			if (tenantId == Constants.TENANT_DEFAULTID)
				form.setItems(fakeUsername, fakePassword, validity, fakeUsername, fakePassword, localCAalias, password,
						passwordAgain, organization, organizationalUnit, countryCode, keytoolCommand, opensslCommand);
			else
				form.setItems(fakeUsername, fakePassword, validity, fakeUsername, fakePassword, localCAalias, password,
						passwordAgain, organization, organizationalUnit, countryCode);

			layout.addMember(form);
			layout.addMember(signatureForm);

			buttons.setMembers(createNew, _import);
		}
		layout.addMember(buttons);

		addMember(layout);
	}

	@SuppressWarnings("unchecked")
	boolean validate() {
		vm.validate();
		if (!vm.hasErrors()) {
			Map<String, Object> values = (Map<String, Object>) vm.getValues();
			keystore.setTenantId(tenantId);
			keystore.setOrganizationAlias((String) values.get("localCAalias"));
			keystore.setValidity(Integer.parseInt(values.get("validity").toString()));
			keystore.setSignX((String) values.get("exprx"));
			keystore.setSignY((String) values.get("expry"));
			keystore.setSignWidth((String) values.get("width"));
			keystore.setSignVisual("yes".equals(values.get("visual").toString()));
			keystore.setSignFontColor(values.get("color").toString());
			keystore.setSignFontSize(Integer.parseInt(values.get("fontsize").toString()));
			keystore.setSignOpacity(Integer.parseInt(values.get("opacity").toString()));
			keystore.setPassword((String) values.get("password"));
			keystore.setKeytoolPath((String) values.get("keytoolCommand"));
			keystore.setOpenSSLPath((String) values.get("opensslCommand"));
			try {
				keystore.setOrganizationDN("O=" + values.get("organization") + ",OU=" + values.get("organizationalUnit")
						+ ",C=" + values.get("countryCode").toString().toUpperCase());
			} catch (Throwable t) {

			}

		}
		return !vm.hasErrors();
	}

	public GUIKeystore getKeystore() {
		return keystore;
	}

	public long getTenantId() {
		return tenantId;
	}
}