function readEnv(name: keyof ImportMetaEnv, fallback: string): string {
  const value = import.meta.env[name]
  return value && value.length > 0 ? value : fallback
}

export const env = {
  apiBaseUrl: readEnv('VITE_API_BASE_URL', '/api'),
  isDev: import.meta.env.DEV,
  isProd: import.meta.env.PROD,
} as const
