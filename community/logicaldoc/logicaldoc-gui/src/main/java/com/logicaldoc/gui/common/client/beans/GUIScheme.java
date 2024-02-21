package com.logicaldoc.gui.common.client.beans;

import java.io.Serializable;

/**
 * Represents a scheme for custom ID / auto naming / auto folding
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.2.2
 *
 */
public class GUIScheme implements Serializable, Comparable<GUIScheme> {

	public static final String CUSTOMID_SCHEME = "customid-scheme";

	public static final String AUTONAMING_SCHEME = "autonaming-scheme";

	public static final String AUTOFOLDING_SCHEME = "autofolding-scheme";

	private static final long serialVersionUID = 1L;

	private long templateId;

	private String templateName;

	private String scheme;

	private boolean evaluateAtCheckin = false;

	private boolean evaluateAtUpdate = false;

	private String type = CUSTOMID_SCHEME;

	public long getTemplateId() {
		return templateId;
	}

	public void setTemplateId(long templateId) {
		this.templateId = templateId;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public boolean isEvaluateAtCheckin() {
		return evaluateAtCheckin;
	}

	public void setEvaluateAtCheckin(boolean regenerate) {
		this.evaluateAtCheckin = regenerate;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	@Override
	public int compareTo(GUIScheme o) {
		int comp = this.type.compareTo(o.getType());
		if (comp == 0)
			return this.templateName.compareTo(o.templateName);
		else
			return comp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isEvaluateAtUpdate() {
		return evaluateAtUpdate;
	}

	public void setEvaluateAtUpdate(boolean evaluateAtUpdate) {
		this.evaluateAtUpdate = evaluateAtUpdate;
	}
}