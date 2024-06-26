/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.bounding.BoundingVolume;
import com.jme3.gde.core.properties.UserDataProperty;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AddUserDataAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ControlsPopup;
import com.jme3.gde.core.sceneexplorer.nodes.actions.NewControlPopup;
import com.jme3.gde.core.sceneexplorer.nodes.actions.NewLightPopup;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ToolPopup;
import com.jme3.light.LightList;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.actions.PasteAction;
import org.openide.actions.RenameAction;
import org.openide.awt.Actions;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = SceneExplorerNode.class)
public class JmeSpatial extends AbstractSceneExplorerNode {

    protected Spatial spatial;
    /* DataFlavor is a kind of metadata/mime-type
     * (https://docs.oracle.com/javase/7/docs/api/java/awt/datatransfer/DataFlavor.html)
     */
    protected final DataFlavor SPATIAL_FLAVOR = new DataFlavor(ClipboardSpatial.class, "Spatial");

    public JmeSpatial() {
        super();
    }
    
    public JmeSpatial(Spatial spatial) {
        super();
        this.spatial = spatial;
        getLookupContents().add(spatial);
        getLookupContents().add(this);
        super.setName(spatial.getName());
    }

    public JmeSpatial(Spatial spatial, JmeSpatialChildren factory) {
        super(factory);
        this.spatial = spatial;
        getLookupContents().add(spatial);
        getLookupContents().add(this);
        super.setName(spatial.getName());
    }

    public JmeSpatial getChild(Spatial spat) {
        if (spat == null) {
            return null;
        }
        if (getLookup().lookup(spat.getClass()) == spat) {
            return this;
        }

        Node[] children = getChildren().getNodes();
        for (Node node: children) {
            if (node instanceof JmeSpatial) {
                JmeSpatial jmeSpatial = (JmeSpatial) node;
                JmeSpatial found = jmeSpatial.getChild(spat);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

//    protected SystemAction[] createActions() {
//        return new SystemAction[]{
//                    SystemAction.get(RenameAction.class),
//                    SystemAction.get(CopyAction.class),
//                    SystemAction.get(CutAction.class),
//                    SystemAction.get(PasteAction.class),
//                    SystemAction.get(DeleteAction.class)
//                };
//    }
    @Override
    public Action[] getActions(boolean context) {
//        return super.getActions(context);
        if (((JmeSpatialChildren) jmeChildren).readOnly) {
            return new Action[]{
                        SystemAction.get(CopyAction.class),
                        new ControlsPopup(this)
                };
        } else {
            return new Action[]{
                        new NewControlPopup(this),
                        new NewLightPopup(this),
                        new ControlsPopup(this),
                        Actions.alwaysEnabled(new AddUserDataAction(this), "Add User Data", "", false),
                        new ToolPopup(this),
                        SystemAction.get(RenameAction.class),
                        SystemAction.get(CopyAction.class),
                        SystemAction.get(CutAction.class),
                        SystemAction.get(PasteAction.class),
                        SystemAction.get(DeleteAction.class)
                    };
        }
    }

    @Override
    public boolean canCopy() {
        // @TODO: Is readOnly really relevant here?
        return !((JmeSpatialChildren) jmeChildren).readOnly;
    }

    @Override
    public boolean canCut() {
        return !((JmeSpatialChildren) jmeChildren).readOnly;
    }

    @Override
    public boolean canDestroy() {
        return !((JmeSpatialChildren) jmeChildren).readOnly;
    }

    @Override
    public boolean canRename() {
        return !((JmeSpatialChildren) jmeChildren).readOnly;
    }

    /**
     * Warning this methods also changes the name of the spatial in the scene
     * it will mark the file as changed and will activate save button.     
     * @param s 
     */
    @Override    
    public void setName(final String s) {
        super.setName(s);
        try {            
            fireSave(true);
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    spatial.setName(s);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void destroy() throws IOException {
        try {
            fireSave(true);
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    spatial.removeFromParent();
                    return null;
                }
            }).get();
            if (getParentNode() instanceof JmeNode) {
                JmeNode node = ((JmeNode) getParentNode());
                if (node != null) {
                    node.refresh(false);
                }
            }
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        Transferable trans = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { SPATIAL_FLAVOR };
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return SPATIAL_FLAVOR.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) 
                    throws UnsupportedFlavorException, IOException {
                if (SPATIAL_FLAVOR.equals(flavor)) {
                    return new ClipboardSpatial(spatial);
                } else {
                    throw new UnsupportedFlavorException(flavor);
                }
            }
        };
        return trans;
    }

    @Override
    public Transferable clipboardCut() throws IOException {
        fireSave(true);
        Transferable trans = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{ SPATIAL_FLAVOR };
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return SPATIAL_FLAVOR.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws 
                    UnsupportedFlavorException, IOException {
                if (SPATIAL_FLAVOR.equals(flavor)) {
                    try {
                        SceneApplication.getApplication().enqueue(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                spatial.removeFromParent();
                                return null;
                            }
                        }).get();
                        
                        // @TODO: not a good cast
                        JmeNode node = ((JmeNode) getParentNode());
                        if (node != null) {
                            node.refresh(false);
                        }
                        
                        return new ClipboardSpatial(spatial);
                    } catch (InterruptedException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (ExecutionException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    return null;
                } else {
                    throw new UnsupportedFlavorException(flavor);
                }
            }
        };
        return trans;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();        

        //@TODO: multithreading.. but we only read
        Collection<String> dataKeys = spatial.getUserDataKeys();
        if (!dataKeys.isEmpty()) {
            Sheet.Set set = Sheet.createPropertiesSet();
            set.setDisplayName("User Data");
            set.setName(Spatial.class.getName() + "_UserData");
            for (String string : dataKeys) {
                UserDataProperty prop = new UserDataProperty(this, string);
                prop.addPropertyChangeListener(this);
                set.put(prop);
            }
            sheet.put(set);
        }
        
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Spatial");
        set.setName(Spatial.class.getName());
        Spatial obj = spatial;//getLookup().lookup(Spatial.class);
        if (obj == null) {
            return sheet;
        }
//        set.put(makeProperty(obj, String.class, "getName", "setName", "name"));

        set.put(makeProperty(obj, int.class, "getVertexCount", "Vertexes"));
        set.put(makeProperty(obj, int.class, "getTriangleCount", "Triangles"));

//        set.put(makeProperty(obj, Transform.class,"getWorldTransform","world transform"));
        set.put(makeProperty(obj, Vector3f.class, "getWorldTranslation", "World Translation"));
        set.put(makeProperty(obj, Quaternion.class, "getWorldRotation", "World Rotation"));
        set.put(makeProperty(obj, Vector3f.class, "getWorldScale", "World Scale"));

        set.put(makeProperty(obj, Vector3f.class, "getLocalTranslation", "setLocalTranslation", "Local Translation"));
        set.put(makeProperty(obj, Quaternion.class, "getLocalRotation", "setLocalRotation", "Local Rotation"));
        set.put(makeProperty(obj, Vector3f.class, "getLocalScale", "setLocalScale", "Local Scale"));

        set.put(makeProperty(obj, BoundingVolume.class, "getWorldBound", "World Bound"));

        /* Global CullHint and ShadowMode disabled because: A) they are confusing
         * as users expect the local version to be shown and B) if the local mode
         * is updated, the global property isn't automatically. This code has to
         * be implemented if one wants the read-only globals.
        */
        //set.put(makeProperty(obj, CullHint.class, "getCullHint", "Cull Hint"));
        set.put(makeProperty(obj, CullHint.class, "getLocalCullHint", "setCullHint", "Local Cull Hint"));
        //set.put(makeProperty(obj, ShadowMode.class, "getShadowMode", "Shadow Mode"));
        set.put(makeProperty(obj, ShadowMode.class, "getLocalShadowMode", "setShadowMode", "Local Shadow Mode"));
        set.put(makeProperty(obj, LightList.class, "getWorldLightList", "World Light List"));

        set.put(makeProperty(obj, RenderQueue.Bucket.class, "getQueueBucket", "setQueueBucket", "Queue Bucket"));

        sheet.put(set);
        return sheet;

    }

    @Override
    public Class<?> getExplorerObjectClass() {
        return Spatial.class;
    }

    @Override
    public Class<?> getExplorerNodeClass() {
        return JmeSpatial.class;
    }

    @Override
    public Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        JmeSpatialChildren children = new JmeSpatialChildren((com.jme3.scene.Spatial) key);
        children.setReadOnly(cookie);
        children.setDataObject(key2);
        return new Node[]{new JmeSpatial((Spatial) key, children).setReadOnly(cookie)};
    }
    
    /**
     * This method will call {@link JmeControl#setEnabled(boolean) } for all children nodes of this.
     * It will enabled/disable the Controls from the Scene Graph and hence being run.
     * Note: There is no isEnabled because it's only passed on to the Controls and they could have several states.
     * @param enable 
     */
    public void setControlsEnabled(boolean enable) {
        for (Node n : getChildren().getNodes()) {
            if (n instanceof JmeSpatial) {
                ((JmeSpatial)n).setControlsEnabled(enable);
            } else if (n instanceof JmeControl) {
                ((JmeControl)n).setEnabled(enable);
            }
        }
    }
}
