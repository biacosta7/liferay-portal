const { chromium } = require('playwright');

(async () => {
  console.log("🚀 Starting Playwright UI Automation Test...");
  const browser = await chromium.launch({
    executablePath: '/usr/bin/google-chrome',
    headless: true
  });
  
  const page = await browser.newPage();
  
  // 1. Navigate to Login Page
  console.log("1. Navigating to Liferay Sign-In page...");
  await page.goto('http://localhost:8080/c/portal/login', { waitUntil: 'networkidle' });
  
  // 2. Perform Login
  console.log("2. Entering credentials and logging in...");
  await page.fill('#_com_liferay_login_web_portlet_LoginPortlet_login', 'test@liferay.com');
  await page.fill('#_com_liferay_login_web_portlet_LoginPortlet_password', 'test');
  await page.click('#_com_liferay_login_web_portlet_LoginPortlet_loginForm button[type="submit"]');
  
  // Wait for home page to load
  await page.waitForURL('**/home**', { timeout: 30000 });
  console.log("   Logged in successfully!");

  // 3. Go to Publisher Dashboard
  console.log("3. Opening Publisher Dashboard...");
  await page.goto('http://localhost:8080/web/one/my-account/publisher-dashboard/#/published-apps', { waitUntil: 'networkidle' });
  
  const customElement = page.locator('liferay-one-custom-element[route="publisher-dashboard"]');
  
  // Locate the 'Publish New App' CTA button and click it
  const publishBtn = customElement.locator('button:has-text("Publish New App")');
  await publishBtn.waitFor({ timeout: 15000 });
  console.log("   Clicking 'Publish New App'...");
  await publishBtn.click();
  
  // Step 1: License Agreement
  console.log("Step 1: Accepting License Agreement...");
  const continueBtn = customElement.locator('button:has-text("Continue")');
  await continueBtn.waitFor({ timeout: 15000 });
  await continueBtn.click();
  
  // Step 2: Profile
  console.log("Step 2: Filling Profile info...");
  const appName = `Playwright Test App ${Date.now()}`;
  await customElement.locator('input[name="name"]').fill(appName);
  await customElement.locator('textarea[name="description"]').fill('This is an automated test app published by Playwright.');
  
  // Select Category
  await customElement.locator('select[name="category"]').selectOption({ index: 1 });
  
  // Select Area (MultiSelect)
  console.log("   Selecting areas...");
  const areaInput = customElement.locator('input[role="combobox"][aria-label="Area"]');
  await areaInput.click();
  await areaInput.fill('E-Commerce');
  await page.waitForTimeout(500);
  await areaInput.press('ArrowDown');
  await areaInput.press('Enter');
  
  // Select Tags (MultiSelect)
  console.log("   Selecting tags...");
  const tagsInput = customElement.locator('input[role="combobox"][aria-label="Tags"]');
  await tagsInput.click();
  await tagsInput.fill('Business Use');
  await page.waitForTimeout(500);
  await tagsInput.press('ArrowDown');
  await tagsInput.press('Enter');
  
  // Click Continue
  await page.waitForTimeout(1000);
  await continueBtn.click();
  
  // Step 3: Build
  console.log("Step 3: Configuring Build details...");
  const selectPlatformBtn = customElement.locator('button:has-text("Choose an Option")');
  await selectPlatformBtn.waitFor({ timeout: 15000 });
  await selectPlatformBtn.click();
  
  // Click 'DXP App' option in dropdown
  await page.click('button[role="menuitem"]:has-text("DXP App")');
  
  // Select Liferay Version
  await customElement.locator('button:has-text("Select Compatible Versions")').click();
  await page.locator('button[role="menuitem"]:visible').first().click();
  await customElement.locator('button:has-text("Apply")').click();
  
  // Click Continue
  await continueBtn.click();
  
  // Step 4: Storefront
  console.log("Step 4: Storefront...");
  await continueBtn.waitFor({ timeout: 15000 });
  await continueBtn.click();
  
  // Step 5: Version
  console.log("Step 5: Version...");
  await customElement.locator('input[name="version"]').waitFor({ timeout: 15000 });
  await customElement.locator('input[name="version"]').fill('1.0.0');
  await customElement.locator('textarea[name="notes"]').fill('Initial automated release.');
  await continueBtn.click();
  
  // Step 6: Pricing (Free)...
  console.log("Step 6: Pricing (Free)...");
  await continueBtn.waitFor({ timeout: 15000 });
  await continueBtn.click();
  
  // Step 7: Licensing...
  console.log("Step 7: Licensing...");
  await continueBtn.waitFor({ timeout: 15000 });
  await continueBtn.click();
  
  // Step 8: Support...
  console.log("Step 8: Support...");
  await customElement.locator('input[name="url"]').waitFor({ timeout: 15000 });
  await customElement.locator('input[name="url"]').fill('https://www.liferay.com');
  await customElement.locator('input[name="email"]').fill('automated-test@liferay.com');
  await continueBtn.click();
  
  // Step 9: Submit / Review Page
  console.log("Step 9: Review and Submit...");
  const submitBtn = customElement.locator('button:has-text("Submit")');
  await submitBtn.waitFor({ timeout: 15000 });
  
  // Accept user agreement checkbox
  const checkbox = await customElement.locator('input[type="checkbox"]');
  await checkbox.check();
  
  // Click Submit and wait for redirect
  console.log("   Clicking Submit...");
  await submitBtn.click();
  
  // Wait for redirect back to the published apps list
  console.log("   Waiting for redirection back to Published Apps list...");
  await page.waitForURL('**/published-apps', { timeout: 45000 });
  
  // Let the list fully load
  console.log("   Waiting for the published apps list to reload...");
  await page.waitForTimeout(6000);
  
  // Extract and print the status column text for our new app
  const rowLocator = customElement.locator(`tr:has-text("${appName}")`);
  await page.waitForTimeout(1000);
  const statusText = await rowLocator.locator('.badge, td').nth(1).textContent();
  console.log(`\n🎉 SUBMITTED APP "${appName}" STATUS IN DASHBOARD UI:`, statusText.trim());
  
  await browser.close();
})();
