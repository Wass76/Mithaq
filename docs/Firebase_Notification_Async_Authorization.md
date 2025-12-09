# Firebase Notification Changes: Authorization & Async Dispatch

This note summarizes the fixes for the Firebase notification feature: securing the send endpoint and offloading delivery to an async queue.

## What changed
- `POST /api/v1/notifications/send` now requires `hasRole('PLATFORM_ADMIN')` (`@PreAuthorize` on the controller).
- Notification delivery is offloaded:
  1. Create a pending notification record.
  2. Enqueue it to a background worker.
  3. Return immediately to the caller while the worker contacts FCM.

## Key classes
- `NotificationController` — exposes `/send` and now guards it with `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`.
- `NotificationService` — creates a pending notification and enqueues work instead of sending synchronously.
- `NotificationQueueService` — in-memory queue + background consumer that calls Firebase delivery.
- `FirebaseNotificationService` — split into `createPendingNotification(...)` and `deliverNotification(...)` used by the queue.
- `NotificationAsyncConfig` — provides the dedicated `notificationTaskExecutor` thread pool.

## Flow
1) Client (admin) calls `POST /api/v1/notifications/send` with `NotificationRequest`.
2) Controller checks role, then `NotificationService.sendNotification` creates a `Notification` with status `PENDING` and enqueues a task.
3) `NotificationQueueService` consumer takes tasks and invokes `FirebaseNotificationService.deliverNotification`.
4) Delivery updates the notification status to `SENT` or `FAILED` and deactivates bad tokens when detected.

## Configuration
- Executor bean lives in `NotificationAsyncConfig` with a small thread pool (core 2, max 4, queue 200). Tune if needed.
- Method security is already enabled globally via `@EnableMethodSecurity` in `SecurityConfiguration`.

## Notes and next steps
- This queue is in-memory; if you need durability/retries across restarts, move to a real broker (RabbitMQ/Kafka/etc.).
- Consider exposing a status endpoint or polling existing notification listing to confirm delivery status.
- Error mapping is still coarse; map specific FCM errors to user-facing codes if desired.

