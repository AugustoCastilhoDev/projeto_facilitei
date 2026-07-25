import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { NegocioDetalhe } from './negocio-detalhe';

describe('NegocioDetalhe', () => {
  let component: NegocioDetalhe;
  let fixture: ComponentFixture<NegocioDetalhe>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NegocioDetalhe],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(NegocioDetalhe);
    fixture.componentRef.setInput('slug', 'barbearia-teste');
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();

    httpMock.expectOne('/api/public/tenants/barbearia-teste').flush({ nome: 'Barbearia Teste', slug: 'barbearia-teste' });
    httpMock.expectOne('/api/public/tenants/barbearia-teste/services').flush([]);

    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
