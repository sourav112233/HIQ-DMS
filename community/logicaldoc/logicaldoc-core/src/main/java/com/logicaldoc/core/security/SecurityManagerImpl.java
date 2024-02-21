package com.logicaldoc.core.security;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.core.PersistenceException;
import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.folder.FolderDAO;
import com.logicaldoc.core.security.dao.GroupDAO;
import com.logicaldoc.core.security.dao.MenuDAO;
import com.logicaldoc.core.security.dao.UserDAO;

/**
 * Basic implementation of <code>SecurityManager</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 3.0
 */
public class SecurityManagerImpl implements SecurityManager {

	protected static Logger log = LoggerFactory.getLogger(SecurityManagerImpl.class);

	protected UserDAO userDAO;

	protected GroupDAO groupDAO;

	protected MenuDAO menuDAO;

	protected FolderDAO folderDAO;

	protected DocumentDAO documentDAO;

	private SecurityManagerImpl() {
	}

	public void setMenuDAO(MenuDAO menuDAO) {
		this.menuDAO = menuDAO;
	}

	public void setGroupDAO(GroupDAO groupDAO) {
		this.groupDAO = groupDAO;
	}

	public void setUserDAO(UserDAO userDAO) {
		this.userDAO = userDAO;
	}

	@Override
	public Set<Group> getAllowedGroups(long menuId) {
		Menu menu = menuDAO.findById(menuId);
		if (menu.getSecurityRef() != null)
			menu = menuDAO.findById(menu.getSecurityRef());
		Set<Group> groups = new HashSet<Group>();
		for (MenuGroup mg : menu.getMenuGroups()) {
			Group group = groupDAO.findById(mg.getGroupId());
			if (!groups.contains(group))
				groups.add(groupDAO.findById(mg.getGroupId()));
		}
		Group admin = groupDAO.findById(Group.GROUPID_ADMIN);
		if (!groups.contains(admin))
			groups.add(admin);
		return groups;
	}

	@Override
	public boolean isMemberOf(long userId, long groupId) {
		try {
			return userDAO.queryForInt(
					"select count(*) from ld_usergroup where ld_userid=" + userId + " and ld_groupid=" + groupId) > 0;
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean isMemberOf(long userId, String groupName) {
		User user = userDAO.findById(userId, true);
		if (user == null)
			return false;
		userDAO.initialize(user);
		for (Group group : user.getGroups())
			if (group.getName().equals(groupName))
				return true;
		return false;
	}

	@Override
	public boolean isWriteEnabled(long docId, long userId) {
		Document doc = documentDAO.findById(docId, true);
		if (doc == null)
			return false;
		else
			return folderDAO.isWriteEnabled(doc.getFolder().getId(), userId);
	}

	@Override
	public boolean isReadEnabled(long docId, long userId) {
		Document doc = documentDAO.findById(docId, true);
		if (doc == null)
			return false;
		else
			return folderDAO.isReadEnabled(doc.getFolder().getId(), userId);
	}

	@Override
	public boolean isPrintEnabled(long docId, long userId) {
		Document doc = documentDAO.findById(docId, true);
		if (doc == null)
			return false;
		else
			return folderDAO.isPrintEnabled(doc.getFolder().getId(), userId);
	}

	@Override
	public boolean isDownloadEnabled(long docId, long userId) {
		Document doc = documentDAO.findById(docId, true);
		if (doc == null)
			return false;
		else
			return folderDAO.isDownloadEnabled(doc.getFolder().getId(), userId);
	}

	@Override
	public boolean isPermissionEnabled(Permission permission, long docId, long userId) {
		Document doc = documentDAO.findById(docId, true);
		if (doc == null)
			return false;
		else
			return folderDAO.isPermissionEnabled(permission, doc.getFolder().getId(), userId);
	}

	@Override
	public Set<Permission> getEnabledPermissions(long docId, long userId) {
		Document doc = documentDAO.findById(docId, true);
		if (doc == null)
			return new HashSet<Permission>();
		else
			return folderDAO.getEnabledPermissions(doc.getFolder().getId(), userId);
	}

	public void setFolderDAO(FolderDAO folderDAO) {
		this.folderDAO = folderDAO;
	}

	public void setDocumentDAO(DocumentDAO documentDAO) {
		this.documentDAO = documentDAO;
	}
}