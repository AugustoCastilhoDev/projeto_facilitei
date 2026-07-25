import { Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-block',
  imports: [MatProgressSpinnerModule],
  templateUrl: './loading-block.html',
  styleUrl: './loading-block.scss',
})
export class LoadingBlock {
  readonly diameter = input(32);
}
