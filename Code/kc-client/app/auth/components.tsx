"use client";

import React, { useState, useId } from "react";

/* ── Helpers ── */
export function isEmail(v: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
}

export function passwordStrength(p: string): number {
  if (!p) return 0;
  let score = 0;
  if (p.length >= 8) score++;
  if (p.length >= 12) score++;
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++;
  if (/\d/.test(p)) score++;
  if (/[^a-zA-Z\d]/.test(p)) score++;
  return score;
}

const strengthLabel = ["", "Weak", "Fair", "Good", "Strong", "Excellent"];
const strengthColor = ["", "#dc2626", "#ea580c", "#ca8a04", "#16a34a", "#15803d"];

/* ── SVG Icons ── */
export function IconMail() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="1.5" y="3.5" width="13" height="9" rx="1.5" />
      <path d="M1.5 5.5l6.5 4 6.5-4" />
    </svg>
  );
}

export function IconLock() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="7" width="10" height="7" rx="1.5" />
      <path d="M5 7V5a3 3 0 0 1 6 0v2" />
      <circle cx="8" cy="10.5" r="1" fill="currentColor" stroke="none" />
    </svg>
  );
}

export function IconUser() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="8" cy="5.5" r="2.5" />
      <path d="M2.5 13.5a5.5 5.5 0 0 1 11 0" />
    </svg>
  );
}

export function IconEye({ off }: { off?: boolean }) {
  return off ? (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 2l12 12M6.5 6.6A3 3 0 0 0 9.4 9.5" />
      <path d="M4.2 4.3C2.9 5.2 2 6.5 2 8c0 2.5 2.7 5 6 5 1.2 0 2.3-.3 3.2-.8" />
      <path d="M11.5 11.6C12.9 10.6 14 9.4 14 8c0-2.5-2.7-5-6-5-.8 0-1.6.2-2.3.4" />
    </svg>
  ) : (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 8c0-2.5 2.7-5 6-5s6 2.5 6 5-2.7 5-6 5-6-2.5-6-5Z" />
      <circle cx="8" cy="8" r="2" />
    </svg>
  );
}

export function IconArrow() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 8h10M9 4l4 4-4 4" />
    </svg>
  );
}

export function IconBack() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M13 8H3M7 4L3 8l4 4" />
    </svg>
  );
}

export function IconCheck() {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 10l4.5 4.5L16 6" />
    </svg>
  );
}

export function IconMail2() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="4" width="20" height="16" rx="2.5" />
      <path d="M2 7l10 7 10-7" />
    </svg>
  );
}

/* ── Field ── */
interface FieldProps {
  label: string;
  type?: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  icon?: React.ReactNode;
  error?: string;
  autoComplete?: string;
}

export function Field({ label, type = "text", value, onChange, placeholder, icon, error, autoComplete }: FieldProps) {
  const id = useId();
  const [showPass, setShowPass] = useState(false);
  const isPassword = type === "password";
  const inputType = isPassword ? (showPass ? "text" : "password") : type;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
      <label
        htmlFor={id}
        style={{
          fontSize: 12.5,
          fontWeight: 500,
          color: "var(--ink-2)",
          letterSpacing: "0.01em",
        }}
      >
        {label}
      </label>
      <div
        style={{
          position: "relative",
          height: "var(--field-h)",
          display: "flex",
          alignItems: "center",
          background: "var(--card)",
          border: `1px solid ${error ? "var(--err)" : "var(--line)"}`,
          borderRadius: "var(--radius)",
          boxShadow: error
            ? "0 0 0 4px color-mix(in srgb, var(--err) 14%, transparent)"
            : undefined,
          transition: "border-color 150ms, box-shadow 150ms",
        }}
        onFocusCapture={(e) => {
          const el = e.currentTarget;
          if (!error) {
            el.style.borderColor = "var(--accent)";
            el.style.boxShadow = "0 0 0 4px var(--accent-soft)";
          }
        }}
        onBlurCapture={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget as Node)) {
            const el = e.currentTarget;
            el.style.borderColor = error ? "var(--err)" : "var(--line)";
            el.style.boxShadow = error ? "0 0 0 4px color-mix(in srgb, var(--err) 14%, transparent)" : "";
          }
        }}
      >
        {icon && (
          <span style={{ position: "absolute", left: 12, color: "var(--ink-3)", pointerEvents: "none", display: "flex" }}>
            {icon}
          </span>
        )}
        <input
          id={id}
          type={inputType}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          style={{
            flex: 1,
            height: "100%",
            background: "transparent",
            border: "none",
            outline: "none",
            padding: `0 ${isPassword ? "40px" : "12px"} 0 ${icon ? "36px" : "12px"}`,
            fontSize: 14.5,
            color: "var(--ink)",
            fontFamily: "inherit",
          }}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setShowPass(!showPass)}
            style={{
              position: "absolute",
              right: 8,
              width: 28,
              height: 28,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              background: "transparent",
              border: "none",
              borderRadius: 6,
              color: "var(--ink-3)",
              cursor: "pointer",
              transition: "background 150ms, color 150ms",
            }}
            onMouseEnter={(e) => {
              (e.currentTarget as HTMLElement).style.background = "var(--accent-soft)";
              (e.currentTarget as HTMLElement).style.color = "var(--accent)";
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLElement).style.background = "transparent";
              (e.currentTarget as HTMLElement).style.color = "var(--ink-3)";
            }}
            aria-label={showPass ? "Hide password" : "Show password"}
          >
            <IconEye off={showPass} />
          </button>
        )}
      </div>
      {error && (
        <span style={{ fontSize: 12, color: "var(--err)" }}>{error}</span>
      )}
    </div>
  );
}

/* ── Primary Button ── */
interface ButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  type?: "button" | "submit";
  loading?: boolean;
  disabled?: boolean;
}

export function Button({ children, onClick, type = "button", loading, disabled }: ButtonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      style={{
        height: "var(--field-h)",
        width: "100%",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: 8,
        background: "var(--ink)",
        color: "#fff",
        border: "none",
        borderRadius: "var(--radius)",
        fontSize: 14.5,
        fontWeight: 600,
        fontFamily: "inherit",
        cursor: disabled || loading ? "not-allowed" : "pointer",
        opacity: disabled || loading ? 0.65 : 1,
        transition: "background 150ms, transform 80ms",
        letterSpacing: "0.01em",
      }}
      onMouseEnter={(e) => {
        if (!disabled && !loading)
          (e.currentTarget as HTMLElement).style.background =
            "color-mix(in oklab, var(--ink) 88%, var(--accent) 12%)";
      }}
      onMouseLeave={(e) => {
        (e.currentTarget as HTMLElement).style.background = "var(--ink)";
      }}
      onMouseDown={(e) => {
        if (!disabled && !loading)
          (e.currentTarget as HTMLElement).style.transform = "translateY(1px)";
      }}
      onMouseUp={(e) => {
        (e.currentTarget as HTMLElement).style.transform = "";
      }}
    >
      {loading ? <span className="spinner" /> : children}
    </button>
  );
}

/* ── Checkbox ── */
interface CheckboxProps {
  checked: boolean;
  onChange: (v: boolean) => void;
  children: React.ReactNode;
  error?: boolean;
}

export function Checkbox({ checked, onChange, children, error }: CheckboxProps) {
  return (
    <label style={{ display: "flex", alignItems: "flex-start", gap: 10, cursor: "pointer", userSelect: "none" }}>
      <span
        style={{
          flexShrink: 0,
          marginTop: 2,
          width: 16,
          height: 16,
          borderRadius: 5,
          border: `1.5px solid ${error ? "var(--err)" : checked ? "var(--accent)" : "var(--ink-4)"}`,
          background: checked ? "var(--accent)" : "var(--card)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          transition: "background 150ms, border-color 150ms",
        }}
        onClick={() => onChange(!checked)}
      >
        {checked && (
          <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M1.5 5l2.5 2.5 5-5" />
          </svg>
        )}
      </span>
      <span style={{ fontSize: 13.5, color: "var(--ink-2)", lineHeight: 1.5 }}>{children}</span>
    </label>
  );
}

/* ── Password Strength Meter ── */
export function PasswordStrength({ password }: { password: string }) {
  const score = passwordStrength(password);
  if (!password) return null;
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
      <div style={{ display: "flex", gap: 4 }}>
        {[1, 2, 3, 4, 5].map((i) => (
          <div
            key={i}
            style={{
              flex: 1,
              height: 4,
              borderRadius: 2,
              background: i <= score ? strengthColor[score] : "var(--ink-4)",
              transition: "background 300ms",
            }}
          />
        ))}
      </div>
      <div style={{ textAlign: "right", fontSize: 12, color: score > 0 ? strengthColor[score] : "var(--ink-3)", fontVariantNumeric: "tabular-nums", fontWeight: 500 }}>
        {strengthLabel[score]}
      </div>
    </div>
  );
}

/* ── Badge Icon ── */
interface BadgeIconProps {
  variant?: "accent" | "success";
  children: React.ReactNode;
}

export function BadgeIcon({ variant = "accent", children }: BadgeIconProps) {
  const bg = variant === "success"
    ? "color-mix(in srgb, var(--ok) 14%, transparent)"
    : "var(--accent-soft)";
  const color = variant === "success" ? "var(--ok)" : "var(--accent)";
  return (
    <div style={{
      width: 48,
      height: 48,
      borderRadius: 14,
      background: bg,
      color,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
    }}>
      {children}
    </div>
  );
}

/* ── Card ── */
export function Card({ children }: { children: React.ReactNode }) {
  return (
    <div style={{
      width: "100%",
      maxWidth: 420,
      background: "var(--card)",
      border: "1px solid var(--line)",
      borderRadius: "var(--radius-card)",
      boxShadow: "0 10px 30px -12px rgba(15,23,42,0.12), 0 2px 6px -1px rgba(15,23,42,0.04), 0 1px 0 rgba(255,255,255,.6) inset",
      padding: "36px 32px 28px",
    }}>
      {children}
    </div>
  );
}

/* ── Social Buttons ── */
export function SocialButtons() {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8 }}>
      {[
        {
          name: "Google",
          icon: (
            <svg width="18" height="18" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
          ),
        },
        {
          name: "Apple",
          icon: (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z" />
            </svg>
          ),
        },
        {
          name: "Facebook",
          icon: (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="#1877F2">
              <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
            </svg>
          ),
        },
      ].map(({ name, icon }) => (
        <button
          key={name}
          type="button"
          style={{
            height: 44,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            background: "var(--card)",
            border: "1px solid var(--line)",
            borderRadius: "var(--radius)",
            cursor: "pointer",
            transition: "border-color 150ms, background 150ms",
          }}
          onMouseEnter={(e) => {
            (e.currentTarget as HTMLElement).style.borderColor = "var(--accent)";
            (e.currentTarget as HTMLElement).style.background = "var(--accent-soft)";
          }}
          onMouseLeave={(e) => {
            (e.currentTarget as HTMLElement).style.borderColor = "var(--line)";
            (e.currentTarget as HTMLElement).style.background = "var(--card)";
          }}
          aria-label={`Sign in with ${name}`}
        >
          {icon}
        </button>
      ))}
    </div>
  );
}

/* ── Divider ── */
export function Divider({ label }: { label: string }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
      <div style={{ flex: 1, height: 1, background: "var(--line)" }} />
      <span style={{ fontSize: 12, color: "var(--ink-3)", whiteSpace: "nowrap" }}>{label}</span>
      <div style={{ flex: 1, height: 1, background: "var(--line)" }} />
    </div>
  );
}
