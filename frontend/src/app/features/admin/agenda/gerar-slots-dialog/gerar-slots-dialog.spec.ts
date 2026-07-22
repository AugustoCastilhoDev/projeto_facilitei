import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { GerarSlotsDialog } from './gerar-slots-dialog';

describe('GerarSlotsDialog', () => {
  let component: GerarSlotsDialog;
  let fixture: ComponentFixture<GerarSlotsDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GerarSlotsDialog],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: { close: () => {} } },
        { provide: MAT_DIALOG_DATA, useValue: { servicos: [], dataSelecionada: '2026-07-21' } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GerarSlotsDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
