package com.ustadmobile.port.gwt.client;

import com.ustadmobile.port.gwt.client.impl.ClientFactory;
import com.ustadmobile.port.gwt.client.impl.MyActivityMapper;
import com.ustadmobile.port.gwt.client.impl.MyHistoryMapper;
import com.ustadmobile.port.gwt.client.view.StartPlace;
import com.ustadmobile.port.gwt.shared.FieldVerifier;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
//import com.google.gwt.event.shared.EventBus;
import com.google.web.bindery.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class App_gwt implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network " + 
			"connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private final GreetingServiceAsync greetingService = 
			GWT.create(GreetingService.class);

	
	private StartPlace startPlace = new StartPlace("start");
	private SimplePanel appPanel = new SimplePanel();
	
	public void onModuleLoad(){
		
		/* Move this to system impl.
		 *  Just testing it here for now*/
		ClientFactory clientFactory = GWT.create(ClientFactory.class);
		PlaceController placeController = clientFactory.getPlaceController();
		
		//HandlerManager eventBus = new HandlerManager(null);
		EventBus eventBus = clientFactory.getEventBus();
		ActivityMapper activityMapper = new MyActivityMapper(clientFactory);
		ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);
		activityManager.setDisplay(appPanel);
		
		MyHistoryMapper historyMapper = GWT.create(MyHistoryMapper.class);
		final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
		historyHandler.register(placeController, eventBus, startPlace);
		
		RootPanel.get().add(appPanel);;
		historyHandler.handleCurrentHistory();
	}
	
	
	
}
