import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssinaturaPage } from './assinatura';

describe('AssinaturaPage', () => {
  let component: AssinaturaPage;
  let fixture: ComponentFixture<AssinaturaPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssinaturaPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AssinaturaPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', async () => {
    httpMock.expectOne((req) => req.url.includes('/assinatura')).flush({
      plano: 'BASICO',
      status: 'TRIAL',
      trialAte: '2026-08-20',
      proximaCobrancaEm: null,
      faturaPendente: null,
    });
    await fixture.whenStable();

    expect(component).toBeTruthy();
  });

  it('deve calcular os dias restantes de trial', async () => {
    const hoje = new Date();
    const trialAte = new Date(hoje.getTime() + 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

    httpMock.expectOne((req) => req.url.includes('/assinatura')).flush({
      plano: 'BASICO',
      status: 'TRIAL',
      trialAte,
      proximaCobrancaEm: null,
      faturaPendente: null,
    });
    await fixture.whenStable();

    expect(component['diasRestantesTrial']()).toBeGreaterThanOrEqual(4);
    expect(component['diasRestantesTrial']()).toBeLessThanOrEqual(5);
  });

  it('nao deve mostrar dias de trial quando a assinatura esta ativa', async () => {
    httpMock.expectOne((req) => req.url.includes('/assinatura')).flush({
      plano: 'PROFISSIONAL',
      status: 'ATIVA',
      trialAte: null,
      proximaCobrancaEm: '2026-09-01',
      faturaPendente: null,
    });
    await fixture.whenStable();

    expect(component['diasRestantesTrial']()).toBeNull();
    expect(component['nomePlano']()).toBe('Profissional');
  });
});
