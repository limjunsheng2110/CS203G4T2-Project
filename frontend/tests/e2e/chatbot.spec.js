import { test, expect } from '@playwright/test';

const MOCK_CHATBOT_RESPONSE = {
  sessionId: 'session-e2e',
  queryId: 'query-e2e',
  candidates: [
    {
      hsCode: '9603.21.00',
      confidence: 0.86,
      rationale: 'Toothbrushes, including dental plate brushes',
      source: 'REFERENCE',
      attributesUsed: ['electric', 'toothbrush'],
    },
    {
      hsCode: '9603.29.00',
      confidence: 0.62,
      rationale: 'Other toothbrushes, incl. interdental brushes',
      source: 'REFERENCE',
      attributesUsed: ['toothbrush'],
    },
  ],
  disambiguationQuestions: [],
  fallback: {
    lastUsedCodes: [
      {
        hsCode: '9603.21.00',
        confidence: 0.78,
        timestamp: new Date().toISOString(),
      },
    ],
    manualSearchUrl: 'https://hts.usitc.gov/',
  },
  notice: {
    message: 'Chat history stored to improve results.',
    privacyPolicyUrl: '/privacy',
    consentGranted: true,
  },
};

const MOCK_PRODUCT_LIST = [
  {
    hsCode: '9603.21.00',
    description: 'Toothbrushes, including dental plate brushes',
    category: 'Personal care',
  },
  {
    hsCode: '9603.29.00',
    description: 'Other toothbrushes, including interdental brushes',
    category: 'Personal care',
  },
];

test.describe('HS Code assistant E2E flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript((userData) => {
      window.localStorage.setItem('authToken', 'e2e-test-token');
      window.localStorage.setItem('user', JSON.stringify(userData));
    }, { id: 1, name: 'Playwright User', role: 'USER' });

    await page.route('**/api/products/**', async (route) => {
      const url = new URL(route.request().url());
      const hsCode = url.pathname.split('/').pop();

      if (hsCode === 'all') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(MOCK_PRODUCT_LIST),
        });
        return;
      }

      const product = MOCK_PRODUCT_LIST.find((item) => item.hsCode === hsCode);
      await route.fulfill({
        status: product ? 200 : 404,
        contentType: 'application/json',
        body: JSON.stringify(
          product || { error: 'Not found' },
        ),
      });
    });
  });

  test('suggests HS code and pre-fills detail page', async ({ page }) => {
    await page.route('**/api/hs/resolve', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_CHATBOT_RESPONSE),
      });
    });

    await page.goto('/');

    await expect(
      page.getByRole('button', { name: 'Get Started' }),
    ).toBeVisible();

    const toggleButton = page.getByRole('button', {
      name: 'Open HS Code assistant',
    });
    await expect(toggleButton).toBeVisible();
    await toggleButton.click();

    const input = page.getByLabel('Describe your product for HS code suggestions');
    await expect(input).toBeVisible();
    await input.fill('Electric toothbrush with oscillating head and rechargeable battery');

    const sendButton = page.getByRole('button', { name: 'Send message' });
    await sendButton.click();

    await expect(
      page.getByText('Here are the HS code suggestions', { exact: false }),
    ).toBeVisible();

    await expect(
      page.getByText('Suggested HS Code: 9603.21.00', { exact: false }),
    ).toBeVisible();

    const useHsCodeButton = page.getByRole('button', { name: 'Use this HS code' });
    await useHsCodeButton.click();

    await expect(
      page.getByText('HS code 9603.21.00 suggested by assistant', { exact: false }),
    ).toBeVisible();

    await expect(
      page.getByText('Toothbrushes, including dental plate brushes', { exact: false }),
    ).toBeVisible();

    await expect(page.getByText('Enter Transaction Information')).toBeVisible();
  });
});

