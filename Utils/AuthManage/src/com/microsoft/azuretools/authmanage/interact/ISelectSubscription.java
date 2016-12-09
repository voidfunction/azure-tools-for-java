package com.microsoft.azuretools.authmanage.interact;

import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;

import java.util.List;

/**
 * Created by shch on 10/3/2016.
 */
public interface ISelectSubscription {
    void init(List<SubscriptionDetail> details);
    List<SubscriptionDetail> update();
}
