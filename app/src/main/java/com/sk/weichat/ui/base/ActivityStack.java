package com.sk.weichat.ui.base;

import java.util.Stack;

import android.app.Activity;

public class ActivityStack {
	private Stack<Activity> stack;
	private static ActivityStack instance;

	private ActivityStack() {
		stack = new Stack<Activity>();
	}

	public static ActivityStack getInstance() {
		if (instance == null) {
			synchronized (ActivityStack.class) {
				if (instance == null) {
					instance = new ActivityStack();
				}
			}
		}
		return instance;
	}

	public void pop(Activity activity) {
		if (activity != null) {
			stack.remove(activity);
		}
	}

	public void push(Activity activity) {
		stack.add(activity);
	}

	public boolean has() {
		return stack.size() > 0;
	}

	public void exit() {
		for (int i = 0; i < stack.size(); i++) {
			Activity activity = stack.get(i);
			stack.remove(i);
			i--;
			if (activity != null) {
				activity.finish();
				activity = null;
			}
		}
	}
	// public void popAllActivityExceptOne(Class cls) {
	// while (true) {
	// Activity activity = currentActivity();
	// if (activity == null) {
	// break;
	// }
	// if (activity.getClass().equals(cls)) {
	// break;
	// }
	// popActivity(activity);
	// }
	// }
	//
	// public void popActivity() {
	// Activity activity = activityStack.lastElement();
	// if (activity != null) {
	// activity.finish();
	// activity = null;
	// }
	// }
	//
	// private Activity currentActivity() {
	// Activity activity = activityStack.lastElement();
	// return activity;
	// }
}
