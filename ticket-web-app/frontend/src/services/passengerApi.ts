import type { TicketPackage, TransitRoute, TransitStation, TravelHistory } from '../types';
import { getStoredAccount, getStoredToken } from './authApi';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ;
// ?? 'http://localhost:8080/api';

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

export interface PassengerCard {
  id: string;
  cardUid?: string;
  maskedCardNumber?: string;
  status?: string;
  type?: string;
  issuedAt?: string;
  expiredAt?: string;
  activatedAt?: string;
  linkedAt?: string;
}

export interface PassengerTicket {
  id: string;
  type?: string;
  mode?: string;
  scope?: string;
  routeId?: string;
  cardId?: string;
  status?: string;
  fare: number;
  validFrom?: string;
  validUntil?: string;
  purchasedAt?: string;
  qrToken?: string;
}

export interface TicketQr {
  ticketId: string;
  qrCode: string;
}

export interface CardIssuanceResponse {
  card?: {
    id?: string;
    cardUid?: string;
    status?: string;
    type?: string;
    supportsMetro?: boolean;
    supportsBus?: boolean;
    linkedUserId?: string;
    activatedAt?: string;
    createdAt?: string;
  };
  ticket?: {
    ticketId?: string;
    type?: string;
    mode?: string;
    scope?: string;
    routeId?: string;
    status?: string;
    cardId?: string;
    userId?: string;
    price?: number | string;
    validFrom?: string;
    validTo?: string;
    purchasedAt?: string;
  };
}

interface FarePackageResponse {
  code?: string;
  packageId?: string;
  name?: string;
  kind?: string;
  mode?: string;
  scope?: string;
  durationType?: string;
  durationDays?: number;
  durationMonths?: number;
  price?: number | string;
  description?: string;
}

interface ExternalFarePriceResponse {
  mode?: string;
  singleTrip?: {
    baseFare?: number | string;
    minPrice?: number | string;
    maxPrice?: number | string;
  };
  passPrices?: Array<{
    durationType?: string;
    durationMonths?: number;
    scope?: string;
    price?: number | string;
  }>;
}

interface PassengerRouteResponse {
  id?: string;
  code?: string;
  name?: string;
  type?: string;
}

interface PassengerStationResponse {
  id?: string;
  routeId?: string;
  code?: string;
  name?: string;
  sequence?: number;
}

interface TravelHistoryResponse {
  externalTripId?: string;
  checkinStationCode?: string;
  checkinStationName?: string;
  checkoutStationCode?: string;
  checkoutStationName?: string;
  checkinTime?: string;
  checkoutTime?: string;
  transportType?: string;
  routeCode?: string;
  mode?: string;
  fareAmount?: number | string;
}

interface CardResponse {
  externalCardId?: string;
  cardId?: string;
  cardUid?: string;
  maskedCardNumber?: string;
  status?: string;
  type?: string;
  issuedAt?: string;
  expiredAt?: string;
  activatedAt?: string;
  linkedAt?: string;
}

interface TicketResponse {
  externalTicketId?: string;
  ticketId?: string;
  ticketTypeCode?: string;
  type?: string;
  physicalCardExternalId?: string;
  cardId?: string;
  status?: string;
  mode?: string;
  scope?: string;
  routeId?: string;
  fare?: number | string;
  price?: number | string;
  validFrom?: string;
  validUntil?: string;
  validTo?: string;
  issuedAt?: string;
  purchasedAt?: string;
  qrToken?: string;
}

export interface SingleTripPurchaseInput {
  mode: string;
  fromStationId: string;
  toStationId: string;
  paymentMethod?: string;
}

export interface PassPurchaseInput {
  mode: string;
  scope?: string;
  routeId?: string;
  passengerType: string;
  validFrom: string;
  durationType: string;
  durationMonths?: number;
  paymentMethod?: string;
}

export interface CardPurchaseInput extends PassPurchaseInput {
  cardUid?: string;
  supportsMetro: boolean;
  supportsBus: boolean;
}

export async function getTicketPackages(): Promise<TicketPackage[]> {
  const farePrices = await apiGet<ExternalFarePriceResponse[]>('/passengers/fare/prices');
  return farePrices.flatMap(mapFarePrice);
}

export async function getTransitRoutes(): Promise<TransitRoute[]> {
  const routes = await apiGet<PassengerRouteResponse[]>('/passengers/routes');
  return routes.map((route, index) => ({
    id: route.id ?? route.code ?? String(index),
    code: route.code ?? route.id ?? 'Chưa có',
    name: route.name ?? route.code ?? 'Tuyến chưa đặt tên',
    type: mapMode(route.type),
    status: 'Đang hoạt động'
  }));
}

export async function getTransitStations(): Promise<TransitStation[]> {
  const stations = await apiGet<PassengerStationResponse[]>('/passengers/stations');
  return stations.map((station, index) => ({
    id: station.id ?? station.code ?? String(index),
    code: station.code ?? station.id ?? 'Chưa có',
    name: station.name ?? station.code ?? 'Nhà ga chưa đặt tên',
    routeId: station.routeId,
    sequence: station.sequence
  }));
}

export async function getPassengerCards(accountId = getRequiredAccountId()): Promise<PassengerCard[]> {
  const cards = await apiGet<CardResponse[]>(`/passengers/${accountId}/cards`, true);
  return cards.map(mapCard);
}

export async function getPassengerTickets(accountId = getRequiredAccountId()): Promise<PassengerTicket[]> {
  const tickets = await apiGet<TicketResponse[]>(`/passengers/${accountId}/tickets`, true);
  return tickets.map(mapTicket);
}

export async function getTicketQr(ticketId: string, accountId = getRequiredAccountId()): Promise<TicketQr> {
  return apiGet<TicketQr>(`/accounts/${accountId}/tickets/${ticketId}/qr`, true);
}

export async function getPassengerTrips(accountId = getRequiredAccountId()): Promise<TravelHistory[]> {
  const trips = await apiGet<TravelHistoryResponse[]>(`/passengers/${accountId}/trips`, true);
  return trips.map((trip, index) => ({
    id: trip.externalTripId ?? String(index),
    time: formatDateTime(trip.checkoutTime ?? trip.checkinTime),
    vehicle: mapVehicle(trip.transportType ?? trip.mode),
    route: trip.routeCode ?? 'Chưa có',
    station: trip.checkoutStationName ?? trip.checkoutStationCode ?? trip.checkinStationName ?? trip.checkinStationCode ?? 'Chưa có',
    status: 'Thành công',
    amount: toNumber(trip.fareAmount)
  }));
}

export async function purchaseSingleTripTicket(input: SingleTripPurchaseInput): Promise<unknown> {
  return apiPost('/tickets/purchase', {
    userId: getRequiredAccountId(),
    ticketType: 'SINGLE_TRIP',
    mode: input.mode,
    fromStationId: input.fromStationId,
    toStationId: input.toStationId,
    paymentMethod: input.paymentMethod ?? 'VNPAY'
  });
}

export async function purchasePassTicket(input: PassPurchaseInput): Promise<unknown> {
  const isMetro = input.mode.toUpperCase() === 'METRO';
  const isSingleRoute = input.scope?.toUpperCase() === 'SINGLE_ROUTE';

  return apiPost('/tickets/purchase', {
    userId: getRequiredAccountId(),
    ticketType: 'PASS',
    mode: input.mode,
    scope: isMetro ? null : input.scope,
    routeId: isMetro ? null : (isSingleRoute ? input.routeId : null),
    passengerType: input.passengerType === 'NO' ? null : input.passengerType,
    validFrom: input.validFrom,
    durationType: input.durationType,
    ...(input.durationMonths == null ? {} : { durationMonths: input.durationMonths }),
    paymentMethod: input.paymentMethod ?? 'VNPAY'
  });
}

export async function issueMonthlyPassCard(input: CardPurchaseInput): Promise<CardIssuanceResponse> {
  const userId = getRequiredAccountId();
  const isMetro = input.mode.toUpperCase() === 'METRO';
  const isSingleRoute = input.scope?.toUpperCase() === 'SINGLE_ROUTE';

  return apiPost('/cards/purchase', {
    card: {
      cardUid: input.cardUid || undefined,
      userId,
      supportsMetro: input.supportsMetro,
      supportsBus: input.supportsBus
    },
    ticket: {
      userId,
      mode: input.mode,
      scope: isMetro ? null : input.scope,
      routeId: isMetro ? null : (isSingleRoute ? input.routeId : null),
      passengerType: input.passengerType === 'NO' ? null : input.passengerType,
      validFrom: input.validFrom,
      durationType: input.durationType,
      durationMonths: input.durationMonths
    }
  });
}

async function apiGet<T>(path: string, authenticated = false): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: authenticated ? authorizationHeader() : undefined
  });
  return readApiResponse<T>(response);
}

async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeader()
    },
    body: JSON.stringify(body)
  });
  return readApiResponse<T>(response);
}

async function readApiResponse<T>(response: Response): Promise<T> {
  const payload = await response.json().catch(() => null) as ApiResponse<T> | T | null;
  const wrapped = payload && typeof payload === 'object' && 'success' in payload ? payload as ApiResponse<T> : null;

  if (!response.ok || (wrapped && !wrapped.success)) {
    throw new Error(wrapped?.message || 'Yêu cầu đến backend thất bại.');
  }

  if (wrapped) {
    return (wrapped.data ?? []) as T;
  }

  return payload as T;
}

function authorizationHeader(): Record<string, string> {
  const token = getStoredToken();
  const tokenType = localStorage.getItem('transitpass.tokenType') || 'Bearer';
  return token ? { Authorization: `${tokenType} ${token}` } : {};
}

function getRequiredAccountId(): string {
  const accountId = getStoredAccount()?.id;
  if (!accountId) {
    throw new Error('Bạn cần đăng nhập trước khi tải dữ liệu hành khách.');
  }
  return accountId;
}

function mapFarePackage(farePackage: FarePackageResponse): TicketPackage {
  const code = farePackage.packageId ?? farePackage.code ?? farePackage.kind ?? 'package';
  const durationDays = farePackage.durationDays ?? monthsToDays(farePackage.durationMonths) ?? 1;

  return {
    id: code,
    name: farePackage.name ?? code,
    description: farePackage.description ?? describePackage(farePackage.mode, farePackage.scope, durationDays),
    price: toNumber(farePackage.price),
    durationDays,
    type: mapPackageType(farePackage.durationType, durationDays),
    mode: farePackage.mode,
    scope: farePackage.scope,
    durationType: farePackage.durationType,
    durationMonths: farePackage.durationMonths
  };
}

function mapFarePrice(farePrice: ExternalFarePriceResponse): TicketPackage[] {
  const mode = farePrice.mode ?? 'TRANSIT';
  const packages: TicketPackage[] = [];

  if (farePrice.singleTrip) {
    packages.push({
      id: `${mode}-single`,
      name: `${mapMode(mode)} vé lượt`,
      description: 'Một lượt đi giữa hai ga',
      price: toNumber(farePrice.singleTrip.baseFare ?? farePrice.singleTrip.minPrice),
      durationDays: 1,
      type: 'single',
      mode,
      durationType: 'SINGLE_TRIP'
    });
  }

  farePrice.passPrices?.forEach((pass, index) => {
    const durationDays = monthsToDays(pass.durationMonths) ?? 1;
    packages.push({
      id: `${mode}-${pass.durationType ?? 'pass'}-${pass.scope ?? 'network'}-${index}`,
      name: `${mapMode(mode)} ${formatDurationType(pass.durationType)}`,
      description: describePackage(mode, pass.scope, durationDays),
      price: toNumber(pass.price),
      durationDays,
      type: mapPassPackageType(pass.durationType, durationDays),
      mode,
      scope: pass.scope,
      durationType: pass.durationType,
      durationMonths: pass.durationMonths
    });
  });

  return packages;
}

function mapCard(card: CardResponse, index: number): PassengerCard {
  return {
    id: card.externalCardId ?? card.cardId ?? card.cardUid ?? String(index),
    cardUid: card.cardUid,
    maskedCardNumber: card.maskedCardNumber,
    status: card.status,
    type: card.type,
    issuedAt: card.issuedAt,
    expiredAt: card.expiredAt,
    activatedAt: card.activatedAt,
    linkedAt: card.linkedAt
  };
}

function mapTicket(ticket: TicketResponse, index: number): PassengerTicket {
  return {
    id: ticket.externalTicketId ?? ticket.ticketId ?? String(index),
    type: ticket.ticketTypeCode ?? ticket.type,
    mode: ticket.mode,
    scope: ticket.scope,
    routeId: ticket.routeId,
    cardId: ticket.physicalCardExternalId ?? ticket.cardId,
    status: ticket.status,
    fare: toNumber(ticket.fare ?? ticket.price),
    validFrom: ticket.validFrom,
    validUntil: ticket.validUntil ?? ticket.validTo,
    purchasedAt: ticket.issuedAt ?? ticket.purchasedAt,
    qrToken: ticket.qrToken
  };
}

function mapPackageType(durationType?: string, durationDays = 1): TicketPackage['type'] {
  const normalized = durationType?.toLowerCase() ?? '';
  if (normalized.includes('year') || durationDays >= 365) return 'yearly';
  if (normalized.includes('month') || durationDays >= 28) return 'monthly';
  if (normalized.includes('week') || durationDays >= 7) return 'weekly';
  if (normalized.includes('day') || durationDays > 1) return 'daily';
  return 'single';
}

function mapPassPackageType(durationType?: string, durationDays = 1): TicketPackage['type'] {
  const packageType = mapPackageType(durationType, durationDays);
  return packageType === 'single' ? 'daily' : packageType;
}

function mapMode(value?: string): TransitRoute['type'] {
  const normalized = value?.toLowerCase() ?? '';
  if (normalized.includes('metro')) return 'Metro';
  if (normalized.includes('tram')) return 'Tàu điện';
  if (normalized.includes('train')) return 'Tàu';
  return 'Xe buýt';
}

function mapVehicle(value?: string): TravelHistory['vehicle'] {
  const normalized = value?.toLowerCase() ?? '';
  if (normalized.includes('metro')) return 'Metro';
  if (normalized.includes('train') || normalized.includes('tram')) return 'Tàu';
  return 'Xe buýt';
}

function describePackage(mode?: string, scope?: string, durationDays?: number) {
  return [mapMode(mode), formatScope(scope), durationDays ? `${durationDays} ngày` : undefined]
    .filter(Boolean)
    .join(' - ');
}

function formatScope(scope?: string) {
  if (!scope) return undefined;
  const normalized = scope.toUpperCase();
  if (normalized === 'SINGLE_ROUTE') return 'Một tuyến';
  if (normalized === 'MULTI_ROUTE') return 'Nhiều tuyến';
  return scope.replace(/_/g, ' ').toLowerCase();
}

function formatDurationType(durationType?: string) {
  if (!durationType) return 'gói';
  const normalized = durationType.toUpperCase();
  if (normalized.includes('MONTH')) return 'vé gói';
  if (normalized.includes('DAY')) return 'vé ngày';
  if (normalized.includes('WEEK')) return 'vé tuần';
  if (normalized.includes('YEAR')) return 'vé năm';
  return 'gói';
}

function monthsToDays(months?: number) {
  return months ? months * 30 : undefined;
}

function toNumber(value?: number | string) {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function formatDateTime(value?: string) {
  if (!value) return 'Chưa có';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short'
  }).format(date);
}
