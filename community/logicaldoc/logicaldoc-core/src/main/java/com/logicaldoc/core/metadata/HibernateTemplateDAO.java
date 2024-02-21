package com.logicaldoc.core.metadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.logicaldoc.core.HibernatePersistentObjectDAO;
import com.logicaldoc.core.PersistenceException;
import com.logicaldoc.core.security.Group;
import com.logicaldoc.core.security.Permission;
import com.logicaldoc.core.security.User;
import com.logicaldoc.core.security.dao.UserDAO;
import com.logicaldoc.util.sql.SqlUtil;


/**
 * Hibernate implementation of <code>TemplateDAO</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 3.0
 */
public class HibernateTemplateDAO extends HibernatePersistentObjectDAO<Template> implements TemplateDAO {

	private UserDAO userDAO;
	
	public HibernateTemplateDAO() {
		super(Template.class);
		super.log = LoggerFactory.getLogger(HibernateTemplateDAO.class);
	}

	public void setUserDAO(UserDAO userDAO) {
		this.userDAO = userDAO;
	}
	
	@Override
	public List<Template> findAll() {
		try {
			return findByWhere(" 1=1", "order by _entity.name", null);
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
			return new ArrayList<Template>();
		}
	}

	@Override
	public List<Template> findAll(long tenantId) {
		try {
			return findByWhere(" _entity.tenantId=" + tenantId, "order by _entity.name", null);
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
			return new ArrayList<Template>();
		}
	}

	@Override
	public Template findByName(String name, long tenantId) {
		Template template = null;

		try {
			List<Template> coll = findByWhere(
					"_entity.name = '" + SqlUtil.doubleQuotes(name) + "' and _entity.tenantId=" + tenantId, null, null);
			if (coll.size() > 0)
				template = coll.iterator().next();
			if (template != null && template.getDeleted() == 1)
				template = null;
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
		}
		return template;
	}

	@Override
	public boolean delete(long id, int code) throws PersistenceException {
		if (!checkStoringAspect())
			return false;

		boolean result = true;
		Template template = (Template) findById(id);

		if (countDocs(id) > 0)
			throw new PersistenceException(String.format("Some documents are referencing the template %s (%d)",
					template.getName(), template.getId()));

		if (countFolders(id) > 0)
			throw new PersistenceException(String.format("Some folders are referencing the template %s (%d)",
					template.getName(), template.getId()));

		if (template != null) {
			template.setDeleted(code);
			template.setName(template.getName() + "." + template.getId());
			saveOrUpdate(template);
		}

		return result;
	}

	public int countFolders(long id) {
		try {
			return queryForInt("select count(*) from ld_folder where ld_deleted=0 and ld_templateid=" + id);
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
			return 0;
		}
	}

	@Override
	public int countDocs(long id) {
		try {
			return queryForInt("select count(*) from ld_document where ld_deleted=0 and ld_templateid=" + id);
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
			return 0;
		}
	}

	@Override
	public List<Template> findByType(int type, long tenantId) {
		try {
			return findByWhere("_entity.type =" + type + " and _entity.tenantId=" + tenantId,
					"order by _entity.name asc", null);
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
			return new ArrayList<Template>();
		}
	}

	@Override
	public void initialize(Template template) {
		try {
			refresh(template);

			if (template.getAttributes() != null)
				template.getAttributes().keySet().size();
			
			if(template.getTemplateGroups()!=null)
				template.getTemplateGroups().size();
		} catch (Throwable t) {
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean isWriteEnable(long templateId, long userId) {
		boolean result = true;
		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return false;
			if (user.isMemberOf("admin"))
				return true;

			Set<Group> groups = user.getGroups();
			if (groups.isEmpty())
				return false;

			Iterator iter = groups.iterator();

			StringBuffer query = new StringBuffer("select distinct(_entity) from Template _entity  ");
			query.append(" left join _entity.templateGroups as _group ");
			query.append(" where _group.write=1 and _group.groupId in (");

			boolean first = true;
			while (iter.hasNext()) {
				if (!first)
					query.append(",");
				Group ug = (Group) iter.next();
				query.append(Long.toString(ug.getId()));
				first = false;
			}
			query.append(") and _entity.id=?1");

			@SuppressWarnings("unchecked")
			List<TemplateGroup> coll = (List<TemplateGroup>) findByQuery(query.toString(),
					new Object[] { Long.valueOf(templateId) }, null);
			result = coll.size() > 0;
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
			result = false;
		}

		return result;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean isReadEnable(long templateId, long userId) {
		boolean result = true;
		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return false;
			if (user.isMemberOf("admin"))
				return true;

			Set<Group> Groups = user.getGroups();
			if (Groups.isEmpty())
				return false;

			Iterator iter = Groups.iterator();

			StringBuffer query = new StringBuffer("select distinct(_entity) from Template _entity  ");
			query.append(" left join _entity.templateGroups as _group ");
			query.append(" where _group.groupId in (");

			boolean first = true;
			while (iter.hasNext()) {
				if (!first)
					query.append(",");
				Group ug = (Group) iter.next();
				query.append(Long.toString(ug.getId()));
				first = false;
			}
			query.append(") and _entity.id=?1");

			@SuppressWarnings("unchecked")
			List<TemplateGroup> coll = (List<TemplateGroup>) findByQuery(query.toString(),
					new Object[] { Long.valueOf(templateId) }, null);
			result = coll.size() > 0;
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
			result = false;
		}

		return result;
	}

	@Override
	public Set<Permission> getEnabledPermissions(long templateId, long userId) {
		Set<Permission> permissions = new HashSet<Permission>();

		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return permissions;

			// If the user is an administrator bypass all controls
			if (user.isMemberOf("admin")) {
				return Permission.all();
			}

			Set<Group> groups = user.getGroups();
			if (groups.isEmpty())
				return permissions;

			StringBuffer query = new StringBuffer("select ld_write as LDWRITE");
			query.append(" from ld_templategroup ");
			query.append(" where ");
			query.append(" ld_wftemplateid=" + templateId);
			query.append(" and ld_groupid in (");

			boolean first = true;
			Iterator<Group> iter = groups.iterator();
			while (iter.hasNext()) {
				if (!first)
					query.append(",");
				Group ug = (Group) iter.next();
				query.append(Long.toString(ug.getId()));
				first = false;
			}
			query.append(")");

			/**
			 * IMPORTANT: the connection MUST be explicitly closed, otherwise it
			 * is probable that the connection pool will leave open it
			 * indefinitely.
			 */
			try (Connection con = getConnection();
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery(query.toString())) {
				while (rs.next()) {
					permissions.add(Permission.READ);
					if (rs.getInt("LDWRITE") == 1)
						permissions.add(Permission.WRITE);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return permissions;
	}
}