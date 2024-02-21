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
 * Converter to convert PDF into image
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.5.1
 */
public class GhostscriptConverter extends AbstractFormatConverter {

	protected static Logger log = LoggerFactory.getLogger(GhostscriptConverter.class);

	@Override
	public void internalConvert(String sid, Document document, File src, File dest) throws IOException {
		String ext = FilenameUtils.getExtension(dest.getName()).toLowerCase();

		try {
			String pages = "";
			String device = "jpeg";
			if ("tif".equals(ext) || "tiff".equals(ext))
				device = "tiff24nc";
			else if ("png".equals(ext)) {
				device = "png16m";
				pages = "-dFirstPage=1 -dLastPage=1";
			} else if ("ps".equals(ext)) {
				device = "ps2write";
				pages = "-dFirstPage=1 -dLastPage=1";
			} else if ("txt".equals(ext))
				device = "txtwrite";
			else if ("eps".equals(ext))
				device = "eps2write ";

			int timeout = 30;
			try {
				timeout=Integer.parseInt(getParameter("timeout"));
			}catch(Throwable t) {}
			
			String commandLine = getParameter("path") + " "
					+ (getParameter("arguments") != null ? getParameter("arguments") : "") + " -sDEVICE=" + device + " "
					+ pages + " -sOutputFile=" + dest.getPath() + " " + src.getPath();
			Exec.exec(commandLine, null, null, timeout);

			if (!dest.exists() || dest.length() < 1)
				throw new Exception("Empty conversion");
		} catch (

		Throwable e) {
			throw new IOException("Error in PDF to image conversion", e);
		}
	}

	@Override
	public List<String> getParameterNames() {
		return Arrays.asList("path", "arguments", "timeout");
	}
}