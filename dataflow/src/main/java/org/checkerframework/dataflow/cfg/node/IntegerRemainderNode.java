package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * A node for the integer remainder:
 *
 * <pre>
 *   <em>expression</em> % <em>expression</em>
 * </pre>
 */
public class IntegerRemainderNode extends BinaryOperationNode {

    /**
     * Constructs an {@link IntegerRemainderNode}.
     *
     * @param tree the binary tree
     * @param left the left operand
     * @param right the right operand
     */
    public IntegerRemainderNode(BinaryTree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Tree.Kind.REMAINDER;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitIntegerRemainder(this, p);
    }

    @Override
    public String toString() {
        return "(" + getLeftOperand() + " % " + getRightOperand() + ")";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof IntegerRemainderNode)) {
            return false;
        }
        IntegerRemainderNode other = (IntegerRemainderNode) obj;
        return getLeftOperand().equals(other.getLeftOperand())
                && getRightOperand().equals(other.getRightOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeftOperand(), getRightOperand());
    }
}
