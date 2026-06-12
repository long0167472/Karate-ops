import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiGet, apiPost, authHeaders, getApiBase } from "./apiClient";
import type { NotificationResponse } from "./types";

function wsUrl(path: string) {
  const apiBase = getApiBase();
  if (apiBase) {
    const url = new URL(apiBase);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    url.pathname = path;
    return url.toString();
  }
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}${path}`;
}

export function useNotifications(userId: string | null) {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const clientRef = useRef<Client | null>(null);

  const load = useCallback(async () => {
    if (!userId) return;
    try {
      const data = await apiGet<NotificationResponse[]>("/api/notifications");
      setNotifications(data);
    } catch {
      // silently ignore - notifications are non-critical
    }
  }, [userId]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!userId) return;
    const client = new Client({
      brokerURL: wsUrl("/ws"),
      connectHeaders: authHeaders(),
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        client.subscribe(`/topic/users/${userId}/notifications`, (message) => {
          const incoming = JSON.parse(message.body) as NotificationResponse;
          setNotifications((prev) => {
            const updated = [incoming, ...prev.filter((n) => n.id !== incoming.id)];
            // keep max 10 in-memory as well
            return updated.slice(0, 10);
          });
        });
      }
    });
    clientRef.current = client;
    client.activate();
    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [userId]);

  const markRead = useCallback(async (id: string) => {
    try {
      const updated = await apiPost<NotificationResponse>(`/api/notifications/${id}/read`, {});
      setNotifications((prev) => prev.map((n) => (n.id === id ? updated : n)));
    } catch {
      // ignore
    }
  }, []);

  const markAllRead = useCallback(async () => {
    try {
      await apiPost("/api/notifications/read-all", {});
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    } catch {
      // ignore
    }
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  return { notifications, unreadCount, markRead, markAllRead };
}
