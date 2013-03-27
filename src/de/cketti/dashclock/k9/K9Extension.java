package de.cketti.dashclock.k9;

import java.util.List;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import de.cketti.dashclock.k9.K9Helper.Account;


public class K9Extension extends DashClockExtension {

    @Override
    protected void onUpdateData(int reason) {

        int unreadCount = 0;
        StringBuilder body = new StringBuilder();

        List<Account> accounts = K9Helper.getAccounts(this);
        if (accounts != null) {
            for (Account account : accounts) {
                int accountUnread = K9Helper.getUnreadCount(this, account);
                unreadCount += accountUnread;

                if (accountUnread > 0) {
                    if (body.length() > 0) {
                        body.append('\n');
                    }

                    body.append(account.name).append(" (").append(accountUnread).append(')');
                }
            }
        }

        ExtensionData data = new ExtensionData()
                .visible(unreadCount > 0)
                .icon(R.drawable.ic_envelope)
                .status(Integer.toString(unreadCount))
                .expandedTitle(getString(R.string.unread_title, unreadCount))
                .expandedBody(body.toString());

        publishUpdate(data);
    }

}
