package com.logicaldoc.core.conversion;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.core.document.Document;
import com.logicaldoc.util.exec.Exec;

/**
 * Converter to convert image files in PDF
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 7.7
 */
public class ImageConverter extends AbstractFormatConverter {

	protected static Logger log = LoggerFactory.getLogger(ImageConverter.class);

	@Override
	public void internalConvert(String sid, Document document, File src, File dest) throws IOException {
		String ext = FilenameUtils.getExtension(dest.getName()).toLowerCase();
		if (!"pdf".equals(ext))
			throw new IOException("Unable to convert image to " + ext);

		try {
			int timeout = 10;
			try {
				timeout=Integer.parseInt(getParameter("timeout"));
			}catch(Throwable t) {}
			
			String commandLine = getParameter("path") + " -compress JPEG " + src.getPath() + " " + dest.getPath();
			Exec.exec(commandLine, null, null, timeout);

			if (!dest.exists() || dest.length() < 1)
				throw new Exception("Empty conversion");
		} catch (Throwable e) {
			throw new IOException("Error in IMG to PDF conversion", e);
		}
	}

	@Override
	public List<String> getParameterNames() {
		return Arrays.asList("path","timeout");
	}
}