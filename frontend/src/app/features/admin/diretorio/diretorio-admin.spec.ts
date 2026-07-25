import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DiretorioAdmin } from './diretorio-admin';

describe('DiretorioAdmin', () => {
  let component: DiretorioAdmin;
  let fixture: ComponentFixture<DiretorioAdmin>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DiretorioAdmin],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(DiretorioAdmin);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    httpMock
      .expectOne((req) => req.url.includes('/diretorio'))
      .flush({ categoria: null, cidade: null, descricao: null, visivelNoDiretorio: false });

    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
