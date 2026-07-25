import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PortalLogin } from './portal-login';

describe('PortalLogin', () => {
  let component: PortalLogin;
  let fixture: ComponentFixture<PortalLogin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PortalLogin],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(PortalLogin);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
