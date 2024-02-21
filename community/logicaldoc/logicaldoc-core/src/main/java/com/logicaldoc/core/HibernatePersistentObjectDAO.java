package com.logicaldoc.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.logicaldoc.core.metadata.Attribute;
import com.logicaldoc.core.metadata.ExtensibleObject;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;

/**
 * Hibernate implementation of <code>PersistentObjectDAO</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 4.0
 */
public abstract class HibernatePersistentObjectDAO<T extends PersistentObject> implements PersistentObjectDAO<T> {
	protected Logger log = LoggerFactory.getLogger(HibernatePersistentObjectDAO.class);

	protected Class<T> entityClass;

	protected SessionFactory sessionFactory;

	protected final static String STORING_ASPECT = "storing";

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	protected HibernatePersistentObjectDAO(Class<T> entityClass) {
		super();
		this.entityClass = entityClass;
	}

	public boolean delete(long id, int code) throws PersistenceException {
		assert (code != 0);

		if (!checkStoringAspect())
			return false;

		boolean result = true;
		try {
			T entity = findById(id);
			if (entity == null)
				return false;
			entity.setDeleted(code);
			store(entity);
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
		return result;
	}

	public boolean delete(long id) throws PersistenceException {
		return delete(id, PersistentObject.DELETED_CODE_DEFAULT);
	}

	public List<T> findAll() {
		try {
			return findByWhere("", "", null);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return new ArrayList<T>();
		}
	}

	public List<T> findAll(long tenantId) {
		try {
			return findByWhere(" _entity.tenantId=" + tenantId, "", null);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return new ArrayList<T>();
		}
	}

	public List<Long> findAllIds() {
		try {
			return findIdsByWhere("", "", null);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return new ArrayList<Long>();
		}
	}

	public List<Long> findAllIds(long tenantId) {
		try {
			return findIdsByWhere(" _entity.tenantId=" + tenantId, "", null);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return new ArrayList<Long>();
		}
	}

	@Override
	public T findById(long id, boolean initialize) {
		T entity = findById(id);
		if (initialize)
			initialize(entity);
		return entity;
	}

	@Override
	public T findById(long id) {
		T entity = null;
		try {
			entity = (T) sessionFactory.getCurrentSession().get(entityClass, id);
			if (entity != null && entity.getDeleted() == 1)
				return null;
			return entity;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public List<T> findByWhere(String where, String order, Integer max) throws PersistenceException {
		return findByWhere(where, null, order, max);
	}

	@Override
	public List<T> findByWhere(String where, Object[] values, String order, Integer max) throws PersistenceException {
		List<T> coll = new ArrayList<T>();
		try {
			String sorting = StringUtils.isNotEmpty(order) && !order.toLowerCase().contains("order by")
					? "order by " + order
					: order;
			String query = "from " + entityClass.getCanonicalName() + " _entity where _entity.deleted=0 "
					+ (StringUtils.isNotEmpty(where) ? " and (" + where + ") " : " ")
					+ (StringUtils.isNotEmpty(sorting) ? sorting : " ");
			coll = findByObjectQuery(query, values, max);
			return coll;
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public List<T> findByObjectQuery(String query, Object[] values, Integer max) throws PersistenceException {
		List<T> coll = new ArrayList<T>();
		try {
			log.debug("Execute query: {}", query);
			Query<T> queryObject = prepareQueryForObject(query, values, max);
			coll = (List<T>) queryObject.list();
			return coll;
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List findByQuery(String query, Object[] values, Integer max) throws PersistenceException {
		List<Object> coll = new ArrayList<Object>();
		try {
			log.debug("Execute query: {}", query);
			Query queryObject = prepareQuery(query, values, max);
			coll = (List<Object>) queryObject.list();
			return coll;
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public List<Long> findIdsByWhere(String where, String order, Integer max) throws PersistenceException {
		return findIdsByWhere(where, new Object[0], order, max);
	}

	@Override
	public List<Long> findIdsByWhere(String where, Object[] values, String order, Integer max)
			throws PersistenceException {
		List<Long> coll = new ArrayList<Long>();
		try {
			String sorting = StringUtils.isNotEmpty(order) && !order.toLowerCase().contains("order by")
					? "order by " + order
					: order;
			String query = "select _entity.id from " + entityClass.getCanonicalName()
					+ " _entity where _entity.deleted=0 "
					+ (StringUtils.isNotEmpty(where) ? " and (" + where + ") " : " ")
					+ (StringUtils.isNotEmpty(sorting) ? sorting : " ");
			log.debug("Execute query: {}", query);
			Query<Long> queryObject = prepareQueryForLong(query, values, max);
			coll = (List<Long>) queryObject.list();
			return coll;
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Checks if the aspect for storing data is enabled
	 */
	protected boolean checkStoringAspect() {
		if (!RunLevel.current().aspectEnabled(STORING_ASPECT)) {
			log.error("Apect {} is disabled", STORING_ASPECT);
			return false;
		}
		return true;
	}

	public boolean store(T entity) throws PersistenceException {
		if (!checkStoringAspect())
			return false;

		try {
			entity.setLastModified(new java.util.Date());

			// Save the entity
			saveOrUpdate(entity);
			return true;
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	protected void saveOrUpdate(Object entity) {
		// Update the attributes
		if (entity instanceof ExtensibleObject) {
			try {
				ExtensibleObject extensibleEntity = (ExtensibleObject) entity;
				for (String name : extensibleEntity.getAttributes().keySet()) {
					Attribute att = extensibleEntity.getAttribute(name);
					if (att.getMultiple() == 1 && att.getType() == Attribute.TYPE_STRING) {
						String vals = extensibleEntity.getValues(name).stream().map(n -> n.toString())
								.collect(Collectors.joining(","));
						att.setStringValues(vals);
					} else
						att.setStringValues(null);
				}
			} catch (Throwable t) {
			}
		}

		sessionFactory.getCurrentSession().saveOrUpdate(entity);
	}

	protected void flush() {
		try {
			sessionFactory.getCurrentSession().flush();
		} catch (Throwable t) {

		}
	}

	protected void refresh(Object entity) {
		try {
			if (!sessionFactory.getCurrentSession().contains(entity)) {
				sessionFactory.getCurrentSession().refresh(entity);
			}
		} catch (Throwable t) {

		}
	}

	protected Object merge(Object entity) {
		try {
			return sessionFactory.getCurrentSession().merge(entity);
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			return null;
		}
	}

	protected void evict(Object entity) {
		sessionFactory.getCurrentSession().evict(entity);
	}

	/**
	 * Utility method useful for preparing an Hibernate query for updates
	 * 
	 * @param expression The expression for the query
	 * @param values The parameters values to be used (optional, if the query is
	 *        parametric)
	 * @param max Optional maximum number of wanted results
	 * 
	 * @return The Hibernate query
	 */
	@SuppressWarnings("rawtypes")
	protected Query prepareQueryForUpdate(String expression, Object[] values, Integer max) {
		Query queryObject = sessionFactory.getCurrentSession().createQuery(expression);
		applyParamsAndLimit(values, max, queryObject);
		return queryObject;
	}

	/**
	 * Utility method useful for preparing an Hibernate query for objects of
	 * this type
	 * 
	 * @param expression The expression for the query
	 * @param values The parameters values to be used (optional, if the query is
	 *        parametric)
	 * @param max Optional maximum number of wanted results
	 * 
	 * @return The Hibernate query
	 */
	protected Query<T> prepareQueryForObject(String expression, Object[] values, Integer max) {
		Query<T> queryObject = sessionFactory.getCurrentSession().createQuery(expression, entityClass);
		applyParamsAndLimit(values, max, queryObject);
		return queryObject;
	}

	/**
	 * Utility method useful for preparing an Hibernate query for generic result
	 * 
	 * @param expression The expression for the query
	 * @param values The parameters values to be used (optional, if the query is
	 *        parametric)
	 * @param max Optional maximum number of wanted results
	 * 
	 * @return The Hibernate query
	 */
	protected Query<Object[]> prepareQuery(String expression, Object[] values, Integer max) {
		Query<Object[]> queryObject = sessionFactory.getCurrentSession().createQuery(expression, Object[].class);
		applyParamsAndLimit(values, max, queryObject);
		return queryObject;
	}

	/**
	 * Utility method useful for preparing an Hibernate query for longs
	 * 
	 * @param expression The expression for the query
	 * @param values The parameters values to be used (optional, if the query is
	 *        parametric)
	 * @param max Optional maximum number of wanted results
	 * 
	 * @return The Hibernate query
	 */
	protected Query<Long> prepareQueryForLong(String expression, Object[] values, Integer max) {
		Query<Long> queryObject = sessionFactory.getCurrentSession().createQuery(expression, Long.class);
		applyParamsAndLimit(values, max, queryObject);
		return queryObject;
	}

	private void applyParamsAndLimit(Object[] values, Integer max, @SuppressWarnings("rawtypes") Query queryObject) {
		if (values != null)
			for (int i = 0; i < values.length; i++)
				queryObject.setParameter(Integer.toString(i + 1), values[i]);

		if (max != null && max > 0)
			queryObject.setMaxResults(max);
	}

	/**
	 * Doesn't do anything by default
	 */
	@Override
	public void initialize(T entity) {
		// By default do nothing
	}

	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	/**
	 * Parses a SQL query and inserts the hits to the SQL processor to restrict
	 * the maximum number of returned records. The syntax varies depending on
	 * the current DBMS.
	 * 
	 * @param srcQuery The source query to parse
	 * @param maxRows Max number of rows.
	 * @return The modified qery
	 */
	private String insertTopClause(String srcQuery, Integer maxRows) {
		if (maxRows == null || maxRows.intValue() <= 0)
			return srcQuery;

		String outQuery = srcQuery;
		if (isMySQL() || isPostgreSQL()) {
			/*
			 * At the end of the query we have to insert the LIMIT clause:
			 * 
			 * SELECT column_name(s) FROM table_name WHERE condition LIMIT
			 * number;
			 */
			if (srcQuery.endsWith(";"))
				outQuery = srcQuery.substring(0, srcQuery.length() - 1);
			outQuery += " LIMIT " + maxRows;
		} else if (isSqlServer()) {
			/*
			 * After the SELECT have to put the TOP clause:
			 * 
			 * SELECT TOP number column_name(s) FROM table_name WHERE condition;
			 */
			if (srcQuery.startsWith("SELECT"))
				outQuery = outQuery.replaceFirst("SELECT", "SELECT TOP " + maxRows + " ");
			else if (srcQuery.startsWith("select"))
				outQuery = outQuery.replaceFirst("select", "select TOP " + maxRows + " ");
		} else if (isOracle()) {
			/*
			 * In the WHERE we have to put the ROWNUM condition:
			 * 
			 * SELECT column_name(s) FROM table_name WHERE ROWNUM <= number;
			 */
			if (srcQuery.contains("WHERE"))
				outQuery = outQuery.replaceFirst("WHERE", "where ROWNUM <= " + maxRows + " and ");
			if (srcQuery.contains("where"))
				outQuery = outQuery.replaceFirst("where", "where ROWNUM <= " + maxRows + " and ");
		}

		return outQuery;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List query(String sql, Object[] args, RowMapper rowMapper, Integer maxRows) throws PersistenceException {
		List list = new ArrayList();
		try {
			DataSource dataSource = (DataSource) Context.get().getBean("DataSource");

			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			if (maxRows != null)
				jdbcTemplate.setMaxRows(maxRows);
			if (args != null)
				list = jdbcTemplate.query(insertTopClause(sql, maxRows), args, rowMapper);
			else
				list = jdbcTemplate.query(insertTopClause(sql, maxRows), rowMapper);
			return list;
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List queryForList(String sql, Object[] args, Class elementType, Integer maxRows)
			throws PersistenceException {

		List list = new ArrayList();
		try {
			DataSource dataSource = (DataSource) Context.get().getBean("DataSource");
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			if (maxRows != null)
				jdbcTemplate.setMaxRows(maxRows);
			if (args != null)
				list = jdbcTemplate.queryForList(insertTopClause(sql, maxRows), args, elementType);
			else
				list = jdbcTemplate.queryForList(insertTopClause(sql, maxRows), elementType);
			return list;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			throw new PersistenceException(e);
		}
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Object[] args, Integer maxRows) throws PersistenceException {
		SqlRowSet rs = null;
		try {
			DataSource dataSource = (DataSource) Context.get().getBean("DataSource");
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			if (maxRows != null)
				jdbcTemplate.setMaxRows(maxRows);
			if (args != null)
				rs = jdbcTemplate.queryForRowSet(insertTopClause(sql, maxRows), args);
			else
				rs = jdbcTemplate.queryForRowSet(insertTopClause(sql, maxRows));
			return rs;
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List queryForList(String sql, Class elementType) throws PersistenceException {
		return queryForList(sql, null, elementType, null);
	}

	@Override
	public int queryForInt(String sql) throws PersistenceException {
		long mytmplong = queryForLong(sql);
		return Long.valueOf(mytmplong).intValue();
	}

	@Override
	public long queryForLong(String sql) throws PersistenceException {
		try {
			DataSource dataSource = (DataSource) Context.get().getBean("DataSource");
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			Long ret = jdbcTemplate.queryForObject(sql, Long.class);
			if (ret != null)
				return ret;
			else
				return 0L;
		} catch (EmptyResultDataAccessException e) {
			return 0L;			
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public String queryForString(String sql) throws PersistenceException {
		try {
			DataSource dataSource = (DataSource) Context.get().getBean("DataSource");
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			return jdbcTemplate.queryForObject(sql, String.class);
		} catch (EmptyResultDataAccessException e) {
			return null;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			throw new PersistenceException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object queryForObject(String sql, @SuppressWarnings("rawtypes") Class type) throws PersistenceException {
		try {
			DataSource dataSource = (DataSource) Context.get().getBean("DataSource");
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			return jdbcTemplate.queryForObject(sql, type);
		} catch (EmptyResultDataAccessException e) {
			return null;						
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public int jdbcUpdate(String statement) throws PersistenceException {
		if (!checkStoringAspect())
			return 0;

		try {
			DataSource dataSource = (DataSource) Context.get().getBean("DataSource");
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			return jdbcTemplate.update(statement);
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public void deleteAll(Collection<T> entities, int code) throws PersistenceException {
		if (!checkStoringAspect())
			return;

		if (entities == null || entities.isEmpty())
			return;

		try {
			StringBuffer ids = new StringBuffer();
			for (T t : entities) {
				if (ids.length() > 0)
					ids.append(",");
				ids.append(Long.toString(t.getId()));
			}

			Query<T> queryObject = prepareQueryForObject("update " + entityClass.getCanonicalName() + " set deleted="
					+ code + " where id in(" + ids.toString() + ")", null, null);
			queryObject.executeUpdate();
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public void deleteAll(Collection<T> entities) throws PersistenceException {
		deleteAll(entities, PersistentObject.DELETED_CODE_DEFAULT);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int bulkUpdate(String expression, Object[] values) throws PersistenceException {
		if (!checkStoringAspect())
			return 0;

		try {
			Query queryObject = prepareQueryForUpdate("update " + entityClass.getCanonicalName() + " " + expression,
					values, null);
			return queryObject.executeUpdate();
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public int jdbcUpdate(String statement, Object... args) throws PersistenceException {
		if (!checkStoringAspect())
			return 0;

		DataSource dataSource = (DataSource) Context.get().getBean("DataSource");
		try {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			return jdbcTemplate.update(statement, args);
		} catch (Throwable e) {
			throw new PersistenceException(e);
		}
	}

	protected Connection getConnection() throws SQLException {
		DataSource dataSource = (DataSource) Context.get().getBean("DataSource");
		return dataSource.getConnection();
	}

	@Override
	public String getDbms() {
		ContextProperties config = Context.get().getProperties();
		return config.getProperty("jdbc.dbms").toLowerCase();
	}

	@Override
	public boolean isOracle() {
		return "oracle".equals(getDbms());
	}

	protected boolean isHsql() {
		return "hsqldb".equals(getDbms());
	}

	protected boolean isMySQL() {
		return "mysql".equals(getDbms());
	}

	protected boolean isPostgreSQL() {
		return "postgresql".equals(getDbms());
	}

	protected boolean isSqlServer() {
		return "mssql".equals(getDbms());
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
}