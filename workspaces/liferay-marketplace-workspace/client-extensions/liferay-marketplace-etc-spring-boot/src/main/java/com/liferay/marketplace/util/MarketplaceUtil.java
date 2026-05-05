/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.marketplace.util;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Category;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.SkuOption;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.marketplace.model.PublisherAssetLink;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ExternalLink;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Keven Leone
 * @author Eduardo Diniz
 */
public class MarketplaceUtil {

	public static File addArtifactMetadata(
			File file, String fileName, Map<String, Properties> propertiesMap)
		throws IOException {

		Path tempDirectoryPath = Files.createTempDirectory("marketplace-temp-");

		Path path = tempDirectoryPath.resolve(fileName);

		try (OutputStream outputStream = Files.newOutputStream(path)) {
			addPropertiesToZipFile(file, propertiesMap, null, outputStream);
		}

		return path.toFile();
	}

	public static void addPropertiesToZipFile(
			File sourceZipFile, Map<String, Properties> propertiesMap,
			Map<String, Path> filesMap, OutputStream outputStream)
		throws IOException {

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(
				outputStream)) {

			if (sourceZipFile != null) {
				Set<String> ignoreEntryNames = Collections.emptySet();

				if (propertiesMap != null) {
					ignoreEntryNames = propertiesMap.keySet();
				}

				try (ZipFile zipFile = new ZipFile(sourceZipFile)) {
					_cloneZipFile(zipFile, zipOutputStream, ignoreEntryNames);
				}
			}

			_addPropertiesToZipFile(propertiesMap, zipOutputStream);

			_addFilesToZipFile(filesMap, zipOutputStream);
		}
	}

	public static ExternalLink[] appendExternalLink(
		ExternalLink[] externalLinks, String domain, String entityId,
		String entityName) {

		if (ArrayUtil.isEmpty(externalLinks)) {
			externalLinks = new ExternalLink[0];
		}

		for (ExternalLink externalLink : externalLinks) {
			if (Objects.equals(externalLink.getDomain(), domain) &&
				Objects.equals(externalLink.getEntityName(), entityName)) {

				if (_log.isInfoEnabled()) {
					_log.info("External link already exists for " + domain);
				}

				return externalLinks;
			}
		}

		ExternalLink externalLink = new ExternalLink();

		externalLink.setDomain(domain);
		externalLink.setEntityId(entityId);
		externalLink.setEntityName(entityName);

		return ArrayUtil.append(externalLinks, externalLink);
	}

	public static JSONArray createCloudProvisioningJSONArray(
		OrderItem[] orderItems) {

		JSONArray jsonArray = new JSONArray();

		for (OrderItem orderItem : orderItems) {
			jsonArray.put(
				new JSONObject(
				).put(
					"deployments", new JSONArray()
				).put(
					"orderItemId", orderItem.getId()
				).put(
					"quantity",
					orderItem.getQuantity(
					).intValue()
				).put(
					"shippedQuantity", 0
				).put(
					"sku", orderItem.getSku()
				));
		}

		return jsonArray;
	}

	public static Properties createMarketplaceProperties(
		Product product, Map<String, String> productSpecificationsMap,
		PublisherAssetLink publisherAssetLink, String bundleSymbolicName,
		String bundleVersion, String bundles, String title) {

		// liferay-marketplace.properties

		Properties productProperties = new Properties();

		String productTitle = null;

		if (product != null) {
			productProperties.setProperty(
				"category",
				GetterUtil.getString(getCategoryName(product.getCategories())));
			productProperties.setProperty(
				"icon-url", GetterUtil.getString(product.getThumbnail()));
			productProperties.setProperty(
				"remote-app-id", String.valueOf(product.getId()));
			productProperties.setProperty(
				"description",
				GetterUtil.getString(
					getDefaultLocale(product.getDescription())));

			productTitle = getDefaultLocale(product.getName());
		}

		productProperties.setProperty(
			"title", _getOrDefault(title, productTitle));

		String pubVersion = null;

		if (publisherAssetLink != null) {
			pubVersion = publisherAssetLink.getVersion();
		}

		productProperties.setProperty(
			"version", _getOrDefault(bundleVersion, pubVersion));

		productProperties.setProperty("required", "false");
		productProperties.setProperty("restart-required", "false");

		if (product != null) {
			productProperties.setProperty(
				"description", getDefaultLocale(product.getDescription()));
		}

		if (Validator.isNotNull(bundleSymbolicName)) {
			productProperties.setProperty(
				"liferay-marketplace-bundle-symbolic-name", bundleSymbolicName);
		}

		if (Validator.isNotNull(bundleVersion)) {
			productProperties.setProperty(
				"liferay-marketplace-bundle-version", bundleVersion);
		}

		if (Validator.isNotNull(bundles)) {
			productProperties.setProperty("bundles", bundles);
		}

		return productProperties;
	}

	public static String createTemporaryDeployment(
			Map<String, String> customFields, JSONArray jsonArray,
			JSONObject jsonObject, String projectId)
		throws Exception {

		UUID uuid = UUID.randomUUID();

		jsonObject.put(
			"deployments",
			jsonObject.getJSONArray(
				"deployments"
			).put(
				new JSONObject(
				).put(
					"id", uuid.toString()
				).put(
					"loading", true
				).put(
					"projectId", projectId
				)
			));

		customFields.put("cloud-provisioning", jsonArray.toString());

		return uuid.toString();
	}

	public static void deleteDeployment(
		String deploymentId, JSONObject jsonObject) {

		JSONArray deploymentsJSONArray = jsonObject.getJSONArray("deployments");

		for (int i = 0; i < deploymentsJSONArray.length(); i++) {
			JSONObject deploymentJSONObject =
				deploymentsJSONArray.getJSONObject(i);

			if (Objects.equals(
					deploymentJSONObject.getString("id"), deploymentId)) {

				deploymentsJSONArray.remove(i);
			}
		}
	}

	public static void deleteTempFile(
		File file, boolean deleteParentDirectory) {

		try {
			if (file != null) {
				Files.deleteIfExists(file.toPath());

				if (deleteParentDirectory) {
					Files.deleteIfExists(
						file.toPath(
						).getParent());
				}
			}
		}
		catch (Exception exception) {
			_log.error(exception);
		}
	}

	public static String format(Date date) {
		return format(date, "Not Applicable");
	}

	public static String format(Date date, String defaultValue) {
		if (date == null) {
			return defaultValue;
		}

		return date.toInstant(
		).atZone(
			ZoneId.of("UTC")
		).format(
			DateTimeFormatter.ofPattern("MMMM d, yyyy")
		);
	}

	public static Map<String, Properties> getArtifactPropertiesMap(
		Product product, Map<String, String> productSpecificationsMap,
		PublisherAssetLink publisherAssetLink, String bundleSymbolicName,
		String bundleVersion, String bundles, String title) {

		return HashMapBuilder.<String, Properties>put(
			"liferay-marketplace.properties",
			() -> createMarketplaceProperties(
				product, productSpecificationsMap, publisherAssetLink,
				bundleSymbolicName, bundleVersion, bundles, title)
		).put(
			"META-INF/marketplace.properties",
			() -> {
				if ((productSpecificationsMap != null) &&
					Objects.equals(
						productSpecificationsMap.get("price-model"), "Paid")) {

					return createMarketplaceProperties(
						product, productSpecificationsMap, publisherAssetLink,
						bundleSymbolicName, bundleVersion, bundles, title);
				}

				return null;
			}
		).build();
	}

	public static String getCategoryName(Category[] categories) {
		if (ArrayUtil.isEmpty(categories)) {
			return "";
		}

		for (Category category : categories) {
			if (_isMarketplaceCategory(category.getVocabulary())) {
				return category.getName();
			}
		}

		return categories[0].getName();
	}

	public static JSONObject getCloudProvisioningJSONObject(
		JSONArray jsonArray, long orderItemId) {

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);

			if (Objects.equals(
					jsonObject.getLong("orderItemId"), orderItemId)) {

				return jsonObject;
			}
		}

		return new JSONObject();
	}

	public static String getDefaultLocale(Map<String, String> localeMap) {
		return localeMap.get("en_US");
	}

	public static Date getOrderPurchaseEndDate(
		String licenseType, String licenseUsageType) {

		ZonedDateTime zonedDateTime = ZonedDateTime.now();

		if (StringUtil.equalsIgnoreCase(licenseType, "3 Months Limited Beta")) {
			return Date.from(
				zonedDateTime.plusMonths(
					3
				).toInstant());
		}

		if (StringUtil.equalsIgnoreCase(licenseUsageType, "Trial")) {
			return Date.from(
				zonedDateTime.plusMonths(
					1
				).toInstant());
		}

		return Date.from(
			zonedDateTime.plusYears(
				1
			).toInstant());
	}

	public static String getSkuOptionValue(String key, SkuOption[] skuOptions) {
		for (SkuOption skuOption : skuOptions) {
			String skuOptionKey = skuOption.getKey();

			if ((skuOptionKey == null) || !skuOptionKey.endsWith(key)) {
				continue;
			}

			return skuOption.getValue();
		}

		return null;
	}

	public static String getSkuOptionValue(String key, String options) {
		JSONArray optionsJSONArray = new JSONArray(options);

		for (int i = 0; i < optionsJSONArray.length(); i++) {
			JSONObject jsonObject = optionsJSONArray.getJSONObject(i);

			String skuOptionKey = jsonObject.optString("key");

			if (!skuOptionKey.endsWith(key)) {
				continue;
			}

			JSONArray jsonArray = jsonObject.getJSONArray("value");

			return jsonArray.getString(0);
		}

		return null;
	}

	private static void _addFilesToZipFile(
			Map<String, Path> filesMap, ZipOutputStream zipOutputStream)
		throws IOException {

		if (filesMap == null) {
			return;
		}

		for (Map.Entry<String, Path> entry : filesMap.entrySet()) {
			zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));

			Files.copy(entry.getValue(), zipOutputStream);

			zipOutputStream.closeEntry();
		}
	}

	private static void _addPropertiesToZipFile(
			Map<String, Properties> propertiesMap,
			ZipOutputStream zipOutputStream)
		throws IOException {

		if (propertiesMap == null) {
			return;
		}

		for (Map.Entry<String, Properties> entry : propertiesMap.entrySet()) {
			Properties properties = entry.getValue();

			if (properties == null) {
				continue;
			}

			zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));

			properties.store(zipOutputStream, null);

			zipOutputStream.closeEntry();
		}
	}

	private static void _cloneZipFile(
			ZipFile zipFile, ZipOutputStream zipOutputStream,
			Set<String> ignoreEntryNames)
		throws IOException {

		Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

		while (enumeration.hasMoreElements()) {
			ZipEntry zipEntry = enumeration.nextElement();

			if (ignoreEntryNames.contains(zipEntry.getName())) {
				continue;
			}

			zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));

			if (!zipEntry.isDirectory()) {
				try (InputStream inputStream = zipFile.getInputStream(
						zipEntry)) {

					inputStream.transferTo(zipOutputStream);
				}
			}

			zipOutputStream.closeEntry();
		}
	}

	private static String _getOrDefault(String primary, String secondary) {
		if (Validator.isNotNull(primary)) {
			return primary;
		}

		return GetterUtil.getString(secondary);
	}

	private static boolean _isMarketplaceCategory(String vocabulary) {
		if (Objects.equals(vocabulary, "marketplace app category") ||
			Objects.equals(vocabulary, "marketplace category")) {

			return true;
		}

		return false;
	}

	private static final Log _log = LogFactory.getLog(MarketplaceUtil.class);

}