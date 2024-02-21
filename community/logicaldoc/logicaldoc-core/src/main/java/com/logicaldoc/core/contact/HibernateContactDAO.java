package com.logicaldoc.core.contact;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.logicaldoc.core.HibernatePersistentObjectDAO;
import com.logicaldoc.core.PersistenceException;
import com.logicaldoc.core.generic.HibernateGenericDAO;

/**
 * Hibernate implementation of <code>ContactDAO</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 4.0
 */
public class HibernateContactDAO extends HibernatePersistentObjectDAO<Contact> implements ContactDAO {

	public HibernateContactDAO() {
		super(Contact.class);
		super.log = LoggerFactory.getLogger(HibernateGenericDAO.class);
	}

	@Override
	public List<Contact> findByUser(Long userId, String email) {
		List<Object> params = new ArrayList<Object>();
		if (userId != null)
			params.add(userId);
		if (email != null)
			params.add(email);

		StringBuffer sb = new StringBuffer("");
		if (userId == null)
			sb.append(" _entity.userId is null ");
		else
			sb.append(" _entity.userId = ?1 ");
		if (email != null)
			sb.append(" and _entity.email = ?2 ");

		try {
			return findByWhere(sb.toString(), params.isEmpty() ? null : params.toArray(new Object[0]),
					"order by _entity.firstName, _entity.lastName", null);
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
			return new ArrayList<Contact>();
		}
	}
}