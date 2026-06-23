import { BellRing, LogOut, Menu, Plus, Search, X } from "lucide-react";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import { buildPrimaryNav, hasRole, roleContextLabel, type PrimaryNavKey } from "../authNavigation";
import { NotificationBell } from "../NotificationBell";
import type { AuthUserResponse } from "../types";
import { cx } from "../utils";

interface Breadcrumb {
  label: string;
  href?: string;
}

interface AuthenticatedShellProps {
  user: AuthUserResponse;
  actualUser: AuthUserResponse;
  viewAsRole: string;
  setViewAsRole: (role: string) => void;
  onLogout: () => void;
  activeNav: PrimaryNavKey;
  eyebrow?: string;
  title: string;
  description: string;
  breadcrumbs?: Breadcrumb[];
  headerActions?: ReactNode;
  topbarAction?: ReactNode;
  children: ReactNode;
}

export function AuthenticatedShell({
  user,
  actualUser,
  viewAsRole,
  setViewAsRole,
  onLogout,
  activeNav,
  eyebrow,
  title,
  description,
  breadcrumbs = [],
  headerActions,
  topbarAction,
  children
}: AuthenticatedShellProps) {
  const [navOpen, setNavOpen] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const navItems = useMemo(() => buildPrimaryNav(user), [user]);

  const goToSearch = () => {
    const keyword = searchValue.trim().toLowerCase();
    if (!keyword) return;
    if (keyword.includes("giải") || keyword.includes("tournament") || keyword.includes("tatami")) {
      window.location.href = "/tournaments";
      return;
    }
    if (keyword.includes("phí") || keyword.includes("fee")) {
      window.location.href = hasRole(user, "GLOBAL_ADMIN") || hasRole(user, "CLUB_MANAGER") || hasRole(user, "COACH")
        ? "/clubs"
        : "/member#fees";
      return;
    }
    window.location.href = hasRole(user, "GLOBAL_ADMIN") || hasRole(user, "CLUB_MANAGER") || hasRole(user, "COACH")
      ? "/clubs"
      : "/member";
  };

  return (
    <main className="auth-shell-page">
      <aside className={cx("auth-shell-sidebar", navOpen && "open")}>
        <div className="auth-shell-sidebar-head">
          <a className="auth-shell-mark" href="/app">K</a>
          <div className="auth-shell-brand-copy">
            <strong>Karate Ops</strong>
            <span>{roleContextLabel(user)}</span>
          </div>
          <button className="auth-shell-mobile-close" onClick={() => setNavOpen(false)} aria-label="Đóng điều hướng">
            <X size={18} />
          </button>
        </div>

        <div className="auth-shell-user-card">
          <strong>{user.displayName}</strong>
          <span>{user.primaryOrganizationName || "Hệ thống toàn cục"}</span>
        </div>

        <nav className="auth-shell-nav">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <a
                key={item.key}
                className={cx("auth-shell-nav-item", activeNav === item.key && "active")}
                href={item.href}
                onClick={() => setNavOpen(false)}
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </a>
            );
          })}
        </nav>

        <div className="auth-shell-sidebar-foot">
          <span>{user.roles.join(" · ")}</span>
        </div>
      </aside>

      {navOpen ? <button className="auth-shell-overlay" onClick={() => setNavOpen(false)} aria-label="Đóng menu" /> : null}

      <section className="auth-shell-main">
        <header className="auth-shell-topbar">
          <div className="auth-shell-topbar-left">
            <button className="auth-shell-menu" onClick={() => setNavOpen(true)} aria-label="Mở điều hướng">
              <Menu size={18} />
            </button>
            <label className="auth-shell-search">
              <Search size={17} />
              <input
                value={searchValue}
                onChange={(event) => setSearchValue(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    event.preventDefault();
                    goToSearch();
                  }
                }}
                placeholder="Tìm CLB, giải đấu, học phí"
              />
            </label>
          </div>

          <div className="auth-shell-topbar-right">
            {topbarAction ?? (
              <a className="auth-shell-topbar-action" href={hasRole(user, "GLOBAL_ADMIN") ? "/clubs" : "/member"}>
                <Plus size={16} />
                <span>{hasRole(user, "GLOBAL_ADMIN") ? "Đi tới CLB" : "Mở portal"}</span>
              </a>
            )}

            {hasRole(actualUser, "GLOBAL_ADMIN") ? (
              <label className="auth-shell-view-as">
                <span>View as</span>
                <select value={viewAsRole} onChange={(event) => setViewAsRole(event.target.value)}>
                  <option value="ACTUAL">Actual</option>
                  <option value="GLOBAL_ADMIN">Global admin</option>
                  <option value="CLUB_MANAGER">Admin CLB</option>
                  <option value="MEMBER">Member</option>
                </select>
              </label>
            ) : null}

            <div className="auth-shell-topbar-icon">
              <BellRing size={16} />
              <NotificationBell userId={actualUser.id} />
            </div>

            <button className="auth-shell-logout" onClick={onLogout}>
              <LogOut size={16} />
              <span>Đăng xuất</span>
            </button>
          </div>
        </header>

        <section className="auth-shell-content">
          <header className="auth-page-header">
            {breadcrumbs.length > 0 ? (
              <nav className="auth-page-breadcrumbs" aria-label="Breadcrumb">
                {breadcrumbs.map((crumb, index) => (
                  <span key={`${crumb.label}-${index}`}>
                    {crumb.href ? <a href={crumb.href}>{crumb.label}</a> : <strong>{crumb.label}</strong>}
                  </span>
                ))}
              </nav>
            ) : null}
            <div className="auth-page-heading">
              <div>
                {eyebrow ? <span className="auth-page-eyebrow">{eyebrow}</span> : null}
                <h1>{title}</h1>
                <p>{description}</p>
              </div>
              {headerActions ? <div className="auth-page-actions">{headerActions}</div> : null}
            </div>
          </header>

          {children}
        </section>
      </section>
    </main>
  );
}
