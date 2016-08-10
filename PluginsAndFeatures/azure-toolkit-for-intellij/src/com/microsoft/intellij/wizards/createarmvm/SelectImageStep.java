/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.wizards.createarmvm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachineOffer;
import com.microsoft.azure.management.compute.VirtualMachinePublisher;
import com.microsoft.azure.management.compute.VirtualMachineSku;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SelectImageStep extends WizardStep<CreateVMWizardModel> {
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JComboBox regionComboBox;

    private JList imageLabelList;
    private JEditorPane imageDescriptionTextPane;
    private JComboBox publisherComboBox;
    private JComboBox offerComboBox;
    private JComboBox skuComboBox;
    private JPanel imageInfoPanel;

    private CreateVMWizardModel model;

    private void createUIComponents() {
        imageInfoPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {

                double height = 0;
                for (Component component : this.getComponents()) {
                    height += component.getHeight();
                }

                Dimension preferredSize = super.getPreferredSize();
                preferredSize.setSize(preferredSize.getWidth(), height);
                return preferredSize;
            }
        };
    }

    List<VirtualMachineImage> virtualMachineImages;
    private Project project;

    public SelectImageStep(final CreateVMWizardModel model, Project project) {
        super("Select a Virtual Machine Image", null, null);

        this.model = model;
        this.project = project;

        model.configStepList(createVmStepsList, 1);

//        regionComboBox.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                fillPublishers();
//                model.setRegion((Region) regionComboBox.getSelectedItem());
//            }
//        });
//
//        regionComboBox.setModel(new DefaultComboBoxModel(Region.values()));
//        regionComboBox.setSelectedIndex(0);

        publisherComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof VirtualMachinePublisher) {
                    setText(((VirtualMachinePublisher) o).name());
                }
            }
        });

        publisherComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillOffers();
                }
            }
        });

        offerComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof VirtualMachineOffer) {
                    setText(((VirtualMachineOffer) o).name());
                }
            }
        });

        offerComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                fillSkus();
            }
        });

        skuComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof VirtualMachineSku) {
                    setText(((VirtualMachineSku) o).name());
                }
            }
        });

        skuComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                fillImages();
            }
        });

        imageLabelList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
                String cellValue = o.toString();

                if (o instanceof VirtualMachineImage) {
                    VirtualMachineImage virtualMachineImage = (VirtualMachineImage) o;
                    cellValue = virtualMachineImage.sku();
                }

                this.setToolTipText(cellValue);
                return super.getListCellRendererComponent(jList, cellValue, i, b, b1);
            }
        });

        imageLabelList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                VirtualMachineImage virtualMachineImage = (VirtualMachineImage) imageLabelList.getSelectedValue();
                model.setVirtualMachineImage(virtualMachineImage);

                if (virtualMachineImage != null) {
                    model.getCurrentNavigationState().NEXT.setEnabled(true);
                }
            }
        });
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        regionComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    selectRegion();
                }
            }
        });

        regionComboBox.setModel(new DefaultComboBoxModel(Region.values()));
        selectRegion();

        if (virtualMachineImages == null) {
            model.getCurrentNavigationState().NEXT.setEnabled(false);

            imageLabelList.setListData(new String[]{"loading..."});
            imageLabelList.setEnabled(false);
        }

        return rootPanel;
    }

    private void selectRegion() {
        fillPublishers();
        model.setRegion((Region) regionComboBox.getSelectedItem());
    }

    private void fillPublishers() {
        model.getCurrentNavigationState().NEXT.setEnabled(false);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading publishers...", false) {
            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    final List<VirtualMachinePublisher> publishers = AzureArmManagerImpl.getManager(project)
                            .getVirtualMachinePublishers(model.getSubscription().getId(), (Region) regionComboBox.getSelectedItem());
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            publisherComboBox.setModel(new DefaultComboBoxModel(publishers.toArray()));
                            fillOffers();
                        }
                    });
                } catch (AzureCmdException e) {
                    String msg = "An error occurred while attempting to retrieve images list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                }
            }
        });
    }

    private void fillOffers() {
        model.getCurrentNavigationState().NEXT.setEnabled(false);

        skuComboBox.setEnabled(true);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading offers...", false) {
            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    final List<VirtualMachineOffer> offers = ((VirtualMachinePublisher) publisherComboBox.getSelectedItem()).offers().list();
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            offerComboBox.setModel(new DefaultComboBoxModel(offers.toArray()));
                            fillSkus();
                        }
                    });
                } catch (CloudException | IOException e) {
                    String msg = "An error occurred while attempting to retrieve offers list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                }
            }
        });
    }

    private void fillSkus() {
        model.getCurrentNavigationState().NEXT.setEnabled(false);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading skus...", false) {
            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    final List<VirtualMachineSku> skus = ((VirtualMachineOffer) offerComboBox.getSelectedItem()).skus().list();
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            skuComboBox.setModel(new DefaultComboBoxModel(skus.toArray()));
                            fillImages();
                        }
                    });
                } catch (CloudException | IOException e) {
                    String msg = "An error occurred while attempting to retrieve skus list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                }
            }
        });
    }

    private void fillImages() {
        model.getCurrentNavigationState().NEXT.setEnabled(false);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading images...", false) {
            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                final List<VirtualMachineImage> images = new ArrayList<VirtualMachineImage>();
                try {
                    VirtualMachineSku sku = (VirtualMachineSku) skuComboBox.getSelectedItem();
                    List<VirtualMachineImage> skuImages = sku.images().list();
                    images.addAll(skuImages);
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            imageLabelList.setListData(images.toArray());
                            imageLabelList.setEnabled(true);
                        }
                    });
                } catch (CloudException | IOException e) {
                    String msg = "An error occurred while attempting to retrieve images list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                }
            }
        });
    }
}
