import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ProfissionalFormDialog } from './profissional-form-dialog';

describe('ProfissionalFormDialog', () => {
  let component: ProfissionalFormDialog;
  let fixture: ComponentFixture<ProfissionalFormDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfissionalFormDialog],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: { close: () => {} } },
        { provide: MAT_DIALOG_DATA, useValue: { profissional: null, servicos: [] } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfissionalFormDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
