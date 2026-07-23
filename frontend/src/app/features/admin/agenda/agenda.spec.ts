import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Slot } from '../../../core/models/slot.model';

import { Agenda } from './agenda';

function slotComReserva(overrides: Partial<Slot> = {}): Slot {
  return {
    id: 1,
    serviceId: 1,
    serviceNome: 'Corte',
    profissionalId: 1,
    profissionalNome: 'Joana',
    dataHora: '2026-07-23T09:00:00-03:00',
    status: 'CONFIRMADO',
    bookingId: 7,
    clienteNome: 'Cliente Teste',
    clienteTelefone: '+5511999998888',
    statusReserva: 'PAGO',
    compareceu: null,
    ...overrides,
  };
}

describe('Agenda', () => {
  let component: Agenda;
  let fixture: ComponentFixture<Agenda>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Agenda],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    httpMock.expectOne((req) => req.url.includes('/services')).flush([]);
    httpMock.expectOne((req) => req.url.includes('/profissionais')).flush([]);
    httpMock.expectOne((req) => req.url.includes('/slots')).flush([]);

    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('podeCancelar e podeMarcarComparecimento refletem o statusReserva do slot', () => {
    expect(component['podeCancelar'](slotComReserva({ statusReserva: 'PAGO' }))).toBe(true);
    expect(component['podeCancelar'](slotComReserva({ statusReserva: 'CANCELADO' }))).toBe(false);
    expect(component['podeMarcarComparecimento'](slotComReserva({ statusReserva: 'SEM_SINAL' }))).toBe(true);
    expect(component['podeMarcarComparecimento'](slotComReserva({ statusReserva: 'PENDENTE' }))).toBe(false);
  });

  it('marcarComparecimento chama o PATCH correto e recarrega a agenda', () => {
    component['marcarComparecimento'](slotComReserva(), true);

    const req = httpMock.expectOne((r) => r.url.includes('/bookings/7/comparecimento'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ compareceu: true });
    req.flush(null);

    httpMock.expectOne((req) => req.url.includes('/slots')).flush([]);
  });

  it('cancelarReserva pede confirmacao e chama o PATCH de cancelamento', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    component['cancelarReserva'](slotComReserva());

    const req = httpMock.expectOne((r) => r.url.includes('/bookings/7/cancelar'));
    expect(req.request.method).toBe('PATCH');
    req.flush(null);

    httpMock.expectOne((req) => req.url.includes('/slots')).flush([]);
  });

  it('cancelarReserva nao chama a API quando o usuario nao confirma', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    component['cancelarReserva'](slotComReserva());

    httpMock.expectNone((r) => r.url.includes('/bookings/7/cancelar'));
  });
});
