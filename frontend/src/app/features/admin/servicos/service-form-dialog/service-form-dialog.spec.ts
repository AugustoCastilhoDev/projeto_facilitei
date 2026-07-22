import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ServiceFormDialog } from './service-form-dialog';

describe('ServiceFormDialog', () => {
  let component: ServiceFormDialog;
  let fixture: ComponentFixture<ServiceFormDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServiceFormDialog],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: { close: () => {} } },
        { provide: MAT_DIALOG_DATA, useValue: { servico: null } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ServiceFormDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
