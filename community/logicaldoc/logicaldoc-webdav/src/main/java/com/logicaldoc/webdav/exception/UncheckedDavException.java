/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package com.logicaldoc.webdav.exception;

import org.apache.jackrabbit.webdav.DavException;

public class UncheckedDavException extends RuntimeException {

	private static final long serialVersionUID = -7686446506961408049L;
	
	private final int errorCode;

	public UncheckedDavException(int errorCode, String message) {
		this(errorCode, message, null);
	}

	public UncheckedDavException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public DavException toDavException() {
		return new DavException(errorCode, getMessage(), getCause(), null);
	}

}
