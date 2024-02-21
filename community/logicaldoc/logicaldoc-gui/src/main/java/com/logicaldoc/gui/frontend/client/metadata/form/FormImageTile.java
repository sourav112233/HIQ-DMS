package com.logicaldoc.gui.frontend.client.metadata.form;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.beans.GUIForm;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.frontend.client.services.DocumentService;
import com.logicaldoc.gui.frontend.client.services.FormService;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.MultiUploader;

/**
 * Displays the header image
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.6.1
 */
public class FormImageTile extends HLayout {

	private GUIForm form;

	private ChangedHandler changedHandler;

	public FormImageTile(GUIForm form, ChangedHandler changedHandler) {
		this.form = form;
		this.changedHandler = changedHandler;

		setMembersMargin(1);
		setAlign(Alignment.LEFT);
		setOverflow(Overflow.HIDDEN);

		init();
	}

	private void init() {
		Canvas[] members = getMembers();
		if (members != null && members.length > 0)
			for (Canvas canvas : members)
				removeChild(canvas);

		IButton upload = new IButton(I18N.message("uploadheader"));
		upload.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Uploader uploader = new Uploader(form);
				uploader.show();
			}
		});
		upload.setDisabled(form.getId() == 0L);

		IButton clear = new IButton(I18N.message("clear"));
		clear.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				form.setHeaderImage(null);
				changedHandler.onChanged(null);
				init();
			}
		});

		HLayout buttons = new HLayout();
		buttons.setMembersMargin(2);
		buttons.setAutoHeight();
		buttons.setMembers(upload, clear);

		HTMLFlow flow = null;
		if (form.getHeaderImage() != null)
			flow = new HTMLFlow(
					"<img src='" + form.getHeaderImage() + "' style='float:body; max-width:300px' align='body' />");
		else
			flow = new HTMLFlow(" ");

		VLayout image = new VLayout();
		image.setAlign(VerticalAlignment.TOP);
		image.setMargin(1);
		image.setMembersMargin(2);
		image.setMembers(buttons, flow);

		setMembers(image);
	}

	private class Uploader extends Window {

		private IButton saveButton;

		private MultiUploader multiUploader;

		private GUIForm form;

		public Uploader(GUIForm form) {
			this.form = form;

			setHeaderControls(HeaderControls.HEADER_LABEL, HeaderControls.CLOSE_BUTTON);
			setTitle(I18N.message("uploadheaderimage"));
			setWidth(460);
			setHeight(150);
			setCanDragResize(true);
			setIsModal(true);
			setShowModalMask(true);
			centerInPage();

			// Create a new uploader panel and attach it to the window
			multiUploader = new MultiUploader();
			multiUploader.addOnStartUploadHandler(onStartUploaderHandler);
			multiUploader.addOnFinishUploadHandler(onFinishUploaderHandler);
			multiUploader.setStyleName("upload");
			multiUploader.setWidth("400px");
			multiUploader.setHeight("50px");
			multiUploader.setFileInputPrefix("LDOC_FORM");
			multiUploader.reset();
			multiUploader.setMaximumFiles(1);
			multiUploader.setValidExtensions("png", "jpg", "jpeg", "gif");

			saveButton = new IButton(I18N.message("save"));
			saveButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {

				@Override
				public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
					onSave();
				}
			});

			VLayout layout = new VLayout();
			layout.setMembersMargin(5);
			layout.setMargin(2);

			layout.addMember(multiUploader);
			layout.addMember(saveButton);

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
		}

		private IUploader.OnFinishUploaderHandler onFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
			public void onFinish(IUploader uploader) {
				if (uploader.getStatus() == Status.SUCCESS && multiUploader.getSuccessUploads() > 0)
					saveButton.setDisabled(false);
			}
		};

		private IUploader.OnStartUploaderHandler onStartUploaderHandler = new IUploader.OnStartUploaderHandler() {
			public void onStart(IUploader uploader) {
				saveButton.setDisabled(true);
			}
		};

		public void onSave() {
			if (multiUploader.getSuccessUploads() <= 0) {
				SC.warn(I18N.message("filerequired"));
				return;
			}

			FormService.Instance.get().processImage(new AsyncCallback<String>() {

				@Override
				public void onFailure(Throwable caught) {
					GuiLog.serverError(caught);
					close();
				}

				@Override
				public void onSuccess(String imageSrc) {
					form.setHeaderImage(imageSrc);
					FormImageTile.this.init();
					DocumentService.Instance.get().cleanUploadedFileFolder(new AsyncCallback<Void>() {

						@Override
						public void onFailure(Throwable caught) {
							GuiLog.serverError(caught);
						}

						@Override
						public void onSuccess(Void result) {
							destroy();
							close();
							if (changedHandler != null)
								changedHandler.onChanged(null);
						}
					});
				}
			});
		}
	}
}
