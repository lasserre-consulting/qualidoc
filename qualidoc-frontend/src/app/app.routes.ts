import { Routes } from '@angular/router';
import { authGuard, editorGuard } from './core/guards/guards';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard.page').then(m => m.DashboardPage)
  },
  {
    path: 'documents',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/documents/pages/document-list.page').then(m => m.DocumentListPage)
      },
      {
        path: 'upload',
        canActivate: [editorGuard],
        loadComponent: () =>
          import('./features/documents/pages/document-upload.page').then(m => m.DocumentUploadPage)
      },
    ]
  },
  {
    path: 'search',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/search/pages/search.page').then(m => m.SearchPage)
  },
  {
    path: 'admin',
    canActivate: [authGuard, editorGuard],
    loadComponent: () =>
      import('./features/admin/pages/admin.page').then(m => m.AdminPage)
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./shared/components/forbidden.component').then(m => m.ForbiddenComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
