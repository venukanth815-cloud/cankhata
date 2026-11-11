package com.cankhata.service;

import com.cankhata.model.SessionUser;
import com.cankhata.util.Sql;

import java.util.List;
import java.util.Map;

/** In-app messages for a logged-in user (for example "your delivery was made"). */
public class NotificationService {

    public List<Map<String, Object>> unread(SessionUser actor) {
        return Sql.query("SELECT id, message, type, created_at FROM notifications WHERE user_id = ? AND read_flag = FALSE ORDER BY id DESC LIMIT 20", actor.getId());
    }

    public void markAllRead(SessionUser actor) {
        Sql.update("Could not update notifications", "UPDATE notifications SET read_flag = TRUE WHERE user_id = ? AND read_flag = FALSE", actor.getId());
    }
}
