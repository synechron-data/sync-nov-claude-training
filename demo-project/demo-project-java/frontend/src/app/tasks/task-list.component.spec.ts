import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TaskListComponent } from './task-list.component';
import { Task, TaskService } from './task.service';

describe('TaskListComponent', () => {
  let fixture: ComponentFixture<TaskListComponent>;
  let component: TaskListComponent;
  let taskService: jasmine.SpyObj<TaskService>;

  const task = (overrides: Partial<Task> = {}): Task => ({
    id: 1,
    title: 'Title',
    description: 'Desc',
    completed: false,
    dueDate: null,
    ownerId: 1,
    createdAt: '2026-11-17T00:00:00Z',
    priority: 'MEDIUM',
    ...overrides,
  });

  /** Local yyyy-MM-dd for today plus an offset in days (matches the component's own definition of "today"). */
  const dateOffset = (days: number): string => {
    const d = new Date();
    d.setDate(d.getDate() + days);
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${month}-${day}`;
  };

  beforeEach(() => {
    taskService = jasmine.createSpyObj<TaskService>('TaskService', [
      'list',
      'create',
      'update',
      'complete',
      'delete',
    ]);
    taskService.list.and.returnValue(of([]));
    taskService.create.and.returnValue(of(task()));
    taskService.update.and.returnValue(of(task()));

    TestBed.configureTestingModule({
      imports: [TaskListComponent],
      providers: [{ provide: TaskService, useValue: taskService }],
    });
    fixture = TestBed.createComponent(TaskListComponent);
    component = fixture.componentInstance;
  });

  describe('isOverdue', () => {
    it('is true for an incomplete task due in the past', () => {
      expect(component.isOverdue(task({ dueDate: dateOffset(-1) }))).toBeTrue();
    });

    it('is false for a task due today (boundary)', () => {
      expect(component.isOverdue(task({ dueDate: dateOffset(0) }))).toBeFalse();
    });

    it('is false for a task due in the future', () => {
      expect(component.isOverdue(task({ dueDate: dateOffset(1) }))).toBeFalse();
    });

    it('is false when there is no due date', () => {
      expect(component.isOverdue(task({ dueDate: null }))).toBeFalse();
    });

    it('is false for a completed task even if its due date has passed', () => {
      expect(component.isOverdue(task({ dueDate: dateOffset(-30), completed: true }))).toBeFalse();
    });
  });

  describe('addTask', () => {
    it('passes null as the due date when none was chosen', () => {
      component.newTitle = 'Write docs';
      component.newDescription = 'Desc';

      component.addTask();

      expect(taskService.create).toHaveBeenCalledOnceWith('Write docs', 'Desc', null);
    });

    it('passes the chosen due date to the service', () => {
      component.newTitle = 'Write docs';
      component.newDescription = 'Desc';
      component.newDueDate = '2026-12-01';

      component.addTask();

      expect(taskService.create).toHaveBeenCalledOnceWith('Write docs', 'Desc', '2026-12-01');
    });

    it('accepts a past due date without complaint', () => {
      component.newTitle = 'Backdated';
      component.newDueDate = '2000-01-01';

      component.addTask();

      expect(taskService.create).toHaveBeenCalledOnceWith('Backdated', '', '2000-01-01');
    });

    it('resets the form fields, including the due date, and refreshes', () => {
      component.newTitle = 'Write docs';
      component.newDescription = 'Desc';
      component.newDueDate = '2026-12-01';

      component.addTask();

      expect(component.newTitle).toBe('');
      expect(component.newDescription).toBe('');
      expect(component.newDueDate).toBe('');
      expect(taskService.list).toHaveBeenCalledTimes(1);
    });

    it('does not call the service for a blank title, even with a due date', () => {
      component.newTitle = '   ';
      component.newDueDate = '2026-12-01';

      component.addTask();

      expect(taskService.create).not.toHaveBeenCalled();
      expect(component.newDueDate).toBe('2026-12-01');
    });
  });

  describe('updateDueDate', () => {
    it('sends the existing title and description with the new due date', () => {
      const t = task({ id: 7, title: 'Keep me', description: 'Keep desc' });

      component.updateDueDate(t, '2026-12-25');

      expect(taskService.update).toHaveBeenCalledOnceWith(7, 'Keep me', 'Keep desc', '2026-12-25');
    });

    it('converts an empty string to null so the due date is cleared', () => {
      component.updateDueDate(task({ id: 7, dueDate: '2026-12-01' }), '');

      expect(taskService.update).toHaveBeenCalledOnceWith(7, 'Title', 'Desc', null);
    });

    it('passes null through as null', () => {
      component.updateDueDate(task({ id: 7 }), null);

      expect(taskService.update).toHaveBeenCalledOnceWith(7, 'Title', 'Desc', null);
    });

    it('refreshes the list after a successful update', () => {
      component.updateDueDate(task(), '2026-12-25');

      expect(taskService.list).toHaveBeenCalledTimes(1);
    });
  });

  describe('template', () => {
    const render = (tasks: Task[]): HTMLElement => {
      taskService.list.and.returnValue(of(tasks));
      fixture.detectChanges();
      return fixture.nativeElement as HTMLElement;
    };

    it('shows the formatted due date when one is set', () => {
      const el = render([task({ dueDate: '2026-12-01' })]);

      const due = el.querySelector('li .due');
      expect(due?.textContent).toContain('Due:');
      expect(due?.textContent).toContain('Dec 1, 2026');
    });

    it('shows no due text when the task has no due date', () => {
      const el = render([task({ dueDate: null })]);

      expect(el.querySelector('li .due')).toBeNull();
    });

    it('marks an overdue incomplete task with the overdue class', () => {
      const el = render([task({ dueDate: dateOffset(-2) })]);

      expect(el.querySelector('li')?.classList).toContain('overdue');
    });

    it('does not mark a future task or a completed task as overdue', () => {
      const el = render([
        task({ id: 1, dueDate: dateOffset(3) }),
        task({ id: 2, dueDate: dateOffset(-3), completed: true }),
      ]);

      const items = el.querySelectorAll('li');
      expect(items.length).toBe(2);
      expect(items[0].classList).not.toContain('overdue');
      expect(items[1].classList).not.toContain('overdue');
    });

    it('renders a date input in the new-task row and one per task', () => {
      const el = render([task({ id: 1 }), task({ id: 2 })]);

      expect(el.querySelectorAll('.new-task input[type="date"]').length).toBe(1);
      expect(el.querySelectorAll('li input[type="date"]').length).toBe(2);
    });
  });
});
