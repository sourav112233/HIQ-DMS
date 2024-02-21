package com.logicaldoc.core.searchengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.core.document.AbstractDocument;
import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.DocumentHistory;
import com.logicaldoc.core.document.DocumentManager;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.security.Tenant;
import com.logicaldoc.core.security.dao.TenantDAO;
import com.logicaldoc.core.task.Task;
import com.logicaldoc.core.threading.NamedThreadFactory;
import com.logicaldoc.i18n.I18N;
import com.logicaldoc.util.CollectionUtil;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.io.FileUtil;
import com.logicaldoc.util.time.TimeDiff;
import com.logicaldoc.util.time.TimeDiff.TimeField;

/**
 * This task enlists all non-indexed documents and performs the indexing
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 4.0
 */
public class IndexerTask extends Task {

	private static Logger lg = LoggerFactory.getLogger(IndexerTask.class);

	/**
	 * This executor will be used to index files
	 */
	private static ScheduledExecutorService executor = null;

	public static final String NAME = "IndexerTask";

	private DocumentManager documentManager;

	private DocumentDAO documentDao;

	private TenantDAO tenantDao;

	private SearchEngine indexer;

	private long indexed = 0;

	private long errors = 0;

	private long indexingTime = 0;

	private long parsingTime = 0;

	public IndexerTask() {
		super(NAME);
		log = LoggerFactory.getLogger(IndexerTask.class);
	}

	public void setDocumentManager(DocumentManager documentManager) {
		this.documentManager = documentManager;
	}

	public void setDocumentDao(DocumentDAO documentDao) {
		this.documentDao = documentDao;
	}

	@Override
	public boolean isIndeterminate() {
		return false;
	}

	@Override
	public boolean isConcurrent() {
		return true;
	}

	@Override
	protected void runTask() throws Exception {
		if (indexer.isLocked()) {
			log.warn("Index locked, skipping indexing");
			return;
		}

		if (!lockManager.get(getName(), transactionId)) {
			log.warn("Unable to acquire lock {}, skipping indexing", getName());
			return;
		}

		log.info("Start indexing of all documents");

		errors = 0;
		indexed = 0;
		indexingTime = 0;
		try {
			ContextProperties config = Context.get().getProperties();
			Integer max = config.getProperty("index.batch") != null
					? Integer.parseInt(config.getProperty("index.batch"))
					: null;

			if (max != null && max.intValue() < size && max.intValue() > 0)
				size = max.intValue();

			if (max != null && max.intValue() < 1)
				max = null;

			// Retrieve the actual transactions
			List<String> transactionIds = lockManager.getAllTransactions();
			String transactionIdsStr = transactionIds.toString().replace("[", "('").replace("]", "')").replace(", ",
					"','");

			// First of all find documents to be indexed and not already
			// involved into a transaction
			String[] query = IndexerTask.prepareQuery();
			List<Long> ids = documentDao.findIdsByWhere(query[0]
					+ " and (_entity.transactionId is null or _entity.transactionId not in " + transactionIdsStr + ") ",
					query[1], max);
			size = ids.size();
			log.info("Found a total of {} documents to be processed", size);

			// Must take into account start and end of the transaction
			size += 2;

			if (!ids.isEmpty()) {
				// Mark all these documents as belonging to the current
				// transaction. This may require time
				String idsStr = ids.toString().replace('[', '(').replace(']', ')');
				documentDao.bulkUpdate(
						" set ld_transactionid = ?1 where ld_transactionid is null and ld_id in " + idsStr,
						new Object[] { transactionId });
			}
			log.info("Documents marked for indexing in transaction {}", transactionId);

			// First step done
			next();

			// Now we can release the lock
			lockManager.release(getName(), transactionId);

			if (!ids.isEmpty()) {
				int threadsTotal = config.getInt("index.threads", 1);
				log.info("Distribute the indexing among {} threads", threadsTotal);

				// Divide the docs in groups of N
				Collection<List<Long>> partitions = CollectionUtil.partition(ids, threadsTotal);

				startIndexerThreads(threadsTotal);
				List<IndexerThread> threads = new ArrayList<IndexerThread>();

				// Prepare the threads and launch them
				int i = 1;
				for (List<Long> partition : partitions) {
					IndexerThread iThread = new IndexerThread(partition, i++);
					threads.add(iThread);
					executor.schedule(new FutureTask<Long>(iThread), 1, TimeUnit.SECONDS);
					log.debug("Launched the thread for documents {}", partition);
				}

				log.info("Waiting for completion");
				boolean threadsCompleted = false;
				while (!threadsCompleted) {
					try {
						Thread.sleep(2);
					} catch (Throwable t) {

					}

					threadsCompleted = true;
					for (IndexerThread iThread : threads) {
						if (!iThread.isCompleted()) {
							threadsCompleted = false;
							;
							break;
						}
					}

					if (interruptRequested)
						threadsCompleted = true;
				}

				log.info("All threads have completed");

				// Collect some stats
				for (IndexerThread thread : threads) {
					indexingTime += thread.getIndexingTime();
					parsingTime += thread.getParsingTime();
					indexed += thread.getIndexed();
					errors += thread.getErrors();
				}
			}
		} finally {
			killIndexerThreads();

			log.info("Indexing finished");
			log.info("Indexing time: {}", TimeDiff.printDuration(indexingTime));
			log.info("Parsing time: {}", TimeDiff.printDuration(parsingTime));
			log.info("Indexed documents: {}", indexed);
			log.info("Errors: {}", errors);

			indexer.unlock();

			// To be safer always release the lock
			lockManager.release(getName(), transactionId);

			// Remove the transaction reference
			documentDao.bulkUpdate("set ld_transactionid = null where ld_transactionId = ?1",
					new Object[] { transactionId });

			// Last step done
			next();
			log.info("Documents released from transaction {}", transactionId);
		}
	}

	/**
	 * Prepares the query to search the documents to be indexed
	 * 
	 * @return array composed by the query to execute and the sorting expression
	 */
	public static String[] prepareQuery() {
		ContextProperties config = Context.get().getProperties();

		// Determine the sorting
		String sorting = config.getProperty("index.sorting");
		if (StringUtils.isNotEmpty(sorting))
			if ("oldestfirst".equals(sorting))
				sorting = "_entity.date asc";
			else if ("mostrecentfirst".equals(sorting))
				sorting = "_entity.date desc";
			else if ("smallestfirst".equals(sorting))
				sorting = "_entity.fileSize asc";
			else
				sorting = "_entity.fileSize desc";

		// This hidden setting overrides the default sorting policy(some really
		// demanding users may need this optimization).
		String sortingCustom = config.getProperty("index.sorting.custom");
		if (StringUtils.isNotEmpty(sortingCustom))
			sorting = sortingCustom;

		String where = " (_entity.indexed = " + AbstractDocument.INDEX_TO_INDEX + " or _entity.indexed = "
				+ AbstractDocument.INDEX_TO_INDEX_METADATA + ") and not _entity.status = "
				+ AbstractDocument.DOC_ARCHIVED;

		return new String[] { where, sorting };
	}

	public void setIndexer(SearchEngine indexer) {
		this.indexer = indexer;
	}

	@Override
	protected String prepareReport(Locale locale) {
		StringBuffer sb = new StringBuffer();
		sb.append(I18N.message("indexationtime", locale) + ": ");
		sb.append(TimeDiff.printDuration(indexingTime));
		sb.append("\n");
		sb.append(I18N.message("indexeddocs", locale) + ": ");
		sb.append(indexed);
		sb.append("\n");
		sb.append(I18N.message("errors", locale) + ": ");
		sb.append(errors);
		return sb.toString();
	}

	public void setTenantDao(TenantDAO tenantDao) {
		this.tenantDao = tenantDao;
	}

	private static void startIndexerThreads(int threadsTotal) {
		if (executor == null || executor.isShutdown() || executor.isTerminated())
			executor = Executors.newScheduledThreadPool(threadsTotal, new NamedThreadFactory("Indexer"));
	}

	/**
	 * Stops all the Indexer threads
	 */
	public static void killIndexerThreads() {
		lg.info("Killing all indexer threads");
		if (executor != null && !executor.isShutdown()) {
			executor.shutdownNow();
			try {
				executor.awaitTermination(3, TimeUnit.SECONDS);
			} catch (InterruptedException e) {

			}
		}
	}

	/**
	 * This is invoked to index a selection of files
	 * 
	 * @author Marco Meschieri - LogicalDOC
	 * @since 8.2.1
	 */
	class IndexerThread implements Callable<Long> {

		private long indexingTime = 0;

		private long parsingTime = 0;

		private List<Long> docIds = new ArrayList<Long>();

		private int number = 1;

		private boolean completed = false;

		private int indexed = 0;

		private int errors = 0;

		IndexerThread(List<Long> docIds, int number) {
			this.docIds = docIds;
		}

		@Override
		public Long call() throws Exception {
			try {
				/*
				 * Prepare the master transaction object
				 */
				DocumentHistory transaction = new DocumentHistory();
				transaction.setUser(userDao.findByUsername("_system"));

				for (Long id : docIds) {
					try {
						log.debug("Thread {}: Indexing document {}", number, id);

						Document doc = documentDao.findById(id);
						Tenant tenant = tenantDao.findById(doc.getTenantId());

						// Check if this document must be marked for skipping
						if (!FileUtil.matches(doc.getFileName(),
								config.getProperty(tenant.getName() + ".index.includes") == null ? ""
										: config.getProperty(tenant.getName() + ".index.includes"),
								config.getProperty(tenant.getName() + ".index.excludes") == null ? ""
										: config.getProperty(tenant.getName() + ".index.excludes"))) {
							documentDao.initialize(doc);
							doc.setIndexed(Document.INDEX_SKIP);
							documentDao.store(doc);
							log.warn("Thread {}: Document {} with filename '{}' marked as unindexable", number, id,
									doc.getFileName());
						} else {
							Date beforeIndexing = new Date();
							parsingTime += documentManager.reindex(id, null, transaction.clone());
							long indexingDiff = TimeDiff.getTimeDifference(beforeIndexing, new Date(),
									TimeField.MILLISECOND);
							indexingTime += indexingDiff;
							log.debug("Thread {}: Indexed document {} in {}ms", number, indexingDiff);
						}
						indexed++;
					} catch (Throwable e) {
						log.error("Thread {}: There was a problem indexing document {}", number, id);
						log.error(e.getMessage(), e);
						errors++;
					} finally {
						next();
					}

					if (interruptRequested) {
						log.debug("Thread {}: interrupt requested", number);
						break;
					}
				}

				log.debug("Thread {} has completed");
				return indexingTime;
			} finally {
				completed = true;
			}
		}

		public long getIndexingTime() {
			return indexingTime;
		}

		public long getParsingTime() {
			return parsingTime;
		}

		public void setIndexingTime(long indexingTime) {
			this.indexingTime = indexingTime;
		}

		public void setParsingTime(long parsingTime) {
			this.parsingTime = parsingTime;
		}

		public boolean isCompleted() {
			return completed;
		}

		public int getIndexed() {
			return indexed;
		}

		public int getErrors() {
			return errors;
		}
	}
}