import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Task, TaskService } from './task.service';

describe('TaskService', () => {
  const baseUrl = 'http://localhost:8080/api/tasks';
  let service: TaskService;
  let httpMock: HttpTestingController;

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

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TaskService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('create', () => {
    it('POSTs title, description and dueDate', () => {
      const created = task({ dueDate: '2026-12-01' });
      let result: Task | undefined;

      service.create('Title', 'Desc', '2026-12-01').subscribe((t) => (result = t));

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ title: 'Title', description: 'Desc', dueDate: '2026-12-01' });
      req.flush(created);
      expect(result).toEqual(created);
    });

    it('sends dueDate as null when none is given', () => {
      service.create('Title', 'Desc').subscribe();

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.body).toEqual({ title: 'Title', description: 'Desc', dueDate: null });
      req.flush(task());
    });

    it('sends an explicit null dueDate as null', () => {
      service.create('Title', 'Desc', null).subscribe();

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.body.dueDate).toBeNull();
      req.flush(task());
    });
  });

  describe('update', () => {
    it('PUTs title, description and dueDate to the task URL', () => {
      const updated = task({ title: 'New', dueDate: '2027-01-15' });
      let result: Task | undefined;

      service.update(5, 'New', 'Desc', '2027-01-15').subscribe((t) => (result = t));

      const req = httpMock.expectOne(`${baseUrl}/5`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ title: 'New', description: 'Desc', dueDate: '2027-01-15' });
      req.flush(updated);
      expect(result).toEqual(updated);
    });

    it('sends dueDate as null so the backend can clear it', () => {
      service.update(5, 'Title', 'Desc', null).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/5`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ title: 'Title', description: 'Desc', dueDate: null });
      req.flush(task());
    });

    it('propagates a 400 error from the backend', () => {
      let status: number | undefined;

      service.update(5, 'Title', 'Desc', 'bad').subscribe({ error: (e) => (status = e.status) });

      httpMock
        .expectOne(`${baseUrl}/5`)
        .flush({ error: 'Malformed request body' }, { status: 400, statusText: 'Bad Request' });
      expect(status).toBe(400);
    });
  });

  describe('existing operations', () => {
    it('list GETs all tasks, including their dueDate', () => {
      const tasks = [task({ dueDate: '2026-12-01' }), task({ id: 2, dueDate: null })];
      let result: Task[] | undefined;

      service.list().subscribe((t) => (result = t));

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(tasks);
      expect(result).toEqual(tasks);
    });

    it('complete PATCHes the complete endpoint', () => {
      service.complete(3).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/3/complete`);
      expect(req.request.method).toBe('PATCH');
      req.flush(task({ completed: true }));
    });

    it('delete DELETEs the task URL', () => {
      service.delete(3).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/3`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
