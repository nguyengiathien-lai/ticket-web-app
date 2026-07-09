import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  calculateDiscountedPrice, getFareDiscounts, getPassengerCards, getPassengerTickets, getPassengerTrips, getTicketPackages,
  getTicketQr, getTransitRoutes, getTransitStations, issueMonthlyPassCard,
  purchasePassTicket, purchaseSingleTripTicket
} from './passengerApi';
import { account, response, setSession } from '../test/testUtils';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);

function ok(data: unknown) {
  fetchMock.mockResolvedValue(response({ success: true, data }));
}

function postedBody() {
  return JSON.parse(fetchMock.mock.calls[0][1].body as string);
}

describe('passengerApi', () => {
  beforeEach(() => {
    fetchMock.mockReset();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('loads fare prices from the passenger endpoint', async () => {
    ok([]);
    await getTicketPackages();
    expect(fetchMock.mock.calls[0][0]).toContain('/passengers/fare/prices');
  });
  it('loads and maps fare discounts', async () => {
    ok([{ passengerType: 'STUDENT', discountType: 'PERCENTAGE', discountValue: '25' }]);
    await expect(getFareDiscounts()).resolves.toEqual([expect.objectContaining({
      passengerType: 'STUDENT', discountType: 'PERCENTAGE', discountValue: 25
    })]);
    expect(fetchMock.mock.calls[0][0]).toContain('/passengers/fare/discounts');
  });
  it('calculates active percentage and fixed discounts', () => {
    expect(calculateDiscountedPrice(200000, 'STUDENT', [{
      passengerType: 'STUDENT', discountType: 'PERCENTAGE', discountValue: 25,
      effectiveFrom: '2026-01-01', effectiveTo: '2026-12-31'
    }], '2026-07-09')).toBe(150000);
    expect(calculateDiscountedPrice(200000, 'PRIORITY', [{
      passengerType: 'PRIORITY', discountType: 'FIXED_AMOUNT', discountValue: 30000
    }], '2026-07-09')).toBe(170000);
  });
  it('does not apply an expired or unrelated discount', () => {
    const rules = [{
      passengerType: 'STUDENT', discountType: 'PERCENTAGE', discountValue: 25,
      effectiveTo: '2025-12-31'
    }];
    expect(calculateDiscountedPrice(200000, 'STUDENT', rules, '2026-07-09')).toBe(200000);
    expect(calculateDiscountedPrice(200000, 'NO', rules, '2025-07-09')).toBe(200000);
  });
  it('maps single-trip fare price', async () => {
    ok([{ mode: 'METRO', singleTrip: { baseFare: '12000' } }]);
    expect(await getTicketPackages()).toEqual([expect.objectContaining({
      id: 'METRO-single', price: 12000, type: 'single', mode: 'METRO'
    })]);
  });
  it('uses minimum price when base fare is absent', async () => {
    ok([{ mode: 'BUS', singleTrip: { minPrice: 7000 } }]);
    expect((await getTicketPackages())[0].price).toBe(7000);
  });
  it('maps monthly pass duration and price', async () => {
    ok([{ mode: 'BUS', passPrices: [{ durationType: 'MONTH', durationMonths: 2, price: '100000' }] }]);
    expect((await getTicketPackages())[0]).toEqual(expect.objectContaining({
      durationDays: 60, durationMonths: 2, price: 100000, type: 'monthly'
    }));
  });
  it('maps invalid fare numbers to zero', async () => {
    ok([{ mode: 'BUS', singleTrip: { baseFare: 'invalid' } }]);
    expect((await getTicketPackages())[0].price).toBe(0);
  });
  it('maps routes and metro mode', async () => {
    ok([{ id: 'r1', code: '01', name: 'Line 1', type: 'METRO' }]);
    expect((await getTransitRoutes())[0]).toEqual({
      id: 'r1', code: '01', name: 'Line 1', type: 'Metro', status: 'Đang hoạt động'
    });
  });
  it.each([
    ['tram', 'Tàu điện'],
    ['TRAIN', 'Tàu'],
    ['BUS', 'Xe buýt']
  ])('maps route type %s', async (type, expected) => {
    ok([{ type }]);
    expect((await getTransitRoutes())[0].type).toBe(expected);
  });
  it('falls back to route index', async () => {
    ok([{}]);
    expect((await getTransitRoutes())[0].id).toBe('0');
  });
  it('maps stations', async () => {
    ok([{ id: 's1', code: 'S1', name: 'Central', routeId: 'r1', sequence: 2 }]);
    expect((await getTransitStations())[0]).toEqual({
      id: 's1', code: 'S1', name: 'Central', routeId: 'r1', sequence: 2
    });
  });
  it('falls back from station id to code', async () => {
    ok([{ code: 'S2' }]);
    expect((await getTransitStations())[0]).toEqual(expect.objectContaining({ id: 'S2', name: 'S2' }));
  });
  it('requires an account for cards', async () => {
    await expect(getPassengerCards()).rejects.toThrow();
    expect(fetchMock).not.toHaveBeenCalled();
  });
  it('loads cards for explicit account', async () => {
    ok([{ externalCardId: 'external', cardUid: 'uid', status: 'ACTIVE' }]);
    expect((await getPassengerCards('user/1'))[0]).toEqual(expect.objectContaining({ id: 'external', cardUid: 'uid' }));
    expect(fetchMock.mock.calls[0][0]).toContain('/passengers/user/1/cards');
  });
  it('falls back through card identifiers', async () => {
    ok([{ cardId: 'card' }, { cardUid: 'uid' }, {}]);
    expect((await getPassengerCards('u')).map(x => x.id)).toEqual(['card', 'uid', '2']);
  });
  it('adds authorization to card request', async () => {
    setSession('secret', 'Token');
    ok([]);
    await getPassengerCards();
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Token secret');
  });
  it('maps tickets and alternate fields', async () => {
    ok([{ ticketId: 't', type: 'PASS', cardId: 'c', price: '55', validTo: 'tomorrow', purchasedAt: 'today' }]);
    expect((await getPassengerTickets('u'))[0]).toEqual(expect.objectContaining({
      id: 't', type: 'PASS', cardId: 'c', fare: 55, validUntil: 'tomorrow', purchasedAt: 'today'
    }));
  });
  it('falls back through ticket identifiers', async () => {
    ok([{ externalTicketId: 'ext' }, { ticketId: 'ticket' }, {}]);
    expect((await getPassengerTickets('u')).map(x => x.id)).toEqual(['ext', 'ticket', '2']);
  });
  it('loads a ticket QR with authentication', async () => {
    setSession();
    ok({ ticketId: 't1', qrCode: 'qr' });
    await expect(getTicketQr('t1')).resolves.toEqual({ ticketId: 't1', qrCode: 'qr' });
    expect(fetchMock.mock.calls[0][0]).toContain('/accounts/account-1/tickets/t1/qr');
  });
  it('maps passenger trip details', async () => {
    ok([{ externalTripId: 'trip', checkoutTime: 'bad-date', transportType: 'METRO', routeCode: '01',
      checkoutStationName: 'Central', fareAmount: '9000' }]);
    expect((await getPassengerTrips('u'))[0]).toEqual({
      id: 'trip', time: 'bad-date', vehicle: 'Metro', route: '01',
      station: 'Central', status: 'Thành công', amount: 9000
    });
  });
  it.each([
    ['train', 'Tàu'],
    ['tram', 'Tàu'],
    ['bus', 'Xe buýt']
  ])('maps trip vehicle %s', async (mode, expected) => {
    ok([{ mode }]);
    expect((await getPassengerTrips('u'))[0].vehicle).toBe(expected);
  });
  it('uses trip fallbacks', async () => {
    ok([{ checkinStationCode: 'S1' }]);
    expect((await getPassengerTrips('u'))[0]).toEqual(expect.objectContaining({
      id: '0', route: 'Chưa có', station: 'S1', amount: 0
    }));
  });
  it('posts a single trip with default payment', async () => {
    setSession();
    ok({ id: 'purchase' });
    await purchaseSingleTripTicket({ mode: 'BUS', fromStationId: 'a', toStationId: 'b' });
    expect(postedBody()).toEqual({
      userId: account.id, ticketType: 'SINGLE_TRIP', mode: 'BUS',
      fromStationId: 'a', toStationId: 'b', paymentMethod: 'VNPAY'
    });
  });
  it('honors a custom single-trip payment method', async () => {
    setSession();
    ok({});
    await purchaseSingleTripTicket({ mode: 'BUS', fromStationId: 'a', toStationId: 'b', paymentMethod: 'CASH' });
    expect(postedBody().paymentMethod).toBe('CASH');
  });
  it('removes scope and route for metro passes', async () => {
    setSession();
    ok({});
    await purchasePassTicket({
      mode: 'metro', scope: 'SINGLE_ROUTE', routeId: 'r', passengerType: 'ADULT',
      validFrom: '2026-01-01', durationType: 'MONTH'
    });
    expect(postedBody()).toEqual(expect.objectContaining({ scope: null, routeId: null }));
  });
  it('retains route for a single-route bus pass', async () => {
    setSession();
    ok({});
    await purchasePassTicket({
      mode: 'BUS', scope: 'single_route', routeId: 'r', passengerType: 'ADULT',
      validFrom: '2026-01-01', durationType: 'MONTH', durationMonths: 3
    });
    expect(postedBody()).toEqual(expect.objectContaining({ scope: 'single_route', routeId: 'r', durationMonths: 3 }));
  });
  it('omits durationMonths when absent', async () => {
    setSession();
    ok({});
    await purchasePassTicket({
      mode: 'BUS', scope: 'NETWORK', passengerType: 'ADULT',
      validFrom: '2026-01-01', durationType: 'DAY'
    });
    expect(postedBody()).not.toHaveProperty('durationMonths');
  });
  it('posts a card and ticket with the same user', async () => {
    setSession();
    ok({ card: { id: 'c' } });
    await issueMonthlyPassCard({
      mode: 'BUS', scope: 'SINGLE_ROUTE', routeId: 'r', passengerType: 'ADULT',
      validFrom: '2026-01-01', durationType: 'MONTH', supportsMetro: false, supportsBus: true
    });
    expect(postedBody()).toEqual(expect.objectContaining({
      card: expect.objectContaining({ userId: account.id, supportsBus: true }),
      ticket: expect.objectContaining({ userId: account.id, routeId: 'r' })
    }));
  });
  it('converts an empty card UID to undefined', async () => {
    setSession();
    ok({});
    await issueMonthlyPassCard({
      mode: 'METRO', passengerType: 'ADULT', validFrom: '2026-01-01',
      durationType: 'MONTH', cardUid: '', supportsMetro: true, supportsBus: false
    });
    expect(postedBody().card).not.toHaveProperty('cardUid');
  });
  it('accepts an unwrapped backend payload', async () => {
    fetchMock.mockResolvedValue(response([{ id: 'r' }]));
    expect((await getTransitRoutes())[0].id).toBe('r');
  });
  it('uses an empty array for wrapped responses without data', async () => {
    fetchMock.mockResolvedValue(response({ success: true }));
    await expect(getTransitRoutes()).resolves.toEqual([]);
  });
  it('uses backend failure messages', async () => {
    fetchMock.mockResolvedValue(response({ success: false, message: 'Fare unavailable' }));
    await expect(getTicketPackages()).rejects.toThrow('Fare unavailable');
  });
  it('rejects failed HTTP responses', async () => {
    fetchMock.mockResolvedValue(response({ success: true, data: [] }, false));
    await expect(getTransitStations()).rejects.toThrow();
  });
  it('propagates network errors', async () => {
    fetchMock.mockRejectedValue(new Error('offline'));
    await expect(getTransitRoutes()).rejects.toThrow('offline');
  });
  it('sets JSON headers and authorization on purchases', async () => {
    setSession('t');
    ok({});
    await purchaseSingleTripTicket({ mode: 'BUS', fromStationId: 'a', toStationId: 'b' });
    expect(fetchMock.mock.calls[0][1].headers).toEqual({
      'Content-Type': 'application/json', Authorization: 'Bearer t'
    });
  });
});
