package com.logicaldoc.util.config;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.logicaldoc.util.io.FileUtil;


/**
 * Test case for <code>SecurityConfigurator</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.7.3
 */
public class SecurityConfiguratorTest {

	File contextSecurityXml = new File("target/context-security.xml");

	@Before
	public void setUp() throws Exception {
		FileUtil.copyResource("/context-security.xml", contextSecurityXml);
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testGetContentSecurityPolicy() {
		SecurityConfigurator config = new SecurityConfigurator(contextSecurityXml.getPath());
		String policies = config.getContentSecurityPolicy();
		Assert.assertNotNull(policies);
		Assert.assertTrue(policies.startsWith("default-src 'self' 'unsafe-inline' 'unsafe-eval'; script-src 'self'"));
	}
}