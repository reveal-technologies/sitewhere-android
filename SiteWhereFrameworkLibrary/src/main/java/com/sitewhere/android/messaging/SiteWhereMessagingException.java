/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sitewhere.android.messaging;

/**
 * Exception thrown when errors occur in SiteWhere messaging operations.
 * 
 * @author Derek
 */
public class SiteWhereMessagingException extends Exception {

	/** Serial version UID */
	private static final long serialVersionUID = 1088751916200134233L;

	public SiteWhereMessagingException() {
	}

	public SiteWhereMessagingException(String detailMessage) {
		super(detailMessage);
	}

	public SiteWhereMessagingException(Throwable throwable) {
		super(throwable);
	}

	public SiteWhereMessagingException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}