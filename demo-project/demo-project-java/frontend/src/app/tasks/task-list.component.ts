import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Task, TaskService } from './task.service';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="task-page">
      <h2>My Tasks</h2>

      <div class="new-task">
        <input [(ngModel)]="newTitle" placeholder="Task title" />
        <input [(ngModel)]="newDescription" placeholder="Description" />
        <input type="date" [(ngModel)]="newDueDate" aria-label="Due date" />
        <button (click)="addTask()">Add</button>
      </div>

      <ul>
        <li *ngFor="let task of tasks" [class.completed]="task.completed" [class.overdue]="isOverdue(task)">
          <span>{{ task.title }}</span>
          <span class="due" *ngIf="task.dueDate">Due: {{ task.dueDate | date: 'mediumDate' }}</span>
          <div class="actions">
            <input
              type="date"
              [ngModel]="task.dueDate"
              (ngModelChange)="updateDueDate(task, $event)"
              aria-label="Change due date"
            />
            <button *ngIf="!task.completed" (click)="complete(task)">Complete</button>
            <button (click)="remove(task)">Delete</button>
          </div>
        </li>
      </ul>
    </div>
  `,
  styles: [
    `
      .task-page { max-width: 480px; margin: 2rem auto; }
      .new-task { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
      ul { list-style: none; padding: 0; }
      li { display: flex; justify-content: space-between; align-items: center; padding: 0.5rem; border-bottom: 1px solid #e5e7eb; }
      li.completed span { text-decoration: line-through; color: #9ca3af; }
      .actions button { margin-left: 0.25rem; }
      .due { font-size: 0.85em; color: #6b7280; }
      li.overdue .due { color: #dc2626; font-weight: 600; }
    `,
  ],
})
export class TaskListComponent implements OnInit {
  tasks: Task[] = [];
  newTitle = '';
  newDescription = '';
  newDueDate = '';

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.taskService.list().subscribe((tasks) => (this.tasks = tasks));
  }

  addTask(): void {
    if (!this.newTitle.trim()) {
      return;
    }
    this.taskService.create(this.newTitle, this.newDescription, this.newDueDate || null).subscribe(() => {
      this.newTitle = '';
      this.newDescription = '';
      this.newDueDate = '';
      this.refresh();
    });
  }

  updateDueDate(task: Task, dueDate: string | null): void {
    this.taskService
      .update(task.id, task.title, task.description, dueDate || null)
      .subscribe(() => this.refresh());
  }

  isOverdue(task: Task): boolean {
    if (task.completed || !task.dueDate) {
      return false;
    }
    // Both sides are yyyy-MM-dd, so a string compare avoids timezone shifts.
    return task.dueDate < this.today();
  }

  private today(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }

  complete(task: Task): void {
    this.taskService.complete(task.id).subscribe(() => this.refresh());
  }

  remove(task: Task): void {
    this.taskService.delete(task.id).subscribe(() => this.refresh());
  }
}
