import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  activateAdminAccount, deactivateAdminAccount, deleteAdminAccount, getAdminAccounts
} from './adminUserApi';
import { account, brokenJsonResponse, response } from '../test/testUtils';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);

describe('adminUserApi', () => {
  beforeEach(() => {
    fetchMock.mockReset();
    localStorage.clear();
  });

  it('gets all accounts', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: [account] }));
    await expect(getAdminAccounts()).resolves.toEqual([account]);
  });
  it('uses the accounts endpoint', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: [] }));
    await getAdminAccounts();
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/accounts$/);
  });
  it('activates an account with PUT', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: account }));
    await activateAdminAccount('a/b');
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/accounts/a/b/activate'), expect.objectContaining({ method: 'PUT' }));
  });
  it('returns activated account', async () => {
    const active = { ...account, isActive: true };
    fetchMock.mockResolvedValue(response({ success: true, data: active }));
    await expect(activateAdminAccount('1')).resolves.toEqual(active);
  });
  it('deactivates an account with PUT', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: account }));
    await deactivateAdminAccount('one');
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/accounts/one/deactivate'), expect.objectContaining({ method: 'PUT' }));
  });
  it('returns deactivated account', async () => {
    const inactive = { ...account, isActive: false };
    fetchMock.mockResolvedValue(response({ success: true, data: inactive }));
    await expect(deactivateAdminAccount('1')).resolves.toEqual(inactive);
  });
  it('deletes an account with DELETE', async () => {
    fetchMock.mockResolvedValue(response({ success: true }));
    await deleteAdminAccount('gone');
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/accounts/gone'), expect.objectContaining({ method: 'DELETE' }));
  });
  it('resolves deletion without response data', async () => {
    fetchMock.mockResolvedValue(response({ success: true }));
    await expect(deleteAdminAccount('1')).resolves.toBeUndefined();
  });
  it('sends empty headers without token', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: [] }));
    await getAdminAccounts();
    expect(fetchMock.mock.calls[0][1].headers).toEqual({});
  });
  it.each([
    ['Bearer', 'abc', 'Bearer abc'],
    ['Token', 'def', 'Token def'],
    ['JWT', 'ghi', 'JWT ghi'],
    [null, 'xyz', 'Bearer xyz']
  ])('uses authorization type %s', async (type, token, expected) => {
    localStorage.setItem('transitpass.token', token);
    if (type) localStorage.setItem('transitpass.tokenType', type);
    fetchMock.mockResolvedValue(response({ success: true, data: [] }));
    await getAdminAccounts();
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe(expected);
  });
  it.each([
    [getAdminAccounts, 'List failed'],
    [() => activateAdminAccount('1'), 'Activation failed'],
    [() => deactivateAdminAccount('1'), 'Deactivation failed'],
    [() => deleteAdminAccount('1'), 'Deletion failed']
  ])('uses server error for operation %#', async (operation, message) => {
    fetchMock.mockResolvedValue(response({ success: false, message }, false));
    await expect(operation()).rejects.toThrow(message);
  });
  it('rejects successful HTTP with failure payload', async () => {
    fetchMock.mockResolvedValue(response({ success: false, message: 'Nope' }));
    await expect(getAdminAccounts()).rejects.toThrow('Nope');
  });
  it('rejects malformed JSON', async () => {
    fetchMock.mockResolvedValue(brokenJsonResponse());
    await expect(getAdminAccounts()).rejects.toThrow();
  });
  it('rejects a null payload', async () => {
    fetchMock.mockResolvedValue(response(null));
    await expect(getAdminAccounts()).rejects.toThrow();
  });
  it('propagates network errors', async () => {
    fetchMock.mockRejectedValue(new Error('offline'));
    await expect(getAdminAccounts()).rejects.toThrow('offline');
  });
  it('does not add a request body to activate', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: account }));
    await activateAdminAccount('1');
    expect(fetchMock.mock.calls[0][1].body).toBeUndefined();
  });
  it('keeps authorization on delete', async () => {
    localStorage.setItem('transitpass.token', 'admin');
    fetchMock.mockResolvedValue(response({ success: true }));
    await deleteAdminAccount('1');
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer admin');
  });
});
