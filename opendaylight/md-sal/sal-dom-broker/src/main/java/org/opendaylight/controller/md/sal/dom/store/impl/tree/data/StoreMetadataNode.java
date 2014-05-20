/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

class StoreMetadataNode implements Immutable, Identifiable<PathArgument> {
    private final Map<PathArgument, StoreMetadataNode> children;
    private final UnsignedLong nodeVersion;
    private final UnsignedLong subtreeVersion;
    private final NormalizedNode<?, ?> data;

    /**
     *
     * @param data
     * @param nodeVersion
     * @param subtreeVersion
     * @param children Map of children, must not be modified externally
     */
    private StoreMetadataNode(final NormalizedNode<?, ?> data, final UnsignedLong nodeVersion,
            final UnsignedLong subtreeVersion, final Map<PathArgument, StoreMetadataNode> children) {
        this.nodeVersion = Preconditions.checkNotNull(nodeVersion);
        this.subtreeVersion = Preconditions.checkNotNull(subtreeVersion);
        this.data = Preconditions.checkNotNull(data);
        this.children = Preconditions.checkNotNull(children);
    }

    public static StoreMetadataNode createEmpty(final NormalizedNode<?, ?> data) {
        return new StoreMetadataNode(data, UnsignedLong.ZERO, UnsignedLong.ZERO,
                Collections.<PathArgument, StoreMetadataNode>emptyMap());
    }

    public static Builder builder(final UnsignedLong version) {
        return new Builder(version);
    }

    public static Builder builder(final StoreMetadataNode node) {
        return new Builder(node);
    }

    public UnsignedLong getNodeVersion() {
        return this.nodeVersion;
    }

    @Override
    public PathArgument getIdentifier() {
        return data.getIdentifier();
    }

    public UnsignedLong getSubtreeVersion() {
        return subtreeVersion;
    }

    public NormalizedNode<?, ?> getData() {
        return this.data;
    }

    Optional<StoreMetadataNode> getChild(final PathArgument key) {
        return Optional.fromNullable(children.get(key));
    }

    @Override
    public String toString() {
        return "StoreMetadataNode [identifier=" + getIdentifier() + ", nodeVersion=" + nodeVersion + "]";
    }

    public static final StoreMetadataNode createRecursively(final NormalizedNode<?, ?> node,
            final UnsignedLong nodeVersion, final UnsignedLong subtreeVersion) {
        Builder builder = builder(nodeVersion) //
                .setSubtreeVersion(subtreeVersion) //
                .setData(node);
        if (node instanceof NormalizedNodeContainer<?, ?, ?>) {

            @SuppressWarnings("unchecked")
            NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>> nodeContainer = (NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>>) node;
            for (NormalizedNode<?, ?> subNode : nodeContainer.getValue()) {
                builder.add(createRecursively(subNode, nodeVersion, subtreeVersion));
            }
        }
        return builder.build();
    }

    public static class Builder {

        private final UnsignedLong nodeVersion;
        private UnsignedLong subtreeVersion;
        private NormalizedNode<?, ?> data;
        private Map<PathArgument, StoreMetadataNode> children;
        private boolean dirty = false;

        private Builder(final UnsignedLong version) {
            this.nodeVersion = Preconditions.checkNotNull(version);
            children = new HashMap<>();
        }

        private Builder(final StoreMetadataNode node) {
            this.nodeVersion = node.getNodeVersion();
            children = new HashMap<>(node.children);
        }

        public Builder setSubtreeVersion(final UnsignedLong version) {
            this.subtreeVersion = version;
            return this;
        }

        public Builder setData(final NormalizedNode<?, ?> data) {
            this.data = data;
            return this;
        }

        public Builder add(final StoreMetadataNode node) {
            if (dirty) {
                children = new HashMap<>(children);
                dirty = false;
            }
            children.put(node.getIdentifier(), node);
            return this;
        }

        public Builder remove(final PathArgument id) {
            if (dirty) {
                children = new HashMap<>(children);
                dirty = false;
            }
            children.remove(id);
            return this;
        }

        public StoreMetadataNode build() {
            checkState(data != null, "Data node should not be null.");
            checkState(subtreeVersion.compareTo(nodeVersion) >= 0,
                    "Subtree version must be equals or greater than node version.");
            dirty = true;
            return new StoreMetadataNode(data, nodeVersion, subtreeVersion, children);
        }
    }

    public static StoreMetadataNode createRecursively(final NormalizedNode<?, ?> node, final UnsignedLong version) {
        return createRecursively(node, version, version);
    }
}
