package com.denisk.appengine.nl.client.ui.views;

import java.util.List;

import com.denisk.appengine.nl.client.overlay.CategoryJavascriptObject;
import com.denisk.appengine.nl.client.overlay.ShopItem;
import com.denisk.appengine.nl.client.service.DtoService;
import com.denisk.appengine.nl.client.service.DtoServiceAsync;
import com.denisk.appengine.nl.client.thirdparty.com.reveregroup.carousel.client.Photo;
import com.denisk.appengine.nl.client.util.Function;
import com.denisk.appengine.nl.shared.UserStatus;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class Nl implements EntryPoint {
	private static final String CATEGORY_URL_PREFIX = "category/";
	private static final String GOOD_URL_PREFIX = "good/";

	private static DtoServiceAsync dtoService = GWT.create(DtoService.class);

	// hidden
	private Label categoriesInfo = new Label();

	private final FlowPanel outputPanel = new FlowPanel();
	private final Label status = new Label();
	private final RootPanel rootPanel = RootPanel.get("container");

	private final RootPanel buttonsContainer = RootPanel
			.get("buttonsContainer");
	private final RootPanel loginContainer = RootPanel.get("loginContainer");

	private HandlerRegistration newButtonClickHandlerRegistration;
	private HandlerRegistration clearButtonHandlerRegistration;

	// disabled
	private Button clearButton;
	private Button newButton;
	private Button backButton;

	private HTML loginUrl;
	private HTML logoutUrl;

	private RootPanel busyIndicator;

	// state fields
	private String selectedCategoryKeyStr;
	private AbstractItemsView currentView;

	// views
	private CategoriesView categoriesView;
	private GoodsView goodsView;

	private void createLogoutUrl() {
		dtoService.getLogoutUrl(new AsyncCallback<String>() {
			@Override
			public void onSuccess(String result) {
				logoutUrl = new HTML();
				logoutUrl.setHTML("<a href='" + result + "'>Logout</a>");
				logoutUrl.setVisible(false);
				loginContainer.add(logoutUrl);
			}

			@Override
			public void onFailure(Throwable caught) {
			}
		});
	}

	private void createLoginUrl() {
		dtoService.getLoginUrl(new AsyncCallback<String>() {
			@Override
			public void onSuccess(String result) {
				loginUrl = new HTML();
				loginUrl.setHTML("<a href='" + result + "'>Login</a>");
				loginUrl.setVisible(false);
				loginContainer.add(loginUrl);
			}

			@Override
			public void onFailure(Throwable caught) {
			}
		});
	}

	private void setAdminButtonHandlers() {
		if (newButtonClickHandlerRegistration != null) {
			newButtonClickHandlerRegistration.removeHandler();
		}

		if (newButton != null) {
			newButtonClickHandlerRegistration = newButton
					.addClickHandler(currentView.getNewItemHandler());
		}
		if (clearButtonHandlerRegistration != null) {
			clearButtonHandlerRegistration.removeHandler();
		}
		if (clearButton != null) {
			clearButtonHandlerRegistration = clearButton
					.addClickHandler(currentView.getClearAllHandler());
		}
	}

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		categoriesInfo.setVisible(false);
		updateLabel();

		rootPanel.add(status);
		rootPanel.add(categoriesInfo);
		rootPanel.add(outputPanel);

		clearButton = new Button("Clear all");
		clearButton.setEnabled(false);
		clearButton.setVisible(false);

		newButton = new Button("New item");
		newButton.setVisible(false);
		buttonsContainer.add(clearButton);
		buttonsContainer.add(newButton);

		outputPanel.addStyleName("outputPanel");
		backButton = new Button("Back");
		backButton.setVisible(false);
		buttonsContainer.add(backButton);

		busyIndicator = RootPanel.get("busyIndicator");

		createLoginUrl();
		createLogoutUrl();

		categoriesView = new CategoriesView(this);
		goodsView = new GoodsView(this);

		History.addValueChangeHandler(new ValueChangeHandler<String>() {

			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				String token = event.getValue();
				if (token == null || token.isEmpty()) {
					switchToCategoriesView();
					renderView(null);
					return;
				}
				String categoryKeyRegexp;
				Function<List<Photo>, Void> callback = null;
				if (token.startsWith(CATEGORY_URL_PREFIX)
						&& !token.contains(GOOD_URL_PREFIX)) {
					categoryKeyRegexp = CATEGORY_URL_PREFIX + "(.+)/";
				} else if (token.startsWith(CATEGORY_URL_PREFIX)
						&& token.contains(GOOD_URL_PREFIX)) {
					categoryKeyRegexp = CATEGORY_URL_PREFIX + "(.+)/good";

					RegExp goodRegexp = RegExp.compile(".+" + GOOD_URL_PREFIX
							+ "(.+)/");
					MatchResult goodMatch = goodRegexp.exec(token);
					if (goodMatch == null) {
						Window.alert("Wrong format for good in URL, should be '"
								+ GOOD_URL_PREFIX + "'");
						History.newItem("", false);
						switchToCategoriesView();
						renderView(callback);
						return;
					}
					final String goodKey = goodMatch.getGroup(1);

					callback = new Function<List<Photo>, Void>() {
						@Override
						public Void apply(List<Photo> input) {
							// pop single good window
							for (Photo photo : input) {
								if (photo.getId().equals(goodKey)) {
									goodsView.selectPhoto(photo);
								}
							}
							return null;
						}
					};
				} else {
					Window.alert("URL must start with '" + CATEGORY_URL_PREFIX
							+ "' token");
					History.newItem("", false);
					switchToCategoriesView();
					renderView(callback);
					return;
				}

				RegExp p = RegExp.compile(categoryKeyRegexp);
				MatchResult m = p.exec(token);
				if (m == null) {
					Window.alert("There is no '" + CATEGORY_URL_PREFIX
							+ " in the URL provided");
					switchToCategoriesView();
					renderView(callback);
					return;
				}
				final String categoryKey = m.getGroup(1);
				setSelectedCategoryKeyStr(categoryKey);
				switchToGoodsView();
				renderView(callback);
				//set background
				dtoService.getCategoryBackgroundKey(categoryKey, new AsyncCallback<String>() {
					
					@Override
					public void onSuccess(String result) {
						//show create and show background
						categoriesView.createAndSetupBackground(result).getElement().getStyle().setOpacity(1);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						Window.alert("Can't get background for category " + categoryKey);
					}
				});
				
			}	
		});

		History.fireCurrentHistoryState();
	}

	public void renderView(Function<?, ?> callback) {
		currentView.render(outputPanel, callback);
		outputCommonControls();
	}

	public void outputCommonControls() {
		dtoService.isAdmin(new AsyncCallback<UserStatus>() {
			@Override
			public void onSuccess(UserStatus userStatus) {
				switch (userStatus) {
				case ADMIN:
					logoutUrl.setVisible(true);
					loginUrl.setVisible(false);

					newButton.setVisible(true);
					clearButton.setVisible(true);

					setAdminButtonHandlers();
					break;
				case NOT_LOGGED_IN:
					logoutUrl.setVisible(false);
					loginUrl.setVisible(true);

					newButton.setVisible(false);
					newButton.setVisible(false);
					break;
				case NOT_ADMIN:
					logoutUrl.setVisible(true);
					loginUrl.setVisible(false);

					newButton.setVisible(false);
					newButton.setVisible(false);
					break;
				}
			}

			@Override
			public void onFailure(Throwable caught) {
			}
		});
	}

	/**
	 * Calculates total items count and updates corresponding label This is
	 * disabled
	 */
	public void updateLabel() {
		/*
		 * dtoService.countEntities(new AsyncCallback<String>() {
		 * 
		 * @Override public void onSuccess(String result) {
		 * status.setText(result); }
		 * 
		 * @Override public void onFailure(Throwable caught) {
		 * status.setText("Can't calculate entities"); } });
		 */
	}

	public void switchToCategoriesView() {
		currentView = categoriesView;
		setAdminButtonHandlers();
		backButton.setVisible(false);
		// this clears everything in the URL starting from '#' inclusive
		History.newItem("", false);

	}

	public void switchToGoodsView() {
		goodsView.getEditGoodForm().setParentCategoryItemKeyStr(
				selectedCategoryKeyStr);
		backButton.setVisible(true);
		this.currentView = goodsView;
	}

	public void showBusyIndicator() {
		this.busyIndicator.setVisible(true);
	}

	public void hideBusyIndicator() {
		this.busyIndicator.setVisible(false);
	}

	public static String getCategoryURLPart(String categoryKeyStr) {
		return CATEGORY_URL_PREFIX + categoryKeyStr + "/";
	}

	public static String getGoodURLPart(String goodKeyStr) {
		return GOOD_URL_PREFIX + goodKeyStr + "/";
	}

	public String getSelectedCategoryKeyStr() {
		return selectedCategoryKeyStr;
	}

	public void setSelectedCategoryKeyStr(String selectedCategoryKeyStr) {
		this.selectedCategoryKeyStr = selectedCategoryKeyStr;
	}

	public DtoServiceAsync getDtoService() {
		return dtoService;
	}

	public FlowPanel getOutputPanel() {
		return outputPanel;
	}

	public Button getBackButton() {
		return backButton;
	}
}
