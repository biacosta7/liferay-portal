const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
  console.log("🚀 Launching Playwright UI Automation...");
  
  // Launch the pre-installed system Google Chrome
  const browser = await chromium.launch({
    executablePath: '/usr/bin/google-chrome',
    headless: true
  });
  
  const page = await browser.newPage();
  
  try {
    // 1. Log in to Liferay
    console.log("1. Logging in as test@liferay.com...");
    await page.goto('http://localhost:8080/c/portal/login', { waitUntil: 'networkidle' });
    await page.fill('#_com_liferay_login_web_portlet_LoginPortlet_login', 'test@liferay.com');
    await page.fill('#_com_liferay_login_web_portlet_LoginPortlet_password', 'test');
    await page.click('#_com_liferay_login_web_portlet_LoginPortlet_loginForm button[type="submit"]');
    await page.waitForURL('**/home**', { timeout: 30000 });
    console.log("   Logged in successfully!");

    // 2. Navigate to Publisher Dashboard
    console.log("2. Opening Publisher Dashboard...");
    await page.goto('http://localhost:8080/web/one/my-account/publisher-dashboard/#/published-apps', { waitUntil: 'networkidle' });
    
    // Pierce Shadow DOM to locate and click 'Publish New App'
    const customElement = page.locator('liferay-one-custom-element[route="publisher-dashboard"]');
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
    console.log("   Selecting Area category...");
    const areaInput = customElement.locator('input[role="combobox"][aria-label="Area"]');
    await areaInput.click();
    await areaInput.fill('E-Commerce');
    await page.waitForTimeout(500);
    await areaInput.press('ArrowDown');
    await areaInput.press('Enter');
    
    // Select Tags (MultiSelect)
    console.log("   Selecting Tags category...");
    const tagsInput = customElement.locator('input[role="combobox"][aria-label="Tags"]');
    await tagsInput.click();
    await tagsInput.fill('Business Use');
    await page.waitForTimeout(500);
    await tagsInput.press('ArrowDown');
    await tagsInput.press('Enter');
    
    // Click Continue
    await continueBtn.click();
    
    // Step 3: Build
    console.log("Step 3: Configuring Build details...");
    const selectPlatformBtn = customElement.locator('button:has-text("Choose an Option")');
    await selectPlatformBtn.waitFor({ timeout: 15000 });
    await selectPlatformBtn.click();
    await page.waitForTimeout(500); // Wait for dropdown animation
    
    // Click 'DXP App' from the dropdown menu using precise role and visibility filter
    await page.locator('button[role="menuitem"]:visible:has-text("DXP App")').click();
    await page.waitForTimeout(1000); // Wait for React state to render "Add Packages"
    
    // Open the Add Packages Modal
    console.log("   Adding a Liferay package archive...");
    const addPackagesBtn = customElement.locator('button:has-text("Add Packages")');
    await addPackagesBtn.waitFor({ timeout: 15000 });
    await addPackagesBtn.click();
    
    // Create a dummy package file to upload
    fs.writeFileSync('dummy-package.lpkg', 'Dummy Liferay Package Content');
    
    // Upload the file inside the modal
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('dummy-package.lpkg');
    
    // Select Liferay Version inside the modal
    await page.click('text="Select Compatible Versions"');
    await page.locator('button[role="menuitem"]:visible:first-child').click();
    await page.click('text="Apply"');
    
    // Click Save inside the modal
    await page.click('button:has-text("Save")');
    
    // Wait for the modal to close and continue
    await page.waitForTimeout(1000);
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
    
    // Step 6: Pricing
    console.log("Step 6: Pricing (Free)...");
    await continueBtn.waitFor({ timeout: 15000 });
    await continueBtn.click();
    
    // Step 7: Licensing
    console.log("Step 7: Licensing...");
    await continueBtn.waitFor({ timeout: 15000 });
    await continueBtn.click();
    
    // Step 8: Support
    console.log("Step 8: Support...");
    await customElement.locator('input[name="url"]').waitFor({ timeout: 15000 });
    await customElement.locator('input[name="url"]').fill('https://www.liferay.com');
    await customElement.locator('input[name="email"]').fill('automated-test@liferay.com');
    await continueBtn.click();
    
    // Step 9: Review and Submit
    console.log("Step 9: Review and Submit...");
    const submitBtn = customElement.locator('button:has-text("Submit")');
    await submitBtn.waitFor({ timeout: 15000 });
    
    // Check user agreement checkbox
    const checkbox = await customElement.locator('input[type="checkbox"]');
    await checkbox.check();
    
    // Click Submit and wait for redirect back to Dashboard
    console.log("   Submitting the app!");
    await submitBtn.click();
    
    console.log("   Waiting for redirection back to Published Apps list...");
    await page.waitForURL('**/published-apps', { timeout: 60000 });
    
    // Allow list to fully reload and sync
    console.log("   Waiting for list to reload...");
    await page.waitForTimeout(6000);
    
    // Take a full-page screenshot of the dashboard list to capture the status
    await page.screenshot({ path: 'dashboard_screenshot.png', fullPage: true });
    console.log("   Screenshot saved to: dashboard_screenshot.png");
    
    // Extract and print the status column text for our new app
    const rowLocator = customElement.locator(`tr:has-text("${appName}")`);
    const statusText = await rowLocator.locator('.badge, td').nth(1).textContent();
    console.log(`\n🎉 SUBMITTED APP "${appName}" STATUS IN THE UI:`, statusText.trim().toUpperCase());
    
  } catch (error) {
    console.error("❌ E2E TEST FAILED:", error);
    await page.screenshot({ path: 'error_screenshot.png' });
    console.log("   Error screenshot saved to error_screenshot.png");
  } finally {
    // Clean up local temp file
    if (fs.existsSync('dummy-package.lpkg')) {
      fs.unlinkSync('dummy-package.lpkg');
    }
    await browser.close();
    console.log("Test execution closed.");
  }
})();
