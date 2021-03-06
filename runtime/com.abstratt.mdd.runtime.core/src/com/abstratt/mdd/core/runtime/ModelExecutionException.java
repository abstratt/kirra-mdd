package com.abstratt.mdd.core.runtime;

import java.util.List;

import org.eclipse.uml2.uml.NamedElement;

import com.abstratt.mdd.core.runtime.ExecutionContext.CallSite;

public class ModelExecutionException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private RuntimeAction executing;
    private NamedElement context;
    private List<CallSite> callSites;

    public ModelExecutionException(String message, NamedElement context, RuntimeAction executing) {
        this(message, context, executing, Runtime.get().getCurrentContext().getCallSites());
    }
    
    public ModelExecutionException(String message, NamedElement context, RuntimeAction executing, List<CallSite> callSites) {
        super(message);
        this.executing = executing;
        this.context = context;
        this.callSites = callSites;
    }

    
    public List<CallSite> getCallSites() {
        return callSites;
    }

    public NamedElement getContext() {
        return context;
    }

    public RuntimeAction getExecuting() {
        return executing;
    }

    public void setExecuting(RuntimeAction executing) {
        this.executing = executing;
    }

    protected String getContextName() {
        return context == null ? null : context.getQualifiedName();
    }
    
    public Integer getLineNumber() {
        CallSite latestSite = getLatestSite();
        return latestSite == null ? null : latestSite.getLineNumber();
    }

    public String getSourceFile() {
        CallSite latestSite = getLatestSite();
        return latestSite == null ? null : latestSite.getSourceFile();
    }

    private CallSite getLatestSite() {
        return callSites.isEmpty() ? null : callSites.get(0);
    }
    
    @Override
    public String getMessage() {
    	return super.getMessage() + " - " + getLatestSite();
    }
}
