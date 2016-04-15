package com.microsoft.azure.hdinsight.serverexplore.collections;

import java.util.Collection;
import java.util.EventObject;

/**
 * Created by joezhang on 15-12-2.
 */

public class ListChangedEvent extends EventObject {
    private ListChangedAction action;
    private Collection<?> newItems;
    private Collection<?> oldItems;

    public ListChangedEvent(
            ObservableList<?> source,
            ListChangedAction action,
            Collection<?> newItems,
            Collection<?> oldItems) {
        super(source);
        this.action = action;
        this.newItems = newItems;
        this.oldItems = oldItems;
    }

    public ListChangedAction getAction() {
        return action;
    }

    public Collection<?> getNewItems() {
        return newItems;
    }

    public Collection<?> getOldItems() {
        return oldItems;
    }
}