package cn.nju.seg.atg.plugin;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * 配置管理类
 * @author ygsx
 * @time 2014/05/08 21:30
 * */
public class AtgPreferencePage extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	
	public AtgPreferencePage() {
		super(GRID);
		setPreferenceStore(AtgActivator.getDefault().getPreferenceStore());
		//setDescription("A demonstration of a preference page implementation");
	}
	
	public void createFieldEditors() {
		/*
		addField(new DirectoryFieldEditor(PreferenceConstants.P_PATH, 
				"&Directory preference:", getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
				PreferenceConstants.P_BOOLEAN,
				"&An example of a boolean preference",
				getFieldEditorParent()));
	
		addField(new RadioGroupFieldEditor(
				PreferenceConstants.P_CHOICE,
			"An example of a multiple-choice preference",
			1,
			new String[][] { { "&Choice 1", "choice1" }, {
				"C&hoice 2", "choice2" }
		}, getFieldEditorParent()));//*/
		addField( new StringFieldEditor(PreferenceConstants.NR_DETECT, 
				"Number of D&etect:", getFieldEditorParent()));
		addField( new StringFieldEditor(PreferenceConstants.NR_CIRCLE, 
				"Number of &Circle:", getFieldEditorParent()));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) { }

}