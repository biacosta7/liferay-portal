/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig, splitVendorChunkPlugin} from 'vite';

export default defineConfig({
	build: {
		outDir: 'build/vite',
		rollupOptions: {
			external: ['@liferay/oauth2-provider-web/client'],
			output: {
				assetFileNames: 'assets/[name][extname]',
				chunkFileNames: '[name]-[hash].js',
				entryFileNames: 'main.js',
			},
		},
	},
	experimental: {
		renderBuiltUrl(filename: string) {
			return `/o/liferay-one-custom-element/${filename}`;
		},
	},
	optimizeDeps: {
		esbuildOptions: {
			define: {
				global: 'globalThis',
			},
		},
		exclude: ['@liferay/oauth2-provider-web/client'],
	},
	plugins: [react(), splitVendorChunkPlugin()],
	resolve: {
		alias: {
			'~': path.resolve(__dirname, './src/'),
		},
	},
	server: {
		origin: 'http://localhost:5173',
	},
});
