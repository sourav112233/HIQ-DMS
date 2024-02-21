package com.logicaldoc.gui.frontend.client.folder;

import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.common.client.widgets.FolderTree;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Dialog;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.TreeGrid;

/**
 * This is the form used to copy some folders into another target folder
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.7.1
 */
public class MergeDialog extends Dialog {
	public MergeDialog() {
		super();
		setHeaderControls(HeaderControls.HEADER_LABEL, HeaderControls.CLOSE_BUTTON);
		setTitle(I18N.message("merge"));
		setCanDragResize(true);
		setIsModal(true);
		setShowModalMask(true);
		centerInPage();
		setAutoSize(true);

		VLayout content = new VLayout();
		content.setWidth100();
		content.setHeight100();
		content.setMembersMargin(3);

		final TreeGrid folders = new FolderTree();
		folders.setWidth100();
		folders.setHeight100();
		folders.setMinHeight(300);

		VLayout buttons = new VLayout();
		buttons.setWidth100();

		Button merge = new Button(I18N.message("merge"));
		merge.setAutoFit(true);
		merge.setMargin(1);
		merge.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				long[] selectedIds = FolderNavigator.get().getSelectedIds();
				String label = FolderNavigator.get().getSelectedRecord().getAttributeAsString("name");
				if (selectedIds.length > 1)
					label = selectedIds.length + " " + I18N.message("folders").toLowerCase();

				LD.ask(I18N.message("merge"),
						I18N.message("mergeask",
								new String[] { label, folders.getSelectedRecord().getAttributeAsString("name") }),
						new BooleanCallback() {

							@Override
							public void execute(Boolean value) {
								if (value) {
									FolderNavigator.get().mergeTo(Long
											.parseLong(folders.getSelectedRecord().getAttributeAsString("folderId")));
								}
								destroy();
							}
						});
			}
		});

		buttons.setMembers(merge);
		content.setMembers(folders, buttons);
		addItem(content);
	}
}