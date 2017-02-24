package com.microsoft.azuretools.eclipse.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.utils.IProgressTaskImpl;
import com.microsoft.azuretools.utils.IWorker;

public class ProgressTaskModal implements IProgressTaskImpl {
	private Shell parentShell;
	
	public ProgressTaskModal(Shell parentShell) {
		this.parentShell = parentShell;
	}

	@Override
	public void doWork(IWorker worker) {
		// TODO Auto-generated method stub
		IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) {
                monitor.beginTask(worker.getName(), IProgressMonitor.UNKNOWN);
                worker.work(new UpdateProgressIndicator(monitor));
                monitor.done();
            }
        };
        try {
        	ProgressDialog.get(parentShell, worker.getName()).run(true, true, op);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 		
	}
}
