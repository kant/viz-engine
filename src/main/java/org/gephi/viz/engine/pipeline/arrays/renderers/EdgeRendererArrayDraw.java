package org.gephi.viz.engine.pipeline.arrays.renderers;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.availability.ArrayDraw;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.pipeline.arrays.ArrayDrawEdgeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class EdgeRendererArrayDraw implements Renderer {

    private final VizEngine engine;
    private final ArrayDrawEdgeData edgeData;

    public EdgeRendererArrayDraw(VizEngine engine, ArrayDrawEdgeData edgeData) {
        this.engine = engine;
        this.edgeData = edgeData;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        edgeData.init(drawable.getGL().getGL2ES2());
    }

    @Override
    public void worldUpdated(GLAutoDrawable drawable) {
        edgeData.updateBuffers();
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(GLAutoDrawable drawable, RenderingLayer layer) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        engine.getModelViewProjectionMatrixFloats(mvpFloats);

        edgeData.drawArrays(gl, layer, engine, mvpFloats);
    }

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.BACK, RenderingLayer.MIDDLE);
    }

    @Override
    public int getOrder() {
        return Constants.RENDERING_ORDER_EDGES;
    }

    @Override
    public String getCategory() {
        return PipelineCategory.EDGE;
    }

    @Override
    public int getPreferenceInCategory() {
        return ArrayDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Edges (Vertex Array)";
    }

    @Override
    public boolean isAvailable(GLAutoDrawable drawable) {
        return ArrayDraw.isAvailable(engine, drawable);
    }
}
