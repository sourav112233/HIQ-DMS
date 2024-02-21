package com.logicaldoc.core.conversion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.core.document.Document;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.io.FileUtil;
import com.logicaldoc.util.io.RarUtil;

/**
 * Converts a Rar file in it's listing
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.7.1
 */
public class RarConverter extends AbstractFormatConverter {

	protected static Logger log = LoggerFactory.getLogger(RarConverter.class);

	@Override
	public void internalConvert(String sid, Document document, File src, File dest) throws IOException {
		List<String> entries = RarUtil.listEntries(src);
		if (entries.size() > 1)
			convertMultipleEntries(sid, document, src, dest, entries);
		else
			convertSingleEntry(sid, document, src, dest, entries.get(0));
	}

	private void convertSingleEntry(String sid, Document document, File src, File dest, String entry)
			throws IOException {
		String entryExtension = FilenameUtils.getExtension(entry);
		File uncompressedEntryFile = File.createTempFile("unRar", "." + entryExtension);

		String targetExtension = FilenameUtils.getExtension(dest.getName()).toLowerCase();
		try {
			RarUtil.extractEntry(src, entry, uncompressedEntryFile);
			FormatConverterManager manager = (FormatConverterManager) Context.get()
					.getBean(FormatConverterManager.class);
			FormatConverter converter = manager.getConverter(entryExtension, targetExtension);

			if (converter == null)
				throw new IOException(
						String.format("Unable to find a converter from %s to %s", entryExtension, targetExtension));
			Document clone = document.clone();
			clone.setFileName(uncompressedEntryFile.getName());
			converter.convert(sid, document, uncompressedEntryFile, dest);
		} finally {
			if (uncompressedEntryFile != null)
				FileUtil.strongDelete(uncompressedEntryFile);
		}
	}

	private void convertMultipleEntries(String sid, Document document, File src, File dest, List<String> entries)
			throws IOException {
		File tempFile = File.createTempFile("rarconvert", ".txt");
		try (FileWriter writer = new FileWriter(tempFile);) {
			for (String line : entries) {
				writer.write(line);
				writer.write("\n");
			}
			writer.flush();

			String targetExtension = FilenameUtils.getExtension(dest.getName()).toLowerCase();
			if ("txt".equals(targetExtension)) {
				FileUtil.copyFile(tempFile, dest);
			} else if ("pdf".equals(targetExtension)) {
				FormatConverterManager manager = (FormatConverterManager) Context.get()
						.getBean(FormatConverterManager.class);
				FormatConverter converter = manager.getConverter("txt", targetExtension);

				if (converter == null)
					throw new IOException(
							String.format("Unable to find a converter from %s to %s", "txt", targetExtension));
				converter.convert(sid, document, tempFile, dest);
			}

			if (!dest.exists() || dest.length() < 1)
				throw new Exception("Empty conversion");
		} catch (Throwable e) {
			throw new IOException("Error in Rar conversion", e);
		} finally {
			if (tempFile != null)
				FileUtil.strongDelete(tempFile);
		}
	}
}