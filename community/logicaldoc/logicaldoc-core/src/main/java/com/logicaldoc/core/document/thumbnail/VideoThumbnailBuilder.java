package com.logicaldoc.core.document.thumbnail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.core.document.Document;

/**
 * Takes care of generating thumbnails for videos
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.2
 */
public class VideoThumbnailBuilder extends AbstractThumbnailBuilder {
	protected static Logger log = LoggerFactory.getLogger(VideoThumbnailBuilder.class);

	@Override
	public synchronized void buildThumbnail(String sid, Document document, String fileVersion, File src, File dest,
			int size, int quality) throws IOException {
		File frameImage = File.createTempFile("album-", ".png");
		try {
			if (document.getFileName().toLowerCase().endsWith(".mp4"))
				writeMp4Frame(src, frameImage);
			else
				writeVideoFrame(src, frameImage);

			ImageThumbnailBuilder imageTBuilder = new ImageThumbnailBuilder();
			imageTBuilder.buildThumbnail(sid, document, fileVersion, frameImage, dest, size, quality);
		} catch (Throwable e) {
			throw new IOException("Error in extracting video frame", e);
		} finally {
			try {
				FileUtils.deleteQuietly(frameImage);
			} catch (Throwable e) {
				// do nothing
			}
		}
	}

	private void writeMp4Frame(File videoFile, File frameFile) throws Exception {
		FFmpegFrameGrabber g = new FFmpegFrameGrabber(videoFile);
		g.start();

		/*
		 * Get a frame in the middle of the video
		 */
		int startFrame = g.getLengthInVideoFrames() / 2;
		g.setVideoFrameNumber(startFrame);

		try {
			for (int i = startFrame; i < g.getLengthInFrames() && frameFile.length() == 0; i++) {
				try {
					Frame frame = g.grab();
					if (frame == null)
						continue;
					BufferedImage img = Java2DFrameUtils.toBufferedImage(frame);
					if (img == null)
						continue;

					ImageIO.write(img, "png", frameFile);
				} catch (Throwable t) {
				}
			}
		} finally {
			g.stop();
			g.close();
		}
	}

	private void writeVideoFrame(File videoFile, File frameFile) throws Exception {
		FrameGrabber g = new OpenCVFrameGrabber(videoFile);
		g.start();

		/*
		 * Try to get a frame after 60 seconds
		 */
		double frameRate = g.getFrameRate();
		int fiveSecondsFrame = (int) (60 * frameRate);
		if (fiveSecondsFrame > g.getLengthInFrames())
			fiveSecondsFrame = 1;

		try {
			for (int i = 0; i < g.getLengthInFrames() && frameFile.length() == 0; i++) {
				try {
					if (i < fiveSecondsFrame) {
						g.grab();
						continue;
					}

					Frame frame = g.grab();
					if (frame == null)
						continue;
					BufferedImage img = Java2DFrameUtils.toBufferedImage(frame);
					if (img == null)
						continue;

					ImageIO.write(img, "png", frameFile);
				} catch (Throwable t) {
				}
			}
		} finally {
			g.stop();
			g.close();
		}
	}

}