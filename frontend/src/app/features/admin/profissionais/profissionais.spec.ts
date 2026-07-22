import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Profissionais } from './profissionais';

describe('Profissionais', () => {
  let component: Profissionais;
  let fixture: ComponentFixture<Profissionais>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Profissionais],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Profissionais);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    httpMock.expectOne((req) => req.url.includes('/services')).flush([]);
    httpMock.expectOne((req) => req.url.includes('/profissionais')).flush([]);

    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
