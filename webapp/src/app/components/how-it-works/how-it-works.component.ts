import {Component} from '@angular/core';
import {NgFor} from '@angular/common';

interface Step {
  number: number;
  title: string;
  description: string;
}

@Component({
  selector: 'app-how-it-works',
  standalone: true,
  imports: [NgFor],
  templateUrl: './how-it-works.component.html',
  styleUrl: './how-it-works.component.scss'
})
export class HowItWorksComponent {
  steps: Step[] = [
    {
      number: 1,
      title: 'Import the OpenAPI Spec',
      description: 'Generate the code aligning the most with your codebase.'
    },
    {
      number: 2,
      title: 'Create a Feature',
      description: 'Define your feature flag and set targeting rules in the dashboard.'
    },
    {number: 3, title: 'Check the Flag', description: 'Wrap your feature code with a simple conditional check.'},
    {number: 4, title: 'Ship & Iterate', description: 'Deploy once. Control everything from the dashboard.'}
  ];
}
