package cn.nju.seg.atg.plugin;

import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TypedListener;

import cn.nju.seg.atg.cfg.CfgPath;
import cn.nju.seg.atg.plugin.CfgCEditor.GraphNode;

/**
 * Cfg标尺，此标尺依照{@link LineNumberRulerColumn}修改
 * @author ygsx
 * @time 2014/05/02 21:00
 * */
public class CfgRulerColumn implements IVerticalRulerColumn {
	
	/**
	 * 可视区域监听器
	 * @author ygsx
	 * @time 2014/05/03 09:46
	 */
	class VisiableAreaListener implements IViewportListener, ITextListener {
	
		private boolean fCachedRedrawState = true;
		/** 是否存在提交的绘制线程 */
		//private boolean fIsRunnablePosted = false;
		/** {@link #fIsRunnablePosted}的同步锁 */
		//private Object fRunnableLock = new Object();
		/** 重新绘制任务 *//*
		private Runnable fRedrawRunnable = new Runnable(){
			public void run() {
				synchronized (fRunnableLock) {
					fIsRunnablePosted= false;
				}
				redraw();
			}
		};//*/
		@Override
		public void viewportChanged(int verticalPosition) {
			if (fCachedRedrawState && verticalPosition != fScrollPos)
				redraw();
		}
		@Override
		public void textChanged(TextEvent event) {

			fCachedRedrawState = event.getViewerRedrawState();
			if (!fCachedRedrawState)
				return;

			layout(event.getViewerRedrawState());
			/*
			if (updateNumberOfDigits()) {
				layout(event.getViewerRedrawState());
				return;
			}

			boolean viewerCompletelyShown = 
					JFaceTextUtil.isShowingEntireContents(fCachedTextWidget);
			if (viewerCompletelyShown || fSensitiveToTextChanges 
					|| event.getDocumentEvent() == null){
				if (fCanvas != null && !fCanvas.isDisposed()) {
					Display display = fCanvas.getDisplay();
					if (display != null) {
						synchronized (fRunnableLock) {
							if (fIsRunnablePosted)
								return;
							fIsRunnablePosted= true;
						}
						display.asyncExec(fRedrawRunnable);
					}
				}
			}
			fSensitiveToTextChanges = viewerCompletelyShown;
			//*/
		}
	}

	/**
	 * 行高委托类，用于获取指定行的高度
	 * */
	public class LineHeightDelegate {
		/**
		 * 获取指定行的高度
		 * @param line 指定的行
		 * @return 行高度，以像素为单位
		 * */
		public int getLineHeight(int line){
			int widgetLine = JFaceTextUtil.modelLineToWidgetLine(
					fCachedTextViewer, line);
			if (widgetLine == -1) return -100;
			
			return fCachedTextWidget.getLineHeight(
					fCachedTextWidget.getOffsetAtLine(widgetLine));
		}
	}
	
	
	CfgPaintingPanel fPaintingPanel = new CfgPaintingPanel();
	LineHeightDelegate fLineHeight = new LineHeightDelegate();
	
	
	/**
	 * 在Mac机上“new GC(canvas)”开销较大
	 */
	private static final boolean IS_MAC= Util.isMac();

	/** Cached text viewer */
	private ITextViewer fCachedTextViewer;
	/** Cached text widget */
	private StyledText fCachedTextWidget;
	/** The columns canvas */
	private Canvas fCanvas;
	/** Cache for the actual scroll position in pixels */
	private int fScrollPos;
	/** The drawable for double buffering */
	private Image doubleBufferImg;
	/** The internal listener */
	private VisiableAreaListener fVsAreaListener = new VisiableAreaListener();
	/** The font of this column */
	private Font fFont;

	/** Indicates whether this column reacts on text change events */
	//private boolean fSensitiveToTextChanges= false;
	/** The foreground color */
	private Color fFgColor;
	/** The background color */
	private Color fBgColor;
	/** Cached number of displayed digits */
	//private int fCachedNumberOfDigits = -1;
	/** Flag indicating whether a relayout is required */
	private boolean fRelayoutRequired = false;

	/** 根节点 */
	private GraphNode fRoot;
	
	/*default*/void setGraphRoot(GraphNode root){
		this.fRoot = root;
	}
	
	@Override
	public void setModel(IAnnotationModel model) { /*do nothing*/ }

	@Override
	public Control getControl() {
		return fCanvas;
	}

	@Override
	public int getWidth() {
		return 180;
	}

	@Override
	public void setFont(Font font) {
		fFont= font;
		if (fCanvas == null || fCanvas.isDisposed()) return;
		fCanvas.setFont(fFont);
		//updateNumberOfDigits();
	}

	//*
	@Override
	public void redraw() {

		if (fRelayoutRequired) {
			layout(true);
			return;
		}

		if (fCachedTextViewer != null && fCanvas != null && !fCanvas.isDisposed()) {
			if (IS_MAC) {
				fCanvas.redraw();
				fCanvas.update();
			} else {
				GC gc= new GC(fCanvas);
				doubleBufferPaint(gc);
				gc.dispose();
			}
		}
	}
	

	public Control createControl(CompositeRuler parentRuler, Composite parentControl) {

		fCachedTextViewer= parentRuler.getTextViewer();
		fCachedTextWidget= fCachedTextViewer.getTextWidget();

		fCanvas = new Canvas(parentControl, SWT.NO_FOCUS ) {
 			public void addMouseListener(MouseListener listener) {
 				TypedListener typedListener= null;
				if (listener != null)
					typedListener= new TypedListener(listener);
				addListener(SWT.MouseDoubleClick, typedListener);
			}
		};
		fCanvas.setBackground(getBgColor(fCanvas.getDisplay()));
		fCanvas.setForeground(fFgColor);

		fCanvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				if (fCachedTextViewer != null)
					doubleBufferPaint(event.gc);
			}
		});

		fCanvas.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {

				if (fCachedTextViewer != null) {
					fCachedTextViewer.removeViewportListener(fVsAreaListener);
					fCachedTextViewer.removeTextListener(fVsAreaListener);
				}

				if (doubleBufferImg != null) {
					doubleBufferImg.dispose();
					doubleBufferImg= null;
				}
				fCachedTextViewer= null;
				fCachedTextWidget= null;
			}
		});

		if (fCachedTextViewer != null) {

			fCachedTextViewer.addViewportListener(fVsAreaListener);
			fCachedTextViewer.addTextListener(fVsAreaListener);

			if (fFont == null) {
				if (fCachedTextWidget != null && !fCachedTextWidget.isDisposed())
					fFont= fCachedTextWidget.getFont();
			}
		}

		if (fFont != null)
			fCanvas.setFont(fFont);

		//updateNumberOfDigits();
		return fCanvas;
	}

	/**
	 * 设置背景色
	 * @param background 背景色
	 */
	public void setBgColor(Color background) {
		fBgColor = background;
		if (fCanvas != null && !fCanvas.isDisposed())
			fCanvas.setBackground(fBgColor);
	}
	/** 获取背景色
	 * @param display 显示对象
	 * @return 背景色
	 *  */
	public Color getBgColor(Display display) {
		if (fBgColor == null)
			return display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		return fBgColor;
	}

	/**
	 * 设置执行路径
	 * @param path 执行路径
	 * */
	public void setExecPath(CfgPath path){
		if(fPaintingPanel != null){
			fPaintingPanel.setFocusPath(path);
		}
	}

	/**
	 * 双缓冲绘图
	 * @param dest 要绘制的图形上下文
	 */
	private void doubleBufferPaint(GC dest) {

		Point size= fCanvas.getSize();

		if (size.x <= 0 || size.y <= 0)
			return;

		if (doubleBufferImg != null) {
			//如果缓冲区的尺寸与当前尺寸不一致，那么销毁之，并重新生成
			Rectangle r= doubleBufferImg.getBounds();
			if (r.width != size.x || r.height != size.y) {
				doubleBufferImg.dispose();
				doubleBufferImg= null;
			}
		}
		if (doubleBufferImg == null)
			doubleBufferImg= new Image(fCanvas.getDisplay(), size.x, size.y);

		GC gc= new GC(doubleBufferImg);
		gc.setFont(fCanvas.getFont());
		if (fFgColor != null)
			gc.setForeground(fFgColor);

		try {
			gc.setBackground(getBgColor(fCanvas.getDisplay()));
			gc.fillRectangle(0, 0, size.x, size.y);

			ILineRange visibleLines = JFaceTextUtil.getVisibleModelLines(fCachedTextViewer);
			if (visibleLines == null)
				return;
			fScrollPos = fCachedTextWidget.getTopPixel();
			doPaint(gc, visibleLines);
		} finally {
			gc.dispose();
		}

		dest.drawImage(doubleBufferImg, 0, 0);
	}




	/**
	 * 绘制标尺
	 *
	 * @param gc 被绘制的图形上下文
	 * @param vsblRange 可视区域
	 */
	protected void doPaint(GC gc, ILineRange visibleRange) {
		if(fPaintingPanel != null){
			int y = -JFaceTextUtil.getHiddenTopLinePixels(
					fCachedTextWidget);
			fPaintingPanel.paint(gc, fRoot, visibleRange, fLineHeight, y);
		}

	}


	/**
	 * Layouts the enclosing viewer to adapt the layout to changes of the
	 * size of the individual components.
	 *
	 * @param redraw <code>true</code> if this column can be redrawn
	 */
	protected void layout(boolean redraw) {
		if (!redraw) {
			fRelayoutRequired= true;
			return;
		}

		fRelayoutRequired= false;
		if (fCachedTextViewer instanceof ITextViewerExtension) {
			ITextViewerExtension extension= (ITextViewerExtension) fCachedTextViewer;
			Control control= extension.getControl();
			if (control instanceof Composite && !control.isDisposed()) {
				Composite composite = (Composite) control;
				composite.layout(true);
			}
		}
	}

}

