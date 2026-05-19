"use client";

import React, { useState, useCallback } from "react";
import {
  Field, Button, Checkbox, PasswordStrength, BadgeIcon, Card,
  SocialButtons, Divider,
  IconMail, IconLock, IconUser, IconArrow, IconBack, IconCheck, IconMail2,
  isEmail, passwordStrength,
} from "./components";

/* ── Screen names and their order (for transition direction) ── */
type ScreenName = "login" | "signup" | "forgot" | "success";
const SCREEN_ORDER: ScreenName[] = ["login", "signup", "forgot", "success"];

interface Payload {
  firstName?: string;
  email?: string;
}

/* ── Login ── */
function LoginScreen({
  go,
  animClass,
}: {
  go: (s: ScreenName, p?: Payload) => void;
  animClass: string;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  function validate() {
    const e: Record<string, string> = {};
    if (!email) e.email = "Email is required.";
    else if (!isEmail(email)) e.email = "Enter a valid email.";
    if (!password) e.password = "Password is required.";
    return e;
  }

  async function handleSubmit() {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setLoading(true);
    await delay(900);
    setLoading(false);
    const first = email.split("@")[0];
    go("success", { firstName: first, email });
  }

  return (
    <div className={animClass}>
      <BrandMark />
      <Card>
        <Stack gap={24}>
          <Heading title="Welcome back" subtitle="Sign in to continue to your account." />
          <Stack gap={16}>
            <Field label="Email" type="email" value={email} onChange={setEmail}
              placeholder="you@example.com" icon={<IconMail />} error={errors.email} autoComplete="email" />
            <Field label="Password" type="password" value={password} onChange={setPassword}
              placeholder="••••••••" icon={<IconLock />} error={errors.password} autoComplete="current-password" />
          </Stack>
          <Row>
            <Checkbox checked={remember} onChange={setRemember}>Remember me</Checkbox>
            <LinkBtn onClick={() => go("forgot")}>Forgot password?</LinkBtn>
          </Row>
          <Button loading={loading} onClick={handleSubmit}>
            Sign in <IconArrow />
          </Button>
          <Divider label="or continue with" />
          <SocialButtons />
          <Footer>
            New here?{" "}
            <LinkBtn onClick={() => go("signup")}>Create an account</LinkBtn>
          </Footer>
        </Stack>
      </Card>
    </div>
  );
}

/* ── Sign Up ── */
function SignUpScreen({
  go,
  animClass,
}: {
  go: (s: ScreenName, p?: Payload) => void;
  animClass: string;
}) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [terms, setTerms] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  function validate() {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = "Name is required.";
    if (!email) e.email = "Email is required.";
    else if (!isEmail(email)) e.email = "Enter a valid email.";
    if (!password) e.password = "Password is required.";
    else if (password.length < 8) e.password = "Minimum 8 characters.";
    if (!terms) e.terms = "You must accept the terms.";
    return e;
  }

  async function handleSubmit() {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setLoading(true);
    await delay(900);
    setLoading(false);
    const firstName = name.trim().split(" ")[0];
    go("success", { firstName, email });
  }

  return (
    <div className={animClass}>
      <BrandMark />
      <Card>
        <Stack gap={24}>
          <Heading title="Create your account" subtitle="It takes less than a minute." />
          <Stack gap={16}>
            <Field label="Full name" type="text" value={name} onChange={setName}
              placeholder="Jane Smith" icon={<IconUser />} error={errors.name} autoComplete="name" />
            <Field label="Email" type="email" value={email} onChange={setEmail}
              placeholder="you@example.com" icon={<IconMail />} error={errors.email} autoComplete="email" />
            <div>
              <Field label="Password" type="password" value={password} onChange={setPassword}
                placeholder="Min. 8 characters" icon={<IconLock />} error={errors.password} autoComplete="new-password" />
              {password && <div style={{ marginTop: 8 }}><PasswordStrength password={password} /></div>}
            </div>
          </Stack>
          <Checkbox checked={terms} onChange={setTerms} error={!!errors.terms}>
            I agree to the{" "}
            <span style={{ color: "var(--accent)", cursor: "pointer" }}>Terms</span>
            {" "}and{" "}
            <span style={{ color: "var(--accent)", cursor: "pointer" }}>Privacy Policy</span>.
          </Checkbox>
          {errors.terms && <span style={{ fontSize: 12, color: "var(--err)", marginTop: -12 }}>{errors.terms}</span>}
          <Button loading={loading} onClick={handleSubmit}>
            Create account <IconArrow />
          </Button>
          <Divider label="or continue with" />
          <SocialButtons />
          <Footer>
            Already have an account?{" "}
            <LinkBtn onClick={() => go("login")}>Sign in</LinkBtn>
          </Footer>
        </Stack>
      </Card>
    </div>
  );
}

/* ── Forgot Password ── */
function ForgotScreen({
  go,
  animClass,
}: {
  go: (s: ScreenName, p?: Payload) => void;
  animClass: string;
}) {
  const [email, setEmail] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  function validate() {
    const e: Record<string, string> = {};
    if (!email) e.email = "Email is required.";
    else if (!isEmail(email)) e.email = "Enter a valid email.";
    return e;
  }

  async function handleSubmit() {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setLoading(true);
    await delay(900);
    setLoading(false);
    setSent(true);
  }

  if (sent) {
    return (
      <div className={animClass}>
        <BrandMark />
        <Card>
          <Stack gap={24}>
            <BadgeIcon><IconMail2 /></BadgeIcon>
            <Heading
              title="Check your inbox"
              subtitle={`We sent a reset link to ${email}. It expires in 30 minutes.`}
            />
            <Button onClick={() => go("login")}>Back to sign in</Button>
            <Footer>
              <LinkBtn onClick={() => setSent(false)}>Didn&apos;t get it? Try again</LinkBtn>
            </Footer>
          </Stack>
        </Card>
      </div>
    );
  }

  return (
    <div className={animClass} style={{ position: "relative" }}>
      <button
        type="button"
        onClick={() => go("login")}
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          display: "flex",
          alignItems: "center",
          gap: 6,
          background: "none",
          border: "none",
          color: "var(--ink-2)",
          fontSize: 13.5,
          fontWeight: 500,
          fontFamily: "inherit",
          cursor: "pointer",
          padding: "4px 0",
        }}
      >
        <IconBack /> Back
      </button>
      <BrandMark />
      <Card>
        <Stack gap={24}>
          <Heading
            title="Forgot password?"
            subtitle="No worries — enter your email and we'll send you reset instructions."
          />
          <Field label="Email" type="email" value={email} onChange={setEmail}
            placeholder="you@example.com" icon={<IconMail />} error={errors.email} autoComplete="email" />
          <Button loading={loading} onClick={handleSubmit}>
            Send reset link <IconArrow />
          </Button>
          <Footer>
            Remembered it?{" "}
            <LinkBtn onClick={() => go("login")}>Sign in</LinkBtn>
          </Footer>
        </Stack>
      </Card>
    </div>
  );
}

/* ── Success ── */
function SuccessScreen({
  go,
  payload,
  animClass,
}: {
  go: (s: ScreenName, p?: Payload) => void;
  payload: Payload;
  animClass: string;
}) {
  return (
    <div className={animClass}>
      <BrandMark />
      <Card>
        <Stack gap={24}>
          <BadgeIcon variant="success">
            <IconCheck />
          </BadgeIcon>
          <Heading
            title={`You're in${payload.firstName ? `, ${payload.firstName}` : ""}.`}
            subtitle="This is where your dashboard would load."
          />
          <Button onClick={() => go("login")}>Sign out</Button>
        </Stack>
      </Card>
    </div>
  );
}

/* ── Root AuthPage ── */
export default function AuthPage() {
  const [screen, setScreen] = useState<ScreenName>("login");
  const [payload, setPayload] = useState<Payload>({});
  const [animClass, setAnimClass] = useState("slide-in-right");

  const go = useCallback((next: ScreenName, p?: Payload) => {
    const currIdx = SCREEN_ORDER.indexOf(screen);
    const nextIdx = SCREEN_ORDER.indexOf(next);
    setAnimClass(nextIdx >= currIdx ? "slide-in-right" : "slide-in-left");
    if (p) setPayload(p);
    setScreen(next);
  }, [screen]);

  const screenProps = { go, animClass };

  return (
    <div style={{
      minHeight: "100vh",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      padding: "40px 16px",
      background: "var(--surface)",
      /* Subtle radial glow behind the card */
      backgroundImage: "radial-gradient(ellipse 60% 40% at 50% 0%, color-mix(in srgb, var(--accent) 8%, transparent), transparent 70%)",
    }}>
      {screen === "login" && <LoginScreen {...screenProps} />}
      {screen === "signup" && <SignUpScreen {...screenProps} />}
      {screen === "forgot" && <ForgotScreen {...screenProps} />}
      {screen === "success" && <SuccessScreen {...screenProps} payload={payload} animClass={animClass} />}
    </div>
  );
}

/* ── Small layout helpers (file-local) ── */
function BrandMark() {
  return (
    <div style={{
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 20,
      gap: 8,
    }}>
      <span style={{
        width: 36,
        height: 36,
        borderRadius: "50%",
        background: "var(--accent)",
        color: "#fff",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: 18,
        fontWeight: 700,
        lineHeight: 1,
      }}>◐</span>
      <span style={{ fontSize: 16, fontWeight: 700, color: "var(--ink)", letterSpacing: "-0.02em" }}>
        Northwind
      </span>
    </div>
  );
}

function Heading({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
      <h1 style={{
        margin: 0,
        fontSize: 24,
        fontWeight: 700,
        color: "var(--ink)",
        letterSpacing: "-0.025em",
        lineHeight: 1.2,
      }}>
        {title}
      </h1>
      <p style={{ margin: 0, fontSize: 14, color: "var(--ink-2)", lineHeight: 1.55 }}>{subtitle}</p>
    </div>
  );
}

function Stack({ children, gap }: { children: React.ReactNode; gap: number }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap }}>
      {children}
    </div>
  );
}

function Row({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
      {children}
    </div>
  );
}

function Footer({ children }: { children: React.ReactNode }) {
  return (
    <p style={{ margin: 0, textAlign: "center", fontSize: 13.5, color: "var(--ink-2)" }}>
      {children}
    </p>
  );
}

function LinkBtn({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        background: "none",
        border: "none",
        padding: 0,
        color: "var(--accent)",
        fontSize: "inherit",
        fontFamily: "inherit",
        fontWeight: 600,
        cursor: "pointer",
        textDecoration: "none",
      }}
    >
      {children}
    </button>
  );
}

function delay(ms: number) {
  return new Promise((r) => setTimeout(r, ms));
}
