/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.util;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.portal.kernel.util.StringUtil;

import java.time.ZonedDateTime;

import java.util.Date;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Keven Leone
 */
public class CommerceOrderUtil {

	public static String getDefaultLocale(Map<String, String> localeMap) {
		if (localeMap == null) {
			return null;
		}

		return localeMap.get("en_US");
	}

	public static JSONObject getOrderMetadataJSONObject(Order order) {
		if (order == null) {
			return new JSONObject();
		}

		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		if (customFields == null) {
			return new JSONObject();
		}

		return new JSONObject(
			customFields.getOrDefault("order-metadata", "{}"));
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

	public static String getSkuOptionValue(String key, String options) {
		if (options == null) {
			return null;
		}

		JSONArray optionsJSONArray = new JSONArray(options);

		for (int i = 0; i < optionsJSONArray.length(); i++) {
			JSONObject jsonObject = optionsJSONArray.getJSONObject(i);

			String skuOptionKey = jsonObject.optString("key");

			if ((skuOptionKey == null) || !skuOptionKey.endsWith(key)) {
				continue;
			}

			JSONArray jsonArray = jsonObject.getJSONArray("value");

			return jsonArray.getString(0);
		}

		return null;
	}

}