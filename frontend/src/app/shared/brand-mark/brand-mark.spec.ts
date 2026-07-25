import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BrandMark } from './brand-mark';

describe('BrandMark', () => {
  let component: BrandMark;
  let fixture: ComponentFixture<BrandMark>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BrandMark],
    }).compileComponents();

    fixture = TestBed.createComponent(BrandMark);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
