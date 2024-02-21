package com.logicaldoc.webservicesamples;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.activation.DataHandler;

import com.logicaldoc.core.folder.Folder;
import com.logicaldoc.core.searchengine.SearchOptions;
import com.logicaldoc.webservice.model.WSAttribute;
import com.logicaldoc.webservice.model.WSDocument;
import com.logicaldoc.webservice.model.WSFolder;
import com.logicaldoc.webservice.model.WSGroup;
import com.logicaldoc.webservice.model.WSParameter;
import com.logicaldoc.webservice.model.WSRight;
import com.logicaldoc.webservice.model.WSSearchOptions;
import com.logicaldoc.webservice.model.WSSearchResult;
import com.logicaldoc.webservice.model.WSSystemInfo;
import com.logicaldoc.webservice.model.WSTagCloud;
import com.logicaldoc.webservice.model.WSUser;
import com.logicaldoc.webservice.soap.client.SoapAuthClient;
import com.logicaldoc.webservice.soap.client.SoapDocumentClient;
import com.logicaldoc.webservice.soap.client.SoapFolderClient;
import com.logicaldoc.webservice.soap.client.SoapSearchClient;
import com.logicaldoc.webservice.soap.client.SoapSecurityClient;
import com.logicaldoc.webservice.soap.client.SoapSystemClient;
import com.logicaldoc.webservice.soap.client.SoapTagClient;

public class SoapWorkbench {

	public static void main(String[] args) throws Exception {

		String base = "http://eva00:9080/services";

		SoapAuthClient auth = new SoapAuthClient(base + "/Auth");
		SoapDocumentClient documentClient = new SoapDocumentClient(base + "/Document");
		SoapFolderClient folderClient = new SoapFolderClient(base + "/Folder", 1, false, 50);
		SoapSearchClient searchClient = new SoapSearchClient(base + "/Search");
		SoapSystemClient systemClient = new SoapSystemClient(base + "/System");
		SoapSecurityClient securityClient = new SoapSecurityClient(base + "/Security");

		SoapTagClient tagClient = new SoapTagClient(base + "/Tag");

		// Open a session
		String sid = auth.login("admin", "admin");
		System.out.println("Server date: " + systemClient.getInfo().getDate());
		System.out.println("Sid: " + sid);

		try {
			// usersAndGroupsManagement(securityClient, sid);

			// foldersAndDocuments01(documentClient, folderClient, sid);

			// systemInfo(systemClient, sid);

			searchFeatures(searchClient, sid);

			tagFeatures(tagClient, sid);

			foldersAndDocuments02(documentClient, folderClient, sid);
		} finally {
			auth.logout(sid);
		}
	}

	private static void foldersAndDocuments02(SoapDocumentClient documentClient, SoapFolderClient folderClient,
			String sid) throws Exception, IOException, FileNotFoundException {

		WSFolder folder = folderClient.getFolder(sid, 6);
		System.out.println("parent id: " + folder.getParentId());
		folderClient.move(sid, 6, 13);
		folder = folderClient.getFolder(sid, 6);
		System.out.println("parent id: " + folder.getParentId());

		WSFolder wsFolderTest = new WSFolder();
		wsFolderTest.setName("new folder");
		wsFolderTest.setDescription("descr folder test");
		wsFolderTest.setParentId(6);

		WSFolder wsFolder = folderClient.create(sid, wsFolderTest);
		System.out.println("folder name: " + wsFolder.getName());
		System.out.println("folder parentid: " + wsFolder.getParentId());

		folderClient.delete(sid, 6);

		folderClient.rename(sid, 14, "paperino");
		WSFolder wsFolder2 = folderClient.getFolder(sid, 14);
		System.out.println("folder name: " + wsFolder2.getName());

		WSFolder[] folders2 = folderClient.listChildren(sid, 4);
		for (WSFolder wsFolderc : folders2) {
			System.out.println("folder id: " + wsFolderc.getId());
			System.out.println("folder name: " + wsFolderc.getName());
			System.out.println("folder descr: " + wsFolderc.getDescription());
			System.out.println("**************************************");
		}

		WSFolder[] folders3 = folderClient.getPath(sid, 14);
		for (WSFolder wsFolderc2 : folders3) {
			System.out.println("folder id: " + wsFolderc2.getId());
			System.out.println("folder name: " + wsFolderc2.getName());
			System.out.println("folder descr: " + wsFolderc2.getDescription());
			System.out.println("**************************************");
		}

		folderClient.grantGroup(sid, 13, 2, 4091, true);
		folderClient.grantGroup(sid, 13, 3, 4091, true);
		folderClient.grantGroup(sid, 13, -20, 0, true);
		folderClient.grantUser(sid, 13, 2, 0, false);
		WSRight[] rights = folderClient.getGrantedUsers(sid, 14);
		System.out.println("--- " + rights.length);
		for (WSRight right : rights) {
			System.out.println("+++ " + right.getId());
		}

		WSDocument wsDoc = documentClient.getDocument(sid, 1);
		wsDoc.setId(0);
		wsDoc.setFileName("document test.pdf");
		wsDoc.setCustomId("xxxxxxx");
		wsDoc.setFolderId(14L);

		DataHandler data = documentClient.getContent(sid, 1);
		// File file = new
		// File("/C:/Users/Matteo/Desktop/doctest/signdoc_en.pdf");
		documentClient.create(sid, wsDoc, data);

		WSDocument[] docs = documentClient.listDocuments(sid, 14, null);
		for (WSDocument wsDocument : docs) {
			System.out.println("doc id: " + wsDocument.getId());
			System.out.println("doc file name: " + wsDocument.getFileName());
		}

		documentClient.delete(sid, 32);

		docs = documentClient.getDocuments(sid, new Long[] { 100L, 101L, 102L, 103L });
		for (WSDocument wsDocument : docs) {
			System.out.println("doc: " + wsDocument.getFileName());
		}

		WSDocument doc = documentClient.getDocument(sid, 1);
		System.out.println("rating: " + doc.getRating());
		doc.setRating(5);
		documentClient.update(sid, doc);

		data = documentClient.getContent(sid, 1);
		doc.setRating(4);
		doc = documentClient.create(sid, doc, data);
		System.out.println("rating: " + doc.getRating());

		data = documentClient.getContent(sid, 68);
		System.out.println("data: " + data.toString());

		documentClient.lock(sid, 30);
		doc = documentClient.getDocument(sid, 30);
		System.out.println("status: " + doc.getStatus());
		System.out.println("locked user id: " + doc.getLockUserId().longValue());

		documentClient.move(sid, 30, 13);
		doc = documentClient.getDocument(sid, 30);
		System.out.println("folderId: " + doc.getFolderId());

		documentClient.unlock(sid, 30);
		doc = documentClient.getDocument(sid, 30);
		System.out.println("status: " + doc.getStatus());
		System.out.println("locked user id: " + doc.getLockUserId().longValue());

		documentClient.rename(sid, 30, "pluto");
		wsDoc = documentClient.getDocument(sid, 30);
		System.out.println("doc file name: " + wsDoc.getFileName());

		docs = documentClient.getDocuments(sid, new Long[] { 55L, 30L, 32L, 29L });
		for (WSDocument wsDocument : docs) {
			System.out.println("doc: " + wsDocument.getFileName());
		}

		doc = documentClient.getDocument(sid, 27);
		System.out.println("rating: " + doc.getRating());
		doc.setRating(5);
		doc.setCustomId("aaaabbbbb");
		documentClient.update(sid, doc);
		doc = documentClient.getDocument(sid, 27);
		System.out.println("rating: " + doc.getRating());
		System.out.println("customid: " + doc.getCustomId());

		documentClient.checkout(sid, 27);

		doc = documentClient.getDocument(sid, 27);
		System.out.println("status: " + doc.getStatus());
		System.out.println("locked user id: " + doc.getLockUserId().longValue());
		System.out.println("indexed: " + doc.getIndexed());

		data = documentClient.getVersionContent(sid, 12724, "1.1");
		data.writeTo(new FileOutputStream("C:/tmp/buf.txt"));

		doc = documentClient.getDocument(sid, 30);
		System.out.println("status: " + doc.getStatus());
		System.out.println("indexed: " + doc.getIndexed());

		doc = documentClient.getDocument(sid, 29);
		documentClient.restore(sid, 29, 13);

		doc = documentClient.getDocument(sid, 29);
		System.out.println("file name: " + doc.getFileName());

		for (WSDocument wsDocument : documentClient.getVersions(sid, 30)) {
			System.out.println("version: " + wsDocument.getVersion());
		}

		docs = documentClient.getRecentDocuments(sid, 4);
		System.out.println("docs: " + docs.length);
		for (WSDocument wsDocument : docs) {
			System.out.println("doc id: " + wsDocument.getId());
			System.out.println("doc file name: " + wsDocument.getFileName());
			System.out.println("doc customid: " + wsDocument.getCustomId());
			System.out.println("--------------------------------------");
		}

		// doc = documentClient.createAlias(sid, 30, 14);
		doc = documentClient.createAlias(sid, 30, 40, "0");
		System.out.println("doc id: " + doc.getId());
		System.out.println("doc file name: " + doc.getFileName());
		System.out.println("doc customid: " + doc.getCustomId());

		documentClient.sendEmail("ciccio", new Long[] { 690L, 32L, 29L }, "test@logicaldoc.com", "Test Invio Mail 2",
				"Questa mail � un test");

		// History method is in the enterprise audit webservices
		// WSHistory[] history = documentClient.getHistory(sid, 12724);
		// for (WSHistory h : history) {
		// System.out.println(h.getDate() + "->" + h.getEvent() + " - " +
		// h.getVersion());
		// }

		WSFolder f = folderClient.findByPath(sid, "/Default/scomar/folder1x3z/folder6");
		System.out.println(f.getId() + " - " + f.getName());

		doc = documentClient.getDocument(sid, 535494657L);
		for (WSAttribute att : doc.getAttributes()) {
			if (att.getName().equals("utente")) {
				WSUser user = new WSUser();
				user.setId(51L);
				user.setName("Meschieri");
				user.setFirstName("Marco");
				att.setValue(user);
			}
		}

		documentClient.update(sid, doc);

		for (WSAttribute att : doc.getAttributes()) {
			System.out.println(att.getName() + "(" + att.getType() + ")=" + att.getValue()
					+ (att.getType() == WSAttribute.TYPE_USER ? " " + att.getStringValue() : ""));
		}

		docs = documentClient.listDocuments(sid, 60L, "E8CC77DF.tmp");
		if (docs != null) {
			for (WSDocument wsDocument : docs) {
				System.out.println(wsDocument.getFileName());
			}
		}
	}

	private static void searchFeatures(SoapSearchClient searchClient, String sid) throws Exception {

		WSDocument[] documents = searchClient.findByFilename(sid, "pizzo.ods");
		System.out.println("---- " + documents.length);

		List<WSDocument> docsList = Arrays.asList(documents);
		for (WSDocument doc : docsList) {
			System.out.println("file name: " + doc.getFileName());
			System.out.println("custom id: " + doc.getCustomId());
			System.out.println("version: " + doc.getVersion());
			System.out.println("date: " + doc.getDate());
			System.out.println("++++++++++++++++++++++++++++++++");
		}

		documents = searchClient.findByFilename(sid, "marketing.txt");
		System.out.println("---- " + documents.length);

		WSFolder[] folders = searchClient.findFolders(sid, "xxx");
		System.out.println("---- " + folders.length);
		List<WSFolder> foldersList = Arrays.asList(folders);
		for (WSFolder folder : foldersList) {
			System.out.println("id: " + folder.getId());
			System.out.println("title: " + folder.getName());
			System.out.println("++++++++++++++++++++++++++++++++");
		}

		WSSearchOptions opt = new WSSearchOptions();
		opt.setLanguage("en");
		opt.setExpression("paper");
		opt.setExpressionLanguage("en");
		opt.setType(SearchOptions.TYPE_FULLTEXT);
		opt.setMaxHits(10);
		WSSearchResult result = searchClient.find(sid, opt);
		System.out.println("---- " + result.getHits().length);
		for (WSDocument hit : result.getHits()) {
			System.out.println("hit customid: " + hit.getCustomId());
			System.out.println("hit score: " + hit.getScore());
			System.out.println("hit folderid: " + hit.getFolderId());
			System.out.println("hit file name: " + hit.getFileName());
			System.out.println("hit creation: " + hit.getCreation());
			System.out.println("hit summary: " + hit.getSummary());
			System.out.println("************************");
		}
	}

	private static void systemInfo(SoapSystemClient systemClient, String sid) throws Exception {
		WSSystemInfo info = systemClient.getInfo();

		System.out.println("installation id: " + info.getInstallationId());
		System.out.println("product name: " + info.getProductName());
		for (String feature : info.getFeatures()) {
			System.out.println("feature: " + feature);
		}

		for (WSParameter param : systemClient.getStatistics(sid)) {
			System.out.println("name: " + param.getName());
			System.out.println("value: " + param.getValue());
			System.out.println("------------------------------");
		}

		for (String lang : systemClient.getLanguages(sid)) {
			System.out.println("lang: " + lang);
			System.out.println("++++++++++++++++++++++++++++++++");
		}
	}

	private static void foldersAndDocuments01(SoapDocumentClient documentClient, SoapFolderClient folderClient,
			String sid) throws Exception {

		WSFolder[] path = folderClient.getPath(sid, 20L);
		System.out.println("\n");
		for (WSFolder wsFolder : path) {
			System.out.print(wsFolder.getName() + "/");
		}

		path = folderClient.getPath(sid, Folder.ROOTID);
		System.out.println("\n");
		for (WSFolder wsFolder : path) {
			System.out.print(wsFolder.getName() + "/");
		}

		WSFolder newFolder = new WSFolder();
		newFolder.setName("ddddd");
		newFolder.setDescription("new folder ddddd");
		newFolder.setParentId(5);
		newFolder = folderClient.create(sid, newFolder);
		WSFolder[] folders = folderClient.listChildren(sid, 5);
		for (WSFolder folder : folders) {
			System.out.println("folder id: " + folder.getId());
			System.out.println("folder name : " + folder.getName());
		}

		System.out.println("folder id : " + newFolder.getId());
		System.out.println("folder desc: " + newFolder.getDescription());

		WSDocument[] docs = documentClient.getDocuments(sid, new Long[] { 100L, 101L, 102L, 103L });
		for (WSDocument wsDocument : docs) {
			System.out.println("doc: " + wsDocument.getFileName());
		}

		WSDocument doc = documentClient.getDocument(sid, 1);
		System.out.println("rating: " + doc.getRating());
		doc.setRating(5);
		documentClient.update(sid, doc);

		DataHandler data = documentClient.getContent(sid, 1);
		doc.setRating(4);
		doc = documentClient.create(sid, doc, data);
		System.out.println("rating: " + doc.getRating());

		DataHandler data2 = documentClient.getContent(sid, 5561);
		System.out.println("data: " + data2.toString());
	}

	private static void usersAndGroupsManagement(SoapSecurityClient securityClient, String sid) throws Exception {

		WSUser wsUserTest = new WSUser();
		wsUserTest.setName("marco");
		wsUserTest.setEmail("marco@acme.com");
		wsUserTest.setUsername("marco");
		wsUserTest.setFirstName("alle");
		long[] ids = { 2, 3 };
		wsUserTest.setGroupIds(ids);

		Long userId = securityClient.storeUser(sid, wsUserTest);
		System.out.println("user id: " + userId);

		securityClient.changePassword(sid, userId, null, "marco1982");

		securityClient.deleteUser(sid, 4);

		WSUser user = securityClient.getUser(sid, 2L);
		user.setCity("Modena");
		user.setPostalcode("41125");
		long[] ids2 = { 4 };
		user.setGroupIds(ids2);
		securityClient.storeUser(sid, user);

		WSUser newUser = new WSUser();
		newUser.setId(0);
		newUser.setName("pippo");
		newUser.setEmail("ciccio@acme.com");
		newUser.setUsername("pippo");
		newUser.setFirstName("ciccio");
		securityClient.storeUser(sid, newUser);

		WSUser[] users = securityClient.listUsers(sid, null);
		WSUser editingUser = null;
		for (WSUser wsUser : users) {
			if (wsUser.getId() == 3) {
				editingUser = wsUser;
				break;
			}
		}

		if (editingUser != null) {
			editingUser.setGroupIds(new long[] { 3 });
			securityClient.storeUser(sid, editingUser);
		}

		WSUser[] users2 = securityClient.listUsers(sid, null);
		for (WSUser wsUser : users2) {
			System.out.println("--- " + wsUser.getId());
			System.out.println("--- " + wsUser.getUsername());
			System.out.println("--- " + wsUser.getEmail());
			System.out.println("--- " + wsUser.getStreet());
			System.out.println("--- " + wsUser.getGroupIds()[0]);
			System.out.println("------------------------------------");
		}

		WSGroup newGroup = new WSGroup();
		newGroup.setName("gruppo3");
		newGroup.setDescription("gruppo3 desc");
		newGroup.setInheritGroupId(2L);
		newGroup.setUserIds(new long[] { 4, 6 });
		Long grpId = securityClient.storeGroup(sid, newGroup);
		System.out.println("group id: " + grpId);

		WSGroup editGroup = new WSGroup();
		editGroup.setId(5);
		editGroup.setName("ciccio");
		editGroup.setDescription("ciccio desc");
		securityClient.storeGroup(sid, editGroup);

		securityClient.deleteGroup(sid, 14);
		securityClient.deleteGroup(sid, 15);

		WSGroup group = securityClient.getGroup(sid, 16L);
		group.setName("pippo");
		group.setDescription("pippoc desc");
		securityClient.storeGroup(sid, group);

		WSGroup[] groups = securityClient.listGroups(sid);
		WSGroup editingGroup = null;
		for (WSGroup wsGroup : groups) {
			if (wsGroup.getId() == 2) {
				editingGroup = wsGroup;
				break;
			}
		}
		if (editingGroup != null) {
			editingGroup.setUserIds(new long[] { 2, 3 });
			securityClient.storeGroup(sid, editingGroup);
		}

		for (WSGroup wsGroup : groups) {
			System.out.println("--- " + wsGroup.getId());
			System.out.println("--- " + wsGroup.getName());
			System.out.println("--- " + wsGroup.getDescription());
			System.out.println("--- " + wsGroup.getUserIds());
			System.out.println("+++++++++++++++++++++++++++++++++++++");
		}
	}

	private static void tagFeatures(SoapTagClient tagClient, String sid) throws Exception {

		WSDocument[] documents = null;
		List<WSDocument> docsList = new ArrayList<WSDocument>();

		documents = tagClient.findDocumentsByTag(sid, "abc");
		System.out.println("---- " + documents.length);
		docsList = Arrays.asList(documents);
		for (WSDocument doc : docsList) {
			System.out.println("title: " + doc.getFileName());
			System.out.println("custom id: " + doc.getCustomId());
			System.out.println("version: " + doc.getVersion());
			System.out.println("++++++++++++++++++++++++++++++++");
		}

		for (String tag : tagClient.getTags(sid)) {
			System.out.println("tag: " + tag);
			System.out.println("++++++++++++++++++++++++++++++++");
		}

		for (WSTagCloud tag : tagClient.getTagCloud(sid)) {
			System.out.println("tag: " + tag.getTag());
			System.out.println("tag count: " + tag.getCount());
			System.out.println("tag scale: " + tag.getScale());
			System.out.println("++++++++++++++++++++++++++++++++");
		}

	}
}
