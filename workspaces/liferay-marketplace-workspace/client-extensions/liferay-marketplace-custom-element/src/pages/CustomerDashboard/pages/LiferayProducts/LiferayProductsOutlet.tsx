/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';

import {NavbarProps} from '../../../../components/Navbar';
import {OrderTypes, orderTypeDocumentationURL} from '../../../../enums/Order';
import useGetProductByOrderId from '../../../../hooks/useGetProductByOrderId';
import i18n from '../../../../i18n';
import {Liferay} from '../../../../liferay/liferay';
import {getSiteURL} from '../../../../utils/site';
import {BaseOutlet} from '../Apps/App/AppOutlet';

type ProductAndOrderPayload = NonNullable<
	ReturnType<typeof useGetProductByOrderId>['data']
>;

const getTabs = (data: ProductAndOrderPayload): NavbarProps['routes'] => {
	const {orderTypeExternalReferenceCode} = data?.placedOrder ?? {};

	if (orderTypeExternalReferenceCode === OrderTypes.AI_HUB) {
		return [];
	}

	const isCMP = orderTypeExternalReferenceCode === OrderTypes.CMP;
	const isDSR = orderTypeExternalReferenceCode === OrderTypes.DSR;
	const isDXP = orderTypeExternalReferenceCode === OrderTypes.DXP;

	return [
		{
			name: i18n.translate('activation-keys'),
			path: '',
			visible: isCMP || isDSR || isDXP,
		},
		{
			name: i18n.translate('bundles'),
			path: 'bundles',
			visible: isDXP,
		},
		{
			name: i18n.translate('workspace'),
			path: 'workspace',
			visible: isDSR,
		},
	];
};

const LiferayProductsOutlet = () => (
	<BaseOutlet
		actionButtons={(props) => {
			const appBeta =
				props?.marketplaceDeliveryProduct?.specificationValues
					?.APP_BETA;

			const isAiHub =
				props?.placedOrder?.orderTypeExternalReferenceCode ===
				OrderTypes.AI_HUB &&
				props?.placedOrder?.orderStatusInfo?.code === 0;

			if (
				[
					OrderTypes.AI_HUB,
					OrderTypes.CMP,
					OrderTypes.DSR,
					OrderTypes.DXP,
				].includes(
					props?.placedOrder
						?.orderTypeExternalReferenceCode as OrderTypes
				)
			) {
				return (
					<div className="mt-6">
						{appBeta && (
							<ClayButton
								className="mr-2"
								displayType="secondary"
								onClick={() => {
									Liferay.Util.navigate(
										`${getSiteURL()}/product-feedback?orderId=${String(props?.placedOrder?.id)}`
									);
								}}
								outline
								size="regular"
							>
								{i18n.translate('share-beta-feedback')}
							</ClayButton>
						)}

						{isAiHub && (
							<ClayButton
								className="mr-2"
								displayType="primary"
								onClick={() => {
									Liferay.Util.navigate(
										`${getSiteURL()}/product-purchase?productId=${props?.product?.productId}&aiHubTokens#/`
									);
								}}
								size="regular"
							>
								{i18n.translate('buy-extra-token')}
							</ClayButton>
						)}

						{[OrderTypes.CMP, OrderTypes.DXP].includes(
							props?.placedOrder
								?.orderTypeExternalReferenceCode as OrderTypes
						) && (
							<ClayButton
								displayType="primary"
								onClick={() => {
									Liferay.Util.navigate(
										`${getSiteURL()}/product-purchase?productId=${props?.product?.productId}#/activation-key-form`
									);
								}}
								outline
								size={appBeta ? 'sm' : 'regular'}
							>
								{i18n.translate('new-activation-key')}
							</ClayButton>
						)}
					</div>
				);
			}
		}}
		backTitle={i18n.translate('back-to-my-products')}
		backURL="../../products"
		description={(props) => {
			const documentationURL =
				orderTypeDocumentationURL[
					props?.placedOrder
						?.orderTypeExternalReferenceCode as OrderTypes
				];

			return (
				<>
					{props?.product?.shortDescription}

					{documentationURL && (
						<span className="d-block mt-2">
							{i18n.translate('need-help-getting-started?')}

							<a
								className="font-weight-bold ml-1"
								href={documentationURL}
								rel="noopener noreferrer"
								target="_blank"
							>
								{i18n.translate('view-the-documentation')}
							</a>
						</span>
					)}
				</>
			);
		}}
		routes={getTabs}
		showActions={false}
	/>
);

export default LiferayProductsOutlet;
