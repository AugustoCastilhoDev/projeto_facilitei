import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Registrar } from './registrar';

describe('Registrar', () => {
  let component: Registrar;
  let fixture: ComponentFixture<Registrar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Registrar],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Registrar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deve preencher o slug automaticamente a partir do nome do negocio', () => {
    component['form'].controls.nomeNegocio.setValue('Barbearia do Ze');
    expect(component['form'].controls.slug.value).toBe('barbearia-do-ze');
  });

  it('nao deve sobrescrever o slug apos edicao manual', () => {
    component['form'].controls.slug.setValue('meu-slug-customizado');
    component['form'].controls.nomeNegocio.setValue('Outro Nome');
    expect(component['form'].controls.slug.value).toBe('meu-slug-customizado');
  });

  it('deve marcar erro quando as senhas nao coincidem', () => {
    component['form'].controls.senhaAdmin.setValue('senha12345');
    component['form'].controls.confirmarSenha.setValue('outrasenha');
    expect(component['form'].hasError('senhasDiferentes')).toBe(true);
  });
});
