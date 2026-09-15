import type { LucideIcon } from 'lucide-react'
import type { ComponentProps, ReactNode } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { AnimatePresence, motion } from 'motion/react'
import { useId, useState } from 'react'
import { useAuthMotion } from '@/features/auth/motion/auth-motion-context'

interface AuthInputProps extends ComponentProps<'input'> {
  label: string
  icon: LucideIcon
  error?: string
  labelAction?: ReactNode
  hint?: string
}

export function AuthInput({ label, icon: Icon, error, labelAction, hint, type = 'text', onFocus, onBlur, onChange, ...props }: AuthInputProps) {
  const id = useId()
  const [visible, setVisible] = useState(false)
  const { controller } = useAuthMotion()
  const isPassword = type === 'password'
  return (
    <div className="auth-field">
      <div className="auth-field-label">
        <label htmlFor={id}>{label}</label>
        {labelAction}
      </div>
      <div className="auth-input-wrap" data-invalid={Boolean(error)}>
        <Icon className="auth-input-icon" size={22} strokeWidth={1.6} aria-hidden="true" />
        <input
          {...props}
          id={id}
          type={isPassword && visible ? 'text' : type}
          readOnly={controller.busy}
          aria-invalid={Boolean(error)}
          aria-describedby={`${id}-help`}
          onFocus={(event) => {
            controller.focus(isPassword ? 'password' : 'email')
            onFocus?.(event)
          }}
          onBlur={(event) => {
            controller.blur()
            onBlur?.(event)
          }}
          onChange={(event) => {
            controller.edited()
            onChange?.(event)
          }}
        />
        {isPassword && (
          <button
            className="auth-password-toggle"
            type="button"
            aria-label={`${visible ? '隐藏' : '显示'}${label}`}
            aria-pressed={visible}
            disabled={controller.busy}
            onMouseDown={event => event.preventDefault()}
            onClick={() => {
              setVisible(!visible)
              controller.focus('password')
            }}
          >
            {visible ? <EyeOff size={22} /> : <Eye size={22} />}
          </button>
        )}
      </div>
      <div className="auth-field-message" id={`${id}-help`}><AnimatePresence mode="wait">{(error || hint) && <motion.p key={error || hint} className={error ? 'auth-field-error' : 'auth-field-hint'} role={error ? 'alert' : undefined} initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.15 }}>{error || hint}</motion.p>}</AnimatePresence></div>
    </div>
  )
}
