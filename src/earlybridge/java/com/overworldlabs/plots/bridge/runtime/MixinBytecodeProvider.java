package com.overworldlabs.plots.bridge.runtime;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;

import java.io.IOException;

public final class MixinBytecodeProvider implements IClassBytecodeProvider {
    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
            throws ClassNotFoundException, IOException {
        // `runTransformers` is not used in this runtime bridge provider.
        ClassReader reader = LaunchEnvironment.get().getClassReader(name);
        ClassNode node = new ClassNode();
        reader.accept(node, readerFlags);
        return node;
    }
}
