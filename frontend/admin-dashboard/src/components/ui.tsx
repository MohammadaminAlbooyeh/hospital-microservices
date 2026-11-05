import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react'
import { Link } from 'react-router-dom'

export type Tone = 'neutral' | 'success' | 'warning' | 'danger' | 'info'

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: Tone }) {
  return <span className={`badge badge-${tone}`}>{children}</span>
}

export function Card({
  title,
  subtitle,
  actions,
  children,
  className = '',
}: {
  title?: ReactNode
  subtitle?: ReactNode
  actions?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={`card ${className}`.trim()}>
      {(title || actions) && (
        <header className="card-head">
          <div>
            {title && <h2>{title}</h2>}
            {subtitle && <p className="muted small">{subtitle}</p>}
          </div>
          {actions && <div className="card-actions">{actions}</div>}
        </header>
      )}
      <div className="card-body">{children}</div>
    </section>
  )
}

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  loading?: boolean
}

export function Button({ variant = 'primary', loading, children, className = '', disabled, ...rest }: ButtonProps) {
  return (
    <button
      className={`btn btn-${variant} ${className}`.trim()}
      disabled={disabled || loading}
      {...rest}
    >
      {loading && <span className="spinner spinner-small" aria-hidden="true" />}
      {children}
    </button>
  )
}

export function LinkButton({
  to,
  children,
  variant = 'secondary',
}: {
  to: string
  children: ReactNode
  variant?: ButtonVariant
}) {
  return (
    <Link className={`btn btn-${variant}`} to={to}>
      {children}
    </Link>
  )
}

export function Field({
  label,
  htmlFor,
  required,
  error,
  hint,
  children,
}: {
  label: string
  htmlFor: string
  required?: boolean
  error?: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div className="field">
      <label htmlFor={htmlFor}>
        {label}
        {required && <span aria-hidden="true"> *</span>}
      </label>
      {children}
      {hint && <small className="muted">{hint}</small>}
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

export function Input({ className = '', ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={`input ${className}`.trim()} {...rest} />
}

export function Select({ className = '', children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={`input ${className}`.trim()} {...rest}>
      {children}
    </select>
  )
}

export function Textarea({ className = '', ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={`input textarea ${className}`.trim()} {...rest} />
}

export interface Column<T> {
  header: string
  cell: (row: T) => ReactNode
  className?: string
}

export function Table<T>({
  columns,
  rows,
  rowKey,
  loading,
  error,
  empty = 'No records found.',
  onRetry,
}: {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  loading?: boolean
  error?: string | null
  empty?: string
  onRetry?: () => void
}) {
  if (loading) return <Loading label="Loading records…" />
  if (error) return <ErrorState message={error} onRetry={onRetry} />
  if (!rows.length) return <EmptyState message={empty} />
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column, index) => (
              <th key={`${column.header}-${index}`} scope="col" className={column.className}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((column, index) => (
                <td key={`${column.header}-${index}`} className={column.className}>
                  {column.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="state" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <p>{label}</p>
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="state state-error" role="alert">
      <p>{message}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}

export function EmptyState({ message, action }: { message: string; action?: ReactNode }) {
  return (
    <div className="state state-empty">
      <span className="empty-mark" aria-hidden="true" />
      <p>{message}</p>
      {action}
    </div>
  )
}

export function Alert({
  children,
  tone = 'info',
  title,
}: {
  children: ReactNode
  tone?: Tone
  title?: string
}) {
  return (
    <div className={`alert alert-${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>
      {title && <strong>{title}</strong>}
      <div>{children}</div>
    </div>
  )
}

export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string
  description?: string
  actions?: ReactNode
}) {
  return (
    <header className="page-head">
      <div>
        <h1>{title}</h1>
        {description && <p className="muted">{description}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  )
}

export function StatCard({
  label,
  value,
  hint,
  tone = 'info',
}: {
  label: string
  value: ReactNode
  hint?: string
  tone?: Tone
}) {
  return (
    <div className={`stat stat-${tone}`}>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  )
}

export function Spinner() {
  return <span className="spinner" aria-hidden="true" />
}
