//#if MC == 12110 || MC == 12111
package com.litematlist.preview;

import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
//#if MC == 12110
import net.minecraft.client.render.BufferBuilder;
//#endif
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;

public class DummyVertexConsumerProvider extends VertexConsumerProvider.Immediate {

    public static final VertexConsumer DUMMY_VERTEX_CONSUMER = new VertexConsumer() {
        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
//#if MC == 12111
//$$            return this;
//#endif
//#if MC == 12111
//$$        }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$        @Override
//#endif
//#if MC == 12111
//$$        public VertexConsumer color(int argb) {
//#endif
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$        // 1.21.11 新增的抽象方法
//#endif
//#if MC == 12111
//$$        @Override
//#endif
//#if MC == 12111
//$$        public VertexConsumer lineWidth(float width) {
//#endif
//#if MC == 12111
//$$            return this;
//#endif
//#if MC == 12111
//$$        }
//#endif
    };
    public static final BufferAllocator EMPTY_BUFFER_ALLOCATOR = BufferAllocator.fixedSized(8);

    public DummyVertexConsumerProvider() {
        super(EMPTY_BUFFER_ALLOCATOR, Object2ObjectSortedMaps.emptyMap());
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return DUMMY_VERTEX_CONSUMER;
    }

//#if MC == 12110
    @Override
//#endif
//#if MC == 12110
    protected void draw(RenderLayer layer, BufferBuilder builder) {
//#endif
//#if MC == 12110
        currentLayer = null;
//#endif
//#if MC == 12110
    }
//#endif
//#if MC == 12110

//#endif
}

//#endif