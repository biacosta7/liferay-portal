/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.marketplace.model;

/**
 * @author Beatriz Costa
 */
public class JarMetadata {

	public String getBundleName() {
		return _bundleName;
	}

	public String getFileName() {
		return _fileName;
	}

	public String getSymbolicName() {
		return _symbolicName;
	}

	public String getVersion() {
		return _version;
	}

	public void setBundleName(String bundleName) {
		_bundleName = bundleName;
	}

	public void setFileName(String fileName) {
		_fileName = fileName;
	}

	public void setSymbolicName(String symbolicName) {
		_symbolicName = symbolicName;
	}

	public void setVersion(String version) {
		_version = version;
	}

	private String _bundleName;
	private String _fileName;
	private String _symbolicName;
	private String _version;

}