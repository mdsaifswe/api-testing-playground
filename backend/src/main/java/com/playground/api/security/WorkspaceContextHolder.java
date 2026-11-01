package com.playground.api.security;

import com.playground.api.entity.WorkspaceEntity;
import com.playground.api.exception.UnauthorizedException;

public class WorkspaceContextHolder {

    private static final ThreadLocal<WorkspaceEntity> CURRENT_WORKSPACE = new ThreadLocal<>();

    public static void set(WorkspaceEntity workspace) {
        CURRENT_WORKSPACE.set(workspace);
    }

    public static WorkspaceEntity get() {
        return CURRENT_WORKSPACE.get();
    }

    public static WorkspaceEntity requireCurrent() {
        WorkspaceEntity workspace = CURRENT_WORKSPACE.get();
        if (workspace == null) {
            throw new UnauthorizedException("Workspace authorization context is missing or invalid.");
        }
        return workspace;
    }

    public static void clear() {
        CURRENT_WORKSPACE.remove();
    }
}
