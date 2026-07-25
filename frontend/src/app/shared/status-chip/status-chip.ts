import { Component, input } from '@angular/core';

export type StatusChipTone = 'success' | 'warning' | 'danger' | 'neutral' | 'info';

@Component({
  selector: 'app-status-chip',
  imports: [],
  templateUrl: './status-chip.html',
  styleUrl: './status-chip.scss',
})
export class StatusChip {
  readonly tone = input<StatusChipTone>('neutral');
  readonly label = input.required<string>();
}
