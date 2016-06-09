package com.microsoft.azure.hdinsight.common2;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azure.hdinsight.Activator;
import com.microsoft.azure.hdinsight.SparkSubmissionToolWindowView;
import com.microsoft.tooling.msservices.helpers.NotNull;

import com.microsoft.azure.hdinsight.common.MessageInfoType;
import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;

public class HDInsightUtil {
	private static SparkSubmissionToolWindowView sparkSubmissionToolWindowView;
	
	public static SparkSubmissionToolWindowView getSparkSubmissionToolWindowView() {
		if (sparkSubmissionToolWindowView == null) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						sparkSubmissionToolWindowView = (SparkSubmissionToolWindowView) PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage()
								.showView("com.microsoft.azure.hdinsight.sparksubmissiontoolwindowview");
					} catch (PartInitException e) {
						Activator.getDefault().log(e.getMessage(), e);
					}
				}
			});
		}
		return sparkSubmissionToolWindowView;
	}
	
	public static synchronized void setHyperLinkWithText(final String text, final String hyperlinkUrl, final String anchorText) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				getSparkSubmissionToolWindowView().setHyperLinkWithText(text, hyperlinkUrl, anchorText);
			}
		});
	}
	
    public static void showInfoOnSubmissionMessageWindow(/*@NotNull */final String message, boolean isNeedClear) {
        showInfoOnSubmissionMessageWindow(Info, message, isNeedClear);
    }

    public static void showInfoOnSubmissionMessageWindow(@NotNull final String message) {
        showInfoOnSubmissionMessageWindow(Info, message, false);
    }

    public static void showErrorMessageOnSubmissionMessageWindow(@NotNull final String message) {
        showInfoOnSubmissionMessageWindow(Error, message, false);
    }

    public static void showWarningMessageOnSubmissionMessageWindow(@NotNull final String message) {
        showInfoOnSubmissionMessageWindow(Warning, message, false);
    }

    private static void showInfoOnSubmissionMessageWindow(@NotNull final MessageInfoType type, @NotNull final String message, @NotNull final boolean isNeedClear) {
//        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);

//        if (!toolWindow.isVisible()) {
//             synchronized (LOCK) {
//                if (!toolWindow.isVisible()) {
//                    if (ApplicationManager.getApplication().isDispatchThread()) {
//                        toolWindow.show(null);
//                    } else {
//                        try {
                            
//                        	SparkSubmissionToolWindowView sparkSubmissionview = null;
//							try {
//								sparkSubmissionview = (SparkSubmissionToolWindowView) PlatformUI
//												.getWorkbench().getActiveWorkbenchWindow()
//												.getActivePage().showView("com.microsoft.azure.hdinsight.sparksubmissiontoolwindowview");
//							} catch (PartInitException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
                            
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        }
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				showSubmissionMessage(getSparkSubmissionToolWindowView(), message, type, isNeedClear);
			}
		});
	}

    private static void showSubmissionMessage(SparkSubmissionToolWindowView sparkSubmissionView, @NotNull String message, @NotNull MessageInfoType type, @NotNull final boolean isNeedClear) {
        if (isNeedClear) {
            sparkSubmissionView.clearAll();
        }

        switch (type) {
            case Error:
                sparkSubmissionView.setError(message);
                break;
            case Info:
                sparkSubmissionView.setInfo(message);
                break;
            case Warning:
                sparkSubmissionView.setWarning(message);
                break;
        }
    }
}
