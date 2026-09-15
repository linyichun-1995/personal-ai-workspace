import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { useBlocker } from '@tanstack/react-router'
import { Dialog } from '@/shared/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/shared/components/ui/alert-dialog'

export function DraftDialog({
  open,
  onOpenChange,
  dirty,
  busy,
  onDiscard,
  canNavigate,
  onNavigationPermissionChange,
  children,
}: {
  open: boolean
  onOpenChange: (open: boolean, navigating?: boolean) => void
  dirty: boolean
  busy: boolean
  onDiscard: () => void
  canNavigate: () => boolean
  onNavigationPermissionChange: (allowed: boolean) => void
  children: ReactNode
}) {
  const [confirmOpen, setConfirmOpen] = useState(false)
  useEffect(() => {
    if (open) onNavigationPermissionChange(false)
  }, [open, onNavigationPermissionChange])
  const blocker = useBlocker({
    shouldBlockFn: () => open && (dirty || busy) && !canNavigate(),
    enableBeforeUnload: () => open && (dirty || busy) && !canNavigate(),
    withResolver: true,
  })
  const { reset } = blocker
  useEffect(() => {
    if (!open) reset?.()
  }, [open, reset])
  function keepEditing() {
    setConfirmOpen(false)
    blocker.reset?.()
  }
  return (
    <>
      <Dialog
        open={open}
        onOpenChange={(next) => {
          if (busy) return
          if (!next && dirty) {
            setConfirmOpen(true)
            return
          }
          onOpenChange(next)
        }}
      >
        {children}
      </Dialog>
      <AlertDialog
        open={(confirmOpen && open) || blocker.status === 'blocked'}
        onOpenChange={(value) => {
          if (!value) keepEditing()
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{busy ? '正在处理，请稍候' : '放弃未保存的修改？'}</AlertDialogTitle>
            <AlertDialogDescription>
              {busy
                ? '请等待当前操作完成，避免重复提交或丢失处理结果。'
                : '关闭或离开后，本次输入的内容不会保留。'}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={keepEditing}>
              {busy ? '留在当前页面' : '继续编辑'}
            </AlertDialogCancel>
            <AlertDialogAction
              disabled={busy}
              onClick={() => {
                onNavigationPermissionChange(true)
                onDiscard()
                setConfirmOpen(false)
                if (blocker.status === 'blocked') {
                  onOpenChange(false, true)
                  blocker.proceed()
                } else onOpenChange(false)
              }}
            >
              放弃修改
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
