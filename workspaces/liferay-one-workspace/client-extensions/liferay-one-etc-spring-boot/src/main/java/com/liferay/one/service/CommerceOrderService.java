/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.address.client.dto.v1_0.Country;
import com.liferay.headless.admin.address.client.resource.v1_0.CountryResource;
import com.liferay.headless.admin.user.client.dto.v1_0.PostalAddress;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Currency;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Sku;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.CurrencyResource;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.SkuResource;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.BillingAddress;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.headless.commerce.admin.order.client.pagination.Page;
import com.liferay.headless.commerce.admin.order.client.pagination.Pagination;
import com.liferay.headless.commerce.admin.order.client.problem.Problem;
import com.liferay.headless.commerce.admin.order.client.resource.v1_0.OrderResource;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.constants.SupportRegionConstants;
import com.liferay.one.util.SupportRegionUtil;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class CommerceOrderService extends OneBaseService {

	public void calculateTax(long commerceOrderId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		Order order = orderResource.getOrder(commerceOrderId);

		BillingAddress billingAddress = order.getBillingAddress();

		if ((billingAddress == null) ||
			!_isTaxApplicable(order.getAccount(), billingAddress)) {

			return;
		}

		Map<String, String> customFields = _getCustomFields(order);

		BigDecimal subtotalAmount = BigDecimal.valueOf(
			order.getSubtotalAmount());

		BigDecimal taxAmount = subtotalAmount.multiply(
			BigDecimal.valueOf(_TAX_PERCENTAGE));

		BigDecimal total = subtotalAmount.add(taxAmount);

		Order taxedOrder = new Order();

		taxedOrder.setCustomFields(() -> customFields);
		taxedOrder.setTaxAmount(() -> taxAmount);
		taxedOrder.setTotal(() -> total);

		orderResource.patchOrder(commerceOrderId, taxedOrder);

		for (OrderItem orderItem : order.getOrderItems()) {
			BigDecimal finalPrice = orderItem.getFinalPrice();

			OrderItem taxedOrderItem = new OrderItem();

			taxedOrderItem.setFinalPrice(() -> finalPrice);
			taxedOrderItem.setFinalPriceWithTaxAmount(
				() -> finalPrice.add(
					finalPrice.multiply(BigDecimal.valueOf(_TAX_PERCENTAGE))));
			taxedOrderItem.setPriceManuallyAdjusted(() -> true);

			_commerceOrderItemService.patchOrderItem(
				orderItem.getId(), taxedOrderItem);
		}
	}

	public void completeOrder(long orderId, int paymentStatus)
		throws Exception {

		completeOrder(null, orderId, paymentStatus);
	}

	public void completeOrder(
			Map<String, ?> customFields, long orderId, int paymentStatus)
		throws Exception {

		updateOrder(
			customFields, orderId, CommerceOrderConstants.ORDER_STATUS_PENDING);

		updateOrder(
			null, orderId, CommerceOrderConstants.ORDER_STATUS_PROCESSING);

		updateOrder(
			null, orderId, CommerceOrderConstants.ORDER_STATUS_COMPLETED,
			paymentStatus);

		Order order = fetchCommerceOrder(orderId);

		if ((order != null) &&
			Objects.equals(
				order.getOrderTypeExternalReferenceCode(), "AI_HUB")) {

			_provisionAiHub(order);
		}
	}

	public Order fetchCommerceOrder(long commerceOrderId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		try {
			return orderResource.getOrder(commerceOrderId);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	public Order getCommerceOrder(long commerceOrderId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		return orderResource.getOrder(commerceOrderId);
	}

	public List<Order> getOrders(String filterString) throws Exception {
		List<Order> orders = new ArrayList<>();

		OrderResource orderResource = _buildOrderResource();

		int page = 1;

		while (true) {
			Page<Order> ordersPage = orderResource.getOrdersPage(
				null, filterString, Pagination.of(page, _PAGE_SIZE), null);

			orders.addAll(ordersPage.getItems());

			if (page >= ordersPage.getLastPage()) {
				break;
			}

			page++;
		}

		return orders;
	}

	public Country getCountryByA2(String a2) throws Exception {
		CountryResource countryResource = CountryResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		return countryResource.getCountryByA2(a2);
	}

	public OrderResource getOrderResource() {
		return _buildOrderResource();
	}

	public Sku getSku(long skuId) throws Exception {
		SkuResource skuResource = SkuResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		return skuResource.getSku(skuId);
	}

	public String getSupportRegion(long accountId, Long defaultBillingAddressId)
		throws Exception {

		String addressCountry = null;

		if (Validator.isNotNull(defaultBillingAddressId)) {
			PostalAddress postalAddress =
				_postalAddressService.getPostalAddress(defaultBillingAddressId);

			addressCountry = postalAddress.getAddressCountry();
		}

		OrderResource orderResource = _buildOrderResource();

		Page<Order> ordersPage = orderResource.getOrdersPage(
			null, "accountId/any(x:x eq " + accountId + ")", null, null);

		for (Order order : ordersPage.getItems()) {
			Map<String, String> customFields =
				(Map<String, String>)order.getCustomFields();

			if (customFields == null) {
				continue;
			}

			String opportunitySoldBy = customFields.get("opportunitySoldBy");

			if (Validator.isNull(opportunitySoldBy)) {
				continue;
			}

			return SupportRegionUtil.getSupportRegion(
				opportunitySoldBy, addressCountry);
		}

		return SupportRegionConstants.GLOBAL;
	}

	public void updateOrder(
			Map<String, ?> customFields, long orderId, int orderStatus)
		throws Exception {

		OrderResource orderResource = _buildOrderResource();

		Order order = new Order();

		order.setCustomFields(() -> customFields);
		order.setOrderStatus(() -> orderStatus);

		orderResource.patchOrder(orderId, order);
	}

	public void updateOrder(
			Map<String, ?> customFields, long orderId, int orderStatus,
			int paymentStatus)
		throws Exception {

		OrderResource orderResource = _buildOrderResource();

		Order order = new Order();

		order.setCustomFields(() -> customFields);
		order.setOrderStatus(() -> orderStatus);
		order.setPaymentStatus(() -> paymentStatus);

		orderResource.patchOrder(orderId, order);
	}

	private CurrencyResource _buildCurrencyResource() {
		return CurrencyResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();
	}

	private OrderResource _buildOrderResource() {
		return OrderResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameters(
			"nestedFields", "account,billingAddress,customFields,orderItems"
		).build();
	}

	private Map<String, String> _getCustomFields(Order order) throws Exception {
		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		JSONObject orderMetadataJSONObject = new JSONObject(
			customFields.getOrDefault("order-metadata", "{}"));

		if (orderMetadataJSONObject.has("exchangeRate")) {
			return customFields;
		}

		CurrencyResource currencyResource = _buildCurrencyResource();

		Currency currency = currencyResource.getCurrenciesPage(
			null, "code eq 'EUR'",
			com.liferay.headless.commerce.admin.catalog.client.pagination.
				Pagination.of(1, 1),
			null
		).fetchFirstItem();

		if (currency == null) {
			return customFields;
		}

		customFields.put(
			"order-metadata",
			orderMetadataJSONObject.put(
				"exchangeRate", currency.getRate()
			).toString());

		return customFields;
	}

	private boolean _isTaxApplicable(
		Account account, BillingAddress billingAddress) {

		String countryISOCode = billingAddress.getCountryISOCode();

		if (Objects.equals(account.getType(), _ACCOUNT_TYPE_BUSINESS)) {
			return Objects.equals(countryISOCode, "IE");
		}

		if (Objects.equals(account.getType(), _ACCOUNT_TYPE_PERSON)) {
			return _europeanCountryISOCodes.contains(countryISOCode);
		}

		return false;
	}

	private void _provisionAiHub(Order order) throws Exception {
		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		if ((customFields == null) ||
			!customFields.containsKey("order-metadata")) {

			return;
		}

		try {
			JSONObject orderMetadataJSONObject = new JSONObject(
				customFields.get("order-metadata"));

			if (!orderMetadataJSONObject.has("aiHubForm")) {
				return;
			}

			JSONObject aiHubFormJSONObject =
				orderMetadataJSONObject.getJSONObject("aiHubForm");

			String emailAddress = aiHubFormJSONObject.getString(
				"administratorEmailAddress");

			String firstName = "AI Hub";
			String lastName = "Administrator";

			try {
				UserAccount userAccount =
					_userAccountService.getUserAccountByEmailAddress(
						emailAddress);

				if (userAccount != null) {
					firstName = userAccount.getGivenName();
					lastName = userAccount.getFamilyName();
				}
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to fetch user account details for " +
							emailAddress,
						exception);
				}
			}

			JSONObject provisionJSONObject = new JSONObject(
			).put(
				"accountName", aiHubFormJSONObject.getString("aiHubAccountName")
			).put(
				"companyName",
				order.getAccount(
				).getName()
			).put(
				"userAccounts",
				new JSONArray(
				).put(
					new JSONObject(
					).put(
						"emailAddress", emailAddress
					).put(
						"firstName", firstName
					).put(
						"lastName", lastName
					)
				)
			);

			JSONObject aiHubJSONObject = _aiHubService.provision(
				provisionJSONObject);

			if (aiHubJSONObject != null) {
				_aiHubService.putAIHubApplication(
					"AI-HUB-" + order.getAccountExternalReferenceCode(),
					new JSONObject(
					).put(
						"accountEntryId",
						aiHubJSONObject.getInt("accountEntryId")
					).put(
						"accountName",
						aiHubFormJSONObject.getString("aiHubAccountName")
					).put(
						"administratorEmailAddress",
						aiHubFormJSONObject.getString(
							"administratorEmailAddress")
					).put(
						"r_accountToAIHubApplication_accountEntryERC",
						order.getAccount(
						).getExternalReferenceCode()
					).put(
						"r_orderToAIHubApplication_commerceOrderERC",
						order.getExternalReferenceCode()
					));
			}
		}
		catch (Exception exception) {
			_log.error(
				"Unable to provision AI Hub for order: " + order.getId(),
				exception);
		}
	}

	private static final int _ACCOUNT_TYPE_BUSINESS = 2;

	private static final int _ACCOUNT_TYPE_PERSON = 1;

	private static final int _PAGE_SIZE = 500;

	private static final double _TAX_PERCENTAGE = 0.20;

	private static final Log _log = LogFactory.getLog(
		CommerceOrderService.class);

	private static final Set<String> _europeanCountryISOCodes = Set.of(
		"AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR",
		"HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO",
		"SE", "SI", "SK");

	@Autowired
	private AIHubService _aiHubService;

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private PostalAddressService _postalAddressService;

	@Autowired
	private UserAccountService _userAccountService;

}