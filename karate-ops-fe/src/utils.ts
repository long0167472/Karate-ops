import type { Side, TimerState } from "./types";

export function sideLabel(side: Side | null | undefined) {
  if (side === "aka") return "AKA";
  if (side === "ao") return "AO";
  return "None";
}

export function sideName(side: Side) {
  return side === "aka" ? "Aka" : "Ao";
}

export function cx(...parts: Array<string | false | null | undefined>) {
  return parts.filter(Boolean).join(" ");
}

export function formatMs(ms: number) {
  const safe = Math.max(0, Math.round(ms));
  const totalSeconds = Math.ceil(safe / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

export function formatClockTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export function liveRemaining(timer: TimerState, receivedAt: number, now: number) {
  if (!timer.running) return timer.remainingMs;
  return Math.max(0, timer.remainingMs - Math.max(0, now - receivedAt));
}

export function durationFromPreset(seconds: number) {
  return seconds * 1000;
}

export function winnerText(side: Side | null, reason?: string | null) {
  if (!side) return reason || "Decision required";
  return `${sideLabel(side)} wins${reason ? ` - ${reason}` : ""}`;
}
