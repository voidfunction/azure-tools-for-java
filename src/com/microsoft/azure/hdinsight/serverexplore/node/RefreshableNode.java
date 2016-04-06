package com.microsoft.azure.hdinsight.serverexplore.node;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import com.microsoft.azure.hdinsight.common.DefaultLoader;

import java.util.List;

public abstract class RefreshableNode extends Node {
    public RefreshableNode(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public RefreshableNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    @Override
    protected void loadActions() {
        addAction("Refresh", new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                load();
            }
        });

        super.loadActions();
    }

    // Sub-classes are expected to override this method if they wish to
    // refresh items synchronously. The default implementation does nothing.
    protected abstract void refreshItems() throws HDExploreException;

    // Sub-classes are expected to override this method if they wish
    // to refresh items asynchronously. The default implementation simply
    // delegates to "refreshItems" *synchronously* and completes the Future
    // with the result of calling getChildNodes.
    protected void refreshItems(SettableFuture<List<Node>> future) {
        setLoading(true);
        try {
            refreshItems();
            future.set(getChildNodes());
        } catch (HDExploreException e) {
            future.setException(e);
        } finally {
            setLoading(false);
        }
    }

    public ListenableFuture<List<Node>> load() {
        final RefreshableNode node = this;
        final SettableFuture<List<Node>> future = SettableFuture.create();

        DefaultLoader.getIdeHelper().runInBackground(getProject(), "Loading " + getName() + "...", false, true, null,
                new Runnable() {
                    @Override
                    public void run() {
                        final String nodeName = node.getName();
                        node.setName(nodeName + " (Refreshing...)");

                        Futures.addCallback(future, new FutureCallback<List<Node>>() {
                            @Override
                            public void onSuccess(List<Node> nodes) {
                                updateName(null);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                updateName(throwable);
                            }

                            private void updateName(final Throwable throwable) {
                                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (throwable != null) {
                                            node.setName(String.format("%s (Error)",nodeName));
                                        }else {
                                            node.setName(nodeName);
                                        }
                                    }
                                });
                            }
                        });

                        node.refreshItems(future);
                    }
                }
        );

        return future;
    }
}

