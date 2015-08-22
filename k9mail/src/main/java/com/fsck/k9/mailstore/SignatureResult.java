package com.fsck.k9.mailstore;

import java.util.List;

public class SignatureResult {
    private SignatureStatus status;
    private String primaryUserId;
    private List<String> userIds;

    public SignatureResult(final SignatureStatus status, final String primaryUserId, final List<String> userIds) {
        this.status = status;
        this.primaryUserId = primaryUserId;
        this.userIds = userIds;
    }

    public String getPrimaryUserId() {
        return primaryUserId;
    }

    public SignatureStatus getStatus() {
        return status;
    }
}
