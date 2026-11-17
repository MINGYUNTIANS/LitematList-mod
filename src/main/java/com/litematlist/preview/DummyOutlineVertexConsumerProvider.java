//#if MC == 12110 || MC == 12111
package com.litematlist.preview;

import net.minecraft.client.render.*;

public class DummyOutlineVertexConsumerProvider extends OutlineVertexConsumerProvider {

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return DummyVertexConsumerProvider.DUMMY_VERTEX_CONSUMER;
    }

}

//#endif