package cn.nju.seg.atg.plugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import cn.nju.seg.atg.cppmanager.CppManager;


/**
 * 配置初始化类
 * @author ygsx
 * @time 2014/05/08 21:31
 * */
public class AtgPreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = AtgActivator.getDefault().getPreferenceStore();/*
		store.setDefault(PreferenceConstants.P_BOOLEAN, true);
		store.setDefault(PreferenceConstants.P_CHOICE, "choice2");
		store.setDefault(PreferenceConstants.P_STRING, "Default value");//*/
		
		store.setDefault(PreferenceConstants.NR_DETECT, Integer.toString(300));
		store.setDefault(PreferenceConstants.NR_CIRCLE, Integer.toString(5));
		store.setDefault(PreferenceConstants.CMD_COMPILE, 
				CppManager.DEFAULT_CMD_COMPILE);
	}

}
