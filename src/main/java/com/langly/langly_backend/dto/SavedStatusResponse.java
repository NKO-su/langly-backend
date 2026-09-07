package com.langly.langly_backend.dto;

public class SavedStatusResponse {
    private boolean saved;

    public SavedStatusResponse(boolean saved) {
        this.saved = saved;
    }

    public boolean getSaved() {
        return saved;
    }
}
