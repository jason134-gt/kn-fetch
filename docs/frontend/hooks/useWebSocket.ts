/**
 * WebSocket Hook - 实时数据更新
 * 文件路径: src/hooks/useWebSocket.ts
 */

import { useEffect, useRef, useCallback, useState } from 'react';
import type { WSMessage, TaskProgressPayload, ReportUpdatedPayload, ReviewStatusChangedPayload } from '../types';

type MessageHandler<T = any> = (payload: T) => void;

interface UseWebSocketOptions {
  url: string;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (error: Event) => void;
  reconnect?: boolean;
  reconnectInterval?: number;
  maxReconnectAttempts?: number;
}

interface WebSocketState {
  connected: boolean;
  connecting: boolean;
  error: string | null;
}

export function useWebSocket(options: UseWebSocketOptions) {
  const {
    url,
    onOpen,
    onClose,
    onError,
    reconnect = true,
    reconnectInterval = 3000,
    maxReconnectAttempts = 5,
  } = options;

  const wsRef = useRef<WebSocket | null>(null);
  const handlersRef = useRef<Map<string, Set<MessageHandler>>>(new Map());
  const reconnectAttemptsRef = useRef(0);
  const [state, setState] = useState<WebSocketState>({
    connected: false,
    connecting: false,
    error: null,
  });

  // 连接 WebSocket
  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return;
    }

    setState((prev) => ({ ...prev, connecting: true, error: null }));

    try {
      const ws = new WebSocket(url);

      ws.onopen = () => {
        setState({ connected: true, connecting: false, error: null });
        reconnectAttemptsRef.current = 0;
        onOpen?.();
      };

      ws.onclose = () => {
        setState({ connected: false, connecting: false, error: null });
        onClose?.();

        // 自动重连
        if (
          reconnect &&
          reconnectAttemptsRef.current < maxReconnectAttempts
        ) {
          reconnectAttemptsRef.current++;
          setTimeout(connect, reconnectInterval);
        }
      };

      ws.onerror = (error) => {
        setState((prev) => ({
          ...prev,
          error: 'WebSocket 连接错误',
        }));
        onError?.(error);
      };

      ws.onmessage = (event) => {
        try {
          const message: WSMessage = JSON.parse(event.data);
          const handlers = handlersRef.current.get(message.type);
          if (handlers) {
            handlers.forEach((handler) => handler(message.payload));
          }
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e);
        }
      };

      wsRef.current = ws;
    } catch (error: any) {
      setState({
        connected: false,
        connecting: false,
        error: error.message,
      });
    }
  }, [url, onOpen, onClose, onError, reconnect, reconnectInterval, maxReconnectAttempts]);

  // 断开连接
  const disconnect = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
  }, []);

  // 订阅消息
  const subscribe = useCallback(<T = any,>(
    messageType: string,
    handler: MessageHandler<T>
  ) => {
    if (!handlersRef.current.has(messageType)) {
      handlersRef.current.set(messageType, new Set());
    }
    handlersRef.current.get(messageType)!.add(handler as MessageHandler);

    // 返回取消订阅函数
    return () => {
      const handlers = handlersRef.current.get(messageType);
      if (handlers) {
        handlers.delete(handler as MessageHandler);
      }
    };
  }, []);

  // 发送消息
  const send = useCallback(<T = any,>(type: string, payload: T) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(
        JSON.stringify({
          type,
          payload,
          timestamp: new Date().toISOString(),
        })
      );
    }
  }, []);

  // 自动连接
  useEffect(() => {
    connect();
    return disconnect;
  }, [connect, disconnect]);

  return {
    ...state,
    connect,
    disconnect,
    subscribe,
    send,
  };
}

/**
 * 任务进度 Hook
 */
export function useTaskProgress(
  wsUrl: string,
  taskId: string,
  onProgress?: (progress: TaskProgressPayload) => void
) {
  const [progress, setProgress] = useState<TaskProgressPayload | null>(null);

  const { connected, subscribe } = useWebSocket({
    url: wsUrl,
  });

  useEffect(() => {
    const unsubscribe = subscribe<TaskProgressPayload>(
      'task_progress',
      (payload) => {
        if (payload.taskId === taskId) {
          setProgress(payload);
          onProgress?.(payload);
        }
      }
    );

    return unsubscribe;
  }, [subscribe, taskId, onProgress]);

  useEffect(() => {
    const unsubscribe = subscribe(
      'task_completed',
      (payload: { taskId: string }) => {
        if (payload.taskId === taskId) {
          setProgress((prev) => prev ? { ...prev, progress: 100 } : null);
        }
      }
    );

    return unsubscribe;
  }, [subscribe, taskId]);

  return { progress, connected };
}

/**
 * 报告更新 Hook
 */
export function useReportUpdates(
  wsUrl: string,
  reportId: string,
  onUpdate?: (update: ReportUpdatedPayload) => void
) {
  const [lastUpdate, setLastUpdate] = useState<ReportUpdatedPayload | null>(null);

  const { connected, subscribe } = useWebSocket({
    url: wsUrl,
  });

  useEffect(() => {
    const unsubscribe = subscribe<ReportUpdatedPayload>(
      'report_updated',
      (payload) => {
        if (payload.reportId === reportId) {
          setLastUpdate(payload);
          onUpdate?.(payload);
        }
      }
    );

    return unsubscribe;
  }, [subscribe, reportId, onUpdate]);

  return { lastUpdate, connected };
}

/**
 * 审核状态变更 Hook
 */
export function useReviewStatusChanges(
  wsUrl: string,
  recordId: string,
  onStatusChange?: (change: ReviewStatusChangedPayload) => void
) {
  const [lastChange, setLastChange] = useState<ReviewStatusChangedPayload | null>(null);

  const { connected, subscribe } = useWebSocket({
    url: wsUrl,
  });

  useEffect(() => {
    const unsubscribe = subscribe<ReviewStatusChangedPayload>(
      'review_status_changed',
      (payload) => {
        if (payload.recordId === recordId) {
          setLastChange(payload);
          onStatusChange?.(payload);
        }
      }
    );

    return unsubscribe;
  }, [subscribe, recordId, onStatusChange]);

  return { lastChange, connected };
}
