package com.jetbrains.python.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.jetbrains.python.PyElementTypes;
import com.jetbrains.python.PythonDialectsTokenSetProvider;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.stubs.PyParameterListStub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class PyParameterListImpl extends PyBaseElementImpl<PyParameterListStub> implements PyParameterList {
  public PyParameterListImpl(ASTNode astNode) {
    super(astNode);
  }

  public PyParameterListImpl(final PyParameterListStub stub) {
    this(stub, PyElementTypes.PARAMETER_LIST);
  }

  public PyParameterListImpl(final PyParameterListStub stub, IStubElementType nodeType) {
    super(stub, nodeType);
  }

  @Override
  protected void acceptPyVisitor(PyElementVisitor pyVisitor) {
    pyVisitor.visitPyParameterList(this);
  }

  public PyParameter[] getParameters() {
    return getStubOrPsiChildren(PythonDialectsTokenSetProvider.INSTANCE.getParameterTokens(), new PyParameter[0]);
  }

  public void addParameter(final PyNamedParameter param) {
    PsiElement paren = getLastChild();
    if (paren != null && ")".equals(paren.getText())) {
      ASTNode beforeWhat = paren.getNode(); // the closing paren will be this
      PyParameter[] params = getParameters();
      PyUtil.addListNode(this, param, beforeWhat, true, params.length == 0);
    }
  }

  public boolean hasPositionalContainer() {
    for (PyParameter parameter: getParameters()) {
      if (parameter instanceof PyNamedParameter && ((PyNamedParameter) parameter).isPositionalContainer()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasKeywordContainer() {
    for (PyParameter parameter: getParameters()) {
      if (parameter instanceof PyNamedParameter && ((PyNamedParameter) parameter).isKeywordContainer()) {
        return true;
      }
    }
    return false;
  }

  public boolean isCompatibleTo(@NotNull PyParameterList other) {
    PyParameter[] params = getParameters();
    final PyParameter[] otherParams = other.getParameters();
    if (hasPositionalContainer() || hasKeywordContainer()) {
      return true;
    }
    final PyFunction otherFunction = other.getContainingFunction();
    final boolean otherHasArgs = other.hasPositionalContainer();
    final boolean otherHasKwargs = other.hasKeywordContainer();
    if (otherHasArgs || otherHasKwargs) {
      int specialParamsCount = 0;
      if (otherHasArgs) {
        specialParamsCount++;
      }
      if (otherHasKwargs) {
        specialParamsCount++;
      }
      if (otherFunction != null && otherFunction.asMethod() != null) {
        specialParamsCount++;
      }
      return otherParams.length == specialParamsCount;
    }
    return params.length == otherParams.length;
  }

  @Override
  @Nullable
  public PyNamedParameter findParameterByName(@NotNull final String name) {
    final Ref<PyNamedParameter> result = new Ref<PyNamedParameter>();
    ParamHelper.walkDownParamArray(getParameters(), new ParamHelper.ParamVisitor() {
      @Override
      public void visitNamedParameter(PyNamedParameter param, boolean first, boolean last) {
        if (name.equals(param.getName())) {
          result.set(param);
        }
      }
    });
    return result.get();
  }

  public String getPresentableText(final boolean includeDefaultValue) {
    final StringBuilder target = new StringBuilder();
    final String COMMA = ", ";
    target.append("(");
    ParamHelper.walkDownParamArray(
      getParameters(),
      new ParamHelper.ParamWalker() {
        public void enterTupleParameter(PyTupleParameter param, boolean first, boolean last) {
          target.append("(");
        }

        public void leaveTupleParameter(PyTupleParameter param, boolean first, boolean last) {
          target.append(")");
          if (!last) target.append(COMMA);
        }

        public void visitNamedParameter(PyNamedParameter param, boolean first, boolean last) {
          target.append(param.getRepr(includeDefaultValue));
          if (!last) target.append(COMMA);
        }

        public void visitSingleStarParameter(PySingleStarParameter param, boolean first, boolean last) {
          target.append('*');
          if (!last) target.append(COMMA);
        }
      }
    );
    target.append(")");
    return target.toString();
  }

  @Nullable
  @Override
  public PyFunction getContainingFunction() {
    final PsiElement parent = getStubOrPsiParent();
    return parent instanceof PyFunction ? (PyFunction) parent : null;
  }
}
