import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getAdminLoginHistory } from './adminLoginHistoryApi';
import { brokenJsonResponse, response } from '../test/testUtils';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);
const items = [
  { id: 1, email: 'a@b.com', createdAt: '2026-01-01', result: 'SUCCESS' as const },
  { id: 2, user: 'B', createdAt: '2026-01-02', ipAddress: '127.0.0.1', result: 'FAILURE' as const }
];

describe('adminLoginHistoryApi', () => {
  beforeEach(() => {
    fetchMock.mockReset();
    localStorage.clear();
  });

  it('requests login history endpoint', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: items }));
    await getAdminLoginHistory();
    expect(fetchMock.mock.calls[0][0]).toContain('/admin/login-history');
  });
  it('returns login history', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: items }));
    await expect(getAdminLoginHistory()).resolves.toEqual(items);
  });
  it('returns an empty history', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: [] }));
    await expect(getAdminLoginHistory()).resolves.toEqual([]);
  });
  it('preserves success result', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: items }));
    expect((await getAdminLoginHistory())[0].result).toBe('SUCCESS');
  });
  it('preserves failure result', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: items }));
    expect((await getAdminLoginHistory())[1].result).toBe('FAILURE');
  });
  it('sends empty headers without session', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: items }));
    await getAdminLoginHistory();
    expect(fetchMock.mock.calls[0][1].headers).toEqual({});
  });
  it.each([
    ['Bearer', 'a', 'Bearer a'],
    ['Token', 'b', 'Token b'],
    ['Basic', 'c', 'Basic c'],
    [null, 'd', 'Bearer d']
  ])('builds authorization for type %s', async (type, token, expected) => {
    localStorage.setItem('transitpass.token', token);
    if (type) localStorage.setItem('transitpass.tokenType', type);
    fetchMock.mockResolvedValue(response({ success: true, data: items }));
    await getAdminLoginHistory();
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe(expected);
  });
  it.each([
    [{ success: false, message: 'Forbidden' }, 'Forbidden'],
    [{ success: false, message: 'Expired' }, 'Expired'],
    [{ success: true }, undefined],
    [{ success: true, data: null }, undefined],
    [{ data: items }, undefined]
  ])('rejects payload %j', async (body, message) => {
    fetchMock.mockResolvedValue(response(body));
    await expect(getAdminLoginHistory()).rejects.toThrow(message);
  });
  it.each([false, 0, null])('rejects non-success HTTP response marker %j', async (marker) => {
    fetchMock.mockResolvedValue(response({ success: true, data: items }, Boolean(marker)));
    await expect(getAdminLoginHistory()).rejects.toThrow();
  });
  it('rejects malformed JSON', async () => {
    fetchMock.mockResolvedValue(brokenJsonResponse());
    await expect(getAdminLoginHistory()).rejects.toThrow();
  });
  it('propagates fetch failures', async () => {
    fetchMock.mockRejectedValue(new Error('network down'));
    await expect(getAdminLoginHistory()).rejects.toThrow('network down');
  });
  it('passes a headers-only init object', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: items }));
    await getAdminLoginHistory();
    expect(Object.keys(fetchMock.mock.calls[0][1])).toEqual(['headers']);
  });
  it('returns the exact response array', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: items }));
    expect(await getAdminLoginHistory()).toBe(items);
  });
});
