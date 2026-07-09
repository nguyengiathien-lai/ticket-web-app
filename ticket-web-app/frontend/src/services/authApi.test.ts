import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  clearSession, getStoredAccount, getStoredToken, isAdmin, isProfileComplete,
  isSessionValid, login, nextRouteFor, registerAccount, resendEmailOtp, storeSession,
  updateProfile, updateStoredAccount, verifyEmailOtp, type AccountResponse
} from './authApi';
import { account, brokenJsonResponse, response, setSession } from '../test/testUtils';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);

describe('authApi', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    fetchMock.mockReset();
  });

  it('posts login credentials as JSON', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: { token: 't' } }));
    await login('a@b.com', 'secret');
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/auth/login'), expect.objectContaining({
      method: 'POST', body: JSON.stringify({ email: 'a@b.com', password: 'secret' })
    }));
  });
  it('returns login data', async () => {
    const data = { token: 't', tokenType: 'Bearer', expiresAt: 99, mustChangePassword: false, account };
    fetchMock.mockResolvedValue(response({ success: true, data }));
    await expect(login('a', 'b')).resolves.toEqual(data);
  });
  it('uses the server login error', async () => {
    fetchMock.mockResolvedValue(response({ success: false, message: 'Locked' }, false));
    await expect(login('a', 'b')).rejects.toThrow('Locked');
  });
  it('rejects a successful HTTP response with unsuccessful payload', async () => {
    fetchMock.mockResolvedValue(response({ success: false, message: 'No' }));
    await expect(login('a', 'b')).rejects.toThrow('No');
  });
  it('rejects login when data is missing', async () => {
    fetchMock.mockResolvedValue(response({ success: true }));
    await expect(login('a', 'b')).rejects.toThrow();
  });
  it('rejects login when JSON is invalid', async () => {
    fetchMock.mockResolvedValue(brokenJsonResponse());
    await expect(login('a', 'b')).rejects.toThrow();
  });
  it('posts registration data', async () => {
    const request = { email: 'a@b.com', password: 'x', firstName: 'A', lastName: 'B' };
    fetchMock.mockResolvedValue(response({ success: true, data: account }));
    await registerAccount(request);
    expect(fetchMock.mock.calls[0][1].body).toBe(JSON.stringify(request));
  });
  it('returns a registered account', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: account }));
    await expect(registerAccount({ email: '', password: '', firstName: '', lastName: '' })).resolves.toEqual(account);
  });
  it('rejects failed registration', async () => {
    fetchMock.mockResolvedValue(response({ success: false, message: 'Duplicate' }, false));
    await expect(registerAccount({ email: '', password: '', firstName: '', lastName: '' })).rejects.toThrow('Duplicate');
  });
  it('posts email and OTP for verification', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: account }));
    await verifyEmailOtp('a@b.com', '123456');
    expect(fetchMock.mock.calls[0][1].body).toBe(JSON.stringify({ email: 'a@b.com', code: '123456' }));
  });
  it('rejects verification without account data', async () => {
    fetchMock.mockResolvedValue(response({ success: true }));
    await expect(verifyEmailOtp('a', '1')).rejects.toThrow();
  });
  it('posts email when resending OTP', async () => {
    fetchMock.mockResolvedValue(response({ success: true }));
    await resendEmailOtp('a@b.com');
    expect(fetchMock.mock.calls[0][1].body).toBe(JSON.stringify({ email: 'a@b.com' }));
  });
  it('rejects a failed OTP resend', async () => {
    fetchMock.mockResolvedValue(response({ success: false, message: 'Wait' }));
    await expect(resendEmailOtp('a')).rejects.toThrow('Wait');
  });
  it('updates a profile with authorization', async () => {
    setSession('abc', 'Token');
    fetchMock.mockResolvedValue(response({ success: true, data: account }));
    await updateProfile('id/1', { fullName: 'New' });
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/accounts/id/1/profile'), expect.objectContaining({
      method: 'PUT', headers: expect.objectContaining({ Authorization: 'Token abc' })
    }));
  });
  it('stores the updated profile', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: account }));
    await updateProfile('id', {});
    expect(getStoredAccount()).toEqual(account);
  });
  it('storeSession persists every session field', () => {
    storeSession({ token: 't', tokenType: 'JWT', expiresAt: 42, mustChangePassword: false, account });
    expect([...Array.from({ length: 4 }, (_, i) => sessionStorage.key(i))]).toEqual(expect.arrayContaining([
      'transitpass.token', 'transitpass.tokenType', 'transitpass.expiresAt', 'transitpass.account'
    ]));
  });
  it('updateStoredAccount replaces the account', () => {
    updateStoredAccount({ ...account, fullName: 'Changed' });
    expect(getStoredAccount()?.fullName).toBe('Changed');
  });
  it('returns null when no account is stored', () => expect(getStoredAccount()).toBeNull());
  it('clears the session when stored account JSON is malformed', () => {
    setSession();
    sessionStorage.setItem('transitpass.account', '{');
    expect(getStoredAccount()).toBeNull();
    expect(getStoredToken()).toBeNull();
  });
  it('returns the stored token', () => {
    sessionStorage.setItem('transitpass.token', 'xyz');
    expect(getStoredToken()).toBe('xyz');
  });
  it('accepts a non-expired session', () => {
    sessionStorage.setItem('transitpass.token', 'x');
    sessionStorage.setItem('transitpass.expiresAt', String(Math.floor(Date.now() / 1000) + 60));
    expect(isSessionValid()).toBe(true);
  });
  it.each([
    ['missing token', null, '9999999999'],
    ['expired token', 'x', '1'],
    ['invalid expiry', 'x', 'abc']
  ])('rejects session with %s', (_name, token, expiry) => {
    if (token) sessionStorage.setItem('transitpass.token', token);
    sessionStorage.setItem('transitpass.expiresAt', expiry);
    expect(isSessionValid()).toBe(false);
  });
  it('clearSession removes session values', () => {
    setSession();
    sessionStorage.setItem('transitpass.expiresAt', '2');
    clearSession();
    expect(sessionStorage.length).toBe(0);
  });
  it.each([
    [['APP_ADMIN'], true],
    [['PASSENGER'], false],
    [[], false]
  ])('detects admin role %j as %s', (roles, expected) => {
    expect(isAdmin({ ...account, roles })).toBe(expected);
  });
  it('considers null profile complete', () => expect(isProfileComplete(null)).toBe(true));
  it('considers an admin profile complete', () => {
    expect(isProfileComplete({ ...account, roles: ['APP_ADMIN'], fullName: undefined })).toBe(true);
  });
  it('requires every passenger profile field', () => {
    expect(isProfileComplete({ ...account, address: '   ' })).toBe(false);
  });
  it('accepts a complete passenger profile', () => expect(isProfileComplete(account)).toBe(true));
  it.each([
    [['APP_ADMIN'], '/admin'],
    [['PASSENGER'], '/app']
  ])('chooses route for roles %j', (roles, route) => {
    expect(nextRouteFor({ ...account, roles })).toBe(route);
  });
  it('routes incomplete passengers to profile', () => {
    expect(nextRouteFor({ ...account, phoneNumber: undefined })).toBe('/app/profile');
  });
});
