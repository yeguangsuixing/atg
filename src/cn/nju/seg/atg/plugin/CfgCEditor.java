package cn.nju.seg.atg.plugin;

import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings("restriction")
public class CfgCEditor extends CEditor {

	CfgRulerColumn fCfgRulerColumn;
	
	/**
	 * 重写{@link CEditor#createSourceViewer}，添加Cfg标尺
	 * */
	@Override
	protected ISourceViewer createSourceViewer(Composite parent, 
			IVerticalRuler ruler, int styles) {
		/* 添加Cfg标尺 */
		if(ruler instanceof CompositeRuler){
			fCfgRulerColumn = new CfgRulerColumn();
			CompositeRuler comruler = (CompositeRuler)ruler;
			comruler.addDecorator(0, fCfgRulerColumn);
		}
		return super.createSourceViewer(parent, ruler, styles);
	}
	
	/** 设置cfg画笔 */
	public void setCfgPaintingPanel(CfgRulerColumn.ICfgPaintingPanel panel){
		if(fCfgRulerColumn != null){
			fCfgRulerColumn.fPaintingPanel = panel;
		}
	}
	
	/** 获取cfg画笔 */
	public CfgRulerColumn.ICfgPaintingPanel getCfgPaintingPanel(){
		if(fCfgRulerColumn == null)
			return null;
		return fCfgRulerColumn.fPaintingPanel;
	}
}




