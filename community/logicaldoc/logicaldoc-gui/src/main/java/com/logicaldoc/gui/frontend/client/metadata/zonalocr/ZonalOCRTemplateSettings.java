package com.logicaldoc.gui.frontend.client.metadata.zonalocr;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Session;
import com.logicaldoc.gui.common.client.beans.GUIOCRTemplate;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.frontend.client.services.DocumentService;
import com.logicaldoc.gui.frontend.client.services.ZonalOCRService;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.MultiUploader;

/**
 * This popup window is used to upload / edit an OCR template
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.4.2
 */
public class ZonalOCRTemplateSettings extends Window {

	private MultiUploader uploader;

	private ValuesManager vm;

	private DynamicForm form;

	private IButton save;

	private ZonalOCRTemplatesPanel ocrPanel;

	public ZonalOCRTemplateSettings(ZonalOCRTemplatesPanel ocrPanel) {
		setHeaderControls(HeaderControls.HEADER_LABEL, HeaderControls.CLOSE_BUTTON);
		setTitle(I18N.message("ocrtemplate"));
		setCanDragResize(true);
		setIsModal(true);
		setShowModalMask(true);
		centerInPage();
		setAutoSize(true);

		this.ocrPanel = ocrPanel;

		save = new IButton(I18N.message("save"));
		save.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {

			@Override
			public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
				onSave();
			}
		});

		uploader = new MultiUploader();
		uploader.setMaximumFiles(1);
		uploader.setStyleName("upload");
		uploader.setFileInputPrefix("ZONALOCR");
		uploader.setWidth("400px");
		uploader.reset();
		uploader.setHeight("40px");
		uploader.setTitle(I18N.message("sample"));
		uploader.addOnFinishUploadHandler(onFinishUploaderHandler);
		uploader.addOnStartUploadHandler(onStartUploaderHandler);

		prepareForm();

		VLayout layout = new VLayout();
		layout.setMembersMargin(5);
		layout.setWidth100();

		layout.addMember(form);
		layout.addMember(uploader);
		layout.addMember(save);

		// Clean the upload folder if the window is closed
		addCloseClickHandler(new CloseClickHandler() {
			@Override
			public void onCloseClick(CloseClickEvent event) {
				DocumentService.Instance.get().cleanUploadedFileFolder(new AsyncCallback<Void>() {

					@Override
					public void onFailure(Throwable caught) {
						GuiLog.serverError(caught);
					}

					@Override
					public void onSuccess(Void result) {
						destroy();
					}
				});
			}
		});

		addItem(layout);

		// Just to clean the upload folder
		DocumentService.Instance.get().cleanUploadedFileFolder(new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
			}

			@Override
			public void onSuccess(Void result) {
			}
		});
	}

	private void prepareForm() {
		form = new DynamicForm();
		form.setWidth100();
		form.setAlign(Alignment.LEFT);
		form.setColWidths("1px, 100%");
		vm = new ValuesManager();
		form.setValuesManager(vm);

		final StaticTextItem fileNameWaring = ItemFactory.newStaticTextItem("fileNameWarning",
				I18N.message("attention"), I18N.message("filenamewarning"));
		fileNameWaring.setRequired(true);

		TextItem name = ItemFactory.newTextItem("name", "name", ocrPanel.getSelectedOcrTemplate().getName());
		name.setRequired(true);
		name.setDisabled(ocrPanel.getSelectedOcrTemplate().getId() != 0L);

		StaticTextItem id = ItemFactory.newStaticTextItem("id", I18N.message("id"),
				"" + ocrPanel.getSelectedOcrTemplate().getId());
		id.setVisible(ocrPanel.getSelectedOcrTemplate().getId() != 0L);

		SpinnerItem batch = ItemFactory.newSpinnerItem("batch", "batch",
				Session.get().getConfigAsInt("zonalocr.batch"));
		batch.setStep(50);
		batch.setMin(1);

		TextAreaItem description = ItemFactory.newTextAreaItem("description", "description",
				ocrPanel.getSelectedOcrTemplate().getDescription());
		description.setHeight(200);

		if (Session.get().isDefaultTenant())
			form.setItems(id, name, description, batch);
		else
			form.setItems(id, name, description);
	}

	public void onSave() {
		if (ocrPanel.getSelectedOcrTemplate().getId() == 0L && uploader.getSuccessUploads() <= 0) {
			SC.warn(I18N.message("samplerequired"));
			return;
		}
		if (!vm.validate())
			return;

		ocrPanel.getSelectedOcrTemplate().setName(vm.getValueAsString("name"));
		ocrPanel.getSelectedOcrTemplate().setDescription(vm.getValueAsString("description"));

		if (Session.get().isDefaultTenant()) {
			int batch = Integer.parseInt(vm.getValueAsString("batch"));
			Session.get().setConfig("zonalocr.batch", "" + batch);
			ocrPanel.getSelectedOcrTemplate().setBatch(batch);
		}

		ZonalOCRService.Instance.get().save(ocrPanel.getSelectedOcrTemplate(), new AsyncCallback<GUIOCRTemplate>() {
			@Override
			public void onFailure(Throwable caught) {
				GuiLog.serverError(caught);
			}

			@Override
			public void onSuccess(GUIOCRTemplate template) {
				ocrPanel.setSelectedOcrTemplate(template);
				destroy();
			}
		});
	}

	private IUploader.OnFinishUploaderHandler onFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
		public void onFinish(IUploader uploader) {
			if (uploader.getStatus() == Status.SUCCESS) {
				save.setDisabled(false);
			}
		}
	};

	private IUploader.OnStartUploaderHandler onStartUploaderHandler = new IUploader.OnStartUploaderHandler() {
		public void onStart(IUploader uploader) {
			save.setDisabled(true);
		}
	};
}