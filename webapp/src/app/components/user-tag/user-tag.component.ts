import {Component, input} from '@angular/core';

@Component({
  selector: 'app-user-tag',
  standalone: true,
  imports: [],
  templateUrl: './user-tag.component.html',
  styleUrl: './user-tag.component.scss'
})
export class UserTagComponent {
  label = input.required<string>();
  variant = input<'application' | 'role-admin' | 'role-developer' | 'role-viewer'>('application');
}
