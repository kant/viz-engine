package org.gephi.viz.engine.pipeline.common;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import com.jogamp.opengl.GL2ES2;
import java.nio.FloatBuffer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.models.EdgeLineModelDirected;
import org.gephi.viz.engine.models.EdgeLineModelUndirected;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.EdgesCallback;
import static org.gephi.viz.engine.util.Constants.*;
import org.gephi.viz.engine.util.gl.GLBuffer;
import org.gephi.viz.engine.util.gl.GLFunctions;
import org.gephi.viz.engine.util.gl.GLVertexArrayObject;
import org.gephi.viz.engine.util.gl.capabilities.GLCapabilities;

/**
 *
 * @author Eduardo Ramos
 */
public class AbstractEdgeData {

    protected final EdgeLineModelUndirected lineModelUndirected = new EdgeLineModelUndirected();
    protected final EdgeLineModelDirected lineModelDirected = new EdgeLineModelDirected();

    protected final InstanceCounter undirectedInstanceCounter = new InstanceCounter();
    protected final InstanceCounter directedInstanceCounter = new InstanceCounter();

    protected GLBuffer vertexGLBufferUndirected;
    protected GLBuffer vertexGLBufferDirected;
    protected GLBuffer attributesGLBuffer;

    protected final EdgesCallback edgesCallback = new EdgesCallback();
    
    protected static final int ATTRIBS_STRIDE
            = Math.max(
                    EdgeLineModelUndirected.TOTAL_ATTRIBUTES_FLOATS,
                    EdgeLineModelDirected.TOTAL_ATTRIBUTES_FLOATS
            );

    protected static final int VERTEX_COUNT_UNDIRECTED = EdgeLineModelUndirected.VERTEX_COUNT;
    protected static final int VERTEX_COUNT_DIRECTED = EdgeLineModelDirected.VERTEX_COUNT;
    protected static final int VERTEX_COUNT_MAX = Math.max(VERTEX_COUNT_DIRECTED, VERTEX_COUNT_UNDIRECTED);

    protected final boolean instanced;

    public AbstractEdgeData(boolean instanced) {
        this.instanced = instanced;
    }

    public void init(GL2ES2 gl) {
        lineModelDirected.initGLPrograms(gl);
        lineModelUndirected.initGLPrograms(gl);
    }

    protected int updateDirectedData(
            final boolean someEdgesSelection, final boolean hideNonSelected, final int visibleEdgesCount, final Edge[] visibleEdgesArray, final GraphSelection graphSelection, final boolean someNodesSelection, final boolean edgeSelectionColor, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor,
            final float[] attribs, int index
    ) {
        return updateDirectedData(someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray, graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor, attribs, index, null);
    }

    protected int updateDirectedData(
            final boolean someEdgesSelection, final boolean hideNonSelected, final int visibleEdgesCount, final Edge[] visibleEdgesArray, final GraphSelection graphSelection, final boolean someNodesSelection, final boolean edgeSelectionColor, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor,
            final float[] attribs, int index, final FloatBuffer directBuffer
    ) {
        checkBufferIndexing(directBuffer, attribs, index);

        int newEdgesCountUnselected = 0;
        int newEdgesCountSelected = 0;
        if (someEdgesSelection) {
            if (hideNonSelected) {
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (!edge.isDirected()) {
                        continue;
                    }

                    final boolean selected = graphSelection.isEdgeSelected(edge);
                    if (!selected) {
                        continue;
                    }

                    newEdgesCountSelected++;

                    index = fillDirectedEdgeAttributesData(attribs, edge, index, someEdgesSelection, selected, someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }
            } else {
                //First non-selected (bottom):
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (!edge.isDirected()) {
                        continue;
                    }

                    if (graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountUnselected++;

                    index = fillDirectedEdgeAttributesData(attribs, edge, index, someEdgesSelection, false, someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }

                //Then selected ones (up):
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (!edge.isDirected()) {
                        continue;
                    }

                    if (!graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountSelected++;

                    index = fillDirectedEdgeAttributesData(attribs, edge, index, someEdgesSelection, true, someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }
            }
        } else {
            //Just all edges, no selection active:
            for (int j = 0; j < visibleEdgesCount; j++) {
                final Edge edge = visibleEdgesArray[j];
                if (!edge.isDirected()) {
                    continue;
                }

                newEdgesCountSelected++;

                index = fillDirectedEdgeAttributesData(attribs, edge, index, someEdgesSelection, true, someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

                if (directBuffer != null && index == attribs.length) {
                    directBuffer.put(attribs, 0, attribs.length);
                    index = 0;
                }
            }
        }

        //Remaining:
        if (directBuffer != null && index > 0) {
            directBuffer.put(attribs, 0, index);
            index = 0;
        }

        directedInstanceCounter.unselectedCount = newEdgesCountUnselected;
        directedInstanceCounter.selectedCount = newEdgesCountSelected;

        return index;
    }

    protected int updateUndirectedData(
            final boolean someEdgesSelection, final boolean hideNonSelected, final int visibleEdgesCount, final Edge[] visibleEdgesArray, final GraphSelection graphSelection, final boolean someNodesSelection, final boolean edgeSelectionColor, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor,
            final float[] attribs, int index
    ) {
        return updateUndirectedData(someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray, graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor, attribs, index, null);
    }

    protected int updateUndirectedData(
            final boolean someEdgesSelection, final boolean hideNonSelected, final int visibleEdgesCount, final Edge[] visibleEdgesArray, final GraphSelection graphSelection, final boolean someNodesSelection, final boolean edgeSelectionColor, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor,
            final float[] attribs, int index, final FloatBuffer directBuffer
    ) {
        checkBufferIndexing(directBuffer, attribs, index);

        int newEdgesCountUnselected = 0;
        int newEdgesCountSelected = 0;
        //Undirected edges:
        if (someEdgesSelection) {
            if (hideNonSelected) {
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (edge.isDirected()) {
                        continue;
                    }

                    if (!graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountSelected++;

                    index = fillUndirectedEdgeAttributesData(attribs, edge, index, someEdgesSelection, true, someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }
            } else {
                //First non-selected (bottom):
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (edge.isDirected()) {
                        continue;
                    }

                    if (graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountUnselected++;

                    index = fillUndirectedEdgeAttributesData(attribs, edge, index, someEdgesSelection, false, someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }

                //Then selected ones (up):
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (edge.isDirected()) {
                        continue;
                    }

                    if (!graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountSelected++;

                    index = fillUndirectedEdgeAttributesData(attribs, edge, index, someEdgesSelection, true, someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }
            }
        } else {
            //Just all edges, no selection active:
            for (int j = 0; j < visibleEdgesCount; j++) {
                final Edge edge = visibleEdgesArray[j];
                if (edge.isDirected()) {
                    continue;
                }

                newEdgesCountSelected++;

                index = fillUndirectedEdgeAttributesData(attribs, edge, index, someEdgesSelection, true, someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

                if (directBuffer != null && index == attribs.length) {
                    directBuffer.put(attribs, 0, attribs.length);
                    index = 0;
                }
            }
        }

        //Remaining:
        if (directBuffer != null && index > 0) {
            directBuffer.put(attribs, 0, index);
            index = 0;
        }

        undirectedInstanceCounter.unselectedCount = newEdgesCountUnselected;
        undirectedInstanceCounter.selectedCount = newEdgesCountSelected;

        return index;
    }

    private void checkBufferIndexing(final FloatBuffer directBuffer, final float[] attribs, final int index) {
        if (directBuffer != null) {
            if (attribs.length % ATTRIBS_STRIDE != 0) {
                throw new IllegalArgumentException("When filling a directBuffer, attribs buffer length should be a multiple of ATTRIBS_STRIDE = " + ATTRIBS_STRIDE);
            }

            if (index % ATTRIBS_STRIDE != 0) {
                throw new IllegalArgumentException("When filling a directBuffer, index should be a multiple of ATTRIBS_STRIDE = " + ATTRIBS_STRIDE);
            }
        }
    }

    protected int fillUndirectedEdgeAttributesData(final float[] buffer, final Edge edge, final int index, final boolean someEdgesSelection, final boolean selected, final boolean someNodesSelection, final boolean edgeSelectionColor, final GraphSelection graphSelection, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor) {
        final Node source = edge.getSource();
        final Node target = edge.getTarget();

        final float sourceX = source.x();
        final float sourceY = source.y();
        final float targetX = target.x();
        final float targetY = target.y();

        //Position:
        buffer[index + 0] = sourceX;
        buffer[index + 1] = sourceY;

        //Target position:
        buffer[index + 2] = targetX;
        buffer[index + 3] = targetY;

        //Size:
        buffer[index + 4] = (float) edge.getWeight();

        //Source color:
        buffer[index + 5] = Float.intBitsToFloat(source.getRGBA());

        //Target color:
        buffer[index + 6] = Float.intBitsToFloat(target.getRGBA());

        //Color, color bias and color multiplier:
        if (someEdgesSelection) {
            if (selected) {
                if (someNodesSelection && edgeSelectionColor) {
                    boolean sourceSelected = graphSelection.isNodeSelected(source);
                    boolean targetSelected = graphSelection.isNodeSelected(target);

                    if (sourceSelected && targetSelected) {
                        buffer[index + 7] = edgeBothSelectionColor;//Color
                    } else if (sourceSelected) {
                        buffer[index + 7] = edgeOutSelectionColor;//Color
                    } else if (targetSelected) {
                        buffer[index + 7] = edgeInSelectionColor;//Color
                    } else {
                        buffer[index + 7] = Float.intBitsToFloat(edge.getRGBA());//Color
                    }

                    buffer[index + 8] = 0;//Bias
                    buffer[index + 9] = 1;//Multiplier
                } else {
                    if (someNodesSelection && edge.alpha() <= 0) {
                        if (graphSelection.isNodeSelected(source)) {
                            buffer[index + 7] = Float.intBitsToFloat(target.getRGBA());//Color
                        } else {
                            buffer[index + 7] = Float.intBitsToFloat(source.getRGBA());//Color
                        }
                    } else {
                        buffer[index + 7] = Float.intBitsToFloat(edge.getRGBA());//Color
                    }

                    buffer[index + 8] = 0.5f;//Bias
                    buffer[index + 9] = 0.5f;//Multiplier
                }
            } else {
                buffer[index + 7] = Float.intBitsToFloat(edge.getRGBA());//Color
                buffer[index + 8] = 0;//Bias
                buffer[index + 9] = 1;//Multiplier
            }
        } else {
            buffer[index + 7] = Float.intBitsToFloat(edge.getRGBA());//Color
            buffer[index + 8] = 0;//Bias
            buffer[index + 9] = 1;//Multiplier
        }

        return index + ATTRIBS_STRIDE;
    }

    protected int fillDirectedEdgeAttributesData(final float[] buffer, final Edge edge, final int index, final boolean someEdgesSelection, final boolean selected, final boolean someNodesSelection, final boolean edgeSelectionColor, final GraphSelection graphSelection, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor) {
        final Node source = edge.getSource();
        final Node target = edge.getTarget();

        final float sourceX = source.x();
        final float sourceY = source.y();
        final float targetX = target.x();
        final float targetY = target.y();

        //Position:
        buffer[index + 0] = sourceX;
        buffer[index + 1] = sourceY;

        //Target position:
        buffer[index + 2] = targetX;
        buffer[index + 3] = targetY;

        //Size:
        buffer[index + 4] = (float) edge.getWeight();

        //Source color:
        buffer[index + 5] = Float.intBitsToFloat(source.getRGBA());

        //Color, color bias and color multiplier:
        if (someEdgesSelection) {
            if (selected) {
                if (someNodesSelection && edgeSelectionColor) {
                    boolean sourceSelected = graphSelection.isNodeSelected(source);
                    boolean targetSelected = graphSelection.isNodeSelected(target);

                    if (sourceSelected && targetSelected) {
                        buffer[index + 6] = edgeBothSelectionColor;//Color
                    } else if (sourceSelected) {
                        buffer[index + 6] = edgeOutSelectionColor;//Color
                    } else if (targetSelected) {
                        buffer[index + 6] = edgeInSelectionColor;//Color
                    } else {
                        buffer[index + 6] = Float.intBitsToFloat(edge.getRGBA());//Color
                    }

                    buffer[index + 7] = 0;//Bias
                    buffer[index + 8] = 1;//Multiplier
                } else {
                    if (someNodesSelection && edge.alpha() <= 0) {
                        if (graphSelection.isNodeSelected(source)) {
                            buffer[index + 6] = Float.intBitsToFloat(target.getRGBA());//Color
                        } else {
                            buffer[index + 6] = Float.intBitsToFloat(source.getRGBA());//Color
                        }
                    } else {
                        buffer[index + 6] = Float.intBitsToFloat(edge.getRGBA());//Color
                    }

                    buffer[index + 7] = 0.5f;//Bias
                    buffer[index + 8] = 0.5f;//Multiplier
                }
            } else {
                buffer[index + 6] = Float.intBitsToFloat(edge.getRGBA());//Color
                buffer[index + 7] = 0;//Bias
                buffer[index + 8] = 1;//Multiplier
            }
        } else {
            buffer[index + 6] = Float.intBitsToFloat(edge.getRGBA());//Color
            buffer[index + 7] = 0;//Bias
            buffer[index + 8] = 1;//Multiplier
        }

        //Target size:
        buffer[index + 9] = target.size();

        return index + ATTRIBS_STRIDE;
    }

    private UndirectedEdgesVAO undirectedEdgesVAO;
    private DirectedEdgesVAO directedEdgesVAO;

    public void setupUndirectedVertexArrayAttributes(VizEngine engine, GL2ES2 gl) {
        if (undirectedEdgesVAO == null) {
            undirectedEdgesVAO = new UndirectedEdgesVAO(engine.getCapabilities());
        }

        undirectedEdgesVAO.use(gl);
    }

    public void unsetupUndirectedVertexArrayAttributes(GL2ES2 gl) {
        undirectedEdgesVAO.stopUsing(gl);
    }

    public void setupDirectedVertexArrayAttributes(VizEngine engine, GL2ES2 gl) {
        if (directedEdgesVAO == null) {
            directedEdgesVAO = new DirectedEdgesVAO(engine.getCapabilities());
        }

        directedEdgesVAO.use(gl);
    }

    public void unsetupDirectedVertexArrayAttributes(GL2ES2 gl) {
        directedEdgesVAO.stopUsing(gl);
    }

    public void dispose(GL gl) {
        if (vertexGLBufferUndirected != null) {
            vertexGLBufferUndirected.destroy(gl);
        }

        if (vertexGLBufferDirected != null) {
            vertexGLBufferDirected.destroy(gl);
        }

        if (attributesGLBuffer != null) {
            attributesGLBuffer.destroy(gl);
        }
        
        edgesCallback.reset();
    }

    private class UndirectedEdgesVAO extends GLVertexArrayObject {

        public UndirectedEdgesVAO(GLCapabilities capabilities) {
            super(capabilities);
        }

        @Override
        protected void configure(GL2ES2 gl) {
            vertexGLBufferUndirected.bind(gl);
            {
                gl.glVertexAttribPointer(SHADER_VERT_LOCATION, EdgeLineModelUndirected.VERTEX_FLOATS, GL_FLOAT, false, 0, 0);
            }
            vertexGLBufferUndirected.unbind(gl);

            attributesGLBuffer.bind(gl);
            {
                int stride = ATTRIBS_STRIDE * Float.BYTES;
                int offset = 0;
                gl.glVertexAttribPointer(SHADER_POSITION_LOCATION, EdgeLineModelUndirected.POSITION_SOURCE_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.POSITION_SOURCE_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_POSITION_TARGET_LOCATION, EdgeLineModelUndirected.POSITION_TARGET_LOCATION, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.POSITION_TARGET_LOCATION * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_SIZE_LOCATION, EdgeLineModelUndirected.SIZE_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.SIZE_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_SOURCE_COLOR_LOCATION, EdgeLineModelUndirected.SOURCE_COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelUndirected.SOURCE_COLOR_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_TARGET_COLOR_LOCATION, EdgeLineModelUndirected.TARGET_COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelUndirected.TARGET_COLOR_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_COLOR_LOCATION, EdgeLineModelUndirected.COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelUndirected.COLOR_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_COLOR_BIAS_LOCATION, EdgeLineModelUndirected.COLOR_BIAS_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.COLOR_BIAS_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_COLOR_MULTIPLIER_LOCATION, EdgeLineModelUndirected.COLOR_MULTIPLIER_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.COLOR_MULTIPLIER_FLOATS * Float.BYTES;
            }
            attributesGLBuffer.unbind(gl);
        }

        @Override
        protected int[] getUsedAttributeLocations() {
            return new int[]{
                SHADER_VERT_LOCATION,
                SHADER_POSITION_LOCATION,
                SHADER_POSITION_TARGET_LOCATION,
                SHADER_SIZE_LOCATION,
                SHADER_SOURCE_COLOR_LOCATION,
                SHADER_TARGET_COLOR_LOCATION,
                SHADER_COLOR_LOCATION,
                SHADER_COLOR_BIAS_LOCATION,
                SHADER_COLOR_MULTIPLIER_LOCATION
            };
        }

        @Override
        protected int[] getInstancedAttributeLocations() {
            if (instanced) {
                return new int[]{
                    SHADER_POSITION_LOCATION,
                    SHADER_POSITION_TARGET_LOCATION,
                    SHADER_SIZE_LOCATION,
                    SHADER_SOURCE_COLOR_LOCATION,
                    SHADER_TARGET_COLOR_LOCATION,
                    SHADER_COLOR_LOCATION,
                    SHADER_COLOR_BIAS_LOCATION,
                    SHADER_COLOR_MULTIPLIER_LOCATION
                };
            } else {
                return null;
            }
        }

    }

    private class DirectedEdgesVAO extends GLVertexArrayObject {

        public DirectedEdgesVAO(GLCapabilities capabilities) {
            super(capabilities);
        }

        @Override
        protected void configure(GL2ES2 gl) {
            vertexGLBufferDirected.bind(gl);
            {
                gl.glVertexAttribPointer(SHADER_VERT_LOCATION, EdgeLineModelDirected.VERTEX_FLOATS, GL_FLOAT, false, 0, 0);
            }
            vertexGLBufferDirected.unbind(gl);

            attributesGLBuffer.bind(gl);
            {
                int stride = ATTRIBS_STRIDE * Float.BYTES;
                int offset = 0;
                gl.glVertexAttribPointer(SHADER_POSITION_LOCATION, EdgeLineModelDirected.POSITION_SOURCE_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.POSITION_SOURCE_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_POSITION_TARGET_LOCATION, EdgeLineModelDirected.POSITION_TARGET_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.POSITION_TARGET_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_SIZE_LOCATION, EdgeLineModelDirected.SIZE_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.SIZE_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_SOURCE_COLOR_LOCATION, EdgeLineModelDirected.SOURCE_COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelDirected.SOURCE_COLOR_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_COLOR_LOCATION, EdgeLineModelDirected.COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelDirected.COLOR_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_COLOR_BIAS_LOCATION, EdgeLineModelDirected.COLOR_BIAS_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.COLOR_BIAS_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_COLOR_MULTIPLIER_LOCATION, EdgeLineModelDirected.COLOR_MULTIPLIER_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.COLOR_MULTIPLIER_FLOATS * Float.BYTES;

                gl.glVertexAttribPointer(SHADER_TARGET_SIZE_LOCATION, EdgeLineModelDirected.TARGET_SIZE_FLOATS, GL_FLOAT, false, stride, offset);

                if (instanced) {
                    GLFunctions.glVertexAttribDivisor(gl, SHADER_POSITION_LOCATION, 1);
                    GLFunctions.glVertexAttribDivisor(gl, SHADER_POSITION_TARGET_LOCATION, 1);
                    GLFunctions.glVertexAttribDivisor(gl, SHADER_SIZE_LOCATION, 1);
                    GLFunctions.glVertexAttribDivisor(gl, SHADER_SOURCE_COLOR_LOCATION, 1);
                    GLFunctions.glVertexAttribDivisor(gl, SHADER_COLOR_LOCATION, 1);
                    GLFunctions.glVertexAttribDivisor(gl, SHADER_COLOR_BIAS_LOCATION, 1);
                    GLFunctions.glVertexAttribDivisor(gl, SHADER_COLOR_MULTIPLIER_LOCATION, 1);
                    GLFunctions.glVertexAttribDivisor(gl, SHADER_TARGET_SIZE_LOCATION, 1);
                }
            }
            attributesGLBuffer.unbind(gl);
        }

        @Override
        protected int[] getUsedAttributeLocations() {
            return new int[]{
                SHADER_VERT_LOCATION,
                SHADER_POSITION_LOCATION,
                SHADER_POSITION_TARGET_LOCATION,
                SHADER_SIZE_LOCATION,
                SHADER_SOURCE_COLOR_LOCATION,
                SHADER_COLOR_LOCATION,
                SHADER_COLOR_BIAS_LOCATION,
                SHADER_COLOR_MULTIPLIER_LOCATION,
                SHADER_TARGET_SIZE_LOCATION
            };
        }

        @Override
        protected int[] getInstancedAttributeLocations() {
            if (instanced) {
                return new int[]{
                    SHADER_POSITION_LOCATION,
                    SHADER_POSITION_TARGET_LOCATION,
                    SHADER_SIZE_LOCATION,
                    SHADER_SOURCE_COLOR_LOCATION,
                    SHADER_COLOR_LOCATION,
                    SHADER_COLOR_BIAS_LOCATION,
                    SHADER_COLOR_MULTIPLIER_LOCATION,
                    SHADER_TARGET_SIZE_LOCATION
                };
            } else {
                return null;
            }
        }

    }
}