import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BookingPage } from './booking-page';

describe('BookingPage', () => {
  let component: BookingPage;
  let fixture: ComponentFixture<BookingPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BookingPage],
    }).compileComponents();

    fixture = TestBed.createComponent(BookingPage);
    fixture.componentRef.setInput('slug', 'barbearia-teste');
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
