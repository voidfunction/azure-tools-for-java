package com.microsoft.azure.hdinsight.serverexplore.node;

import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import com.microsoft.azure.hdinsight.common.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Semaphore;

public class EventHelper {
    public interface EventStateHandle {
        boolean isEventTriggered();
    }

    public interface EventWaitHandle {
        void waitEvent(@NotNull Runnable callback) throws HDExploreException;
    }

    public interface EventHandler {
        EventWaitHandle registerEvent()
                throws HDExploreException;

        void unregisterEvent(@NotNull EventWaitHandle waitHandle)
                throws HDExploreException;

        void interruptibleAction(@NotNull EventStateHandle eventState)
                throws HDExploreException;

        void eventTriggeredAction() throws HDExploreException;
    }

    private static class EventSyncInfo implements EventStateHandle {
        private final Object eventSync = new Object();
        Semaphore semaphore = new Semaphore(0);

        EventWaitHandle eventWaitHandle;
        boolean registeredEvent = false;
        boolean eventTriggered = false;
        HDExploreException exception;

        public boolean isEventTriggered() {
            synchronized (eventSync) {
                return eventTriggered;
            }
        }
    }

    public static void runInterruptible(@NotNull final EventHandler eventHandler)
            throws HDExploreException {
        final EventSyncInfo eventSyncInfo = new EventSyncInfo();

        eventSyncInfo.eventWaitHandle = eventHandler.registerEvent();
        eventSyncInfo.registeredEvent = true;

        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventSyncInfo.eventWaitHandle.waitEvent(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (eventSyncInfo.eventSync) {
                                if (eventSyncInfo.registeredEvent) {
                                    eventSyncInfo.registeredEvent = false;
                                    eventSyncInfo.eventTriggered = true;
                                    eventSyncInfo.semaphore.release();
                                }
                            }
                        }
                    });
                } catch (HDExploreException ignored) {
                }
            }
        });

        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventHandler.interruptibleAction(eventSyncInfo);

                    synchronized (eventSyncInfo.eventSync) {
                        if (eventSyncInfo.registeredEvent) {
                            eventSyncInfo.registeredEvent = false;
                            eventSyncInfo.semaphore.release();
                        }
                    }
                } catch (HDExploreException ex) {
                    synchronized (eventSyncInfo.eventSync) {
                        if (eventSyncInfo.registeredEvent) {
                            eventSyncInfo.registeredEvent = false;
                            eventSyncInfo.exception = ex;
                            eventSyncInfo.semaphore.release();
                        }
                    }
                }
            }
        });

        try {
            eventSyncInfo.semaphore.acquire();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } finally {
            eventHandler.unregisterEvent(eventSyncInfo.eventWaitHandle);
        }

        synchronized (eventSyncInfo.eventSync) {
            if (!eventSyncInfo.eventTriggered) {
                if (eventSyncInfo.exception != null) {
                    throw eventSyncInfo.exception;
                }

                eventHandler.eventTriggeredAction();
            }
        }
    }
}
