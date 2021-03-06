package org.magic.gui.components.dialog;

import java.awt.BorderLayout;
import java.lang.management.ManagementFactory;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import org.jdesktop.swingx.JXTable;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.components.JVMemoryPanel;
import org.magic.gui.models.ThreadsTableModel;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.ThreadManager;
import org.magic.tools.UITools;

public class ThreadMonitor extends MTGUIComponent  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JXTable tableT;
	private ThreadsTableModel modelT;
	private JButton btnRefresh;
	private Timer t;
	private JLabel lblThreads;
	private JVMemoryPanel memoryPanel;
	private JButton btnRunGC;
	
	
	public ThreadMonitor() {
		setLayout(new BorderLayout(0, 0));
		modelT = new ThreadsTableModel();
		tableT = new JXTable();
		tableT.setModel(modelT);
		
		add(new JScrollPane(tableT), BorderLayout.CENTER);
		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		btnRefresh = new JButton("Pause");
		btnRefresh.addActionListener(ae -> {
			if (t.isRunning()) {
				t.stop();
				btnRefresh.setText(MTGControler.getInstance().getLangService().getCapitalize("START"));
			} else {
				t.start();
				btnRefresh.setText(MTGControler.getInstance().getLangService().getCapitalize("PAUSE"));
				lblThreads.setText(ThreadManager.getInstance().getInfo());

			}
		});
		panel.add(btnRefresh);

		lblThreads = new JLabel(MTGControler.getInstance().getLangService().getCapitalize("THREADS"));
		panel.add(lblThreads);

		memoryPanel = new JVMemoryPanel();
		panel.add(memoryPanel);
		
		btnRunGC = new JButton("Kill");
		btnRunGC.addActionListener(e->{
			Long id = (Long) UITools.getTableSelection(tableT,0).get(0);
			Optional<Thread> opt = Thread.getAllStackTraces().keySet().stream().filter(th->th.getId()==id).findFirst();
			
			if(opt.isPresent())
			{
				logger.debug("killing " + opt.get().getName());
				opt.get().interrupt();
			}
			
		});
		panel.add(btnRunGC);
		
		t = new Timer(5000, e ->{ 
			modelT.init(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true));
			memoryPanel.refresh();
			tableT.packAll();
		});
		
		
		t.start();
	}
	

	@Override
	public String getTitle() {
		return MTGControler.getInstance().getLangService().getCapitalize("THREADS");
	}
	
	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_TAB_ADMIN;
	}
	
}
