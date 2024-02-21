package com.logicaldoc.core.folder;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.logicaldoc.core.task.Task;

/**
 * This task calculate the path attributes of the folders(only those folders
 * without path will be processed.
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.3.3
 */
public class PathCalculator extends Task {

	public static final String NAME = "PathCalculator";

	private FolderDAO folderDao;

	private long processed = 0;

	private long errors = 0;

	public PathCalculator() {
		super(NAME);
		log = LoggerFactory.getLogger(PathCalculator.class);
	}

	@Override
	public boolean isIndeterminate() {
		return false;
	}

	@Override
	public boolean isConcurrent() {
		return false;
	}

	@Override
	protected void runTask() throws Exception {
		log.info("Start indexing of all documents");

		errors = 0;
		processed = 0;

		try {
			// First of all find folders to be processed and not already
			// involved into a transaction
			@SuppressWarnings("unchecked")
			List<Long> ids = (List<Long>) folderDao
					.queryForList("select ld_id from ld_folder where ld_deleted=0 and ld_path is null", Long.class);
			log.info("Found a total of {} folders to be processed", ids.size());
			setSize(ids.size());

			if (!ids.isEmpty()) {
				for (Long id : ids) {
					try {
						String path = folderDao.computePath(id);
						folderDao.jdbcUpdate("update ld_folder set ld_path='" + path + "' where ld_id=" + id);
						processed++;
						if (interruptRequested)
							break;
					} catch (Throwable t) {
						log.error("Error processing folder {}: {}", id, t.getMessage(), t);
						errors++;
					} finally {
						next();
					}
				}
			}
		} finally {
			log.info("Path calculation finished");
			log.info("Processed folders: {}", processed);
			log.info("Errors: {}", errors);
		}
	}

	public FolderDAO getFolderDao() {
		return folderDao;
	}

	public void setFolderDao(FolderDAO folderDao) {
		this.folderDao = folderDao;
	}
}