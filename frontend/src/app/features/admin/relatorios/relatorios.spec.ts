import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Relatorios } from './relatorios';

describe('Relatorios', () => {
  let component: Relatorios;
  let fixture: ComponentFixture<Relatorios>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Relatorios],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Relatorios);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create and load the report for the default period', () => {
    const req = httpMock.expectOne((r) => r.url.includes('/relatorios'));
    req.flush({
      faturamentoTotal: 100,
      totalReservasConfirmadas: 2,
      totalComparecimentosMarcados: 1,
      totalNaoComparecimentos: 0,
      taxaNaoComparecimentoPercentual: 0,
      clientesRecorrentes: [],
    });

    expect(component).toBeTruthy();
  });
});
