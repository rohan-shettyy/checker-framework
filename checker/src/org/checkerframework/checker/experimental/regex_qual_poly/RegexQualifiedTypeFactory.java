package org.checkerframework.checker.experimental.regex_qual_poly;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.checker.experimental.regex_qual.RegexQualifierHierarchy;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.base.dataflow.QualAnalysis;
import org.checkerframework.qualframework.base.dataflow.QualTransfer;
import org.checkerframework.qualframework.base.dataflow.QualValue;
import org.checkerframework.qualframework.poly.CombiningOperation;
import org.checkerframework.qualframework.poly.ContainmentHierarchy;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.PolyQual.QualVar;
import org.checkerframework.qualframework.poly.PolyQualHierarchy;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.QualifierParameterTreeAnnotator;
import org.checkerframework.qualframework.poly.QualifierParameterTypeAnnotator;
import org.checkerframework.qualframework.poly.QualifierParameterTypeFactory;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.Wildcard;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.QualifierContext;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The QualifiedTypeFactory for the Regex-Qual-Param type system.
 *
 *
 */
public class RegexQualifiedTypeFactory extends QualifierParameterTypeFactory<Regex> {

    private CombiningOperation<Regex> lubOp = new CombiningOperation.Lub<>(new RegexQualifierHierarchy());

    /**
     * The Pattern.compile method.
     *
     * @see Pattern#compile(String)
     */
    private final ExecutableElement patternCompile;

    public RegexQualifiedTypeFactory(QualifierContext<QualParams<Regex>> checker) {
        super(checker);

        patternCompile = TreeUtils.getMethod("java.util.regex.Pattern", "compile",
                1, getContext().getProcessingEnvironment());
    }

    @Override
    protected QualifierHierarchy<Regex> createGroundQualifierHierarchy() {
        return new RegexQualifierHierarchy();
    }

    @Override
    protected RegexAnnotationConverter createAnnotationConverter() {
        return new RegexAnnotationConverter();
    }

    @Override
    protected QualifierParameterTreeAnnotator<Regex> createTreeAnnotator() {
        return new QualifierParameterTreeAnnotator<Regex>(this) {

            /**
             * Create a Regex qualifier based on the contents of string and char literals.
             * Null literals are Regex.BOTTOM.
             */
            @Override
            public QualifiedTypeMirror<QualParams<Regex>> visitLiteral(LiteralTree tree, ExtendedTypeMirror type) {
                QualifiedTypeMirror<QualParams<Regex>> result = super.visitLiteral(tree, type);

                if (tree.getKind() == Kind.NULL_LITERAL) {
                    return SetQualifierVisitor.apply(result, QualParams.<Regex>getBottom());
                }

                String regexStr = null;
                if (tree.getKind() == Kind.STRING_LITERAL) {
                    regexStr = (String) tree.getValue();
                } else if (tree.getKind() == Kind.CHAR_LITERAL) {
                    regexStr = Character.toString((Character) tree.getValue());
                }

                if (regexStr != null) {
                    Regex regexQual;
                    if (isRegex(regexStr)) {
                        int groupCount = getGroupCount(regexStr);
                        regexQual = new Regex.RegexVal(groupCount);
                    } else {
                        regexQual = new Regex.PartialRegex(regexStr);
                    }
                    QualParams<Regex> clone = result.getQualifier().clone();
                    clone.setPrimary(new GroundQual<>(regexQual));
                    result = SetQualifierVisitor.apply(result, clone);
                }

                return result;
            }

            /**
             * Handle string compound assignment.
             */
            @Override
            public QualifiedTypeMirror<QualParams<Regex>> visitCompoundAssignment(CompoundAssignmentTree tree,
                    ExtendedTypeMirror type) {

                QualifiedTypeMirror<QualParams<Regex>> result = super.visitCompoundAssignment(tree, type);

                if (TreeUtils.isStringConcatenation(tree)
                        || (tree instanceof CompoundAssignmentTree
                        && TreeUtils.isStringCompoundConcatenation(tree))) {

                    QualParams<Regex> lRegex = getQualifiedType(tree.getExpression()).getQualifier();
                    QualParams<Regex> rRegex = getQualifiedType(tree.getVariable()).getQualifier();
                    return handleBinaryOperation(tree, lRegex, rRegex, result);
                } else {
                    return result;
                }
            }

            /**
             * Add polymorphism to the Pattern.compile and Pattern.matcher methods.
             */
            @Override
            public QualifiedTypeMirror<QualParams<Regex>> visitMethodInvocation(MethodInvocationTree tree, ExtendedTypeMirror type) {
                // TODO: Also get this to work with 2 argument Pattern.compile.

                QualifiedTypeMirror<QualParams<Regex>> result = super.visitMethodInvocation(tree, type);

                if (TreeUtils.isMethodInvocation(tree, patternCompile,
                        getContext().getProcessingEnvironment())) {

                    ExpressionTree arg0 = tree.getArguments().get(0);
                    if (getQualifiedType(arg0).getQualifier() == QualParams.<Regex>getBottom()) {
                        result = SetQualifierVisitor.apply(result, QualParams.<Regex>getBottom());
                    } else {
                        Regex qual = getQualifiedType(arg0).getQualifier().getPrimary().getMaximum();
                        QualParams<Regex> clone = result.getQualifier().clone();
                        clone.setPrimary(new GroundQual<>(qual));
                        result = SetQualifierVisitor.apply(result, clone);
                    }
                }
                return result;
            }

            /**
             * Handle concatenation of Regex or PolyRegex String/char literals.
             * Also handles concatenation of partial regular expressions.
             */
            @Override
            public QualifiedTypeMirror<QualParams<Regex>> visitBinary(BinaryTree tree, ExtendedTypeMirror type) {

                QualifiedTypeMirror<QualParams<Regex>> result = super.visitBinary(tree, type);

                if (TreeUtils.isStringConcatenation(tree)
                        || (tree instanceof CompoundAssignmentTree
                        && TreeUtils.isStringCompoundConcatenation((CompoundAssignmentTree)tree))) {

                    QualParams<Regex> lRegex = getQualifiedType(tree.getLeftOperand()).getQualifier();
                    QualParams<Regex> rRegex = getQualifiedType(tree.getRightOperand()).getQualifier();
                    return handleBinaryOperation(tree, lRegex, rRegex, result);
                } else {
                    return result;
                }
            }

            /**
             * Returns the QualifiedTypeMirror that is the result of the binary operation represented by tree.
             * Handles concatenation of Regex and PolyRegex qualifiers.
             *
             * @param tree A BinaryTree or a CompoundAssignmentTree
             * @param lRegexParam The qualifier of the left hand side of the expression
             * @param rRegexParam The qualifier of the right hand side of the expression
             * @param result The current QualifiedTypeMirror result
             * @return result if operation is not a string concatenation or compound assignment. Otherwise
             *          a copy of result with the new qualifier applied is returned.
             */
            private QualifiedTypeMirror<QualParams<Regex>> handleBinaryOperation(Tree tree, QualParams<Regex> lRegexParam,
                    QualParams<Regex> rRegexParam, QualifiedTypeMirror<QualParams<Regex>> result) {

                if (TreeUtils.isStringConcatenation(tree)
                        || (tree instanceof CompoundAssignmentTree
                            && TreeUtils.isStringCompoundConcatenation((CompoundAssignmentTree)tree))) {

                    PolyQual<Regex> resultQual = null;

                    PolyQual<Regex> rPrimary = rRegexParam.getPrimary();
                    PolyQual<Regex> lPrimary = lRegexParam.getPrimary();

                    Regex rRegex = getQualifierHierarchy().getBottom() == rRegexParam ?
                            new Regex.RegexVal(0) : rPrimary.getMaximum();
                    Regex lRegex = getQualifierHierarchy().getBottom() == lRegexParam ?
                            new Regex.RegexVal(0) : lPrimary.getMaximum();

                    if (isPolyPlusRegex(rPrimary, lRegex)) {
                        // Poly + Regex == Poly
                        resultQual = rPrimary;

                    } else if (isPolyPlusRegex(lPrimary, rRegex)) {
                        // Regex + Poly == Poly
                        resultQual = lPrimary;

                    } else if (lRegex.isRegexVal() && rRegex.isRegexVal()) {
                        // Regex(a) + Regex(b) = Regex(a + b)
                        int resultCount = ((Regex.RegexVal) lRegex).getCount() + ((Regex.RegexVal) rRegex).getCount();
                        resultQual = new GroundQual<Regex>(new Regex.RegexVal(resultCount));

                    } else if (lRegex.isPartialRegex() && rRegex.isPartialRegex()) {
                        // Partial + Partial == Regex or Partial
                        String concat = ((Regex.PartialRegex) lRegex).getPartialValue() + ((Regex.PartialRegex) rRegex).getPartialValue();
                        if (isRegex(concat)) {
                            int groupCount = getGroupCount(concat);
                            resultQual = new GroundQual<Regex>(new Regex.RegexVal(groupCount));
                        } else {
                            resultQual = new GroundQual<Regex>(new Regex.PartialRegex(concat));
                        }

                    } else if (lRegex.isRegexVal() && rRegex.isPartialRegex()) {
                        // Regex + Partial == Partial
                        String concat = "e" + ((Regex.PartialRegex) rRegex).getPartialValue();
                        resultQual = new GroundQual<Regex>(new Regex.PartialRegex(concat));

                    } else if (rRegex.isRegexVal() && lRegex.isPartialRegex()) {
                        // Partial + Regex == Partial
                        String concat = ((Regex.PartialRegex) lRegex).getPartialValue() + "e";
                        resultQual = new GroundQual<Regex>(new Regex.PartialRegex(concat));
                    }

                    if (resultQual != null) {
                        QualParams<Regex> clone = result.getQualifier().clone();
                        clone.setPrimary(resultQual);
                        result = SetQualifierVisitor.apply(result, clone);
                    }
                }
                return result;
            }

        };
    }

    private boolean isPolyPlusRegex(PolyQual<Regex> possiblePoly, Regex other) {
        return possiblePoly instanceof QualVar
                && ((QualVar) possiblePoly).getName().equals(SimpleQualifierParameterAnnotationConverter.POLY_NAME)
                && other.isRegexVal();
    }

    /**
     * Returns the number of groups in the given regex String.
     */
    public static int getGroupCount(
            /*@org.checkerframework.checker.experimental.regex_qual_poly.qual.Regex*/ String regex) {

        return Pattern.compile(regex).matcher("").groupCount();
    }

    /** This method is a copy of RegexUtil.isValidRegex.
     * We cannot directly use RegexUtil, because it uses type annotations
     * which cannot be used in IDEs (yet).
     */
    /*@SuppressWarnings("purity")*/ // the checker cannot prove that the method is pure, but it is
    /*@org.checkerframework.dataflow.qual.Pure*/
    private static boolean isRegex(String s) {
        try {
            Pattern.compile(s);
        } catch (PatternSyntaxException e) {
            return false;
        }
        return true;
    }

    @Override
    public QualAnalysis<QualParams<Regex>> createFlowAnalysis(List<Pair<VariableElement, QualValue<QualParams<Regex>>>> fieldValues) {
        return new QualAnalysis<QualParams<Regex>>(this.getContext()) {
            @Override
            public QualTransfer<QualParams<Regex>> createTransferFunction() {
                return new RegexQualifiedTransfer(this);
            }
        };
    }

    @Override
    protected Wildcard<Regex> combineForSubstitution(Wildcard<Regex> a, Wildcard<Regex> b) {
        return a.combineWith(b, lubOp, lubOp);
    }

    @Override
    protected PolyQual<Regex> combineForSubstitution(PolyQual<Regex> a, PolyQual<Regex> b) {
        return a.combineWith(b, lubOp);
    }
}