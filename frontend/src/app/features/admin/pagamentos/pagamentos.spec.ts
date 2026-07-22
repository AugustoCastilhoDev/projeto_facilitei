import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Pagamentos } from './pagamentos';

describe('Pagamentos', () => {
  let component: Pagamentos;
  let fixture: ComponentFixture<Pagamentos>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Pagamentos],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Pagamentos);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    httpMock
      .expectOne((req) => req.url.includes('/asaas-config'))
      .flush({ configurado: false, webhookUrl: 'http://localhost:8080/api/webhooks/asaas', webhookToken: null });

    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
