import { LayoutDashboard, Settings2, Shield, Trophy } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { AuthUserResponse } from "./types";

export type PrimaryNavKey =
  | "overview"
  | "clubs"
  | "tournaments"
  | "settings";

export interface ShellNavItem {
  key: PrimaryNavKey;
  label: string;
  href: string;
  icon: LucideIcon;
}

export function hasRole(user: AuthUserResponse, role: string) {
  return user.roles.includes(role);
}

export function isGlobalAdmin(user: AuthUserResponse) {
  return hasRole(user, "GLOBAL_ADMIN");
}

export function canManageClub(user: AuthUserResponse) {
  return hasRole(user, "GLOBAL_ADMIN") || hasRole(user, "CLUB_MANAGER") || hasRole(user, "COACH");
}

export function buildPrimaryNav(user: AuthUserResponse): ShellNavItem[] {
  if (!canManageClub(user)) {
    return [
      { key: "overview", label: "Portal của tôi", href: "/member", icon: LayoutDashboard },
      { key: "tournaments", label: "Giải đấu", href: "/tournaments/public", icon: Trophy },
      { key: "settings", label: "Cài đặt", href: "/member", icon: Settings2 }
    ];
  }

  return [
    { key: "overview", label: "Tổng quan", href: "/app", icon: LayoutDashboard },
    { key: "clubs", label: "Câu lạc bộ", href: "/clubs", icon: Shield },
    { key: "tournaments", label: "Giải đấu", href: "/tournaments", icon: Trophy },
    { key: "settings", label: "Cài đặt", href: "/member", icon: Settings2 }
  ];
}

export function roleContextLabel(user: AuthUserResponse) {
  if (isGlobalAdmin(user)) return "Global admin";
  if (hasRole(user, "CLUB_MANAGER")) return "Quản lý CLB";
  if (hasRole(user, "COACH")) return "Huấn luyện viên";
  return "Thành viên";
}
