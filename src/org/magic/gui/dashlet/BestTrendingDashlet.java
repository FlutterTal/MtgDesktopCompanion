package org.magic.gui.dashlet;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableRowSorter;

import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.CardShake;
import org.magic.api.interfaces.abstracts.AbstractDashBoard.FORMAT;
import org.magic.gui.abstracts.AbstractJDashlet;
import org.magic.gui.models.CardsShakerTableModel;
import org.magic.gui.renderer.CardShakeRenderer;
import org.magic.services.MTGControler;
import org.magic.services.ThreadManager;

import javafx.scene.control.Skinnable;
import net.coderazzi.filters.gui.AutoChoices;
import net.coderazzi.filters.gui.TableFilterHeader;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class BestTrendingDashlet extends AbstractJDashlet{

	private JXTable table;
	private CardsShakerTableModel modStandard;
	private JSpinner spinner;
	
	public BestTrendingDashlet() {
		super();
		setTitle(getName());
		setResizable(true);
		setClosable(true);
		setIconifiable(true);
		setMaximizable(true);
		
		
	}
	
	@Override
	public String getName() {
		return "Winners/Loosers";
	}

	

	@Override
	public void init() {
		ThreadManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				
				
				try {
					List<CardShake> shakes = new ArrayList<CardShake>();
							shakes.addAll(MTGControler.getInstance().getEnabledDashBoard().getShakerFor(FORMAT.modern.toString()));
							
					
					Collections.sort(shakes,new Comparator<CardShake>() {

						public int compare(CardShake o1, CardShake o2) {
							if(o1.getPercentDayChange()>o2.getPercentDayChange())
									return -1;
								else
									return 1;
						}
					});
					
					int val = (Integer)spinner.getValue(); 
					save("LIMIT", String.valueOf(val));
					List<CardShake> ret = new ArrayList<CardShake>();
					ret.addAll(shakes.subList(0, val));//X first
					ret.addAll(shakes.subList(shakes.size()-(val+1), shakes.size()-1)); //x last
					
					modStandard.init(ret);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				table.setModel(modStandard);
				table.setRowSorter(new TableRowSorter(modStandard) );
				table.packAll();
				table.getColumnModel().getColumn(3).setCellRenderer(new CardShakeRenderer());
				modStandard.fireTableDataChanged();
				
				
			}
		}, "Init best Dashlet");
		
	}

	@Override
	public void initGUI() {
		JPanel panneauHaut = new JPanel();
		getContentPane().add(panneauHaut, BorderLayout.NORTH);
		
		spinner = new JSpinner();
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				init();
			}
		});
		spinner.setModel(new SpinnerNumberModel(new Integer(5), new Integer(1), null, new Integer(1)));
		panneauHaut.add(spinner);
		
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		modStandard = new CardsShakerTableModel();
		table = new JXTable(modStandard);
		scrollPane.setViewportView(table);
		initToolTip(table);
		
		if(props.size()>0) {
			Rectangle r = new Rectangle((int)Double.parseDouble(props.getProperty("x")), 
										(int)Double.parseDouble(props.getProperty("y")),
										(int)Double.parseDouble(props.getProperty("w")),
										(int)Double.parseDouble(props.getProperty("h")));
			
			try {
				spinner.setValue(Integer.parseInt(props.getProperty("LIMIT","5")));
			} catch (Exception e) {
				//logger.error("can't get LIMIT value",e);
			}
			setBounds(r);
			}
		
		setVisible(true);
	}


}
