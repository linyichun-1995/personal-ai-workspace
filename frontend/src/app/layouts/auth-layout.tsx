import { Link, useRouterState } from '@tanstack/react-router'
import { AnimatePresence, motion } from 'motion/react'
import { AuthScene } from '@/features/auth/components/auth-scene'
import { AuthMotionProvider, useAuthMotion } from '@/features/auth/motion/auth-motion-context'
import { ForgotPasswordPage } from '@/features/auth/pages/forgot-password-page'
import { LoginPage } from '@/features/auth/pages/login-page'
import { RegisterPage } from '@/features/auth/pages/register-page'
import '@/features/auth/styles/auth.css'

function AuthExperience({ pathname }: { pathname: string }) {
  const { snapshot } = useAuthMotion()
  return (
    <motion.div className="auth-shell" data-motion={snapshot.reduced ? 'reduced' : 'full'} data-auth-state={snapshot.state} initial={{ opacity: 0 }} animate={{ opacity: snapshot.exiting ? 0 : 1, scale: snapshot.exiting && !snapshot.reduced ? 0.96 : 1 }} transition={{ duration: 0.3, ease: 'easeInOut' }}>
      <a href="#auth-form" className="auth-skip-link">跳转到登录表单</a>
      <header className="auth-header">
        <Link className="auth-brand" to="/login" aria-label="AI Personal Workspace 首页">
          <svg viewBox="0 0 38 38" width="35" height="35" fill="none" aria-hidden="true">
            <defs>
              <linearGradient id="workspace-logo" x1="4" y1="5" x2="28" y2="34" gradientUnits="userSpaceOnUse">
                <stop stopColor="#deeaff" />
                <stop offset="1" stopColor="#84a2fc" />
              </linearGradient>
            </defs>
            <path d="M4 24C4 15 9 7 18 4V23C18 29 12 33 4 33V24Z" fill="url(#workspace-logo)" />
            <path d="M23 15C23 8 27 4 33 4V33H28C25 33 23 30 23 27V15Z" fill="url(#workspace-logo)" />
            <path d="M23 18H33V22H23Z" fill="#e9f1ff" />
          </svg>
          <span>AI Personal Workspace</span>
        </Link>
      </header>
      <main className="auth-main">
        <AuthScene />
        <section className="auth-panel" aria-label={pathname === '/register' ? '创建工作空间' : '进入工作空间'}>
          {/* Explicit route elements keep the outgoing form stable during crossfade. */}
          <AnimatePresence mode="wait" initial={false}>
            <motion.div key={pathname} className="auth-panel-content" initial={{ opacity: 0, y: snapshot.reduced ? 0 : 4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: snapshot.reduced ? 0 : -4 }} transition={{ duration: 0.2 }}>
              {pathname === '/register' ? <RegisterPage /> : pathname === '/forgot-password' ? <ForgotPasswordPage /> : <LoginPage />}
            </motion.div>
          </AnimatePresence>
        </section>
      </main>
    </motion.div>
  )
}

export function AuthLayout() {
  const pathname = useRouterState({ select: state => state.location.pathname })
  return <AuthMotionProvider mode={pathname === '/register' ? 'register' : 'login'}><AuthExperience pathname={pathname} /></AuthMotionProvider>
}
