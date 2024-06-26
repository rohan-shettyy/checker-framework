package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * A node for the unary plus operation:
 *
 * <pre>
 *   + <em>expression</em>
 * </pre>
 */
public class NumericalPlusNode extends UnaryOperationNode {

    /**
     * Constructs a {@link NumericalPlusNode}.
     *
     * @param tree the tree of the whole operation
     * @param operand the operand of the operation
     */
    public NumericalPlusNode(UnaryTree tree, Node operand) {
        super(tree, operand);
        assert tree.getKind() == Tree.Kind.UNARY_PLUS;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitNumericalPlus(this, p);
    }

    @Override
    public String toString() {
        return "(+ " + getOperand() + ")";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof NumericalPlusNode)) {
            return false;
        }
        NumericalPlusNode other = (NumericalPlusNode) obj;
        return getOperand().equals(other.getOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(NumericalPlusNode.class, getOperand());
    }
}
