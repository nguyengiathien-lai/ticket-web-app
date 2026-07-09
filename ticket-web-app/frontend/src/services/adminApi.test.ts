import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getAdminDashboardSummary } from './adminApi';
import { brokenJsonResponse, response } from '../test/testUtils';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);
const summary = {
  totalAccounts: 10,
  newRegistrationsToday: 2,
  loginTrafficToday: [{ time: '09:00', logins: 4 }]
};

describe('adminApi', () => {
  beforeEach(() => {
    fetchMock.mockReset();
    localStorage.clear();
  });

  it('requests the dashboard summary endpoint', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: summary }));
    await getAdminDashboardSummary();
    expect(fetchMock.mock.calls[0][0]).toContain('/admin/dashboard/summary');
  });
  it('uses GET implicitly', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: summary }));
    await getAdminDashboardSummary();
    expect(fetchMock.mock.calls[0][1].method).toBeUndefined();
  });
  it('returns dashboard data', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: summary }));
    await expect(getAdminDashboardSummary()).resolves.toEqual(summary);
  });
  it('preserves traffic points', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: summary }));
    expect((await getAdminDashboardSummary()).loginTrafficToday[0]).toEqual({ time: '09:00', logins: 4 });
  });
  it('sends no authorization without token', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: summary }));
    await getAdminDashboardSummary();
    expect(fetchMock.mock.calls[0][1].headers).toEqual({});
  });
  it.each([
    ['Bearer', 'abc', 'Bearer abc'],
    ['Token', 'xyz', 'Token xyz'],
    ['JWT', '123', 'JWT 123'],
    ['', 'abc', 'Bearer abc']
  ])('uses %s token type', (tokenType, token, expected) => {
    localStorage.setItem('transitpass.token', token);
    if (tokenType) localStorage.setItem('transitpass.tokenType', tokenType);
    fetchMock.mockResolvedValue(response({ success: true, data: summary }));
    return getAdminDashboardSummary().then(() => {
      expect(fetchMock.mock.calls[0][1].headers).toEqual({ Authorization: expected });
    });
  });
  it.each([400, 401, 403, 404, 500])('rejects HTTP %s', async (status) => {
    fetchMock.mockResolvedValue(response({ success: true, data: summary }, false));
    await expect(getAdminDashboardSummary()).rejects.toThrow();
    expect(status).toBeGreaterThanOrEqual(400);
  });
  it.each([
    [{ success: false, message: 'Denied' }, 'Denied'],
    [{ success: false, message: 'Unavailable' }, 'Unavailable'],
    [{ success: true }, undefined],
    [{ data: summary }, undefined]
  ])('rejects invalid payload %j', async (body, message) => {
    fetchMock.mockResolvedValue(response(body));
    await expect(getAdminDashboardSummary()).rejects.toThrow(message);
  });
  it('rejects null JSON', async () => {
    fetchMock.mockResolvedValue(response(null));
    await expect(getAdminDashboardSummary()).rejects.toThrow();
  });
  it('rejects malformed JSON', async () => {
    fetchMock.mockResolvedValue(brokenJsonResponse());
    await expect(getAdminDashboardSummary()).rejects.toThrow();
  });
  it('propagates a network error', async () => {
    fetchMock.mockRejectedValue(new TypeError('offline'));
    await expect(getAdminDashboardSummary()).rejects.toThrow('offline');
  });
  it('does not mutate returned data', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: summary }));
    expect(await getAdminDashboardSummary()).toBe(summary);
  });
});
