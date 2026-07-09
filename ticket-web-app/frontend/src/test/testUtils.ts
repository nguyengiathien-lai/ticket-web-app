import type { AccountResponse } from '../services/authApi';
import { vi } from 'vitest';

export const account: AccountResponse = {
  id: 'account-1',
  email: 'passenger@example.com',
  fullName: 'Passenger One',
  phoneNumber: '0900000000',
  personalId: '0123456789',
  address: 'Ho Chi Minh City',
  dateOfBirth: '2000-01-01',
  gender: 'OTHER',
  isActive: true,
  isEmailVerified: true,
  mustChangePassword: false,
  roles: ['PASSENGER']
};

export function response(body: unknown, ok = true): Response {
  return {
    ok,
    json: vi.fn().mockResolvedValue(body)
  } as unknown as Response;
}

export function brokenJsonResponse(ok = true): Response {
  return {
    ok,
    json: vi.fn().mockRejectedValue(new SyntaxError('invalid JSON'))
  } as unknown as Response;
}

export function setSession(token = 'token', tokenType = 'Bearer') {
  sessionStorage.setItem('transitpass.token', token);
  sessionStorage.setItem('transitpass.tokenType', tokenType);
  sessionStorage.setItem('transitpass.account', JSON.stringify(account));
}
