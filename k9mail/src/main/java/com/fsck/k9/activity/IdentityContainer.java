package com.fsck.k9.activity;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;

/**
 * Used to store an {@link Identity} instance together with the {@link Account} it belongs to.
 *
 * @see com.fsck.k9.adapter.IdentityAdapter
 */
public class IdentityContainer {
    public final Identity identity;
    public final Account account;

    public IdentityContainer(Identity identity, Account account) {
        this.identity = identity;
        this.account = account;
    }

    public String getIdentityDescription() {
        return String.format("%s <%s>", identity.getName(), identity.getEmail());
    }
}
