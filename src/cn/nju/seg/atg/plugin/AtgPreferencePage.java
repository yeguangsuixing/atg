package cn.nju.seg.atg.plugin;

import org.eclipse.cdt.ui.newui.MultiLineTextFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
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
		addField( new IntegerFieldEditor(PreferenceConstants.NR_CIRCLE, 
				"Number of &Cycle:", getFieldEditorParent()));
		addField( new IntegerFieldEditor(PreferenceConstants.NR_DETECT, 
				"Number of D&etect:", getFieldEditorParent()));
		addField( new MultiLineTextFieldEditor(PreferenceConstants.CMD_COMPILE, 
				"Co&mpiling Command:", getFieldEditorParent()));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) { }

}



