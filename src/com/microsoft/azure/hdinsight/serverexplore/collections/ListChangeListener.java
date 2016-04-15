package com.microsoft.azure.hdinsight.serverexplore.collections;

import java.util.EventListener;

public interface ListChangeListener extends EventListener {
    void listChanged(ListChangedEvent e);
}
