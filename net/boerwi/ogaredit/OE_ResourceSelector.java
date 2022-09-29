package net.boerwi.ogaredit;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;

public class OE_ResourceSelector{
	private OE_DB db;
	public OE_ResourceSelector(OE_DB db){
		assert db != null;
		this.db = db;
	}
	public OE_Resource singleSelect(OE_ResType type){
		Dir root = db.getRootForType(type);
		Node[] ret = select(root, type, false, false);
		assert ret.length <= 1;
		if(ret.length == 1){
			return ((Entry)ret[0]).getData();
		}else{
			return null;
		}
	}
	public OE_Resource[] multiSelect(OE_ResType type){
		Dir root = db.getRootForType(type);
		Node[] selections = select(root, type, true, false);
		OE_Resource[] ret = new OE_Resource[selections.length];
		for(int idx = 0; idx < selections.length; idx++){
			ret[idx] = ((Entry)selections[idx]).getData();
		}
		return ret;
	}
	public Dir selectDirectory(OE_ResType type){
		Dir root = db.getRootForType(type);
		Node[] ret = select(root, type, false, true);
		assert ret.length <= 1;
		if(ret.length == 1){
			return (Dir)(ret[0]);
		}else{
			return null;
		}
	}
	private Node[] select(Dir root, OE_ResType type, boolean multi, boolean dirSelect){
		if(root == null) return null;
		final ArrayList<Node> ret = new ArrayList<>();
		final boolean[] selectPressed = new boolean[]{false};
		JDialog dialog = new JDialog(null, type.toString()+" Selector", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		Container content = dialog.getContentPane();
		content.setLayout(new BorderLayout());
		JPanel buttonPane = new JPanel(new GridLayout(1,2));
		JButton btCancel = new JButton("Cancel");
		JButton btSelect = new JButton("Select");
		ActionListener btListen = new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				Object src = evt.getSource();
				if(src == btCancel){
				}else if(src == btSelect){
					selectPressed[0] = true;
				}else{
					assert false;
				}
				dialog.dispose();
			}
		};
		btCancel.addActionListener(btListen);
		btSelect.addActionListener(btListen);
		btSelect.setEnabled(false);
		buttonPane.add(btCancel);
		buttonPane.add(btSelect);
		content.add(buttonPane, BorderLayout.SOUTH);
		JTree tree = new JTree(new DirSelectorTreeModel(root, dirSelect));
		DefaultTreeSelectionModel treeSelModel = new DefaultTreeSelectionModel();
		treeSelModel.setSelectionMode(multi ? DefaultTreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION : DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setSelectionModel(treeSelModel);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath[] selected = tree.getSelectionPaths();
				ret.clear();
				for(TreePath sel : selected){
					Object f = sel.getLastPathComponent();
					if(f instanceof Entry || dirSelect){
						ret.add( (Node)f );
					}
				}
				btSelect.setEnabled(ret.size() != 0);
			}
		});
		content.add(new JScrollPane(tree));
		dialog.pack();
		dialog.setVisible(true);
		if(selectPressed[0]){
			return ret.toArray(new Node[0]);
		}else{
			return new Node[0];
		}
	}
}
class DirSelectorTreeModel implements TreeModel{
	ArrayList<TreeModelListener> tmListeners = new ArrayList<>();
	Dir root;
	boolean dirOnly;
	DirSelectorTreeModel(Dir root, boolean dirOnly){
		this.root = root;
		this.dirOnly = dirOnly;
	}
	public void addTreeModelListener(TreeModelListener l){
		tmListeners.add(l);
	}
	public Object getChild(Object parent, int index){
		if(dirOnly){
			int dirChildCount = 0;
			for(int cIdx = 0; true; cIdx++){
				Node testChild = ((Dir)parent).getChild(cIdx);
				if(testChild instanceof Dir){
					if(index == dirChildCount){
						return testChild;
					}
					dirChildCount++;
				}
			}
		}else{
			return ((Dir)parent).getChild(index);
		}
	}
	public int getChildCount(Object parent){
		if(parent instanceof Dir){
			int realCCount = ((Dir)parent).getChildCount();
			if(dirOnly){
				int dirChildCount = 0;
				for(int cIdx = 0; cIdx < realCCount; cIdx++){
					if(((Dir)parent).getChild(cIdx) instanceof Dir){
						dirChildCount++;
					}
				}
				return dirChildCount;
			}else{
				return realCCount;
			}
		}else{
			return 0;
		}
	}
	public int getIndexOfChild(Object parent, Object child){
		if(dirOnly){
			int dirChildCount = 0;
			for(int cIdx = 0; true; cIdx++){
				Node testChild = ((Dir)parent).getChild(cIdx);
				if(testChild instanceof Dir){
					if(testChild == child){
						return dirChildCount;
					}
					dirChildCount++;
				}
			}
		}else{
			return ((Dir)parent).getIndexOfChild((Node)child);
		}
	}
	public Object getRoot(){
		return root;
	}
	public boolean isLeaf(Object node){
		return ((Node)node).isLeaf();
	}
	public void removeTreeModelListener(TreeModelListener l){
		tmListeners.remove(l);
	}
	public void valueForPathChanged(TreePath path, Object newValue){}
}
