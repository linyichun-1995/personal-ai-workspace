import type { RouterContext } from '@/app/router/context'

import { createRootRouteWithContext, createRoute, redirect } from '@tanstack/react-router'
import { AppLayout } from '@/app/layouts/app-layout'
import { AuthLayout } from '@/app/layouts/auth-layout'
import { RootLayout } from '@/app/layouts/root-layout'
import { RouterError, RouterNotFound, RouterPending } from '@/app/router/fallback'
import { requireAuth, requireGuest } from '@/app/router/guards'
import { AiChatPage, AiPage } from '@/features/ai'
import { ForgotPasswordPage, LoginPage, RegisterPage } from '@/features/auth'
import { FilesPage } from '@/features/file'
import { IntegrationsPage } from '@/features/integration'
import { NoteDetailPage, NotesPage } from '@/features/note'
import { ProjectDetailPage, ProjectsPage } from '@/features/project'
import { SearchPage } from '@/features/search'
import { TasksPage } from '@/features/task'
import { DashboardPage, SettingsPage } from '@/features/workspace'

export const rootRoute = createRootRouteWithContext<RouterContext>()({
  component: RootLayout,
  pendingComponent: RouterPending,
  errorComponent: RouterError,
  notFoundComponent: RouterNotFound,
})

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  beforeLoad: () => {
    throw redirect({ to: '/app/dashboard' })
  },
})

const authLayoutRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'auth-layout',
  beforeLoad: requireGuest,
  component: AuthLayout,
})

const loginRoute = createRoute({
  getParentRoute: () => authLayoutRoute,
  path: '/login',
  component: LoginPage,
})

const registerRoute = createRoute({
  getParentRoute: () => authLayoutRoute,
  path: '/register',
  component: RegisterPage,
})

const forgotPasswordRoute = createRoute({
  getParentRoute: () => authLayoutRoute,
  path: '/forgot-password',
  component: ForgotPasswordPage,
})

const appRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/app',
  beforeLoad: requireAuth,
  component: AppLayout,
})

const dashboardRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'dashboard',
  component: DashboardPage,
})

const projectsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'projects',
  component: ProjectsPage,
})

const activeProjectsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'projects/active',
  component: () => <ProjectsPage view="active" />,
})

const archivedProjectsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'projects/archived',
  component: () => <ProjectsPage view="archived" />,
})

const projectTemplatesRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'projects/templates',
  component: () => <ProjectsPage view="templates" />,
})

const projectDetailRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'projects/$projectId',
  component: ProjectDetailPage,
})

const tasksRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'tasks',
  component: TasksPage,
})

const todayTasksRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'tasks/today',
  component: () => <TasksPage activeView="today" />,
})

const upcomingTasksRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'tasks/upcoming',
  component: () => <TasksPage activeView="upcoming" />,
})

const completedTasksRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'tasks/completed',
  component: () => <TasksPage activeView="completed" />,
})

const notesRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'notes',
  component: NotesPage,
})

const noteDetailRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'notes/$noteId',
  component: NoteDetailPage,
})

const filesRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'files',
  component: FilesPage,
})

const searchRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'search',
  component: SearchPage,
})

const aiRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'ai',
  component: AiPage,
})

const aiKnowledgeRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'ai/knowledge',
  component: () => <AiPage view="knowledge" />,
})

const aiAgentsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'ai/agents',
  component: () => <AiPage view="agents" />,
})

const aiChatRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'ai/chat/$conversationId',
  component: AiChatPage,
})

const integrationsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'integrations',
  component: IntegrationsPage,
})

const settingsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: 'settings',
  component: SettingsPage,
})

export const routeTree = rootRoute.addChildren([
  indexRoute,
  authLayoutRoute.addChildren([loginRoute, registerRoute, forgotPasswordRoute]),
  appRoute.addChildren([
    dashboardRoute,
    projectsRoute,
    activeProjectsRoute,
    archivedProjectsRoute,
    projectTemplatesRoute,
    projectDetailRoute,
    tasksRoute,
    todayTasksRoute,
    upcomingTasksRoute,
    completedTasksRoute,
    notesRoute,
    noteDetailRoute,
    filesRoute,
    searchRoute,
    aiRoute,
    aiKnowledgeRoute,
    aiAgentsRoute,
    aiChatRoute,
    integrationsRoute,
    settingsRoute,
  ]),
])
