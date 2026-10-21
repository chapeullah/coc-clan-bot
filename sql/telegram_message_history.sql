-- Incoming commands and every attempt to send their response.
-- Several rows for one part are retries; different parts share delivery_id.
SELECT i.update_id,
       i.chat_id,
       i.thread_id,
       i.user_id AS command_user_id,
       i.text AS command_text,
       o.delivery_id,
       o.part_number,
       o.part_count,
       o.attempt_number,
       o.status,
       o.request_text AS response_html,
       o.text AS response_text,
       o.message_id,
       o.ephemeral_message_id,
       o.receiver_user_id,
       o.sent_at,
       o.http_status,
       o.api_description,
       o.error_message
FROM telegram_message_history i
LEFT JOIN telegram_outgoing_message_history o ON o.source_update_id = i.update_id
ORDER BY i.created_at DESC, o.delivery_id, o.part_number, o.attempt_number;

-- Notifications and system errors also go through outgoing history.
SELECT id, type, notification_id, operation, chat_id, thread_id,
       delivery_id, part_number, attempt_number, status,
       request_text, created_at, sent_at, error_message
FROM telegram_outgoing_message_history
WHERE source_update_id IS NULL
ORDER BY created_at DESC, id DESC;

-- Full request and response are retained even when no DTO field exists for them.
-- SENT means Telegram accepted the request; it does not confirm that a user read it.
-- UNKNOWN means there was no reliable confirmation of delivery.
SELECT id, delivery_id, part_number, attempt_number, status,
       request_json, raw_json, response_headers_json,
       http_status, api_error_code, api_description,
       retry_after_seconds, migrate_to_chat_id, error_type, error_message
FROM telegram_outgoing_message_history
ORDER BY created_at DESC, id DESC;
