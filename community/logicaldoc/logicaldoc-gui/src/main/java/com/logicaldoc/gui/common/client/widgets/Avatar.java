package com.logicaldoc.gui.common.client.widgets;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Session;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.services.SecurityService;
import com.logicaldoc.gui.common.client.util.Util;
import com.logicaldoc.gui.frontend.client.services.DocumentService;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.MultiUploader;

/**
 * Displays an Avatar
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.6.1
 */
public class Avatar extends HLayout {

	private long userId;

	private AsyncCallback<Void> callback;

	public Avatar(long userId) {
		this(userId, null);
	}

	public Avatar(long userId, AsyncCallback<Void> callback) {
		this.userId = userId;
		this.callback = callback;

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

		int avatarSize = Session.get().getConfigAsInt("gui.avatar.size");
		Img avatarImage = new Img(Util.avatarUrl(Long.toString(userId), true), avatarSize, avatarSize);
		avatarImage.setLeft(0);
		avatarImage.setLayoutAlign(Alignment.LEFT);
		avatarImage.setAlign(Alignment.LEFT);
		avatarImage.setContextMenu(prepareContextMenu());
		setMembers(avatarImage);
	}

	private Menu prepareContextMenu() {
		MenuItem reset = new MenuItem();
		reset.setTitle(I18N.message("reset"));
		reset.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
			public void onClick(MenuItemClickEvent event) {
				SecurityService.Instance.get().resetAvatar(userId, new AsyncCallback<Void>() {
					@Override
					public void onFailure(Throwable caught) {
						GuiLog.serverError(caught);
					}

					@Override
					public void onSuccess(Void arg) {
						Avatar.this.init();
						if (callback != null)
							callback.onSuccess(null);
					}
				});
			}
		});

		MenuItem update = new MenuItem();
		update.setTitle(I18N.message("update"));
		update.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
			public void onClick(MenuItemClickEvent event) {
				Uploader uploader = new Uploader(userId);
				uploader.show();
			}
		});

		Menu contextMenu = new Menu();
		contextMenu.setItems(reset, update);
		return contextMenu;
	}

	private class Uploader extends Window {

		private IButton saveButton;

		private MultiUploader multiUploader;

		private long userId;

		public Uploader(long userId) {
			this.userId = userId;

			setHeaderControls(HeaderControls.HEADER_LABEL, HeaderControls.CLOSE_BUTTON);
			setTitle(I18N.message("uploadavatar"));
			setWidth(460);
			setHeight(170);
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
			multiUploader.setFileInputPrefix("LDOC_AVATAR");
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

			Label hint = new Label(I18N.message("avatarhint", Session.get().getConfig("gui.avatar.size"),
					Session.get().getConfig("gui.avatar.size")));
			hint.setHeight(20);

			VLayout layout = new VLayout();
			layout.setMembersMargin(5);
			layout.setMargin(2);

			layout.addMember(hint);
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

			SecurityService.Instance.get().saveAvatar(userId, new AsyncCallback<Void>() {

				@Override
				public void onFailure(Throwable caught) {
					GuiLog.serverError(caught);
					close();
				}

				@Override
				public void onSuccess(Void arg) {
					Avatar.this.init();
					DocumentService.Instance.get().cleanUploadedFileFolder(new AsyncCallback<Void>() {

						@Override
						public void onFailure(Throwable caught) {
							GuiLog.serverError(caught);
						}

						@Override
						public void onSuccess(Void result) {
							destroy();
							close();
						}
					});
					if (callback != null)
						callback.onSuccess(arg);
				}
			});
		}
	}
}
