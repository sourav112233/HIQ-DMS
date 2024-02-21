package com.logicaldoc.gui.frontend.client.folder;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.beans.GUIFolder;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.frontend.client.document.DocumentsPanel;
import com.logicaldoc.gui.frontend.client.services.FolderService;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.util.ValueCallback;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.ColorItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.PickerIcon;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;

/**
 * Shows the folder's user interface settings
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.4.1
 */
public class FolderInterfacePanel extends FolderDetailTab {

	private DynamicForm form = new DynamicForm();

	private ValuesManager vm = new ValuesManager();

	private FormItemIcon applyToSubFolders = new FormItemIcon();

	public FolderInterfacePanel(GUIFolder folder, ChangedHandler changedHandler) {
		super(folder, changedHandler);
		setWidth100();
		setHeight100();
		setMembersMargin(20);

		try {
			refresh();
		} catch (Throwable t) {
			GuiLog.error(t.getMessage(),null,null);
			SC.warn(t.getMessage());
		}
	}

	private void refresh() {
		vm = new ValuesManager();

		if (form != null)
			form.destroy();

		if (contains(form))
			removeChild(form);

		form = new DynamicForm();
		form.setValuesManager(vm);
		form.setWrapItemTitles(false);
		form.setTitleOrientation(TitleOrientation.TOP);
		form.setNumCols(1);

		SpinnerItem position = ItemFactory.newSpinnerItem("position", "position", folder.getPosition());

		ColorItem color = ItemFactory.newColorItemPicker("color", "color", folder.getColor(), true, changedHandler);

		StaticTextItem docsGrid = ItemFactory.newStaticTextItem("grid", "docsgridlayout",
				folder.getGrid() != null && !folder.getGrid().isEmpty() ? I18N.message("customized")
						: I18N.message("notcustomized"));

		FormItemIcon copyCurrentLayout = new FormItemIcon();
		copyCurrentLayout.setSrc("[SKIN]/paste.gif");
		copyCurrentLayout.setPrompt(I18N.message("copycurrentfolderlayout"));
		copyCurrentLayout.setWidth(12);
		copyCurrentLayout.setHeight(12);
		copyCurrentLayout.addFormItemClickHandler(new FormItemClickHandler() {
			@Override
			public void onFormItemClick(FormItemIconClickEvent event) {
				String gridState = DocumentsPanel.get().getDocsGridViewState();
				if (gridState != null) {
					folder.setGrid(gridState);
					event.getItem().setValue(I18N.message("customized"));
					applyToSubFolders.setDisabled(true);
					if (changedHandler != null)
						changedHandler.onChanged(null);
				}
			}
		});

		FormItemIcon editLayout = new FormItemIcon();
		editLayout.setSrc("[SKIN]/paste.gif");
		editLayout.setPrompt(I18N.message("editlayout"));
		editLayout.setWidth(12);
		editLayout.setHeight(12);
		editLayout.addFormItemClickHandler(new FormItemClickHandler() {
			@Override
			public void onFormItemClick(FormItemIconClickEvent event) {
				TextAreaItem textArea = ItemFactory.newTextAreaItem("docsgridlayout", I18N.message("docsgridlayout"),
						null);
				textArea.setHeight(300);
				LD.askForValue(I18N.message("docsgridlayout"), I18N.message("docsgridlayout"),
						folder.getGrid() != null ? folder.getGrid() : "", textArea, 400, new ValueCallback() {
							@Override
							public void execute(final String value) {
								vm.setValue("grid",
										(value != null && !value.trim().isEmpty()) ? I18N.message("customized")
												: I18N.message("notcustomized"));

								folder.setGrid(value);
								if (changedHandler != null)
									changedHandler.onChanged(null);

							}
						});
				event.cancel();
			}
		});

		applyToSubFolders = new FormItemIcon();
		applyToSubFolders.setSrc("[SKIN]/download.png");
		applyToSubFolders.setPrompt(I18N.message("applytosubfolders"));
		applyToSubFolders.setWidth(12);
		applyToSubFolders.setHeight(12);
		applyToSubFolders.addFormItemClickHandler(new FormItemClickHandler() {
			@Override
			public void onFormItemClick(FormItemIconClickEvent event) {
				LD.contactingServer();
				FolderService.Instance.get().applyGridLayout(folder.getId(), new AsyncCallback<Void>() {
					@Override
					public void onFailure(Throwable caught) {
						LD.clearPrompt();
						GuiLog.serverError(caught);
					}

					@Override
					public void onSuccess(Void arg0) {
						LD.clearPrompt();
						GuiLog.info(I18N.message("appliedgridonsubfolders"));
					}
				});
			}
		});

		PickerIcon clear = new PickerIcon(PickerIcon.CLEAR, new FormItemClickHandler() {
			@Override
			public void onFormItemClick(FormItemIconClickEvent event) {
				folder.setGrid(null);
				event.getItem().setValue(I18N.message("notcustomized"));
				applyToSubFolders.setDisabled(true);
				if (changedHandler != null)
					changedHandler.onChanged(null);
			}
		});
		clear.setWidth(12);
		clear.setHeight(12);

		docsGrid.setIcons(copyCurrentLayout, applyToSubFolders, editLayout, clear);

		if (folder.isWrite()) {
			if (changedHandler != null) {
				color.addChangedHandler(changedHandler);
				position.addChangedHandler(changedHandler);
				docsGrid.addChangedHandler(changedHandler);
			} else
				docsGrid.setHidden(true);
		} else {
			color.setDisabled(true);
			position.setDisabled(true);
			docsGrid.setDisabled(true);
		}

		form.setItems(position, color, docsGrid);
		addMember(form);
	}

	public boolean validate() {
		vm.validate();
		if (!vm.hasErrors()) {
			folder.setPosition(
					vm.getValueAsString("position") != null ? Integer.parseInt(vm.getValueAsString("position")) : 1);
			folder.setColor(vm.getValueAsString("color"));

			if (vm.getValueAsString("grid") == null || vm.getValueAsString("grid").isEmpty()
					|| I18N.message("notcustomized").equals(vm.getValueAsString("grid")))
				folder.setGrid(null);
		}
		return !vm.hasErrors();
	}
}