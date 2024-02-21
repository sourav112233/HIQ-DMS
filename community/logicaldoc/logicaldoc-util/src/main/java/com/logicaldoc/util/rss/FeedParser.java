package com.logicaldoc.util.rss;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import com.logicaldoc.util.http.HttpUtil;

/**
 * Parses an RSS feed
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 6.1
 */
public class FeedParser {
	public static final String TITLE = "title";

	public static final String DESCRIPTION = "description";

	public static final String CHANNEL = "channel";

	public static final String LANGUAGE = "language";

	public static final String COPYRIGHT = "copyright";

	public static final String LINK = "link";

	public static final String AUTHOR = "author";

	public static final String ITEM = "item";

	public static final String PUB_DATE = "pubDate";

	public static final String GUID = "guid";

	public final URL url;

	public static void main(String[] args) {
		FeedParser parser = new FeedParser(null);
		Feed feed = parser.readFeed();
		List<FeedMessage> messages = feed.getMessages();
		for (FeedMessage message : messages) {
			System.out.println(message.getTitle() + " " + message.getPubDate());
		}
	}

	public FeedParser(String feedUrl) {
		try {
			if (feedUrl != null && StringUtils.isNotEmpty(feedUrl))
				this.url = new URL(feedUrl);
			else
				this.url = new URL("https://www.logicaldoc.com/news/rss");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public Feed readFeed() {
		Feed feed = null;
		try {

			boolean isFeedHeader = true;
			// Set header values intial to the empty string
			String description = "";
			String title = "";
			String link = "";
			String language = "";
			String copyright = "";
			String author = "";
			String pubdate = "";
			String guid = "";

			SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

			// First create a new XMLInputFactory
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Setup a new eventReader
			Reader in = read();
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			// Read the XML document
			while (eventReader.hasNext()) {

				XMLEvent event = eventReader.nextEvent();

				if (event.isStartElement()) {
					if (event.asStartElement().getName().getLocalPart() == (ITEM)) {
						if (isFeedHeader) {
							isFeedHeader = false;
							feed = new Feed(title, link, description, language, copyright, pubdate);
						}
						event = eventReader.nextEvent();
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (TITLE)) {
						event = eventReader.nextEvent();
						title = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (DESCRIPTION)) {
						event = eventReader.nextEvent();
						try {
							description = event.asCharacters().getData();
						} catch (Throwable t) {
						}
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (LINK)) {
						event = eventReader.nextEvent();
						try {
							link = event.asCharacters().getData();
						} catch (Throwable t) {

						}
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (GUID)) {
						event = eventReader.nextEvent();
						guid = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (LANGUAGE)) {
						event = eventReader.nextEvent();
						language = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (AUTHOR)) {
						event = eventReader.nextEvent();
						author = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (PUB_DATE)) {
						event = eventReader.nextEvent();
						pubdate = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (COPYRIGHT)) {
						event = eventReader.nextEvent();
						copyright = event.asCharacters().getData();
						continue;
					}
				} else if (event.isEndElement()) {
					if (event.asEndElement().getName().getLocalPart() == (ITEM)) {
						FeedMessage message = new FeedMessage();
						message.setAuthor(author);
						message.setDescription(description);
						message.setGuid(guid);
						message.setLink(link);
						message.setTitle(title);
						if (StringUtils.isNotBlank(pubdate))
							try {
								message.setPubDate(df.parse(pubdate));
							} catch (ParseException e) {
								System.err.println(e.getMessage());
							}
						feed.getMessages().add(message);
						event = eventReader.nextEvent();
						continue;
					}
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
		return feed;

	}

	private Reader read() {
		CloseableHttpClient httpclient = null;
		try {
			httpclient = HttpUtil.getNotValidatingClient(40);

			HttpGet get = new HttpGet(url.toString());
			get.setHeader("User-Agent",
					"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

			CloseableHttpResponse response = null;
			try {
				response = httpclient.execute(get);
				int result = response.getStatusLine().getStatusCode();
				if (result != HttpStatus.SC_OK)
					throw new IOException("HTTP error " + result);

				return new StringReader(HttpUtil.getBodyString(response));
			} finally {
				if (response != null)
					response.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (httpclient != null)
				try {
					httpclient.close();
				} catch (IOException e) {
				}
		}
	}
}
