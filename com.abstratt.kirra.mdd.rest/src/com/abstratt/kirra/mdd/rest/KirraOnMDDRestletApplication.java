package com.abstratt.kirra.mdd.rest;

import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.service.LogService;

public class KirraOnMDDRestletApplication extends JaxRsApplication {
	public static String ID = KirraOnMDDRestletApplication.class.getPackage().getName();
	private KirraStatusService customStatusService;
	private LogService customLogService;
	private Component component;
	
	
	public KirraOnMDDRestletApplication(Component component) {
		customStatusService = new KirraStatusService();
		customLogService = new LogService(false);
		this.component = component;
        this.component.setStatusService(customStatusService);
        this.component.setLogService(customLogService);
		this.getServices().remove(this.getStatusService());
		this.setStatusService(customStatusService);
	}
	
	public Restlet createInboundRoot() {
		return new KirraRepositoryFilter(super.createInboundRoot()) {
			@Override
			protected String getWorkspace(Request request) {
				String workspace = request.getResourceRef().getSegments().get(2);
				return workspace;
			}
		};
	}
}
